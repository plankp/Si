/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.type;

public final class UnitType extends CoreType {

    public static final UnitType INSTANCE = new UnitType();

    private UnitType() {
        // Singleton
    }

    @Override
    protected boolean assignableFrom(Type t) {
        return this.equivalent(t);
    }

    @Override
    protected boolean equivalent(Type t) {
        return t.expandBound() == this;
    }

    @Override
    public String toString() {
        return "()";
    }
}