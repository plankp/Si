/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Supplier;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.statement.*;
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

public class TypeChecker extends SiBaseVisitor<Object> {

    private static interface UnaryOpCodeGen {

        public void generate(Value a);
    }

    private static interface BinaryOpCodeGen {

        public void generate(Value a, Value b);
    }

    private static final Type TYPE_INT = ImmInteger.TYPE;
    private static final Type TYPE_DOUBLE = ImmDouble.TYPE;
    private static final Type TYPE_BOOL = ImmBoolean.TYPE;
    private static final Type TYPE_CHAR = ImmCharacter.TYPE;
    private static final Type TYPE_STRING = ImmString.TYPE;

    private final TypeBank<Type> OPERATOR_CAST = new TypeBank<>();

    private final TypeBank<Type> OPERATOR_NOT = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_POS = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_NEG = new TypeBank<>();

    private final TypeBank<Type> OPERATOR_ADD = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_SUB = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_MUL = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_DIV = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_LT = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_LE = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_GE = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_GT = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_EQV = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_NEQ = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_AND = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_XOR = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_OR = new TypeBank<>();
    private final TypeBank<Type> OPERATOR_THREE_WAY_COMP = new TypeBank<>();

    private final Map<Path, SiParser.FileContext> importMap = new LinkedHashMap<>();

    private final Scope<String, TypeBank<Type>> definedTypes = new Scope<>();
    private final Map<String, InstantiatedFunction> nonGenericFunctions = new LinkedHashMap<>();
    private final Map<String, List<ParametricFunction>> parametricFunctions = new LinkedHashMap<>();
    private final Map<String, InstantiatedFunction.Local> instantiatedGenericFunctions = new LinkedHashMap<>();

    private final LinkedList<InstantiatedFunction.Local> queuedInstantiatedFunctions = new LinkedList<>();

    private final Scope<String, Binding> locals = new Scope<>();

    private String namespacePrefix = "";

    private Path currentFile;

    private final CodeGenState cgenState = new CodeGenState();

    public TypeChecker() {
        this.reset();
        this.buildOperatorMap();
    }

    public Scope<String, TypeBank<Type>> getUserDefinedTypes() {
        return this.definedTypes;
    }

    public Map<String, TypeBank<FunctionType>> getUserDefinedFunctions() {
        final Map<String, TypeBank<FunctionType>> m = new LinkedHashMap<>();

        for (final Map.Entry<String, InstantiatedFunction> e : nonGenericFunctions.entrySet()) {
            final String key = e.getKey();
            TypeBank<FunctionType> bank = m.get(key);
            if (bank == null) {
                bank = new TypeBank<>();
                m.put(key, bank);
            }

            bank.setSimpleType(e.getValue().getType());
        }

        for (final Map.Entry<String, List<ParametricFunction>> e : parametricFunctions.entrySet()) {
            final String key = e.getKey();
            TypeBank<FunctionType> bank = m.get(key);
            if (bank == null) {
                bank = new TypeBank<>();
                m.put(key, bank);
            }

            for (final ParametricFunction p : e.getValue()) {
                bank.addParametricType(p.getType());
            }
        }
        return m;
    }

    public Map<String, List<FunctionType>> getInstantiatedGenericFunctions() {
        final Map<String, List<FunctionType>> m = new LinkedHashMap<>();
        for (final InstantiatedFunction f : this.instantiatedGenericFunctions.values()) {
            m.computeIfAbsent(f.getSimpleName(), k -> new LinkedList<>()).add(f.getType());
        }
        return m;
    }

    public Map<String, Subroutine> getAllInstantiatedFunctions() {
        final HashMap<String, Subroutine> m = new HashMap<>();
        this.nonGenericFunctions.forEach((k, v) -> m.put(k, v.getSubroutine()));
        this.instantiatedGenericFunctions.forEach((k, v) -> m.put(k, v.getSubroutine()));
        return m;
    }

    public final void reset() {
        this.importMap.clear();

        this.locals.clear();

        this.nonGenericFunctions.clear();
        this.parametricFunctions.clear();
        this.instantiatedGenericFunctions.clear();
        this.queuedInstantiatedFunctions.clear();

        this.namespacePrefix = "";

        this.definedTypes.clear();
        this.definedTypes.enter();
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
        for (final SiParser.ImportDeclContext importDecls : ctx.imports) {
            this.visitImportDecl(importDecls);
        }

        return null;
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
        for (final Map.Entry<Path, SiParser.FileContext> entry : this.importMap.entrySet()) {
            this.currentFile = entry.getKey();
            this.processModule(entry.getValue());
        }

        // Process the queued functions that are instantiated from generic functions
        InstantiatedFunction.Local ifunc;
        while ((ifunc = this.queuedInstantiatedFunctions.pollFirst()) != null) {
            // Type check it
            this.typeCheckInstantiatedFunction(ifunc);
        }

        for (final Subroutine sub : this.getAllInstantiatedFunctions().values()) {
            sub.validate();
            System.out.println("Pre-optimize:");
            System.out.println(sub);
            System.out.println();

            sub.optimize();
        }

        // TODO: Don't optimize unless requested to do so...
        for (final Subroutine sub : this.getAllInstantiatedFunctions().values()) {
            sub.optimize();
            System.out.println("Post-optimize:");
            System.out.println(sub);
            System.out.println();
        }
    }

    private void processModule(SiParser.FileContext ctx) {
        this.namespacePrefix = "";
        // Create the namespace
        if (ctx.ns != null) {
            this.visitNamespaceDecl(ctx.ns);
        }

        // Process the types first, queue everything else
        final LinkedList<ParseTree> queued = new LinkedList<>();
        for (SiParser.TopLevelDeclContext decl : ctx.decls) {
            final ParseTree tree = decl.getChild(decl.getChildCount() - 2);
            if (tree instanceof SiParser.DeclTypeContext) {
                this.visit(tree);
            } else {
                queued.addLast(tree);
            }
        }

        // Then process the queued stuff
        ParseTree tree;
        while ((tree = queued.pollFirst()) != null) {
            this.visit(tree);
        }
    }

    @Override
    public FreeType visitParamFreeType(SiParser.ParamFreeTypeContext ctx) {
        return new FreeType(ctx.name.getText());
    }

    @Override
    public FreeType visitParamEquivType(SiParser.ParamEquivTypeContext ctx) {
        return new FreeType(ctx.name.getText(), this.getTypeSignature(ctx.bound));
    }

    @Override
    public List<FreeType> visitDeclGeneric(SiParser.DeclGenericContext ctx) {
        return ctx.args.stream().map(this::getTypeRestriction).collect(Collectors.toList());
    }

    private Type typeDeclarationHelper(SiParser.DeclGenericContext generic, SiParser.CoreTypesContext rawType) {
        if (generic == null) {
            return this.getTypeSignature(rawType);
        }

        this.definedTypes.enter();
        final List<FreeType> bound = this.visitDeclGeneric(generic);
        for (final FreeType e : bound) {
            final String name = e.getName();
            final TypeBank<Type> bank = this.definedTypes.getCurrentOrInit(name, TypeBank::new);

            try {
                bank.setSimpleType(e.expandBound());
            } catch (DuplicateDefinitionException ex) {
                throw new DuplicateDefinitionException("Duplicate type parameter: " + name, ex);
            }
        }

        final Type ret = new ParametricType<>(this.getTypeSignature(rawType), bound);
        this.definedTypes.exit();
        return ret;
    }

