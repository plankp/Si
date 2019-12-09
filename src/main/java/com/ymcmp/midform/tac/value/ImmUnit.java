/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.type.UnitType;

public final class ImmUnit extends Value {

    public static final ImmUnit INSTANCE = new ImmUnit();

    private ImmUnit() {
        // Singleton
    }

    @Override
    public Type getType() {
        return UnitType.INSTANCE;
    }

    @Override
    public Value replaceBinding(Binding binding, Value t) {
        return this;
    }

    @Override
    public boolean isCompileTimeConstant() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public String toString() {
        return "()";
    }
}