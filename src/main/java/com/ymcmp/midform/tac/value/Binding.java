/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import java.util.Objects;

import com.ymcmp.midform.tac.type.Type;

public abstract class Binding extends Value {

    public final String name;
    public final Type type;
    public final int scopeDepth;

    private Binding(String name, int scopeDepth, Type t) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(t);
        this.scopeDepth = scopeDepth;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public int hashCode() {
        return (this.name.hashCode() * 17 + this.type.hashCode()) * 17 + this.scopeDepth;
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
        return this.scopeDepth == lbl.scopeDepth
            && this.name.equals(lbl.name)
            && this.type.equals(lbl.type);
    }

    @Override
    public String toString() {
        return this.name + '_' + this.scopeDepth;
    }

    public static final class Immutable extends Binding {

        public Immutable(String name, Type t) {
            super(name, 0, t);
        }

        public Immutable(String name, int scopeDepth, Type t) {
            super(name, scopeDepth, t);
        }

        @Override
        public boolean containsLocalBinding() {
            return true;
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
            super(name, 0, t);
        }

        public Parameter(String name, int scopeDepth, Type t) {
            super(name, scopeDepth, t);
        }

        public Parameter changeType(Type newType) {
            return new Parameter(this.name, this.scopeDepth, newType);
        }

        @Override
        public boolean containsLocalBinding() {
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Parameter) {
                return equalsHelper((Binding) obj);
            }
            return false;
        }
    }

    public static final class Mutable extends Binding {

        public Mutable(String name, Type t) {
            super(name, 0, t);
        }

        public Mutable(String name, int scopeDepth, Type t) {
            super(name, scopeDepth, t);
        }

        @Override
        public boolean containsLocalBinding() {
            return true;
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