    @Override
    public Object visitDeclTypeAlias(SiParser.DeclTypeAliasContext ctx) {
        final String name = this.namespacePrefix + '\\' + ctx.name.getText();

        final Type type = this.typeDeclarationHelper(ctx.generic, ctx.type);
        final TypeBank<Type> bank = this.getFromDefinedTypes(name);

        try {
            if (type instanceof ParametricType) {
                @SuppressWarnings("unchecked")
                final ParametricType<Type> pt = (ParametricType<Type>) type;
                bank.addParametricType(pt);
            } else {
                bank.setSimpleType(type);
            }
        } catch (DuplicateDefinitionException ex) {
            throw new DuplicateDefinitionException("Duplicate definition of type: " + name, ex);
        }
        return null;
    }

    @Override
    public List<Type> visitTypeParams(SiParser.TypeParamsContext ctx) {
        return ctx.types.stream().map(this::getTypeSignature).collect(Collectors.toList());
    }

    @Override
    public Type visitCoreNomialType(SiParser.CoreNomialTypeContext ctx) {
        final String type = ctx.getText();
        switch (type) {
        case "int":
            return TYPE_INT;
        case "double":
            return TYPE_DOUBLE;
        case "bool":
            return TYPE_BOOL;
        case "char":
            return TYPE_CHAR;
        case "string":
            return TYPE_STRING;
        default:
            throw new AssertionError("Unhandled primitive type: " + type);
        }
    }

