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

    protected Subroutine sub;

    private InstantiatedFunction(T ast) {
        this.ast = Objects.requireNonNull(ast);
    }

    public final String getSimpleName() {
        return this.sub.getSimpleName();
    }

    public final String getName() {
        return this.sub.getName();
    }

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
        return this.sub.getNamespace();
    }

    public final boolean isExported() {
        return this.sub.export;
    }

    @Override
    public String toString() {
        return this.getName() + ' ' + this.getType();
    }

    public static final class Native extends InstantiatedFunction<SiParser.DeclNativeFuncContext> {

        public Native(SiParser.DeclNativeFuncContext ast, FunctionType type, String ns, boolean global) {
            super(ast);

            this.sub = new Subroutine(ns, ast.name.getText(), type, false, global);
        }

        @Override
        public int hashCode() {
            // Note: sub does not participate
            return (this.ast.hashCode() * 17 + this.sub.type.hashCode()) * 17 + this.getNamespace().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // Note: sub does not participate
            if (obj instanceof Native) {
                final Native ifunc = (Native) obj;
                return this.isExported() == ifunc.isExported()
                    && this.ast.equals(ifunc.ast)
                    && this.sub.type.equals(ifunc.sub.type)
                    && this.getNamespace().equals(ifunc.getNamespace());
            }
            return false;
        }
    }

    public static final class Local extends InstantiatedFunction<SiParser.DeclFuncContext> {

        private final Map<String, Type> subMap;

        public Local(SiParser.DeclFuncContext ast, FunctionType type, String ns, boolean global) {
            this(ast, type, ns, null, global);
        }

        public Local(SiParser.DeclFuncContext ast, FunctionType type, String ns, Map<String, Type> subMap, boolean global) {
            super(ast);

            this.subMap = subMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(subMap);
            this.sub = new Subroutine(ns, ast.name.getText(), type, ast.evalImm != null, global);
            this.sub.setTypeParameters(this.subMap.values().stream().collect(Collectors.toList()));
        }

        public Map<String, Type> getParametrization() {
            return this.subMap;
        }

        @Override
        public int hashCode() {
            // Note: sub does not participate
            return ((this.ast.hashCode() * 17 + this.sub.type.hashCode()) * 17 + this.subMap.hashCode()) * 17 + this.getNamespace().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // Note: sub does not participate
            if (obj instanceof Local) {
                final Local ifunc = (Local) obj;
                return this.isExported() == ifunc.isExported()
                    && this.ast.equals(ifunc.ast)
                    && this.sub.type.equals(ifunc.sub.type)
                    && this.subMap.equals(ifunc.subMap)
                    && this.getNamespace().equals(ifunc.getNamespace());
            }
            return false;
        }
    }
}