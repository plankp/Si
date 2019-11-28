/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.HashMap;
import java.util.List;

import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.FunctionType;
import com.ymcmp.si.lang.type.ParametricType;
import com.ymcmp.si.lang.type.Type;

public final class ParametricFunction {

    private final SiParser.DeclFuncContext ast;
    private final ParametricType<FunctionType> type;
    private final String ns;

    public ParametricFunction(SiParser.DeclFuncContext ast, ParametricType<FunctionType> type, String ns) {
        this.ast = ast;
        this.type = type;
        this.ns = ns;
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

    public InstantiatedFunction instantiateTypes(List<Type> types) {
        final FunctionType ft = this.type.parametrize(types);
        final HashMap<String, Type> repl = new HashMap<>();
        final int limit = this.type.numberOfTypeRestrictions();
        for (int i = 0; i < limit; ++i) {
            repl.put(this.type.getTypeRestrictionAt(i).getName(), types.get(i));
        }
        return new InstantiatedFunction(this.ast, ft, this.ns, repl);
    }

    @Override
    public int hashCode() {
        return (this.ast.hashCode() * 17 + this.type.hashCode()) * 17 + this.ns.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ParametricFunction) {
            final ParametricFunction pfunc = (ParametricFunction) obj;
            return this.ast.equals(pfunc.ast) && this.type.equals(pfunc.type)
                && this.ns.equals(pfunc.ns);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.ns + '\\' + this.ast.name.getText() + type;
    }
}