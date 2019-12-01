/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ymcmp.si.lang.grammar.SiBaseVisitor;
import com.ymcmp.si.lang.grammar.SiLexer;
import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.FreeType;
import com.ymcmp.si.lang.type.FunctionType;
import com.ymcmp.si.lang.type.InferredType;
import com.ymcmp.si.lang.type.NomialType;
import com.ymcmp.si.lang.type.ParametricType;
import com.ymcmp.si.lang.type.TupleType;
import com.ymcmp.si.lang.type.Type;
import com.ymcmp.si.lang.type.TypeMismatchException;
import com.ymcmp.si.lang.type.TypeUtils;
import com.ymcmp.si.lang.type.UnitType;
import com.ymcmp.si.lang.type.VariantType;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

public class TypeChecker extends SiBaseVisitor<Object> {

    private static final Type TYPE_INT = new NomialType("int");
    private static final Type TYPE_DOUBLE = new NomialType("double");
    private static final Type TYPE_BOOL = new NomialType("bool");
    private static final Type TYPE_CHAR = new NomialType("char");
    private static final Type TYPE_STRING = new NomialType("string");

    private static final TypeBank<Type> OPERATOR_NOT = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_POS = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_NEG = new TypeBank<>();

    private static final TypeBank<Type> OPERATOR_ADD = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_SUB = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_MUL = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_DIV = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_REL = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_EQV = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_AND = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_OR = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_THREE_WAY_COMP = new TypeBank<>();

    static {
        // // Parametric types are only for TypeBank to select the correct output type
        final FreeType rBool = new FreeType(TYPE_BOOL.toString(), TYPE_BOOL);
        final FreeType rUnit = new FreeType(UnitType.INSTANCE.toString(), UnitType.INSTANCE);
        final FreeType rInt = new FreeType(TYPE_INT.toString(), TYPE_INT);
        final FreeType rDouble = new FreeType(TYPE_DOUBLE.toString(), TYPE_DOUBLE);
        final FreeType rChar = new FreeType(TYPE_CHAR.toString(), TYPE_CHAR);
        final FreeType rString = new FreeType(TYPE_STRING.toString(), TYPE_STRING);

        // Unary operators
        final ParametricType<Type> b_b = new ParametricType<>(TYPE_BOOL, Collections.singletonList(rBool));
        final ParametricType<Type> i_i = new ParametricType<>(TYPE_INT, Collections.singletonList(rInt));
        final ParametricType<Type> d_d = new ParametricType<>(TYPE_DOUBLE, Collections.singletonList(rDouble));

        OPERATOR_NOT.addParametricType(i_i);
        OPERATOR_NOT.addParametricType(b_b);

        OPERATOR_NEG.addParametricType(i_i);
        OPERATOR_NEG.addParametricType(d_d);

        OPERATOR_POS.addParametricType(i_i);
        OPERATOR_POS.addParametricType(d_d);

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

        OPERATOR_ADD.addParametricType(ii_i);
        OPERATOR_ADD.addParametricType(dd_d);
        OPERATOR_ADD.addParametricType(id_d);
        OPERATOR_ADD.addParametricType(di_d);

        OPERATOR_SUB.addParametricType(ii_i);
        OPERATOR_SUB.addParametricType(dd_d);
        OPERATOR_SUB.addParametricType(id_d);
        OPERATOR_SUB.addParametricType(di_d);

        OPERATOR_MUL.addParametricType(ii_i);
        OPERATOR_MUL.addParametricType(dd_d);
        OPERATOR_MUL.addParametricType(id_d);
        OPERATOR_MUL.addParametricType(di_d);

        OPERATOR_DIV.addParametricType(ii_i);
        OPERATOR_DIV.addParametricType(dd_d);
        OPERATOR_DIV.addParametricType(id_d);
        OPERATOR_DIV.addParametricType(di_d);

        OPERATOR_THREE_WAY_COMP.addParametricType(ii_i);
        OPERATOR_THREE_WAY_COMP.addParametricType(dd_i);
        OPERATOR_THREE_WAY_COMP.addParametricType(id_i);
        OPERATOR_THREE_WAY_COMP.addParametricType(di_i);
        OPERATOR_THREE_WAY_COMP.addParametricType(cc_i);
        OPERATOR_THREE_WAY_COMP.addParametricType(ss_i);

        // Relational operators support everything that is
        // supported by the three-way comparison, except
        // it returns a boolean type instead of an integer

        OPERATOR_REL.addParametricType(ii_b);
        OPERATOR_REL.addParametricType(dd_b);
        OPERATOR_REL.addParametricType(id_b);
        OPERATOR_REL.addParametricType(di_b);
        OPERATOR_REL.addParametricType(cc_b);
        OPERATOR_REL.addParametricType(ss_b);

        // Equivalence operators support everything that is
        // supported by the three-way comparison, except
        // it returns a boolean type instead of an integer

        OPERATOR_EQV.addParametricType(ii_b);
        OPERATOR_EQV.addParametricType(dd_b);
        OPERATOR_EQV.addParametricType(id_b);
        OPERATOR_EQV.addParametricType(di_b);
        OPERATOR_EQV.addParametricType(cc_b);
        OPERATOR_EQV.addParametricType(ss_b);

        OPERATOR_EQV.addParametricType(uu_b);
        OPERATOR_EQV.addParametricType(bb_b);

        OPERATOR_AND.addParametricType(ii_i);
        OPERATOR_AND.addParametricType(bb_b);

        OPERATOR_OR.addParametricType(ii_i);
        OPERATOR_OR.addParametricType(bb_b);
    }

