/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.type.FunctionType;
import com.ymcmp.midform.tac.type.Type;

import com.ymcmp.si.lang.grammar.SiParser;

public final class InstantiatedFunction {

    private final SiParser.DeclFuncContext ast;
    private final Map<String, Type> subMap;
    private final String ns;

    private final Subroutine sub;

    public InstantiatedFunction(SiParser.DeclFuncContext ast, FunctionType type, String ns) {
        this(ast, type, ns, null);
    }

    public InstantiatedFunction(SiParser.DeclFuncContext ast, FunctionType type, String ns, Map<String, Type> subMap) {
        this.ast = ast;
        this.subMap = subMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(subMap);
        this.ns = ns;

        this.sub = new Subroutine(this.getName(), type);
    }

    public SiParser.DeclFuncContext getSyntaxTree() {
        return this.ast;
    }

    public FunctionType getType() {
        return this.sub.type;
    }

    public Map<String, Type> getParametrization() {
        return this.subMap;
    }

    public Subroutine getSubroutine() {
        return this.sub;
    }

    public String getNamespace() {
        return this.ns;
    }

    public String getSimpleName() {
        return this.ns + '\\' + this.ast.name.getText();
    }

    public String getName() {
        final String simpleName = this.getSimpleName();
        if (this.subMap.isEmpty()) {
            return simpleName;
        }
        return simpleName + this.subMap.values().stream()
                .map(Object::toString)
                .collect(Collectors.joining(",", "{", "}"));
    }

    @Override
    public int hashCode() {
        // Note: sub does not participate
        return ((this.ast.hashCode() * 17 + this.sub.type.hashCode()) * 17 + this.subMap.hashCode()) * 17 + this.ns.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // Note: sub does not participate
        if (obj instanceof InstantiatedFunction) {
            final InstantiatedFunction ifunc = (InstantiatedFunction) obj;
            return this.ast.equals(ifunc.ast)
                && this.sub.type.equals(ifunc.sub.type)
                && this.subMap.equals(ifunc.subMap)
                && this.ns.equals(ifunc.ns);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.getName() + ' ' + this.sub.type;
    }
}