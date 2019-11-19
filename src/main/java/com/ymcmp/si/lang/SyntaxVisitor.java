/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ymcmp.si.lang.grammar.SiBaseVisitor;
import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.FunctionType;
import com.ymcmp.si.lang.type.NomialType;
import com.ymcmp.si.lang.type.TupleType;
import com.ymcmp.si.lang.type.Type;
import com.ymcmp.si.lang.type.TypeDelegate;

public class SyntaxVisitor extends SiBaseVisitor<Object> {

    public static final TupleType UNIT_TYPE = new TupleType(null);

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

    private final Map<String, Type> userDefinedTypes = new HashMap<>();

    @Override
    public Object visitFile(SiParser.FileContext ctx) {
        final Object obj = super.visitFile(ctx);

        System.err.println("Debug:");
        System.err.println("- User defined types: " + userDefinedTypes);

        return obj;
    }

    @Override
    public Object visitDeclTypeAlias(SiParser.DeclTypeAliasContext ctx) {
        if (ctx.generic != null) {
            throw new UnsupportedOperationException("Generic bound is not supported yet: " + ctx.getText());
        }

        final SiParser.DeclVarContext binding = ctx.var;
        final String name = binding.name.getText();
        final Type type = getTypeSignature(binding.type);

        final Type prev = this.userDefinedTypes.get(name);
        if (prev != null) {
            throw new DuplicateDefinitionException("Duplicate definition of type: " + name + " as: " + prev);
        }
        this.userDefinedTypes.put(name, type);
        return null;
    }

    @Override
    public Object visitDeclNewType(SiParser.DeclNewTypeContext ctx) {
        if (ctx.generic != null) {
            throw new UnsupportedOperationException("Generic bound is not supported yet: " + ctx.getText());
        }

        final SiParser.DeclVarContext binding = ctx.var;
        final String name = binding.name.getText();
        final Type type = getTypeSignature(binding.type);

        final Type prev = this.userDefinedTypes.get(name);
        if (prev != null) {
            throw new DuplicateDefinitionException("Duplicate definition of type: " + name + " as: " + prev);
        }
        this.userDefinedTypes.put(name, new TypeDelegate(name, type));
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
    public TupleType visitCoreTuple(SiParser.CoreTupleContext ctx) {
        if (ctx.el == null) {
            return UNIT_TYPE;
        }

        return new TupleType(visitTypeSeq(ctx.el));
    }

    @Override
    public FunctionType visitCoreFunc(SiParser.CoreFuncContext ctx) {
        final Type input = this.getTypeSignature(ctx.in);
        final Type output = this.getTypeSignature(ctx.out);

        return new FunctionType(input, output);
    }

    @Override
    public List<Type> visitTypeSeq(SiParser.TypeSeqContext ctx) {
        return ctx.t.stream().map(this::getTypeSignature).collect(Collectors.toList());
    }

    private Type getTypeSignature(SiParser.CoreTypesContext ctx) {
        return (Type) this.visit(ctx);
    }
}