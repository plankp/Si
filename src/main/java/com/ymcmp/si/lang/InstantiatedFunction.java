/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.type.FunctionType;
import com.ymcmp.midform.tac.type.Type;

import com.ymcmp.si.lang.grammar.SiParser;

import org.antlr.v4.runtime.tree.ParseTree;

public abstract class InstantiatedFunction<T extends ParseTree> {

    protected final T ast;
    protected final String ns;

    protected Subroutine sub;

    private InstantiatedFunction(T ast, String ns) {
        this.ast = Objects.requireNonNull(ast);
        this.ns = Objects.requireNonNull(ns);
    }

    public abstract String getSimpleName();
    public abstract String getName();

    public final T getSyntaxTree() {
        return this.ast;
    }

    public final Subroutine getSubroutine() {
        return this.sub;
    }

    public final FunctionType getType() {
        return this.sub.type;
    }

    public final String getNamespace() {
        return this.ns;
    }

    @Override
    public String toString() {
        return this.getName() + ' ' + this.getType();
    }

    public static final class Native extends InstantiatedFunction<SiParser.DeclNativeFuncContext> {

        public Native(SiParser.DeclNativeFuncContext ast, FunctionType type, String ns) {
            super(ast, ns);

            this.sub = new Subroutine(this.getName(), type, false);
        }

        @Override
        public String getSimpleName() {
            return this.ns + '\\' + this.ast.name.getText();
        }

        @Override
        public String getName() {
            // no type parameters, so simple and full name is the same
            return this.getSimpleName();
        }

        @Override
        public int hashCode() {
            // Note: sub does not participate
            return (this.ast.hashCode() * 17 + this.sub.type.hashCode()) * 17 + this.ns.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // Note: sub does not participate
            if (obj instanceof Native) {
                final Native ifunc = (Native) obj;
                return this.ast.equals(ifunc.ast)
                    && this.sub.type.equals(ifunc.sub.type)
                    && this.ns.equals(ifunc.ns);
            }
            return false;
        }
    }

    public static final class Local extends InstantiatedFunction<SiParser.DeclFuncContext> {

        private final Map<String, Type> subMap;

        public Local(SiParser.DeclFuncContext ast, FunctionType type, String ns) {
            this(ast, type, ns, null);
        }

        public Local(SiParser.DeclFuncContext ast, FunctionType type, String ns, Map<String, Type> subMap) {
            super(ast, ns);

            this.subMap = subMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(subMap);
            this.sub = new Subroutine(this.getName(), type, ast.evalImm != null);
        }

        public Map<String, Type> getParametrization() {
            return this.subMap;
        }

        @Override
        public String getSimpleName() {
            return this.ns + '\\' + this.ast.name.getText();
        }

        @Override
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
            if (obj instanceof Local) {
                final Local ifunc = (Local) obj;
                return this.ast.equals(ifunc.ast)
                    && this.sub.type.equals(ifunc.sub.type)
                    && this.subMap.equals(ifunc.subMap)
                    && this.ns.equals(ifunc.ns);
            }
            return false;
        }
    }
}