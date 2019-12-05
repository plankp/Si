/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

import static com.ymcmp.midform.tac.type.NomialType.safeTrim;

import java.util.Optional;

import com.ymcmp.midform.tac.type.Type;

public final class FreeType implements ExtensionType {

    public final String name;
    public final Optional<Type> bound;

    public FreeType(String name) {
        this(name, null);
    }

    public FreeType(String name, Type bound) {
        name = safeTrim(name);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Type name cannot be empty or null");
        }

        this.name = name;
        this.bound = Optional.ofNullable(bound);
    }

    public String getName() {
        return this.name;
    }

    public Optional<Type> getBound() {
        return this.bound;
    }

    @Override
    public Type expandBound() {
        return this.bound.orElse(this);
    }

    @Override
    public boolean assignableFrom(final Type t) {
        // If there is a bound, we check if it is compatible
        // If there isn't, then type has no restriction, assignable from all types
        return this.bound.map(e -> e.assignableFrom(t)).orElse(true);
    }

    @Override
    public boolean equivalent(Type t) {
        return t == this;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 17 + this.bound.hashCode();
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof FreeType) {
            final FreeType ft = (FreeType) t;
            return this.name.equals(ft.name) && this.bound.equals(ft.bound);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.name + this.bound.map(e -> "::" + e).orElse("");
    }
}