    @Override
    public Type visitUserDefType(SiParser.UserDefTypeContext ctx) {
        final String rawName = ctx.base.getText();
        String selectedName = null;
        TypeBank<Type> bank = null;

        if (!rawName.contains("\\")) {
            // It might be a generic type
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
        return bank.getSimpleType();
    }

    @Override
    public Type visitTypeParenthesis(SiParser.TypeParenthesisContext ctx) {
        final Type in = ctx.e == null ? UnitType.INSTANCE : this.getTypeSignature(ctx.e);
        return ctx.out == null ? in : new FunctionType(in, this.getTypeSignature(ctx.out));
    }

    @Override
    public Type visitTupleLevel(SiParser.TupleLevelContext ctx) {
        final List<Type> seq = ctx.t.stream().map(this::getTypeSignature).collect(Collectors.toList());
        if (seq.size() == 1) {
            return seq.get(0);
        }
        return new TupleType(seq);
    }

    @Override
    public Type visitExtensionLevel(SiParser.ExtensionLevelContext ctx) {
        final List<Type> seq = ctx.t.stream().map(this::visitTupleLevel).collect(Collectors.toList());
        if (seq.size() == 1) {
            return seq.get(0);
        }
        throw new UnsupportedOperationException("Type extensions are not supported yet: " + seq);
    }

    @Override
    public Type visitCoreTypes(SiParser.CoreTypesContext ctx) {
        final List<Type> seq = ctx.t.stream().map(this::visitExtensionLevel).collect(Collectors.toList());
        if (seq.size() == 1) {
            return seq.get(0);
        }
        return new VariantType(seq);
    }

    @Override
    public Type visitParametrizeGeneric(SiParser.ParametrizeGenericContext ctx) {
        final String name = this.visitNamespacePath(ctx.base);
        final TypeBank<Type> bank = this.definedTypes.get(name);
        if (bank == null) {
            throw new UnboundDefinitionException("Attempt to use undefined type: " + name);
        }

        final List<Type> args = this.visitTypeParams(ctx.args);
        try {
            final ParametricType<Type> pt = bank.selectParametrization(args);
            return pt.parametrize(args);
        } catch (TypeMismatchException ex) {
            throw new TypeMismatchException("Cannot parametrize type: " + name, ex);
        }
    }

    @Override
    public Type visitFuncSig(SiParser.FuncSigContext ctx) {
        final List<FreeType> bound;
        if (ctx.generic == null) {
            bound = null;
        } else {
            this.definedTypes.enter();
            bound = this.visitDeclGeneric(ctx.generic);
            for (final FreeType e : bound) {
                final TypeBank<Type> bank = this.definedTypes.getCurrentOrInit(e.getName(), TypeBank::new);
                bank.setSimpleType(e.expandBound());
            }
        }

        final List<Type> rawIn = new LinkedList<>();

        // Enter scope for parameters
        this.locals.enter();
        for (final SiParser.FuncParamContext arg : ctx.in) {
            rawIn.add(this.visitFuncParam(arg).type);
        }

        final Type out = ctx.out == null ? new InferredType() : this.getTypeSignature(ctx.out);

        final Type in;
        switch (rawIn.size()) {
        case 0: // unit type
            in = UnitType.INSTANCE;
            break;
        case 1: // singleton type
            in = rawIn.get(0);
            break;
        default: // tuple type
            in = new TupleType(rawIn);
            break;
        }

        // XXX: WE INTENTIONALLY SKIP EXITING locals

        final Type synthesized = new FunctionType(in, out);
        if (ctx.generic != null) {
            // XXX: WE INTENTIONALLY SKIP EXITING definedTypes
            return new ParametricType<>(synthesized, bound);
        }
        return synthesized;
    }

    @Override
    public Object visitDeclFunc(SiParser.DeclFuncContext ctx) {
        final String name = this.namespacePrefix + '\\' + ctx.name.getText();

        final Type funcSig = this.visitFuncSig(ctx.sig);
        this.locals.exit();

        if (funcSig instanceof FunctionType) {
            final InstantiatedFunction prev = this.nonGenericFunctions.get(name);
            if (prev != null) {
                throw new DuplicateDefinitionException(
                        "Duplicate function name: " + name + " previously defined as: " + prev);
            }

            final InstantiatedFunction.Local ifunc = new InstantiatedFunction.Local(ctx, (FunctionType) funcSig, this.namespacePrefix);
            this.nonGenericFunctions.put(name, ifunc);
            this.queuedInstantiatedFunctions.add(ifunc);
        } else {
            @SuppressWarnings("unchecked")
            final ParametricType<FunctionType> pt = (ParametricType<FunctionType>) funcSig;
            this.parametricFunctions.computeIfAbsent(name, k -> new LinkedList<>())
                    .add(new ParametricFunction(ctx, pt, this.namespacePrefix));
            this.definedTypes.exit();
        }

        return null;
    }

    @Override
    public Object visitDeclNativeFunc(SiParser.DeclNativeFuncContext ctx) {
        final String external = ctx.nat.getText();
        final String name = this.namespacePrefix + '\\' + ctx.name.getText();

        final List<Type> rawIn = new LinkedList<>();
        final List<Binding> params = new LinkedList<>();
        for (final SiParser.FuncParamContext arg : ctx.in) {
            final Type t = this.getTypeSignature(arg.type);
            rawIn.add(t);
            params.add(new Binding.Immutable(arg.name.getText(), t));
        }
        final Type out = this.getTypeSignature(ctx.out);

        final Type in;
        final Value arg;
        switch (rawIn.size()) {
        case 0: // unit type
            in = UnitType.INSTANCE;
            arg = ImmUnit.INSTANCE;
            break;
        case 1: // singleton type
            in = rawIn.get(0);
            arg = params.get(0);
            break;
        default: // tuple type
            final TupleType type = new TupleType(rawIn);
            in = type;
            arg = new Tuple(params, type);
            break;
        }

        final FunctionType typeSig = new FunctionType(in, out);
        final InstantiatedFunction prev = this.nonGenericFunctions.get(name);
        if (prev != null) {
            throw new DuplicateDefinitionException(
                    "Duplicate function name: " + name + " previously defined as: " + prev);
        }

        final InstantiatedFunction ifunc = new InstantiatedFunction.Native(ctx, typeSig, this.namespacePrefix);
        this.nonGenericFunctions.put(name, ifunc);

        // construct a function that just does a tail call
        // this function will guaranteed be inlined

        ifunc.getSubroutine().setParameters(params);

        // this function just calls the native function
        final Block entry = new Block("_entry");
        entry.setStatements(Collections.singletonList(
                new TailCallStatement(new FuncRef.Native(external, typeSig), arg)));
        ifunc.getSubroutine().setInitialBlock(entry);

        ifunc.getSubroutine().validate();

        return null;
    }

    private synchronized TypeBank<Type> getFromDefinedTypes(String name) {
        return this.definedTypes.getOrInit(name, TypeBank::new);
    }

    private Type getTypeSignature(ParseTree ctx) {
        final Type t = (Type) this.visit(ctx);
        if (t == null) {
            throw new NullPointerException("Null type should not happen (probably a syntax error): " + ctx.getText());
        }
        return t.expandBound();
    }

    private FreeType getTypeRestriction(SiParser.GenericParamContext ctx) {
        final FreeType t = (FreeType) this.visit(ctx);
        if (t == null) {
            throw new NullPointerException(
                    "Null type restriction should not happen (probably a syntax error): " + ctx.getText());
        }
        return t;
    }

    private void typeCheckInstantiatedFunction(InstantiatedFunction.Local ifunc) {
        final SiParser.DeclFuncContext ctx = ifunc.getSyntaxTree();

        final String name = ctx.name.getText();
        final FunctionType funcType = ifunc.getType();

        this.namespacePrefix = ifunc.getNamespace();

        // Enter the generic type parameters scope if necessary
        if (!ifunc.getParametrization().isEmpty()) {
            this.definedTypes.enter();
            for (final Map.Entry<String, Type> e : ifunc.getParametrization().entrySet()) {
                this.definedTypes.put(e.getKey(), TypeBank.withSimpleType(e.getValue()));
            }
        }

        this.cgenState.reset();
        final Block headBlock = this.cgenState.makeAndSetBlock("_entry");

        try {
            // Enter the parameters scope
            this.locals.enter();
            final List<SiParser.FuncParamContext> args = ctx.sig.in;
            final int limit = args.size();

            {
                final LinkedList<Binding> params = new LinkedList<>();
                for (int i = 0; i < limit; ++i) {
                    // This needs to get the information from the function signature
                    // that is why we do not do this#visitFuncParamContext(arg)
                    final SiParser.FuncParamContext arg = args.get(i);
                    params.addLast(this.declareLocalVariable(arg.name.getText(), funcType.getSplattedInput(i), true));
                }
                ifunc.getSubroutine().setParameters(params);
            }

            final Type analyzedOutput = this.getTypeSignature(ctx.e);
            final Type resultType = funcType.getOutput();
            if (!Types.assignableFrom(resultType, analyzedOutput)) {
                throw new TypeMismatchException("Expected output convertible to: " + resultType + " but got: " + analyzedOutput);
            }

            // Exit the parameters scope
            this.locals.exit();
        } catch (CompileTimeException ex) {
            throw new CompileTimeException("Validation failed in function: " + name, ex);
        }

        // Exit the generic type parameters scsope if necessary
        if (!ifunc.getParametrization().isEmpty()) {
            this.definedTypes.exit();
        }

        // Construct the subroutine
        // Add implicit return
        this.cgenState.addStatement(new ReturnStatement(this.cgenState.getTemporary()));
        this.cgenState.buildCurrentBlock();

        ifunc.getSubroutine().setInitialBlock(headBlock);
        ifunc.getSubroutine().validate();
    }

    @Override
    public Binding visitDeclVar(SiParser.DeclVarContext ctx) {
        final String name = ctx.name.getText();
        final Type type = ctx.type == null ? new InferredType() : this.getTypeSignature(ctx.type);

        return this.declareLocalVariable(name, type, ctx.mut == null);
    }

    @Override
    public Binding visitFuncParam(SiParser.FuncParamContext ctx) {
        final String name = ctx.name.getText();
        final Type type = this.getTypeSignature(ctx.type);

        // XXX: All function parameters immutable bindings!
        return this.declareLocalVariable(name, type, true);
    }

    private Binding declareLocalVariable(String name, Type type, boolean immutable) {
        // Only search in the current scope!
        final Binding prev = this.locals.getCurrent(name);
        if (prev != null) {
            throw new DuplicateDefinitionException("Duplicate local of binding: " + name + " as: " + prev.type);
        }

        // need to add scope depth to make sure the internal name
        // does not collide with the ones in the outer scope
        final String mangled = name + '_' + this.locals.getDepth();
        final Binding binding = immutable ? new Binding.Immutable(mangled, type) : new Binding.Mutable(mangled, type);
        locals.put(name, binding);
        return binding;
    }

    @Override
    public Type visitExprBinding(SiParser.ExprBindingContext ctx) {
        final String rawName = ctx.base.getText();

        if (!rawName.contains("\\")) {
            // It might be a local binding
            final Binding lvar = this.locals.get(rawName);
            if (lvar != null) {
                this.cgenState.setTemporary(lvar);
                return lvar.type;
            }
        }

        // It might be a non-generic function
        final String name = this.visitNamespacePath(ctx.base);
        final InstantiatedFunction ifunc = this.nonGenericFunctions.get(name);
        if (ifunc != null) {
            // XXX: Assumes InstantiatedFunction does satisfy type requirements
            this.cgenState.setTemporary(new FuncRef.Local(ifunc.getSubroutine()));
            return ifunc.getType();
        }

        // Error reporting mechanism
        if (this.parametricFunctions.containsKey(name)) {
            throw new TypeMismatchException("Missing type paramters for function: " + name);
        }

        throw new UnboundDefinitionException("Unbound definition for binding: " + name);
    }

    @Override
    public FunctionType visitExprParametrize(SiParser.ExprParametrizeContext ctx) {
        final String name = this.visitNamespacePath(ctx.base);

        // It has to be a generic function (local variables cannot be parametric)
        final List<ParametricFunction> funcs = this.parametricFunctions.get(name);
        if (funcs == null || funcs.isEmpty()) {
            // Technically list (can be null but) can never be empty
            // isEmpty as sanity check!
            throw new UnboundDefinitionException("Unbound definition for generic function: " + name);
        }

        final List<Type> args = this.visitTypeParams(ctx.args);
        final StringBuilder errMsg = new StringBuilder("Cannot parametrize generic function: " + name);
        for (final ParametricFunction pt : funcs) {
            try {
                // Instantiate the function
                final InstantiatedFunction.Local ifunc = pt.instantiateTypes(args);
                Subroutine sub = ifunc.getSubroutine();
                // Check if it is already instantiated
                final InstantiatedFunction.Local old = this.instantiatedGenericFunctions.get(ifunc.getName());
                if (old == null) {
                    // Queue it
                    this.instantiatedGenericFunctions.put(ifunc.getName(), ifunc);
                    this.queuedInstantiatedFunctions.add(ifunc);
                } else {
                    // Reference the existing definition
                    sub = old.getSubroutine();
                }

                // XXX: Assume the function we instantiated works
                this.cgenState.setTemporary(new FuncRef.Local(sub));
                return ifunc.getType();
            } catch (TypeMismatchException ex) {
                errMsg.append("\n- ").append(ex.getMessage());
            }
        }

        // Reaching here means all potential matches failed
        throw new TypeMismatchException(errMsg.toString());
    }

    @Override
    public Type visitExprImmInt(SiParser.ExprImmIntContext ctx) {
        this.cgenState.setTemporary(new ImmInteger(convertIntLiteral(ctx.getText())));
        return TYPE_INT;
    }

    @Override
    public Type visitExprImmDouble(SiParser.ExprImmDoubleContext ctx) {
        this.cgenState.setTemporary(new ImmDouble(convertDoubleLiteral(ctx.getText())));
        return TYPE_DOUBLE;
    }

    @Override
    public Type visitExprImmBool(SiParser.ExprImmBoolContext ctx) {
        return this.generateImmBoolean(convertBoolLiteral(ctx.getText()));
    }

    private Type generateImmBoolean(boolean b) {
        this.cgenState.setTemporary(new ImmBoolean(b));
        return TYPE_BOOL;
    }

    @Override
    public Type visitExprImmChr(SiParser.ExprImmChrContext ctx) {
        this.cgenState.setTemporary(new ImmCharacter(convertCharLiteral(ctx.getText())));
        return TYPE_CHAR;
    }

    @Override
    public Type visitExprImmStr(SiParser.ExprImmStrContext ctx) {
        this.cgenState.setTemporary(new ImmString(convertStringLiteral(ctx.getText())));
        return TYPE_STRING;
    }

    @Override
    public Type visitExprParenthesis(SiParser.ExprParenthesisContext ctx) {
        final int limit = ctx.e.size();
        switch (limit) {
        case 0:
            this.cgenState.setTemporary(ImmUnit.INSTANCE);
            return UnitType.INSTANCE;
        case 1:
            return this.getTypeSignature(ctx.e.get(0));
        default:
            final List<Value> vs = new ArrayList<>(limit);
            final List<Type> ts = new ArrayList<>(limit);
            for (final SiParser.ExprContext e : ctx.e) {
                ts.add(this.getTypeSignature(e));
                vs.add(this.cgenState.getTemporary());
            }

            final TupleType type = new TupleType(ts);
            this.cgenState.setTemporary(new Tuple(vs, type));
            return type;
        }
    }

    @Override
    public Type visitExprDoEnd(SiParser.ExprDoEndContext ctx) {
        Type last = null;
        for (final ParseTree t : ctx.e) {
            last = this.getTypeSignature(t);
        }
        // last cannot be null due to grammar (at least one statement)
        return last;
    }

    @Override
    public Type visitExprVarDecl(SiParser.ExprVarDeclContext ctx) {
        // Evaluate the value in the outer scope
        final Type analyzedType = this.getTypeSignature(ctx.v);

        // Create a new scope
        this.locals.enter();

        // Then declare the binding
        final Binding decl = this.visitDeclVar(ctx.binding);
        final Type declType = decl.type;

        if (!Types.assignableFrom(declType, analyzedType)) {
            throw new TypeMismatchException("Binding: " + ctx.binding.name.getText()
                    + " expected value convertible to: " + declType + " but got: " + analyzedType);
        }

        // Note: we do not update the temporary
        this.cgenState.addStatement(new MoveStatement(decl, this.cgenState.getTemporary()));

        // Type check the expresssion
        final Type ret = this.getTypeSignature(ctx.e);

        // Leave the new scope
        this.locals.exit();

        return ret;
    }

    private Type unaryOperatorHelper(TypeBank<Type> bank, SiParser.ExprContext base) {
        final Type baseType = this.getTypeSignature(base);
        final Value baseTemporary = this.cgenState.getTemporary();

        final List<Type> args = Collections.singletonList(baseType);
        final ParametricType<Type> pt = bank.selectParametrization(args);

        @SuppressWarnings("unchecked")
        final UnaryOpCodeGen codegen = (UnaryOpCodeGen) bank.getMapping(pt);
        codegen.generate(baseTemporary);
        return pt.parametrize(args);
    }

    private Type binaryOperatorHelper(TypeBank<Type> bank, SiParser.ExprContext lhs, SiParser.ExprContext rhs) {
        final Type lhsType = this.getTypeSignature(lhs);
        final Value lhsTemporary = this.cgenState.getTemporary();

        final Type rhsType = this.getTypeSignature(rhs);
        final Value rhsTemporary = this.cgenState.getTemporary();

        final List<Type> args = Arrays.asList(lhsType, rhsType);
        final ParametricType<Type> pt = bank.selectParametrization(args);

        @SuppressWarnings("unchecked")
        final BinaryOpCodeGen codegen = (BinaryOpCodeGen) bank.getMapping(pt);
        codegen.generate(lhsTemporary, rhsTemporary);
        return pt.parametrize(args);
    }

    @Override
    public Type visitExprTypeCast(SiParser.ExprTypeCastContext ctx) {
        final List<Type> list = this.visitTypeParams(ctx.conv);
        if (list.size() != 1) {
            throw new TypeMismatchException("Cast only accepts one type argument: " + list);
        }

        final Type output = list.get(0);
        final Type input = this.getTypeSignature(ctx.e);

        // if output is assignable from input,
        // in other words, expr{T}(k) where val t T = k is sound,
        // then we just return output directly
        if (Types.assignableFrom(output, input)) {
            return output;
        }

        // then we actually see if cast is allowed

        // internally, think of cast as something like:
        // {input, output}(input)output

        final ParametricType<Type> pt = OPERATOR_CAST.selectParametrization(Arrays.asList(input, output));
        final UnaryOpCodeGen codegen = (UnaryOpCodeGen) OPERATOR_CAST.getMapping(pt);
        codegen.generate(this.cgenState.getTemporary());

        // no need to re-parametrize the type:
        // the whole point of a cast is to get the type $output at the end
        return output;
    }

    @Override
    public Type visitExprUnary(SiParser.ExprUnaryContext ctx) {
        final String op = ctx.op.getText();
        final TypeBank<Type> bank;
        switch (op) {
        case "~":
            bank = OPERATOR_NOT;
            break;
        case "+":
            bank = OPERATOR_POS;
            break;
        case "-":
            bank = OPERATOR_NEG;
            break;
        default:
            throw new AssertionError("Unhandled operator: " + op);
        }
        return this.unaryOperatorHelper(bank, ctx.base);
    }

    @Override
    public Type visitExprMulDiv(SiParser.ExprMulDivContext ctx) {
        final String op = ctx.op.getText();
        final TypeBank<Type> bank;
        switch (op) {
        case "*":
            bank = OPERATOR_MUL;
            break;
        case "/":
            bank = OPERATOR_DIV;
            break;
        default:
            throw new AssertionError("Unhandled operator: " + op);
        }
        return this.binaryOperatorHelper(bank, ctx.lhs, ctx.rhs);
    }

    @Override
    public Type visitExprAddSub(SiParser.ExprAddSubContext ctx) {
        final String op = ctx.op.getText();
        final TypeBank<Type> bank;
        switch (op) {
        case "+":
            bank = OPERATOR_ADD;
            break;
        case "-":
            bank = OPERATOR_SUB;
            break;
        default:
            throw new AssertionError("Unhandled operator: " + op);
        }
        return this.binaryOperatorHelper(bank, ctx.lhs, ctx.rhs);
    }

    @Override
    public Type visitExprThreeWayCompare(SiParser.ExprThreeWayCompareContext ctx) {
        return this.binaryOperatorHelper(OPERATOR_THREE_WAY_COMP, ctx.lhs, ctx.rhs);
    }

    @Override
    public Type visitExprRelational(SiParser.ExprRelationalContext ctx) {
        final String op = ctx.op.getText();
        final TypeBank<Type> bank;
        switch (op) {
        case "<":
            bank = OPERATOR_LT;
            break;
        case "<=":
            bank = OPERATOR_LE;
            break;
        case ">=":
            bank = OPERATOR_GE;
            break;
        case ">":
            bank = OPERATOR_GT;
            break;
        default:
            throw new AssertionError("Unhandled operator: " + op);
        }
        return this.binaryOperatorHelper(bank, ctx.lhs, ctx.rhs);
    }

    @Override
    public Type visitExprEquivalence(SiParser.ExprEquivalenceContext ctx) {
        final String op = ctx.op.getText();
        final TypeBank<Type> bank;
        switch (op) {
        case "==":
            bank = OPERATOR_EQV;
            break;
        case "<>":
            bank = OPERATOR_NEQ;
            break;
        default:
            throw new AssertionError("Unhandled operator: " + op);
        }
        return this.binaryOperatorHelper(bank, ctx.lhs, ctx.rhs);
    }

    @Override
    public Type visitExprAnd(SiParser.ExprAndContext ctx) {
        return this.binaryOperatorHelper(OPERATOR_AND, ctx.lhs, ctx.rhs);
    }

    @Override
    public Type visitExprXor(SiParser.ExprXorContext ctx) {
        return this.binaryOperatorHelper(OPERATOR_XOR, ctx.lhs, ctx.rhs);
    }

    @Override
    public Type visitExprOr(SiParser.ExprOrContext ctx) {
        return this.binaryOperatorHelper(OPERATOR_OR, ctx.lhs, ctx.rhs);
    }

    @Override
    public Type visitExprFuncCall(SiParser.ExprFuncCallContext ctx) {
        final FunctionType f = (FunctionType) this.getTypeSignature(ctx.base);
        final Value fptr = this.cgenState.getTemporary();

        final Type arg = this.getTypeSignature(ctx.arg);
        final Value argVal = this.cgenState.getTemporary();
        if (!f.canApply(arg)) {
            throw new TypeMismatchException(
                    "Function input expected: " + f.getInput() + " but got incompatible: " + arg);
        }

        final Type output = f.getOutput();
        this.cgenState.addStatement(new CallStatement(this.cgenState.makeAndSetTemporary(output), fptr, argVal));
        return output;
    }

    private Type generateIfElseExpr(Supplier<Type> ctxTest, Supplier<Type> ctxIfTrue, Supplier<Type> ctxIfFalse) {
        final Type test = ctxTest.get();
        final Value testTemporary = this.cgenState.getTemporary();
        if (!Types.assignableFrom(TYPE_BOOL, test)) {
            throw new TypeMismatchException("If condition expected: " + TYPE_BOOL + " but got: " + test);
        }

        Block blockTrue = this.cgenState.makeBlock();
        Block blockFalse = this.cgenState.makeBlock();
        final Block blockEnd = this.cgenState.makeBlock();

        this.cgenState.addStatement(new ConditionalJumpStatement(
            ConditionalJumpStatement.ConditionalOperator.EQ_ZZ,
            blockTrue,
            blockFalse,
            this.cgenState.getTemporary(),
            new ImmBoolean(true)
        ));
        this.cgenState.buildCurrentBlock();

        this.cgenState.setCurrentBlock(blockTrue);
        final Type ifTrue = ctxIfTrue.get();
        final Value trueVal = this.cgenState.getTemporary();
        final List<Statement> trueStmts = this.cgenState.clearStatements();
        blockTrue = this.cgenState.getCurrentBlock();

        this.cgenState.setCurrentBlock(blockFalse);
        final Type ifFalse = ctxIfFalse.get();
        final Value falseVal = this.cgenState.getTemporary();
        final List<Statement> falseStmts = this.cgenState.clearStatements();
        blockFalse = this.cgenState.getCurrentBlock();

        final Type unifiedType = Types.unify(ifTrue, ifFalse).orElseThrow(() ->
                new TypeMismatchException("Cannot unify unrelated: " + ifTrue + " and: " + ifFalse));
        final Binding result = this.cgenState.makeAndSetTemporary(unifiedType);

        // Move the respected results into the correct temporary
        // And jump to the end block

        trueStmts.add(new MoveStatement(result, trueVal));
        trueStmts.add(new GotoStatement(blockEnd));

        falseStmts.add(new MoveStatement(result, falseVal));
        falseStmts.add(new GotoStatement(blockEnd));

        // fill the statements into the correct block

        blockTrue.setStatements(trueStmts);
        blockFalse.setStatements(falseStmts);

        this.cgenState.setCurrentBlock(blockEnd);

        return unifiedType;
    }

    @Override
    public Type visitExprIfElse(SiParser.ExprIfElseContext ctx) {
        return this.generateIfElseExpr(
                () -> this.getTypeSignature(ctx.test),
                () -> this.getTypeSignature(ctx.ifTrue),
                () -> this.getTypeSignature(ctx.ifFalse));
    }

    @Override
    public Type visitExprCondAnd(SiParser.ExprCondAndContext ctx) {
        // a and b <=> if a then b else false
        return this.generateIfElseExpr(
                () -> this.getTypeSignature(ctx.lhs),
                () -> this.getTypeSignature(ctx.rhs),
                () -> this.generateImmBoolean(false));
    }

    @Override
    public Type visitExprCondOr(SiParser.ExprCondOrContext ctx) {
        // a or b <=> if a then true else b
        return this.generateIfElseExpr(
                () -> this.getTypeSignature(ctx.lhs),
                () -> this.generateImmBoolean(true),
                () -> this.getTypeSignature(ctx.rhs));
    }

    public static int convertIntLiteral(String raw) {
        if (raw.length() >= 3) {
            if (raw.charAt(0) == '0') {
                switch (raw.charAt(1)) {
                    case 'b':   Integer.parseInt(raw.substring(2), 2);
                    case 'c':   Integer.parseInt(raw.substring(2), 8);
                    case 'd':   Integer.parseInt(raw.substring(2), 10);
                    case 'x':   Integer.parseInt(raw.substring(2), 16);
                    default:    throw new AssertionError("Illegal base changer 0" + raw.charAt(1));
                }
            }
        }

        // parse as base 10
        return Integer.parseInt(raw);
    }

    public static double convertDoubleLiteral(String raw) {
        return Double.parseDouble(raw);
    }

    public static boolean convertBoolLiteral(String raw) {
        switch (raw) {
            case "true":    return true;
            case "false":   return false;
            default:        throw new AssertionError("Illegal boolean " + raw);
        }
    }

    public static char convertCharLiteral(String raw) {
        final String s = convertStringLiteral(raw);
        if (s.length() != 1) {
            // This happens either when s is empty
            // or s contains more than one UTF16 codepoint
            throw new AssertionError("Illegal char (UTF16 codepoint) " + raw);
        }
        return s.charAt(0);
    }

    public static String convertStringLiteral(String raw) {
        final char[] array = raw.toCharArray();
        final int limit = array.length - 1;
        final StringBuilder sb = new StringBuilder(limit - 1);

        for (int i = 1; i < limit; ++i) {
            final char ch = array[i];
            if (ch != '\\') {
                sb.append(ch);
                continue;
            }

            // Assumes string is escaped properly!
            final char next = array[++i];
            switch (next) {
            case 'a':   sb.append((char) 0x07); break;
            case 'b':   sb.append((char) 0x08); break;
            case 'f':   sb.append('\f'); break;
            case 'n':   sb.append('\n'); break;
            case 'r':   sb.append('\r'); break;
            case 't':   sb.append('\t'); break;
            case 'v':   sb.append((char) 0x0B); break;
            case '\"':  sb.append('\"'); break;
            case '\'':  sb.append('\''); break;
            case '\\':  sb.append('\\'); break;
            case 'u':
                sb.append((char) Integer.parseInt("" + array[++i] + array[++i] + array[++i] + array[++i], 16));
                break;
            case 'U': {
                sb.append(Character.toChars((int) Long.parseLong("" + array[++i] + array[++i] + array[++i] + array[++i] + array[++i] + array[++i] + array[++i] + array[++i], 16)));
                break;
            }
            default:
                throw new AssertionError("Illegal escape \\" + next);
            }
        }
        return sb.toString();
    }

    private void buildOperatorMap() {
        // Parametric types are only for TypeBank to select the correct output type
        final FreeType rBool = new FreeType(TYPE_BOOL.toString(), TYPE_BOOL);
        final FreeType rUnit = new FreeType(UnitType.INSTANCE.toString(), UnitType.INSTANCE);
        final FreeType rInt = new FreeType(TYPE_INT.toString(), TYPE_INT);
        final FreeType rDouble = new FreeType(TYPE_DOUBLE.toString(), TYPE_DOUBLE);
        final FreeType rChar = new FreeType(TYPE_CHAR.toString(), TYPE_CHAR);
        final FreeType rString = new FreeType(TYPE_STRING.toString(), TYPE_STRING);

        // Type casts

        final ParametricType<Type> uz = new ParametricType<>(UnitType.INSTANCE, Arrays.asList(rUnit, rBool));
        final ParametricType<Type> ui = new ParametricType<>(UnitType.INSTANCE, Arrays.asList(rUnit, rInt));
        final ParametricType<Type> ud = new ParametricType<>(UnitType.INSTANCE, Arrays.asList(rUnit, rDouble));
        final ParametricType<Type> uc = new ParametricType<>(UnitType.INSTANCE, Arrays.asList(rUnit, rChar));
        final ParametricType<Type> us = new ParametricType<>(UnitType.INSTANCE, Arrays.asList(rUnit, rString));

        final ParametricType<Type> id = new ParametricType<>(UnitType.INSTANCE, Arrays.asList(rInt, rDouble));
        final ParametricType<Type> iz = new ParametricType<>(UnitType.INSTANCE, Arrays.asList(rInt, rBool));
        final ParametricType<Type> di = new ParametricType<>(UnitType.INSTANCE, Arrays.asList(rDouble, rInt));
        final ParametricType<Type> zi = new ParametricType<>(UnitType.INSTANCE, Arrays.asList(rBool, rInt));

        // expr{T}() is the equivalent of default(T) in C#
        OPERATOR_CAST.addParametricType(uz, (UnaryOpCodeGen) (src) -> {
            this.cgenState.setTemporary(new ImmBoolean(false));
        });
        OPERATOR_CAST.addParametricType(ui, (UnaryOpCodeGen) (src) -> {
            this.cgenState.setTemporary(new ImmInteger(0));
        });
        OPERATOR_CAST.addParametricType(ud, (UnaryOpCodeGen) (src) -> {
            this.cgenState.setTemporary(new ImmDouble(0));
        });
        OPERATOR_CAST.addParametricType(uc, (UnaryOpCodeGen) (src) -> {
            this.cgenState.setTemporary(new ImmCharacter('\0'));
        });
        OPERATOR_CAST.addParametricType(us, (UnaryOpCodeGen) (src) -> {
            this.cgenState.setTemporary(new ImmString(""));
        });

        OPERATOR_CAST.addParametricType(di, (UnaryOpCodeGen) (src) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_INT);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.D2I, t, src));
        });
        OPERATOR_CAST.addParametricType(id, (UnaryOpCodeGen) (src) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, src));
        });
        OPERATOR_CAST.addParametricType(zi, (UnaryOpCodeGen) (src) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_INT);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.Z2I, t, src));
        });
        OPERATOR_CAST.addParametricType(iz, (UnaryOpCodeGen) (src) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_BOOL);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2Z, t, src));
        });

        // Unary operators
        final ParametricType<Type> b_b = new ParametricType<>(TYPE_BOOL, Collections.singletonList(rBool));
        final ParametricType<Type> i_i = new ParametricType<>(TYPE_INT, Collections.singletonList(rInt));
        final ParametricType<Type> d_d = new ParametricType<>(TYPE_DOUBLE, Collections.singletonList(rDouble));

        OPERATOR_NOT.addParametricType(i_i, (UnaryOpCodeGen) (src) -> {
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.NOT_I, this.cgenState.makeAndSetTemporary(TYPE_INT), src));
        });
        OPERATOR_NOT.addParametricType(b_b, (UnaryOpCodeGen) (src) -> {
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.NOT_Z, this.cgenState.makeAndSetTemporary(TYPE_BOOL), src));
        });

        OPERATOR_NEG.addParametricType(i_i, (UnaryOpCodeGen) (src) -> {
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.NEG_I, this.cgenState.makeAndSetTemporary(TYPE_INT), src));
        });
        OPERATOR_NEG.addParametricType(d_d, (UnaryOpCodeGen) (src) -> {
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.NEG_D, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), src));
        });

        OPERATOR_POS.addParametricType(i_i, (UnaryOpCodeGen) (src) -> {
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.POS_I, this.cgenState.makeAndSetTemporary(TYPE_INT), src));
        });
        OPERATOR_POS.addParametricType(d_d, (UnaryOpCodeGen) (src) -> {
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.POS_D, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), src));
        });

        // Binary operators

        final ParametricType<Type> ii_i = new ParametricType<>(TYPE_INT, Arrays.asList(rInt, rInt));
        final ParametricType<Type> dd_i = new ParametricType<>(TYPE_INT, Arrays.asList(rDouble, rDouble));
        final ParametricType<Type> id_i = new ParametricType<>(TYPE_INT, Arrays.asList(rInt, rDouble));
        final ParametricType<Type> di_i = new ParametricType<>(TYPE_INT, Arrays.asList(rDouble, rInt));
        final ParametricType<Type> cc_i = new ParametricType<>(TYPE_INT, Arrays.asList(rChar, rChar));
        final ParametricType<Type> ss_i = new ParametricType<>(TYPE_INT, Arrays.asList(rString, rString));

        final ParametricType<Type> uu_b = new ParametricType<>(TYPE_BOOL, Arrays.asList(rUnit, rUnit));
        final ParametricType<Type> bb_b = new ParametricType<>(TYPE_BOOL, Arrays.asList(rBool, rBool));
        final ParametricType<Type> ii_b = new ParametricType<>(TYPE_BOOL, Arrays.asList(rInt, rInt));
        final ParametricType<Type> dd_b = new ParametricType<>(TYPE_BOOL, Arrays.asList(rDouble, rDouble));
        final ParametricType<Type> id_b = new ParametricType<>(TYPE_BOOL, Arrays.asList(rInt, rDouble));
        final ParametricType<Type> di_b = new ParametricType<>(TYPE_BOOL, Arrays.asList(rDouble, rInt));
        final ParametricType<Type> cc_b = new ParametricType<>(TYPE_BOOL, Arrays.asList(rChar, rChar));
        final ParametricType<Type> ss_b = new ParametricType<>(TYPE_BOOL, Arrays.asList(rString, rString));

        final ParametricType<Type> dd_d = new ParametricType<>(TYPE_DOUBLE, Arrays.asList(rDouble, rDouble));
        final ParametricType<Type> id_d = new ParametricType<>(TYPE_DOUBLE, Arrays.asList(rInt, rDouble));
        final ParametricType<Type> di_d = new ParametricType<>(TYPE_DOUBLE, Arrays.asList(rDouble, rInt));

        OPERATOR_ADD.addParametricType(ii_i, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.ADD_II, this.cgenState.makeAndSetTemporary(TYPE_INT), a, b));
        });
        OPERATOR_ADD.addParametricType(dd_d, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.ADD_DD, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), a, b));
        });
        OPERATOR_ADD.addParametricType(id_d, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, a));
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.ADD_DD, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), t, b));
        });
        OPERATOR_ADD.addParametricType(di_d, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, b));
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.ADD_DD, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), a, t));
        });

        OPERATOR_SUB.addParametricType(ii_i, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.SUB_II, this.cgenState.makeAndSetTemporary(TYPE_INT), a, b));
        });
        OPERATOR_SUB.addParametricType(dd_d, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.SUB_DD, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), a, b));
        });
        OPERATOR_SUB.addParametricType(id_d, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, a));
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.SUB_DD, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), t, b));
        });
        OPERATOR_SUB.addParametricType(di_d, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, b));
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.SUB_DD, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), a, t));
        });

        OPERATOR_MUL.addParametricType(ii_i, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.MUL_II, this.cgenState.makeAndSetTemporary(TYPE_INT), a, b));
        });
        OPERATOR_MUL.addParametricType(dd_d, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.MUL_DD, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), a, b));
        });
        OPERATOR_MUL.addParametricType(id_d, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, a));
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.MUL_DD, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), t, b));
        });
        OPERATOR_MUL.addParametricType(di_d, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, b));
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.MUL_DD, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), a, t));
        });

        OPERATOR_DIV.addParametricType(ii_i, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.DIV_II, this.cgenState.makeAndSetTemporary(TYPE_INT), a, b));
        });
        OPERATOR_DIV.addParametricType(dd_d, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.DIV_DD, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), a, b));
        });
        OPERATOR_DIV.addParametricType(id_d, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, a));
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.DIV_DD, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), t, b));
        });
        OPERATOR_DIV.addParametricType(di_d, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, b));
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.DIV_DD, this.cgenState.makeAndSetTemporary(TYPE_DOUBLE), a, t));
        });

        OPERATOR_THREE_WAY_COMP.addParametricType(ii_i, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.CMP_II, this.cgenState.makeAndSetTemporary(TYPE_INT), a, b));
        });
        OPERATOR_THREE_WAY_COMP.addParametricType(dd_i, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.CMP_DD, this.cgenState.makeAndSetTemporary(TYPE_INT), a, b));
        });
        OPERATOR_THREE_WAY_COMP.addParametricType(id_i, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, a));
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.CMP_DD, this.cgenState.makeAndSetTemporary(TYPE_INT), t, b));
        });
        OPERATOR_THREE_WAY_COMP.addParametricType(di_i, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, b));
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.CMP_DD, this.cgenState.makeAndSetTemporary(TYPE_INT), a, t));
        });
        OPERATOR_THREE_WAY_COMP.addParametricType(cc_i, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.CMP_CC, this.cgenState.makeAndSetTemporary(TYPE_INT), a, b));
        });
        OPERATOR_THREE_WAY_COMP.addParametricType(ss_i, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.CMP_SS, this.cgenState.makeAndSetTemporary(TYPE_INT), a, b));
        });

        // Relational operators support everything that is
        // supported by the three-way comparison, except
        // it returns a boolean type instead of an integer

        OPERATOR_LT.addParametricType(ii_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.LT_II));
        OPERATOR_LT.addParametricType(dd_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.LT_DD));
        OPERATOR_LT.addParametricType(id_b, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, a));
            this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.LT_DD).generate(t, b);
        });
        OPERATOR_LT.addParametricType(di_b, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, b));
            this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.LT_DD).generate(a, t);
        });
        OPERATOR_LT.addParametricType(cc_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.LT_CC));
        OPERATOR_LT.addParametricType(ss_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.LT_SS));

        // see OPERATOR_LT (does same thing, but LE)

        OPERATOR_LE.addParametricType(ii_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.LE_II));
        OPERATOR_LE.addParametricType(dd_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.LE_DD));
        OPERATOR_LE.addParametricType(id_b, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, a));
            this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.LE_DD).generate(t, b);
        });
        OPERATOR_LE.addParametricType(di_b, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, b));
            this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.LE_DD).generate(a, t);
        });
        OPERATOR_LE.addParametricType(cc_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.LE_CC));
        OPERATOR_LE.addParametricType(ss_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.LE_SS));

        // see OPERATOR_LT (does same thing, but GE)

        OPERATOR_GE.addParametricType(ii_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.GE_II));
        OPERATOR_GE.addParametricType(dd_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.GE_DD));
        OPERATOR_GE.addParametricType(id_b, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, a));
            this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.GE_DD).generate(t, b);
        });
        OPERATOR_GE.addParametricType(di_b, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, b));
            this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.GE_DD).generate(a, t);
        });
        OPERATOR_GE.addParametricType(cc_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.GE_CC));
        OPERATOR_GE.addParametricType(ss_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.GE_SS));

        // see OPERATOR_LT (does same thing, but GT)

        OPERATOR_GT.addParametricType(ii_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.GT_II));
        OPERATOR_GT.addParametricType(dd_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.GT_DD));
        OPERATOR_GT.addParametricType(id_b, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, a));
            this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.GT_DD).generate(t, b);
        });
        OPERATOR_GT.addParametricType(di_b, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, b));
            this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.GT_DD).generate(a, t);
        });
        OPERATOR_GT.addParametricType(cc_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.GT_CC));
        OPERATOR_GT.addParametricType(ss_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.GT_SS));

        // Equivalence operators support everything that is
        // supported by the three-way comparison, except
        // it returns a boolean type instead of an integer

        OPERATOR_EQV.addParametricType(ii_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.EQ_II));
        OPERATOR_EQV.addParametricType(dd_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.EQ_DD));
        OPERATOR_EQV.addParametricType(id_b, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, a));
            this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.EQ_DD).generate(t, b);
        });
        OPERATOR_EQV.addParametricType(di_b, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, b));
            this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.EQ_DD).generate(a, t);
        });
        OPERATOR_EQV.addParametricType(cc_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.EQ_CC));
        OPERATOR_EQV.addParametricType(ss_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.EQ_SS));

        OPERATOR_EQV.addParametricType(uu_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.EQ_UU));
        OPERATOR_EQV.addParametricType(bb_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.EQ_ZZ));

        // see OPERATOR_EQV (does same thing, but negated opcode)

        OPERATOR_NEQ.addParametricType(ii_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.NE_II));
        OPERATOR_NEQ.addParametricType(dd_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.NE_DD));
        OPERATOR_NEQ.addParametricType(id_b, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, a));
            this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.NE_DD).generate(t, b);
        });
        OPERATOR_NEQ.addParametricType(di_b, (BinaryOpCodeGen) (a, b) -> {
            final Binding t = this.cgenState.makeAndSetTemporary(TYPE_DOUBLE);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2D, t, b));
            this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.NE_DD).generate(a, t);
        });
        OPERATOR_NEQ.addParametricType(cc_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.NE_CC));
        OPERATOR_NEQ.addParametricType(ss_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.NE_SS));

        OPERATOR_NEQ.addParametricType(uu_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.NE_UU));
        OPERATOR_NEQ.addParametricType(bb_b, this.generateRelationalCode(ConditionalJumpStatement.ConditionalOperator.NE_ZZ));

        OPERATOR_AND.addParametricType(ii_i, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.AND_II, this.cgenState.makeAndSetTemporary(TYPE_INT), a, b));
        });
        OPERATOR_AND.addParametricType(bb_b, this.generateBoolTest(true));

        OPERATOR_XOR.addParametricType(ii_i, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.XOR_II, this.cgenState.makeAndSetTemporary(TYPE_INT), a, b));
        });
        OPERATOR_XOR.addParametricType(bb_b, (BinaryOpCodeGen) (a, b) -> {
            // convert both to ints, do xor on int, then convert it back
            final Binding t1 = this.cgenState.makeTemporary(TYPE_INT);
            final Binding t2 = this.cgenState.makeTemporary(TYPE_INT);
            final Binding t3 = this.cgenState.makeTemporary(TYPE_INT);
            final Binding t4 = this.cgenState.makeAndSetTemporary(TYPE_BOOL);
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.Z2I, t1, a));
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.Z2I, t2, b));
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.XOR_II, t3, t1, t2));
            this.cgenState.addStatement(new UnaryStatement(UnaryStatement.UnaryOperator.I2Z, t4, t3));
        });

        OPERATOR_OR.addParametricType(ii_i, (BinaryOpCodeGen) (a, b) -> {
            this.cgenState.addStatement(new BinaryStatement(BinaryStatement.BinaryOperator.OR_II, this.cgenState.makeAndSetTemporary(TYPE_INT), a, b));
        });
        OPERATOR_OR.addParametricType(bb_b, this.generateBoolTest(false));
    }

    private BinaryOpCodeGen generateRelationalCode(final ConditionalJumpStatement.ConditionalOperator op) {
        // prev:
        //     <<a:expr>>
        //     <<b:expr>>
        //     <<op>> ifTrue, ifFalse, a, b
        // ifFalse:
        //     mov result, false
        //     jmp end
        // ifTrue:
        //     mov result, true
        //     jmp end
        // end:

        return (BinaryOpCodeGen) (a, b) -> {
            final Block ifFalse = this.cgenState.makeBlock();
            final Block ifTrue = this.cgenState.makeBlock();
            final Binding result = this.cgenState.makeAndSetTemporary(TYPE_BOOL);

            this.cgenState.addStatement(new ConditionalJumpStatement(
                    op,
                    ifTrue,
                    ifFalse,
                    a,
                    b
            ));
            this.cgenState.buildCurrentBlock();

            final Block endBlock = this.cgenState.makeAndSetBlock();

            ifFalse.setStatements(Arrays.asList(
                    new MoveStatement(result, new ImmBoolean(false)),
                    new GotoStatement(endBlock)));

            ifTrue.setStatements(Arrays.asList(
                new MoveStatement(result, new ImmBoolean(true)),
                new GotoStatement(endBlock)));
        };
    }

    private BinaryOpCodeGen generateBoolTest(final boolean value) {
        // prev:
        //     <<a:expr>>
        //     <<b:expr>>
        //     eq.zz ifTrue, ifFalse, a, <<value>>
        // ifFalse:
        //     mov result, ¬<<value>>
        //     jmp end
        // ifTrue:
        //     mov result, t1
        //     jmp end
        // end:

        return (BinaryOpCodeGen) (a, b) -> {
            final Block ifFalse = this.cgenState.makeBlock();
            final Block ifTrue = this.cgenState.makeBlock();
            final Binding result = this.cgenState.makeAndSetTemporary(TYPE_BOOL);

            this.cgenState.addStatement(new ConditionalJumpStatement(
                ConditionalJumpStatement.ConditionalOperator.EQ_ZZ,
                ifTrue,
                ifFalse,
                a,
                new ImmBoolean(value)
            ));
            this.cgenState.buildCurrentBlock();

            final Block endBlock = this.cgenState.makeAndSetBlock();

            ifFalse.setStatements(Arrays.asList(
                    new MoveStatement(result, new ImmBoolean(!value)),
                    new GotoStatement(endBlock)));

            ifTrue.setStatements(Arrays.asList(
                    new MoveStatement(result, b),
                    new GotoStatement(endBlock)));
        };
    }
}
