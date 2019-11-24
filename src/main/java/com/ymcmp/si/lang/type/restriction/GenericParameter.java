/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type.restriction;

import com.ymcmp.si.lang.type.Type;

public final class GenericParameter implements Type {

    private final TypeRestriction restriction;

    /* package */ GenericParameter(TypeRestriction r) {
        if (r == null) {
            throw new IllegalArgumentException("Generic parameter must have an associated type restriction");
        }

        this.restriction = r;
    }

    public TypeRestriction getAssociatedRestriction() {
        return this.restriction;
    }

    public String getName() {
        return this.restriction.name;
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
    public Type substitute(GenericParameter from, Type to) {
        return this.equivalent(from) ? to : this;
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof GenericParameter) {
            return this.getName().equals(((GenericParameter) t).getName());
        }
        return false;
    }

    @Override
    public String toString() {
        return this.getName();
    }
}