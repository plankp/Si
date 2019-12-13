/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.type.IntegerType;

public final class ImmByte extends Value {

    public static final IntegerType TYPE = new IntegerType(8);

    public final byte content;

    public ImmByte(byte content) {
        this.content = content;
    }

    @Override
    public Type getType() {
        return TYPE;
    }

    @Override
    public Value replaceBinding(Binding binding, Value t) {
        return this;
    }

    @Override
    public boolean containsLocalBinding() {
        return false;
    }

    @Override
    public boolean isCompileTimeConstant() {
        return true;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.content);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImmByte) {
            final ImmByte imm = (ImmByte) obj;
            return this.content == imm.content;
        }
        return false;
    }

    @Override
    public String toString() {
        return Byte.toString(this.content);
    }
}