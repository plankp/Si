/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import static com.ymcmp.si.lang.LiteralUtils.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ymcmp.midform.tac.value.*;
import com.ymcmp.midform.tac.type.*;

import com.ymcmp.si.lang.grammar.SiBaseVisitor;
import com.ymcmp.si.lang.grammar.SiLexer;
import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.*;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

public final class TypeChecker extends SiBaseVisitor<Object> {

    private static final class TypeCheckPendingFunction {

        public final String namespace;
        public final List<Binding.Parameter> params;
        public final Type output;
        public final SiParser.ExprContext body;
        public final Map<String, Type> subst;

        public TypeCheckPendingFunction(String namespace, List<Binding.Parameter> params, Type output, SiParser.ExprContext body) {
            this(namespace, params, output, body, null);
        }

        public TypeCheckPendingFunction(String namespace, List<Binding.Parameter> params, Type output, SiParser.ExprContext body, Map<String, Type> subst) {
            this.namespace = namespace;
            this.params = params;
            this.output = output;
            this.body = body;
            this.subst = subst == null ? Collections.emptyMap() : subst;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.namespace, this.params, this.output, this.body);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TypeCheckPendingFunction) {
                final TypeCheckPendingFunction pf = (TypeCheckPendingFunction) obj;
                return this.namespace.equals(pf.namespace)
                    && this.params.equals(pf.params)
                    && this.output.equals(pf.output)
                    && this.body.equals(pf.body);
            }
            return false;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();

            if (this.params.isEmpty()) {
                sb.append("()");
            } else {
                sb.append(this.params);
                sb.setCharAt(0, '(');
                sb.setCharAt(sb.length() - 1, ')');
            }
            sb.append(this.output).append(' ').append(this.body.getText());

