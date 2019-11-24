/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

import com.ymcmp.si.lang.type.restriction.GenericParameter;

public final class NomialType implements Type {

    public final String name;

    public NomialType(String name) {
        name = safeTrim(name);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Type name cannot be empty or null");
        }

        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean assignableFrom(Type t) {
        return this.equivalent(t);
    }

    @Override
    public boolean equivalent(Type t) {
        return this.equals(t);
    }

    @Override
    public NomialType substitute(GenericParameter from, Type to) {
        return this;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof NomialType) {
            return this.name.equals(((NomialType) t).name);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static String safeTrim(String str) {
        return str == null ? null : str.trim();
    }
}