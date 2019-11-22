/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ymcmp.si.lang.grammar.SiBaseVisitor;
import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.FunctionType;
import com.ymcmp.si.lang.type.NomialType;
import com.ymcmp.si.lang.type.ParametricType;
import com.ymcmp.si.lang.type.TupleType;
import com.ymcmp.si.lang.type.Type;
import com.ymcmp.si.lang.type.UnitType;
import com.ymcmp.si.lang.type.VariantType;
import com.ymcmp.si.lang.type.restriction.AssignableFromRestriction;
import com.ymcmp.si.lang.type.restriction.AssignableToRestriction;
import com.ymcmp.si.lang.type.restriction.EquivalenceRestriction;
import com.ymcmp.si.lang.type.restriction.TypeRestriction;
import com.ymcmp.si.lang.type.restriction.UnboundedRestriction;

import org.antlr.v4.runtime.tree.ParseTree;

public class SyntaxVisitor extends SiBaseVisitor<Object> {

    public static final Map<String, Type> PRIMITIVE_TYPES;

    static {
        final HashMap<String, Type> map = new HashMap<>();
        map.put("int", new NomialType("int"));
        map.put("double", new NomialType("double"));
        map.put("bool", new NomialType("bool"));
        map.put("char", new NomialType("char"));
        map.put("string", new NomialType("string"));

        PRIMITIVE_TYPES = Collections.unmodifiableMap(map);
    }

    private final Scope<String, Type> userDefinedTypes = new Scope<>();
    private final Scope<String, Type> userDefinedFunctions = new Scope<>();
    private final Scope<String, Type> locals = new Scope<>();

    public Scope<String, Type> getUserDefinedTypes() {
        return this.userDefinedTypes;
    }

    public Scope<String, Type> getUserDefinedFunctions() {
        return this.userDefinedFunctions;
    }

    public void visitFileHelper(SiParser.FileContext ctx, boolean unshift) {
        this.userDefinedTypes.enter();
        this.userDefinedFunctions.enter();

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

        if (unshift) {
            this.userDefinedTypes.exit();
            this.userDefinedFunctions.exit();
        }
    }

    @Override
    public Object visitFile(SiParser.FileContext ctx) {
        this.visitFileHelper(ctx, true);
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

        this.userDefinedTypes.enter();
        final List<TypeRestriction> bound = this.visitDeclGeneric(generic);
        for (final TypeRestriction e : bound) {
            final String name = e.getName();
            final Type prev = this.userDefinedTypes.getCurrent(name);
            if (prev != null) {
                throw new DuplicateDefinitionException("Duplicate type parameter: " + name + " as: " + prev);
            }
            this.userDefinedTypes.put(name, e.getAssociatedType());
        }

        final Type ret = new ParametricType(this.getTypeSignature(rawType), bound);
        this.userDefinedTypes.exit();
        return ret;
    }

    @Override
    public Object visitDeclTypeAlias(SiParser.DeclTypeAliasContext ctx) {
        final String name = ctx.name.getText();

        final Type type = this.typeDeclarationHelper(ctx.generic, ctx.type);
        final Type prev = this.userDefinedTypes.get(name);
        if (prev != null) {
            throw new DuplicateDefinitionException("Duplicate definition of type: " + name + " as: " + prev);
        }
        this.userDefinedTypes.put(name, type);
        return null;
    }

    @Override
    public Type visitCoreNomialType(SiParser.CoreNomialTypeContext ctx) {
        final String type = ctx.getText();
        final Type t = PRIMITIVE_TYPES.get(type);

        if (t == null) {
            throw new AssertionError("Unhandled primitive type: " + type);
        }
        return t;
    }

    @Override
    public Type visitUserDefType(SiParser.UserDefTypeContext ctx) {
        final String type = ctx.getText();
        final Type t = this.userDefinedTypes.get(type);

        if (t == null) {
            throw new UnboundDefinitionException("Unbound definition for type: " + type);
        }
        return t;
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
    public Type visitVariantLevel(SiParser.VariantLevelContext ctx) {
        final List<Type> seq = ctx.t.stream().map(this::visitExtensionLevel).collect(Collectors.toList());
        if (seq.size() == 1) {
            return seq.get(0);
        }
        return new VariantType(seq);
    }

    @Override
    public Type visitParametrizeGeneric(SiParser.ParametrizeGenericContext ctx) {
        final Type base = this.getTypeSignature(ctx.base);
        if (ctx.args == null || ctx.args.isEmpty()) {
            return base;
        }

        final ParametricType parametricBase = (ParametricType) base;
        final List<Type> args = ctx.args.stream().map(this::getTypeSignature).collect(Collectors.toList());
        return parametricBase.parametrize(args);
    }

    @Override
    public Type visitCoreTypes(SiParser.CoreTypesContext ctx) {
        final Type input = this.getTypeSignature(ctx.in);
        if (ctx.out == null) {
            return input;
        }

        return new FunctionType(input, this.getTypeSignature(ctx.out));
    }

    @Override
    public Type visitDeclVar(SiParser.DeclVarContext ctx) {
        final String name = ctx.name.getText();
        final Type prev = this.locals.get(name);
        if (prev != null) {
            throw new DuplicateDefinitionException("Duplicate local of binding: " + name + " as: " + prev);
        }

        final Type type = this.getTypeSignature(ctx.type);
        locals.put(name, type);

        return type;
    }

    @Override
    public Object visitDeclFunc(SiParser.DeclFuncContext ctx) {
        final SiParser.FuncSigContext sig = ctx.sig;
        final String name = ctx.name.getText();

        final Type prev = this.userDefinedFunctions.get(name);
        if (prev != null) {
            throw new DuplicateDefinitionException("Duplicate function name: " + name + " as: " + prev);
        }

        // ignore expr specified right now

        final List<TypeRestriction> bound;
        if (ctx.generic == null) {
            bound = null;
        } else {
            this.userDefinedTypes.enter();
            bound = this.visitDeclGeneric(ctx.generic);
            for (TypeRestriction e : bound) {
                this.userDefinedTypes.put(e.getName(), e.getAssociatedType());
            }
        }

        final List<Type> rawIn = sig.in.stream().map(e -> this.getTypeSignature(e.type)).collect(Collectors.toList());
        final Type out = this.getTypeSignature(sig.out);

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

        final Type synthesized = new FunctionType(in, out);

        if (ctx.generic == null) {
            this.userDefinedFunctions.put(name, synthesized);
        } else {
            this.userDefinedFunctions.put(name, new ParametricType(synthesized, bound));
            this.userDefinedTypes.exit();
        }

        return null;
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
}