    private final Map<Path, SiParser.FileContext> importMap = new LinkedHashMap<>();

    private final Scope<String, TypeBank<Type>> definedTypes = new Scope<>();
    private final Map<String, InstantiatedFunction> nonGenericFunctions = new LinkedHashMap<>();
    private final Map<String, List<ParametricFunction>> parametricFunctions = new LinkedHashMap<>();

    private final LinkedList<InstantiatedFunction> queuedInstantiatedFunctions = new LinkedList<>();

    // TODO: Change Type to LocalVar
    // - Type does not keep track of expr, val, or var
    private final Scope<String, Type> locals = new Scope<>();

    private String namespacePrefix = "";

    private Path currentFile;

    public TypeChecker() {
        this.reset();
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

    public final void reset() {
        this.importMap.clear();

        this.locals.clear();

        this.nonGenericFunctions.clear();
        this.parametricFunctions.clear();
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

        // Perform type check *only* on non-generic functions
        for (final InstantiatedFunction ifunc : this.nonGenericFunctions.values()) {
            this.typeCheckInstantiatedFunction(ifunc);
        }

        // Process the queued functions that are instantiated from generic functions
        final HashSet<InstantiatedFunction> set = new HashSet<>();
        InstantiatedFunction ifunc;
        while ((ifunc = this.queuedInstantiatedFunctions.pollFirst()) != null) {
            if (set.contains(ifunc)) {
                // Two identical type instantiations will yield the same result
                // no need to check it again
                continue;
            }

            // Type check it
            this.typeCheckInstantiatedFunction(ifunc);
            set.add(ifunc);
        }

        // Release memory for GC to reclaim!
        set.clear();
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
            final ParseTree tree = decl.getChild(0);
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
    public InferredType visitInferredType(SiParser.InferredTypeContext ctx) {
        return new InferredType();
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
        for (final SiParser.DeclVarContext arg : ctx.in) {
            rawIn.add(this.visitDeclVar(arg));
        }

        final Type out = this.getTypeSignature(ctx.out);

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

        // TODO: Take expr into account

        final Type funcSig = this.visitFuncSig(ctx.sig);
        this.locals.exit();

        if (funcSig instanceof FunctionType) {
            final InstantiatedFunction prev = this.nonGenericFunctions.get(name);
            if (prev != null) {
                throw new DuplicateDefinitionException(
                        "Duplicate function name: " + name + " previously defined as: " + prev);
            }
            this.nonGenericFunctions.put(name, new InstantiatedFunction(ctx, (FunctionType) funcSig, this.namespacePrefix));
        } else {
            @SuppressWarnings("unchecked")
            final ParametricType<FunctionType> pt = (ParametricType<FunctionType>) funcSig;
            this.parametricFunctions.computeIfAbsent(name, k -> new LinkedList<>())
                    .add(new ParametricFunction(ctx, pt, this.namespacePrefix));
            this.definedTypes.exit();
        }

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

    private void typeCheckInstantiatedFunction(InstantiatedFunction ifunc) {
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

        try {
            // Enter the parameters scope
            this.locals.enter();
        for (final SiParser.DeclVarContext arg : ctx.sig.in) {
            this.visitDeclVar(arg);
        }

            final Type analyzedOutput = this.getTypeSignature(ctx.e);
            final Type resultType = funcType.getOutput();
            if (!resultType.assignableFrom(analyzedOutput)) {
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
    }

    @Override
    public Type visitDeclVar(SiParser.DeclVarContext ctx) {
        final String name = ctx.name.getText();
        final Type prev = this.locals.get(name);
        if (prev != null) {
            throw new DuplicateDefinitionException("Duplicate local of binding: " + name + " as: " + prev);
        }

        // TODO: REMEMBER TO HANDLE val, var, and expr
        final Type type = this.getTypeSignature(ctx.type);
        locals.put(name, type);

        return type;
    }

    @Override
    public Type visitExprBinding(SiParser.ExprBindingContext ctx) {
        final String rawName = ctx.base.getText();

        if (!rawName.contains("\\")) {
            // It might be a local binding
            final Type type = this.locals.get(rawName);
            if (type != null) {
                return type;
            }
        }

        // It might be a non-generic function
        final String name = this.visitNamespacePath(ctx.base);
        final InstantiatedFunction ifunc = this.nonGenericFunctions.get(name);
        if (ifunc != null) {
            // XXX: Assumes InstantiatedFunction does satisfy type requirements
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
                final InstantiatedFunction ifunc = pt.instantiateTypes(args);
                // Queue it for type checking
                this.queuedInstantiatedFunctions.add(ifunc);
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
        return TYPE_INT;
    }

    @Override
    public Type visitExprImmDouble(SiParser.ExprImmDoubleContext ctx) {
        return TYPE_DOUBLE;
    }

    @Override
    public Type visitExprImmBool(SiParser.ExprImmBoolContext ctx) {
        return TYPE_BOOL;
    }

    @Override
    public Type visitExprImmChr(SiParser.ExprImmChrContext ctx) {
        return TYPE_CHAR;
    }

    @Override
    public Type visitExprImmStr(SiParser.ExprImmStrContext ctx) {
        return TYPE_STRING;
    }

    @Override
    public Type visitExprParenthesis(SiParser.ExprParenthesisContext ctx) {
        final List<Type> el = ctx.e.stream().map(this::getTypeSignature).collect(Collectors.toList());
        switch (el.size()) {
        case 0:
            return UnitType.INSTANCE;
        case 1:
            return el.get(0);
        default:
            return new TupleType(el);
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
        // Creates a new scope
        this.locals.enter();
        // Declare the binding
        final Type declType = this.visitDeclVar(ctx.binding);

        final Type analyzedType = this.getTypeSignature(ctx.v);
        if (!declType.assignableFrom(analyzedType)) {
            throw new TypeMismatchException("Binding: " + ctx.binding.name.getText()
                    + " expected value convertible to: " + declType + " but got: " + analyzedType);
        }

        // Type check the expresssion
        final Type ret = this.getTypeSignature(ctx.e);

        // Leave the new scope
        this.locals.exit();

        return ret;
    }

    private Type unaryOperatorHelper(TypeBank<Type> bank, SiParser.ExprContext base) {
        final Type baseType = this.getTypeSignature(base);
        final List<Type> args = Collections.singletonList(baseType);
        final ParametricType<Type> pt = bank.selectParametrization(args);
        return pt.parametrize(args);
    }

    private Type binaryOperatorHelper(TypeBank<Type> bank, SiParser.ExprContext lhs, SiParser.ExprContext rhs) {
        final Type lhsType = this.getTypeSignature(lhs);
        final Type rhsType = this.getTypeSignature(rhs);
        final List<Type> args = Arrays.asList(lhsType, rhsType);
        final ParametricType<Type> pt = bank.selectParametrization(args);
        return pt.parametrize(args);
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
        return this.binaryOperatorHelper(OPERATOR_REL, ctx.lhs, ctx.rhs);
    }

    @Override
    public Type visitExprEquivalence(SiParser.ExprEquivalenceContext ctx) {
        return this.binaryOperatorHelper(OPERATOR_EQV, ctx.lhs, ctx.rhs);
    }

    @Override
    public Type visitExprAnd(SiParser.ExprAndContext ctx) {
        return this.binaryOperatorHelper(OPERATOR_AND, ctx.lhs, ctx.rhs);
    }

    @Override
    public Type visitExprOr(SiParser.ExprOrContext ctx) {
        return this.binaryOperatorHelper(OPERATOR_OR, ctx.lhs, ctx.rhs);
    }

    @Override
    public Type visitExprFuncCall(SiParser.ExprFuncCallContext ctx) {
        final FunctionType f = (FunctionType) this.getTypeSignature(ctx.base);
        final Type arg = this.getTypeSignature(ctx.arg);
        if (!f.canApply(arg)) {
            throw new TypeMismatchException(
                    "Function input expected: " + f.getInput() + " but got incompatible: " + arg);
        }
        return f.getOutput();
    }

    @Override
    public Type visitExprIfElse(SiParser.ExprIfElseContext ctx) {
        final Type test = this.getTypeSignature(ctx.test);
        if (!TYPE_BOOL.assignableFrom(test)) {
            throw new TypeMismatchException("If condition expected: " + TYPE_BOOL + " but got: " + test);
        }

        final Type ifTrue = this.getTypeSignature(ctx.ifTrue);
        final Type ifFalse = this.getTypeSignature(ctx.ifFalse);
        return TypeUtils.unify(ifTrue, ifFalse);
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
}
