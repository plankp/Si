/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import com.ymcmp.si.lang.type.Type;

public final class TypeBinding {

    public enum Mutability {
        RUNTIME_FIXED("val"), RUNTIME_MUTABLE("var");

        final String str;

        Mutability(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return this.str;
        }
    };

    public final Type type;
    public final Mutability mut;

    public TypeBinding(Type type, Mutability mut) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        if (mut == null) {
            throw new IllegalArgumentException("Mutability level cannot be null");
        }

        this.type = type;
        this.mut = mut;
    }

    public Type getType() {
        return this.type;
    }

    public Mutability getMutabilityLevel() {
        return this.mut;
    }

    public boolean assignableFrom(TypeBinding binding) {
        return this.mut.ordinal() >= binding.mut.ordinal() && this.type.assignableFrom(binding.type);
    }

    @Override
    public int hashCode() {
        return this.type.hashCode() * 17 + this.type.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TypeBinding) {
            final TypeBinding tb = (TypeBinding) obj;
            return this.mut == tb.mut && this.type.equals(tb.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.mut.toString() + ' ' + this.type.toString();
    }
}