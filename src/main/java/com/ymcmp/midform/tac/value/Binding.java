/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import java.util.Objects;

import com.ymcmp.midform.tac.type.Type;

public abstract class Binding extends Value {

    public final String name;
    public final Type type;

    private Binding(String name, Type t) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(t);
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 17 + this.type.hashCode();
    }

    @Override
    public Value replaceBinding(Binding binding, Value t) {
        return binding.equals(this) ? t : this;
    }

    @Override
    public boolean isCompileTimeConstant() {
        // Bindings alone are not compile-time constants
        // but combined with constant propagation, they could be removed
        return false;
    }

    protected final boolean equalsHelper(Binding lbl) {
        return this.name.equals(lbl.name)
            && this.type.equals(lbl.type);
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static final class Immutable extends Binding {

        public Immutable(String name, Type t) {
            super(name, t);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Immutable) {
                return equalsHelper((Binding) obj);
            }
            return false;
        }
    }

    public static final class Parameter extends Binding {

        public Parameter(String name, Type t) {
            super(name, t);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Parameter) {
                return equalsHelper((Binding) obj);
            }
            return false;
        }

        @Override
        public String toString() {
            return this.name + ' ' + this.type;
        }
    }

    public static final class Mutable extends Binding {

        public Mutable(String name, Type t) {
            super(name, t);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Mutable) {
                return equalsHelper((Binding) obj);
            }
            return false;
        }
    }
}