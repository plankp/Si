/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.type;

public final class NomialType extends CoreType {

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
    protected boolean assignableFrom(Type t) {
        return this.equivalent(t);
    }

    @Override
    protected boolean equivalent(Type t) {
        return this.equals(t.expandBound());
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