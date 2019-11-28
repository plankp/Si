/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.Collections;
import java.util.Map;

import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.FunctionType;
import com.ymcmp.si.lang.type.Type;

public final class InstantiatedFunction {

    private final SiParser.DeclFuncContext ast;
    private final FunctionType type;
    private final Map<String, Type> subMap;
    private final String ns;

    public InstantiatedFunction(SiParser.DeclFuncContext ast, FunctionType type, String ns) {
        this(ast, type, ns, null);
    }

    public InstantiatedFunction(SiParser.DeclFuncContext ast, FunctionType type, String ns, Map<String, Type> subMap) {
        this.ast = ast;
        this.type = type;
        this.subMap = subMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(subMap);
        this.ns = ns;
    }

    public SiParser.DeclFuncContext getSyntaxTree() {
        return this.ast;
    }

    public FunctionType getType() {
        return this.type;
    }

    public Map<String, Type> getParametrization() {
        return this.subMap;
    }

    public String getNamespace() {
        return this.ns;
    }

    @Override
    public int hashCode() {
        return ((this.ast.hashCode() * 17 + this.type.hashCode()) * 17 + this.subMap.hashCode()) * 17 + this.ns.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InstantiatedFunction) {
            final InstantiatedFunction ifunc = (InstantiatedFunction) obj;
            return this.ast.equals(ifunc.ast) && this.type.equals(ifunc.type) && this.subMap.equals(ifunc.subMap)
                && this.ns.equals(ifunc.ns);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.ns + '\\' + this.ast.name.getText() + ' ' + type;
    }
}