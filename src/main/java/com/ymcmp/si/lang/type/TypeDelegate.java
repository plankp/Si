/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

public final class TypeDelegate implements Type {

    public final String name;
    public final Type inner;

    public TypeDelegate(String name, Type inner) {
        name = NomialType.safeTrim(name);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Type delegate name cannot be empty or null");
        }
        if (inner == null) {
            throw new IllegalArgumentException("Cannot create a delegate to null");
        }

        this.name = name;
        this.inner = inner;
    }

    public String getName() {
        return this.name;
    }

    public Type getInner() {
        return this.inner;
    }

    @Override
    public boolean assignableFrom(Type t) {
        return this.equivalent(t);
    }

    @Override
    public boolean equivalent(Type t) {
        if (t instanceof TypeDelegate) {
            final TypeDelegate td = (TypeDelegate) t;
            return this.name.equals(td.name) && this.inner.equivalent(td.inner);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.inner.hashCode();
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof TypeDelegate) {
            final TypeDelegate td = (TypeDelegate) t;
            return this.name.equals(td.name) && this.inner.equals(td.inner);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.name;
    }
}