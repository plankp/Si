/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import com.ymcmp.midform.tac.Subroutine;

import java.util.Objects;

public abstract class FuncRef extends Value {

    private FuncRef() {
    }

    public static final class Native extends FuncRef {

        public final String name;

        public Native(String name) {
            this.name = Objects.requireNonNull(name);
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Native) {
                final Native imm = (Native) obj;
                return this.name.equals(imm.name);
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
            return this.sub.name;
        }
    }
}