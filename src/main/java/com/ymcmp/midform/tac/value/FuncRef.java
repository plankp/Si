/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import java.util.Objects;

import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.type.FunctionType;
import com.ymcmp.midform.tac.type.Type;

public abstract class FuncRef extends Value {

    @Override
    public final Value replaceBinding(Binding binding, Value t) {
        return this;
    }

    @Override
    public boolean containsLocalBinding() {
        return false;
    }

    public static final class Native extends FuncRef {

        public final String name;
        public final FunctionType type;

        public Native(String name, FunctionType type) {
            this.name = Objects.requireNonNull(name);
            this.type = Objects.requireNonNull(type);
        }

        @Override
        public Type getType() {
            return this.type;
        }

        @Override
        public boolean isCompileTimeConstant() {
            // Native function references depend on the runtime platform.
            // Therefore, it is *not* a compile-time constant
            return false;
        }

        @Override
        public int hashCode() {
            return this.type.hashCode() * 17 + this.name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Native) {
                final Native imm = (Native) obj;
                return this.type.equals(imm.type)
                    && this.name.equals(imm.name);
            }
            return false;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static final class Local extends FuncRef {

        public final Subroutine sub;

        public Local(Subroutine sub) {
            this.sub = Objects.requireNonNull(sub);
        }

        @Override
        public Type getType() {
            return this.sub.type;
        }

        @Override
        public boolean isCompileTimeConstant() {
            return true;
        }

        @Override
        public int hashCode() {
            return this.sub.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Local) {
                final Local imm = (Local) obj;
                return this.sub.equals(imm.sub);
            }
            return false;
        }

        @Override
        public String toString() {
            return this.sub.getName();
        }
    }
}