            return sb.toString();
        }
    }

    private static final class ParsedFunction {

        public final String namespace;
        public final String name;
        public final List<Binding.Parameter> params;
        public final Type output;
        public final SiParser.ExprContext body;
        public final boolean exported;

        public ParsedFunction(String namespace, String name, List<Binding.Parameter> params, Type output, SiParser.ExprContext body, boolean exported) {
            this.namespace = namespace;
            this.name = name;
            this.params = params;
            this.output = output;
            this.body = body;
            this.exported = exported;
        }
    }

    private final Map<Path, SiParser.FileContext> importMap = new LinkedHashMap<>();

    private final Scope<String, TypeBank<Type, Boolean>> definedTypes = new Scope<>();
    private final Map<String, TypeBank<FunctionType, ParsedFunction>> definedFunctions = new LinkedHashMap<>();

    private final LinkedList<TypeCheckPendingFunction> pendingQueue = new LinkedList<>();

    private final Scope<String, Binding> locals = new Scope<>();

    private String namespacePrefix;
    private Path currentFile;
    private boolean isExported;

    public TypeChecker() {
        this.reset();
    }

    public void reset() {
        this.importMap.clear();

        this.definedTypes.clear();
        this.definedFunctions.clear();

        this.pendingQueue.clear();

        this.locals.clear();

        this.namespacePrefix = "";
        this.currentFile = null;
        this.isExported = false;
    }

    public boolean loadSource(final String raw) {
        return this.loadSource(Paths.get(raw));
    }

    public boolean loadSource(final Path rawPath) {
        final Path path = rawPath.normalize().toAbsolutePath();
        if (this.importMap.containsKey(path)) {
            return false;
        }

        final Path saved = this.currentFile;

        try {
            final SiLexer lexer = new SiLexer(CharStreams.fromPath(path));
            final CommonTokenStream tokens = new CommonTokenStream(lexer);
            final SiParser parser = new SiParser(tokens);

            final SiParser.FileContext ctx = parser.file();

            this.importMap.put(path, ctx);
            this.currentFile = path;
            this.visitFile(ctx);

            this.importMap.remove(path);
            this.importMap.put(path, ctx);
            return true;
        } catch (IOException ex) {
            throw new IllegalImportException("Cannot import " + rawPath, ex);
        } finally {
            this.currentFile = saved;
        }
    }

    public void processLoadedModules() {
        final LinkedList<Pair<String, SiParser.TopLevelDeclContext>> queue = new LinkedList<>();
        for (final SiParser.FileContext ctx : this.importMap.values()) {
            this.processModule(queue, ctx);
        }

        // sort the queued declarations by their process priority
        queue.sort((a, b) -> Integer.compare(getProcessPriority(a.b), getProcessPriority(b.b)));

        // setup global variables
        this.definedTypes.enter();

        // then process the declarations
        Pair<String, SiParser.TopLevelDeclContext> item;
        while ((item = queue.pollFirst()) != null) {
            this.namespacePrefix = item.a;
            this.visitTopLevelDecl(item.b);
        }

        final HashSet<TypeCheckPendingFunction> processed = new HashSet<>();
        TypeCheckPendingFunction ifunc;
        while ((ifunc = this.pendingQueue.pollFirst()) != null) {
            if (processed.contains(ifunc)) {
                // already processed this one, no need to perform validation again
                continue;
            }

            processed.add(ifunc);
            this.processFunction(ifunc);
        }

        processed.clear();
    }

    private void processModule(List<Pair<String, SiParser.TopLevelDeclContext>> queue, SiParser.FileContext ctx) {
        // create the namespace
        this.namespacePrefix = "";
        if (ctx.ns != null) this.visitNamespaceDecl(ctx.ns);

        // then queue the declaration
        for (final SiParser.TopLevelDeclContext decl : ctx.decls) {
            queue.add(new Pair<>(this.namespacePrefix, decl));
        }
    }

    private static int getProcessPriority(SiParser.TopLevelDeclContext ctx) {
        // modules can contain three types of declarations:
        // - (local) types:    these have to be processed first
        // - native functions: these go after
        // - local functions:  these go last

        final ParseTree tree = getDeclarationContext(ctx);
        if (tree instanceof SiParser.DeclTypeContext)       return 0;
        if (tree instanceof SiParser.DeclNativeFuncContext) return 1;
        if (tree instanceof SiParser.DeclFuncContext)       return 2;

        // why not? (just in case)
        return Integer.MAX_VALUE;
    }

    private static ParseTree getDeclarationContext(SiParser.TopLevelDeclContext ctx) {
        return ctx.getChild(ctx.getChildCount() - 2);
    }

    @Override
    public String visitNamespacePath(SiParser.NamespacePathContext ctx) {
        final String prefix = (ctx.root == null ? this.namespacePrefix : "") + '\\';
        return ctx.parts.stream().map(Token::getText).collect(Collectors.joining("\\", prefix, ""));
    }

    @Override
    public Object visitNamespaceDecl(SiParser.NamespaceDeclContext ctx) {
        this.namespacePrefix = this.visitNamespacePath(ctx.ns);
        return null;
    }

    @Override
    public Object visitImportDecl(SiParser.ImportDeclContext ctx) {
        final String filePath = convertStringLiteral(ctx.path.getText());

        // Want to resolve it against the directory of the current file
        // that's why use resolveSibling instead of resolve
        this.loadSource(this.currentFile.resolveSibling(filePath));
        return null;
    }

    @Override
    public Object visitFile(SiParser.FileContext ctx) {
        // Only process the imports. For actual processing of declarations,
        // see methods processLoadedModules and processModule.
        for (final SiParser.ImportDeclContext importDecls : ctx.imports) {
            this.visitImportDecl(importDecls);
        }
        return null;
    }

    @Override
    public Object visitTopLevelDecl(SiParser.TopLevelDeclContext ctx) {
        this.isExported = ctx.vis != null;
        return this.visit(getDeclarationContext(ctx));
    }

    @Override
    public Object visitDeclTypeAlias(SiParser.DeclTypeAliasContext ctx) {
        final String name = this.namespacePrefix + '\\' + ctx.name.getText();
        final TypeBank<Type, Boolean> bank = definedTypes.getCurrentOrInit(name, TypeBank::new);

        final Type aliased;
        if (ctx.generic != null) {
            this.definedTypes.enter();
            final List<FreeType> boundary = this.visitDeclGeneric(ctx.generic);
            final ParametricType<Type> pt = new ParametricType<>(this.visitCoreTypes(ctx.type), boundary);
            aliased = pt;
            bank.addParametricType(pt, this.isExported);
            this.definedTypes.exit();
        } else {
            final Type t = this.visitCoreTypes(ctx.type);
            aliased = t;
            bank.setSimpleType(t, this.isExported);
        }

        System.out.println("scope  =   " + this.namespacePrefix);
        System.out.println("alias  :   " + ctx.name.getText());
        System.out.println(" type  :   " + aliased);
        return null;
    }

    @Override
    public FreeType visitParamFreeType(SiParser.ParamFreeTypeContext ctx) {
        return new FreeType(ctx.name.getText());
    }

    @Override
    public FreeType visitParamEquivType(SiParser.ParamEquivTypeContext ctx) {
        return new FreeType(ctx.name.getText(), (Type) this.visit(ctx.bound));
    }

    @Override
    public List<FreeType> visitDeclGeneric(SiParser.DeclGenericContext ctx) {
        final ArrayList<FreeType> list = new ArrayList<>(ctx.args.size());
        for (final SiParser.GenericParamContext t : ctx.args) {
            final FreeType bound = (FreeType) this.visit(t);
            list.add(bound);

            // need to add it to the current type scope
            final String name = bound.getName();
            final TypeBank<Type, Boolean> bank = this.definedTypes.getCurrentOrInit(name, TypeBank::new);

            try {
                bank.setSimpleType(bound.expandBound(), true);
            } catch (DuplicateDefinitionException ex) {
                throw new DuplicateDefinitionException("Duplicate type parameter: " + name, ex);
            }
        }

        return list;
    }

    @Override
    public List<Type> visitTypeParams(SiParser.TypeParamsContext ctx) {
        final ArrayList<Type> list = new ArrayList<>(ctx.types.size());
        for (final SiParser.BaseLevelContext t : ctx.types) {
            list.add((Type) this.visit(t));
        }

        return list;
    }

    @Override
    public Type visitCoreNomialType(SiParser.CoreNomialTypeContext ctx) {
        switch (ctx.getText()) {
            case "int":     return IntegerType.INT32;
            case "double":  return ImmDouble.TYPE;
            case "bool":    return ImmBoolean.TYPE;
            case "byte":    return IntegerType.INT8;
            case "char":    return ImmCharacter.TYPE;
            case "string":  return ImmString.TYPE;
            default:        throw new UnboundDefinitionException("Unknown primitive type: " + ctx.getText());
        }
    }

    @Override
    public Type visitUserDefType(SiParser.UserDefTypeContext ctx) {
        final String rawName = ctx.base.getText();
        String selectedName = null;
        TypeBank<Type, Boolean> bank = null;

        if (!rawName.contains("\\")) {
            // It might be a type parameter (from generic types)
            selectedName = rawName;
            bank = this.definedTypes.get(rawName);
        }

        if (bank == null) {
            // It might be a file-level type
            selectedName = this.visitNamespacePath(ctx.base);
            bank = this.definedTypes.get(selectedName);
        }

        if (bank == null) {
            throw new UnboundDefinitionException("Attempt to use undefined type: " + rawName);
        }

        // This has to be a simple type (based on grammar)
        if (!bank.hasSimpleType()) {
            if (bank.hasParametricType()) {
                throw new TypeMismatchException("Missing type paramters for type: " + selectedName);
            }
            throw new UnboundDefinitionException("Unbound definition for type: " + selectedName);
        }

        if (!bank.getSimpleMapping()) {
            // If hidden, check if namespace allows us to do so
            if (!isAccessible(selectedName, this.namespacePrefix)) {
                throw new UnboundDefinitionException("Using hidden type: " + selectedName + " from: " + this.namespacePrefix);
            }
        }
        return bank.getSimpleType();
    }

    @Override
    public Type visitParametrizeGeneric(SiParser.ParametrizeGenericContext ctx) {
        final String name = this.visitNamespacePath(ctx.base);
        final TypeBank<Type, Boolean> bank = this.definedTypes.get(name);
        if (bank == null) {
            throw new UnboundDefinitionException("Attempt to use undefined type: " + name);
        }

        final List<Type> args = this.visitTypeParams(ctx.args);
        try {
            final ParametricType<Type> pt = bank.selectParametrization(args);
            if (!bank.getParametricMapping(pt)) {
                // If hidden, check if namespace allows us to do so
                if (!isAccessible(name, this.namespacePrefix)) {
                    throw new UnboundDefinitionException("Using hidden type: " + name + " from: " + this.namespacePrefix);
                }
            }
            return pt.parametrize(args);
        } catch (TypeMismatchException ex) {
            throw new TypeMismatchException("Cannot parametrize type: " + name, ex);
        }
    }

    @Override
    public Type visitTypeParenthesis(SiParser.TypeParenthesisContext ctx) {
        final Type input = ctx.e == null ? UnitType.INSTANCE : this.visitCoreTypes(ctx.e);
        if (ctx.out == null) {
            // the parenthesis was just to group types
            return input;
        }
        return new FunctionType(input, (Type) this.visit(ctx.out));
    }

    @Override
    public Type visitTupleLevel(SiParser.TupleLevelContext ctx) {
        final ArrayList<Type> list = new ArrayList<>(ctx.t.size());
        for (final SiParser.BaseLevelContext t : ctx.t) {
            list.add((Type) this.visit(t));
        }

        if (list.size() == 1) {
            return list.get(0);
        }
        return new TupleType(list);
    }

    @Override
    public Type visitExtensionLevel(SiParser.ExtensionLevelContext ctx) {
        final ArrayList<Type> list = new ArrayList<>(ctx.t.size());
        for (final SiParser.TupleLevelContext t : ctx.t) {
            list.add(this.visitTupleLevel(t));
        }

        if (list.size() != 1) {
            throw new UnsupportedOperationException("Extension type is not supported (yet)");
        }
        return list.get(0);
    }

    @Override
    public Type visitCoreTypes(SiParser.CoreTypesContext ctx) {
        final ArrayList<Type> list = new ArrayList<>(ctx.t.size());
        for (final SiParser.ExtensionLevelContext t : ctx.t) {
            list.add(this.visitExtensionLevel(t));
        }

        if (list.size() != 1) {
            throw new UnsupportedOperationException("Variant type is not supported (yet)");
        }
        return list.get(0);
    }

    @Override
    public Object visitDeclNativeFunc(SiParser.DeclNativeFuncContext ctx) {
        final String external = ctx.nat.getText();
        final String name = this.namespacePrefix + '\\' + ctx.name.getText();

        this.locals.enter();

        final LinkedList<Binding.Parameter> params = new LinkedList<>();
        final FunctionType funcType = this.generateFunctionType(params, ctx.params, ctx.out);

        this.locals.exit();

        // native functions are always simple (non-parametric)
        this.definedFunctions.computeIfAbsent(name, k -> new TypeBank<>())
                .setSimpleType(funcType, new ParsedFunction(
                        this.namespacePrefix,
                        ctx.name.getText(),
                        params,
                        funcType.getOutput(),
                        null,
                        this.isExported));

        System.out.println("scope  =   " + this.namespacePrefix);
        System.out.println("native :   " + ctx.name.getText());
        System.out.println("  type :   " + funcType);
        return null;
    }

    private FunctionType generateFunctionType(List<Binding.Parameter> outlist, SiParser.FuncParamsContext input, SiParser.CoreTypesContext output) {
        final List<Binding.Parameter> params = this.visitFuncParams(input);
        final Type out = output == null ? new InferredType() : this.visitCoreTypes(output);

        final Type in;
        switch (params.size()) {
            case 0:
                in = UnitType.INSTANCE;
                break;
            case 1:
                in = params.get(0).getType();
                break;
            default:
                in = new TupleType(params.stream().map(Binding::getType).collect(Collectors.toList()));
                break;
        }

        if (outlist != null) {
            outlist.addAll(params);
        }

        return new FunctionType(in, out);
    }

    @Override
    public Binding.Parameter visitFuncParam(SiParser.FuncParamContext ctx) {
        final String name = ctx.name.getText();
        final Type type = (Type) this.visit(ctx.type);

        return this.declareFuncParam(name, type);
    }

    @Override
    public List<Binding.Parameter> visitFuncParams(SiParser.FuncParamsContext ctx) {
        final ArrayList<Binding.Parameter> list = new ArrayList<>(ctx.in.size());
        for (final SiParser.FuncParamContext param : ctx.in) {
            list.add(this.visitFuncParam(param));
        }

        return list;
    }

    @Override
    public Object visitDeclFunc(SiParser.DeclFuncContext ctx) {
        final String name = ctx.name.getText();
        final String fullName = this.namespacePrefix + '\\' + name;
        final TypeBank<FunctionType, ParsedFunction> bank = this.definedFunctions.computeIfAbsent(fullName, k -> new TypeBank<>());

        final LinkedList<Binding.Parameter> params = new LinkedList<>();
        this.locals.enter();

        final Type aliased;
        if (ctx.generic != null) {
            this.definedTypes.enter();
            final List<FreeType> boundary = this.visitDeclGeneric(ctx.generic);
            final FunctionType type = this.generateFunctionType(params, ctx.params, ctx.out);
            final ParametricType<FunctionType> pt = new ParametricType<>(type, boundary);
            aliased = pt;
            bank.addParametricType(pt, new ParsedFunction(this.namespacePrefix, name, params, type.getOutput(), ctx.e, this.isExported));
            this.definedTypes.exit();
        } else {
            final FunctionType type = this.generateFunctionType(params, ctx.params, ctx.out);
            aliased = type;
            bank.setSimpleType(type, new ParsedFunction(this.namespacePrefix, name, params, type.getOutput(), ctx.e, this.isExported));

            this.pendingQueue.addLast(new TypeCheckPendingFunction(this.namespacePrefix, params, type.getOutput(), ctx.e));
        }

        this.locals.exit();

        System.out.println("scope  =   " + this.namespacePrefix);
        System.out.println(" func  :   " + name);
        System.out.println(" type  :   " + aliased);
        return null;
    }

    private Binding.Parameter declareFuncParam(String name, Type type) {
        // Only search in the current scope!
        final Binding prev = this.locals.getCurrent(name);
        if (prev != null) {
            throw new DuplicateDefinitionException("Duplicate local binding: " + name + " as: " + prev.type);
        }

        // need to add scope depth to make sure the internal name
        // does not collide with the ones in the outer scope
        final Binding.Parameter binding = new Binding.Parameter(name, this.locals.getDepth(), type);
        locals.put(name, binding);
        return binding;
    }

    private Binding declareLocalVariable(String name, Type type, boolean immutable) {
        // Only search in the current scope!
        final Binding prev = this.locals.getCurrent(name);
        if (prev != null) {
            throw new DuplicateDefinitionException("Duplicate local binding: " + name + " as: " + prev.type);
        }

        // need to add scope depth to make sure the internal name
        // does not collide with the ones in the outer scope
        final int depth = this.locals.getDepth();
        final Binding binding = immutable ? new Binding.Immutable(name, depth, type) : new Binding.Mutable(name, depth, type);
        locals.put(name, binding);
        return binding;
    }

    private void processFunction(TypeCheckPendingFunction func) {
        this.locals.enter();

        // setup namespace
        this.namespacePrefix = func.namespace;

        // setup type parameters (if necessary)
        if (!func.subst.isEmpty()) {
            this.definedTypes.enter();
            for (final Map.Entry<String, Type> entry : func.subst.entrySet()) {
                this.definedTypes.put(entry.getKey(), TypeBank.withSimpleType(entry.getValue(), true));
            }
        }

        // setup parameters
        for (final Binding.Parameter param : func.params) {
            this.locals.put(param.name, param);
        }

        // analyze body and return type
        try {
            final Type analyzed = (Type) this.visit(func.body);
            if (!Types.assignableFrom(func.output, analyzed)) {
                throw new TypeMismatchException("Expected output convertible to: " + func.output + " but got: " + analyzed);
            }
        } catch (RuntimeException ex) {
            throw new CompileTimeException("Invalid function body: " + func, ex);
        }

        System.out.println("Processing " + func);
        System.out.println("Namespace: " + this.namespacePrefix);
        System.out.println("Args:      " + func.params);
        System.out.println("Output:    " + func.output.expandBound());

        // cleanup (if necessary)
        if (!func.subst.isEmpty()) {
            this.definedTypes.exit();
        }

        this.locals.exit();
    }

    @Override
    public Binding visitDeclVar(SiParser.DeclVarContext ctx) {
        final String name = ctx.name.getText();
        final Type type = ctx.type == null ? new InferredType() : (Type) this.visit(ctx.type);

        return this.declareLocalVariable(name, type, ctx.mut == null);
    }

    @Override
    public Type visitExprBinding(SiParser.ExprBindingContext ctx) {
        final String rawName = ctx.base.getText();

        if (!rawName.contains("\\")) {
            // it might be a local binding
            final Binding lvar = this.locals.get(rawName);
            if (lvar != null) {
                return lvar.type;
            }
        }

        // it might be a non-generic function
        final String name = this.visitNamespacePath(ctx.base);
        final TypeBank<FunctionType, ParsedFunction> bank = this.definedFunctions.get(name);

        if (bank != null) {
            if (!bank.hasSimpleType()) {
                // this means the binding exist, but as parametric type. report it!
                throw new TypeMismatchException("Missing type paramters for function: " + name);
            }

            if (!bank.getSimpleMapping().exported) {
                if (!isAccessible(name, this.namespacePrefix)) {
                    throw new UnboundDefinitionException("Accessing unexported function: " + name + " from: " + this.namespacePrefix);
                }
            }

            // we assume the function has a valid type
            return bank.getSimpleType();
        }

        throw new UnboundDefinitionException("Unbound definition for binding: " + rawName);
    }

    @Override
    public Type visitExprParametrize(SiParser.ExprParametrizeContext ctx) {
        // it has to be a generic function (otherwise referring an undefined name)
        final String rawName = ctx.base.getText();
        final String name = this.visitNamespacePath(ctx.base);
        final TypeBank<FunctionType, ParsedFunction> bank = this.definedFunctions.get(name);

        if (bank != null) {
            final List<Type> args = this.visitTypeParams(ctx.args);
            final ParametricType<FunctionType> pt = bank.selectParametrization(args);
            final ParsedFunction func = bank.getParametricMapping(pt);

            if (!func.exported) {
                if (!isAccessible(name, this.namespacePrefix)) {
                    throw new UnboundDefinitionException("Accessing unexported function: " + name + " from: " + this.namespacePrefix);
                }
            }

            // need to instantiate the function and then queue it
            final FunctionType instantiated = pt.parametrize(args);
            final ArrayList<Binding.Parameter> params = new ArrayList<>(func.params);

            // substitute the parameter list (in case parametrization was on function input)
            for (int i = 0; i < params.size(); ++i) {
                final Binding.Parameter param = params.get(i);
                params.set(i, param.changeType(instantiated.getSplattedInput(i)));
            }

            // need to supply the type substitution mapping as well
            final Map<String, Type> subst = new HashMap<>();
            for (int i = 0; i < args.size(); ++i) {
                subst.put(pt.getTypeRestrictionAt(i).name, args.get(i));
            }

            // then queue it for processing
            this.pendingQueue.addLast(new TypeCheckPendingFunction(
                    func.namespace,
                    params,
                    instantiated.getOutput(),
                    func.body,
                    subst));

            // we assume the instantiated function has a valid type
            return instantiated;
        }

        throw new UnboundDefinitionException("Unbound definition for binding: " + rawName);
    }

    @Override
    public Type visitExprImmInt(SiParser.ExprImmIntContext ctx) {
        return IntegerType.INT32;
    }

    @Override
    public Type visitExprImmDouble(SiParser.ExprImmDoubleContext ctx) {
        return ImmDouble.TYPE;
    }

    @Override
    public Type visitExprImmBool(SiParser.ExprImmBoolContext ctx) {
        return ImmBoolean.TYPE;
    }

    @Override
    public Type visitExprImmChr(SiParser.ExprImmChrContext ctx) {
        return ImmCharacter.TYPE;
    }

    @Override
    public Type visitExprImmStr(SiParser.ExprImmStrContext ctx) {
        return ImmString.TYPE;
    }

    @Override
    public Type visitExprParenthesis(SiParser.ExprParenthesisContext ctx) {
        final ArrayList<Type> elements = new ArrayList<>(ctx.e.size());
        for (final SiParser.ExprContext e : ctx.e) {
            elements.add((Type) this.visit(e));
        }

        switch (elements.size()) {
            case 0:
                return UnitType.INSTANCE;
            case 1:
                return elements.get(0);
            default:
                return new TupleType(elements);
        }
    }

    @Override
    public Type visitExprFuncCall(SiParser.ExprFuncCallContext ctx) {
        final FunctionType f = (FunctionType) this.visit(ctx.base);
        final Type arg = (Type) this.visit(ctx.arg);

        if (!f.canApply(arg)) {
            throw new TypeMismatchException(
                    "Function input expected: " + f.getInput() + " but got incompatible: " + arg);
        }

        return f.getOutput();
    }

    @Override
    public Type visitExprIfElse(SiParser.ExprIfElseContext ctx) {
        final Type test = (Type) this.visit(ctx.test);
        if (!Types.assignableFrom(ImmBoolean.TYPE, test)) {
            throw new TypeMismatchException("If condition expected: " + ImmBoolean.TYPE + " but got: " + test);
        }

        final Type retTrue = (Type) this.visit(ctx.ifTrue);
        final Type retFalse = (Type) this.visit(ctx.ifFalse);
        return Types.unify(retTrue, retFalse).orElseThrow(() -> new TypeMismatchException(
                "Cannot unify unrelated: " + retTrue + " and: " + retFalse));
    }

    @Override
    public Type visitExprDoEnd(SiParser.ExprDoEndContext ctx) {
        Type acc = null;
        for (final SiParser.ExprContext e : ctx.e) {
            acc = (Type) this.visit(e);
        }
        return acc;
    }

    @Override
    public Type visitExprVarDecl(SiParser.ExprVarDeclContext ctx) {
        // evaluate the value in the outer scope
        final Type analyzedType = (Type) this.visit(ctx.v);

        // create a new scope
        this.locals.enter();

        // declare binding in new scope
        final Binding decl = this.visitDeclVar(ctx.binding);
        final Type declType = decl.type;

        if (!Types.assignableFrom(declType, analyzedType)) {
            throw new TypeMismatchException("Binding: " + decl.name
                    + " expected value convertible to: " + declType + " but got: " + analyzedType);
        }

        // type-check the expression in the new scope
        final Type ret = (Type) this.visit(ctx.e);

        // leave the new scope
        this.locals.exit();

        return ret;
    }

    private static boolean isAccessible(String id, String accScope) {
        // id = \com\ymcmp\si\lang\id
        // is accessible if accScope starts with \com\ymcmp\si\lang\
        return (accScope + '\\').startsWith(id.substring(0, id.lastIndexOf('\\') + 1));
    }
}
