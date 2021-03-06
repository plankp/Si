/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.LinkedHashMap;
import java.util.List;

import com.ymcmp.midform.tac.type.FunctionType;
import com.ymcmp.midform.tac.type.Type;

import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.ParametricType;

public final class ParametricFunction {

    private final SiParser.DeclFuncContext ast;
    private final ParametricType<FunctionType> type;
    private final String ns;
    private final boolean global;

    public ParametricFunction(SiParser.DeclFuncContext ast, ParametricType<FunctionType> type, String ns, boolean global) {
        this.ast = ast;
        this.type = type;
        this.ns = ns;
        this.global = global;
    }

    public SiParser.DeclFuncContext getSyntaxTree() {
        return this.ast;
    }

    public ParametricType<FunctionType> getType() {
        return this.type;
    }

    public String getNamespace() {
        return this.ns;
    }

    public final boolean isExported() {
        return this.global;
    }

    public InstantiatedFunction.Local instantiateTypes(List<Type> types) {
        final FunctionType ft = this.type.parametrize(types);
        final LinkedHashMap<String, Type> repl = new LinkedHashMap<>();
        final int limit = this.type.numberOfTypeRestrictions();
        for (int i = 0; i < limit; ++i) {
            repl.put(this.type.getTypeRestrictionAt(i).getName(), types.get(i));
        }
        return new InstantiatedFunction.Local(this.ast, ft, this.ns, repl, this.global);
    }

    @Override
    public int hashCode() {
        return (this.ast.hashCode() * 17 + this.type.hashCode()) * 17 + this.ns.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ParametricFunction) {
            final ParametricFunction pfunc = (ParametricFunction) obj;
            return this.global == pfunc.global
                && this.ast.equals(pfunc.ast)
                && this.type.equals(pfunc.type)
                && this.ns.equals(pfunc.ns);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.ns + '\\' + this.ast.name.getText() + type;
    }
}