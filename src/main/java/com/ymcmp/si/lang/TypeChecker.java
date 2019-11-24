/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.ymcmp.si.lang.grammar.SiBaseVisitor;
import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.FunctionType;
import com.ymcmp.si.lang.type.NomialType;
import com.ymcmp.si.lang.type.ParametricType;
import com.ymcmp.si.lang.type.TupleType;
import com.ymcmp.si.lang.type.Type;
import com.ymcmp.si.lang.type.TypeMismatchException;
import com.ymcmp.si.lang.type.UnitType;
import com.ymcmp.si.lang.type.VariantType;
import com.ymcmp.si.lang.type.restriction.AssignableFromRestriction;
import com.ymcmp.si.lang.type.restriction.AssignableToRestriction;
import com.ymcmp.si.lang.type.restriction.EquivalenceRestriction;
import com.ymcmp.si.lang.type.restriction.GenericParameter;
import com.ymcmp.si.lang.type.restriction.TypeRestriction;
import com.ymcmp.si.lang.type.restriction.UnboundedRestriction;

import org.antlr.v4.runtime.tree.ParseTree;

public class TypeChecker extends SiBaseVisitor<Object> {

    private static final Type TYPE_INT = new NomialType("int");
    private static final Type TYPE_DOUBLE = new NomialType("double");
    private static final Type TYPE_BOOL = new NomialType("bool");
    private static final Type TYPE_CHAR = new NomialType("char");
    private static final Type TYPE_STRING = new NomialType("string");

    private static final TypeBank<Type> OPERATOR_ADD = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_SUB = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_MUL = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_DIV = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_REL = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_EQV = new TypeBank<>();
    private static final TypeBank<Type> OPERATOR_THREE_WAY_COMP = new TypeBank<>();

    static {
        // Parametric types are only for TypeBank to select the correct output type
        final EquivalenceRestriction rBool = new EquivalenceRestriction(TYPE_BOOL.toString(), TYPE_BOOL);
        final EquivalenceRestriction rUnit = new EquivalenceRestriction(UnitType.INSTANCE.toString(),
                UnitType.INSTANCE);
        final EquivalenceRestriction rInt = new EquivalenceRestriction(TYPE_INT.toString(), TYPE_INT);
        final EquivalenceRestriction rDouble = new EquivalenceRestriction(TYPE_DOUBLE.toString(), TYPE_DOUBLE);
        final EquivalenceRestriction rChar = new EquivalenceRestriction(TYPE_CHAR.toString(), TYPE_CHAR);
        final EquivalenceRestriction rString = new EquivalenceRestriction(TYPE_STRING.toString(), TYPE_STRING);

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
    }

    private final Scope<String, TypeBank<Type>> definedTypes = new Scope<>();
    private final Scope<String, TypeBank<FunctionType>> definedFunctions = new Scope<>();

    // TODO: Change Type to LocalVar
    // - Type does not keep track of expr, val, or var
    private final Scope<String, Type> locals = new Scope<>();

    public TypeChecker() {
        this.reset();
    }

    public Scope<String, TypeBank<Type>> getUserDefinedTypes() {
        return this.definedTypes;
    }

    public Scope<String, TypeBank<FunctionType>> getUserDefinedFunctions() {
        return this.definedFunctions;
    }

    public final void reset() {
        this.locals.clear();

        this.definedTypes.clear();
        this.definedFunctions.clear();

        this.definedTypes.enter();
        this.definedFunctions.enter();
    }

    @Override
    public Object visitFile(SiParser.FileContext ctx) {
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
        for (final ParseTree tree : queued) {
            this.visit(tree);
        }

        for (final ParseTree tree : queued) {
            if (tree instanceof SiParser.DeclFuncContext) {
                this.typeCheckFunctionBody((SiParser.DeclFuncContext) tree);
            }
        }

        return null;
    }

    @Override
    public UnboundedRestriction visitParamFreeType(SiParser.ParamFreeTypeContext ctx) {
        return new UnboundedRestriction(ctx.name.getText());
    }

