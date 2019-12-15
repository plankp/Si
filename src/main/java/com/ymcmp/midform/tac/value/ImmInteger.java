/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import java.util.Arrays;

import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.type.IntegerType;

public final class ImmInteger extends Value {

    public final IntegerType type;
    public final long content;

    public ImmInteger(IntegerType type, long value) {
        this.type = type;
        this.content = value;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    public int getBitWidth() {
        return this.type.getBitWidth();
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
        return Long.hashCode(this.content);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImmInteger) {
            final ImmInteger imm = (ImmInteger) obj;
            return this.content == imm.content;
        }
        return false;
    }

    @Override
    public String toString() {
        return Long.toString(this.content);
    }

    // ***** A bunch of arithmetic methods *****
    //
    // these all promote to 32 bits if less
    // or take the greater size of the two (if applicable)

    public ImmInteger not() {
        final IntegerType resultType = this.type == IntegerType.INT64 ? IntegerType.INT64 : IntegerType.INT32;
        return new ImmInteger(resultType, ~this.content);
    }

    public ImmInteger negate() {
        final IntegerType resultType = this.type == IntegerType.INT64 ? IntegerType.INT64 : IntegerType.INT32;
        return new ImmInteger(resultType, -this.content);
    }

    public ImmInteger and(ImmInteger other) {
        final int width = Math.max(this.type.getBitWidth(), other.type.getBitWidth());

        if (width <= 32) {
            return new ImmInteger(IntegerType.INT32, (int) this.content & (int) other.content);
        }
        return new ImmInteger(IntegerType.INT64, this.content & other.content);
    }

    public ImmInteger or(ImmInteger other) {
        final int width = Math.max(this.type.getBitWidth(), other.type.getBitWidth());

        if (width <= 32) {
            return new ImmInteger(IntegerType.INT32, (int) this.content | (int) other.content);
        }
        return new ImmInteger(IntegerType.INT64, this.content | other.content);
    }

    public ImmInteger xor(ImmInteger other) {
        final int width = Math.max(this.type.getBitWidth(), other.type.getBitWidth());

        if (width <= 32) {
            return new ImmInteger(IntegerType.INT32, (int) this.content ^ (int) other.content);
        }
        return new ImmInteger(IntegerType.INT64, this.content ^ other.content);
    }

    public ImmInteger add(ImmInteger other) {
        final int width = Math.max(this.type.getBitWidth(), other.type.getBitWidth());

        if (width <= 32) {
            return new ImmInteger(IntegerType.INT32, (int) this.content + (int) other.content);
        }
        return new ImmInteger(IntegerType.INT64, this.content + other.content);
    }

    public ImmInteger sub(ImmInteger other) {
        final int width = Math.max(this.type.getBitWidth(), other.type.getBitWidth());

        if (width <= 32) {
            return new ImmInteger(IntegerType.INT32, (int) this.content - (int) other.content);
        }
        return new ImmInteger(IntegerType.INT64, this.content - other.content);
    }

    public ImmInteger mul(ImmInteger other) {
        final int width = Math.max(this.type.getBitWidth(), other.type.getBitWidth());

        if (width <= 32) {
            return new ImmInteger(IntegerType.INT32, (int) this.content * (int) other.content);
        }
        return new ImmInteger(IntegerType.INT64, this.content * other.content);
    }

    public ImmInteger div(ImmInteger other) {
        final int width = Math.max(this.type.getBitWidth(), other.type.getBitWidth());

        if (width <= 32) {
            return new ImmInteger(IntegerType.INT32, (int) this.content / (int) other.content);
        }
        return new ImmInteger(IntegerType.INT64, this.content / other.content);
    }

    public ImmInteger mod(ImmInteger other) {
        final int width = Math.max(this.type.getBitWidth(), other.type.getBitWidth());

        if (width <= 32) {
            return new ImmInteger(IntegerType.INT32, (int) this.content % (int) other.content);
        }
        return new ImmInteger(IntegerType.INT64, this.content % other.content);
    }
}