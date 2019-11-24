/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

public final class UnitType implements Type {

    public static final UnitType INSTANCE = new UnitType();

    private UnitType() {
        // Singleton
    }

    @Override
    public boolean assignableFrom(Type t) {
        return t == INSTANCE;
    }

    @Override
    public boolean equivalent(Type t) {
        return t == INSTANCE;
    }

    @Override
    public String toString() {
        return "()";
    }
}