    @Override
    public EquivalenceRestriction visitParamEquivType(SiParser.ParamEquivTypeContext ctx) {
        return new EquivalenceRestriction(ctx.name.getText(), this.getTypeSignature(ctx.bound));
    }

    @Override
    public AssignableToRestriction visitParamAssignableToType(SiParser.ParamAssignableToTypeContext ctx) {
        return new AssignableToRestriction(ctx.name.getText(), this.getTypeSignature(ctx.bound));
    }

    @Override
    public AssignableFromRestriction visitParamAssignableFromType(SiParser.ParamAssignableFromTypeContext ctx) {
        return new AssignableFromRestriction(ctx.name.getText(), this.getTypeSignature(ctx.bound));
    }

    @Override
    public List<TypeRestriction> visitDeclGeneric(SiParser.DeclGenericContext ctx) {
        return ctx.args.stream().map(this::getTypeRestriction).collect(Collectors.toList());
    }

    private Type typeDeclarationHelper(SiParser.DeclGenericContext generic, SiParser.CoreTypesContext rawType) {
        if (generic == null) {
            return this.getTypeSignature(rawType);
        }

        this.definedTypes.enter();
        final List<TypeRestriction> bound = this.visitDeclGeneric(generic);
        for (final TypeRestriction e : bound) {
            final String name = e.getName();
            final TypeBank<Type> bank = this.definedTypes.getCurrentOrInit(name, TypeBank::new);

            try {
                bank.setSimpleType(e.getAssociatedType());
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
        final String name = ctx.name.getText();

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
        final String name = ctx.getText();
        final TypeBank<Type> bank = this.definedTypes.get(name);
        if (bank == null) {
            throw new UnboundDefinitionException("Attempt to use undefined type: " + name);
        }

        // This has to be a simple type (based on grammar)
        if (!bank.hasSimpleType()) {
            if (bank.hasParametricType()) {
                throw new TypeMismatchException("Missing type paramters for type: " + name);
            }
            throw new UnboundDefinitionException("Unbound definition for type: " + name);
        }
        return bank.getSimpleType();
    }

    @Override
    public Type visitCoreUnitType(SiParser.CoreUnitTypeContext ctx) {
        return UnitType.INSTANCE;
    }

    @Override
    public Type visitTypeParenthesis(SiParser.TypeParenthesisContext ctx) {
        return this.getTypeSignature(ctx.e);
    }

    @Override
    public FunctionType visitCoreFuncType(SiParser.CoreFuncTypeContext ctx) {
        final Type input = (Type) this.visit(ctx.in);
        final Type output = (Type) this.visit(ctx.out);
        return new FunctionType(input, output);
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
        final String name = ctx.base.getText();
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
        final List<TypeRestriction> bound;
        if (ctx.generic == null) {
            bound = null;
        } else {
            this.definedTypes.enter();
            bound = this.visitDeclGeneric(ctx.generic);
            for (final TypeRestriction e : bound) {
                final TypeBank<Type> bank = this.definedTypes.getCurrentOrInit(e.getName(), TypeBank::new);
                bank.setSimpleType(e.getAssociatedType());
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
        final String name = ctx.name.getText();
        final TypeBank<FunctionType> bank = this.getFromDefinedFunctions(name);

        // TODO: Take expr into account

        final Type funcSig = this.visitFuncSig(ctx.sig);
        this.locals.exit();

        try {
            if (funcSig instanceof FunctionType) {
                bank.setSimpleType((FunctionType) funcSig);
            } else {
                @SuppressWarnings("unchecked")
                final ParametricType<FunctionType> pt = (ParametricType<FunctionType>) funcSig;
                bank.addParametricType(pt);
                this.definedTypes.exit();
            }
        } catch (DuplicateDefinitionException ex) {
            throw new DuplicateDefinitionException("Duplicate function name: " + name, ex);
        }

        return null;
    }

    private synchronized TypeBank<Type> getFromDefinedTypes(String name) {
        return this.definedTypes.getOrInit(name, TypeBank::new);
    }

    private synchronized TypeBank<FunctionType> getFromDefinedFunctions(String name) {
        return this.definedFunctions.getOrInit(name, TypeBank::new);
    }

    private Type getTypeSignature(ParseTree ctx) {
        final Type t = (Type) this.visit(ctx);
        if (t == null) {
            throw new NullPointerException("Null type should not happen (probably a syntax error): " + ctx.getText());
        }
        return t;
    }

    private TypeRestriction getTypeRestriction(SiParser.GenericParamContext ctx) {
        final TypeRestriction t = (TypeRestriction) this.visit(ctx);
        if (t == null) {
            throw new NullPointerException(
                    "Null type restriction should not happen (probably a syntax error): " + ctx.getText());
        }
        return t;
    }

    private void typeCheckFunctionBody(SiParser.DeclFuncContext ctx) {
        final String name = ctx.name.getText();
        final Type funcSig = this.visitFuncSig(ctx.sig);
        final boolean isParametric = (funcSig instanceof ParametricType);
        @SuppressWarnings("unchecked")
        final FunctionType funcType = isParametric ? ((ParametricType<FunctionType>) funcSig).getBase()
                : (FunctionType) funcSig;
        final Type resultType = funcType.getOutput();

        final Type analyzedOutput = this.getTypeSignature(ctx.e);
        if (!resultType.assignableFrom(analyzedOutput)) {
            throw new TypeMismatchException("Function: " + name + " expected output convertible to: " + resultType
                    + " but got: " + analyzedOutput);
        }

        // Exit the parameters scope
        this.locals.exit();

        if (isParametric) {
            // Exit the generic types scope
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
        final String name = ctx.name.getText();

        Type t = this.locals.get(name);
        if (t == null) {
            final TypeBank<FunctionType> bank = this.definedFunctions.get(name);

            if (bank != null) {
                // This has to be a simple type (based on grammar)
                if (bank.hasSimpleType()) {
                    t = bank.getSimpleType();
                } else if (bank.hasParametricType()) {
                    throw new TypeMismatchException("Missing type paramters for function: " + name);
                }
            }
        }
        if (t == null) {
            throw new UnboundDefinitionException("Unbound definition for binding: " + name);
        }
        return t;
    }

    @Override
    public FunctionType visitExprParametrize(SiParser.ExprParametrizeContext ctx) {
        final String name = ctx.base.getText();

        // It has to be a function (local variables cannot be parametric)
        final TypeBank<FunctionType> bank = this.definedFunctions.get(name);
        if (bank == null) {
            throw new UnboundDefinitionException("Unbound definition for function: " + name);
        }

        final List<Type> args = this.visitTypeParams(ctx.args);
        try {
            final ParametricType<FunctionType> p = bank.selectParametrization(args);
            return p.parametrize(args);
        } catch (TypeMismatchException ex) {
            throw new TypeMismatchException("Cannot parametrize function: " + name, ex);
        }
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

    private Type binaryOperatorHelper(TypeBank<Type> bank, SiParser.ExprContext lhs, SiParser.ExprContext rhs) {
        final Type lhsType = this.getTypeSignature(lhs);
        final Type rhsType = this.getTypeSignature(rhs);
        final List<Type> args = Arrays.asList(lhsType, rhsType);
        final ParametricType<Type> pt = bank.selectParametrization(args);
        return pt.parametrize(args);
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
    public Type visitExprFuncCall(SiParser.ExprFuncCallContext ctx) {
        final FunctionType f = (FunctionType) this.getTypeSignature(ctx.base);
        final Type arg = this.getTypeSignature(ctx.arg);
        if (!f.canApply(arg)) {
            throw new TypeMismatchException(
                    "Function input expected: " + f.getInput() + " but got incompatible: " + arg);
        }
        return f.getOutput();
    }
}