/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type.restriction;

import com.ymcmp.si.lang.type.Type;

public abstract class TypeRestriction {

    public final String name;

    private final GenericType type;

    TypeRestriction(String name) {
        this.name = name;
        this.type = new GenericType();
    }

    public final String getName() {
        return this.name;
    }

    public final Type getAssociatedType() {
        return this.type;
    }

    public abstract boolean isValidType(Type t);

    protected final class GenericType implements Type {

        public TypeRestriction getAssociatedRestriction() {
            return TypeRestriction.this;
        }

        public String getName() {
            return TypeRestriction.this.name;
        }

        @Override
        public boolean assignableFrom(Type t) {
            return this.equivalent(t);
        }

        @Override
        public boolean equivalent(Type t) {
            return this == t;
        }

        @Override
        public Type substitute(Type from, Type to) {
            return this.equivalent(from) ? to : this;
        }

        @Override
        public int hashCode() {
            return this.getName().hashCode();
        }

        @Override
        public boolean equals(Object t) {
            if (t instanceof GenericType) {
                return this.getName().equals(((GenericType) t).getName());
            }
            return false;
        }

        @Override
        public String toString() {
            return this.getName();
        }
    }
}
