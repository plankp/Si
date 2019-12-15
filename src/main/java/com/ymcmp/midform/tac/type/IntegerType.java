/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.type;

import com.ymcmp.midform.tac.value.ImmInteger;

public final class IntegerType extends CoreType {

    public static final IntegerType INT8  = new IntegerType(8);
    public static final IntegerType INT16 = new IntegerType(16);
    public static final IntegerType INT32 = new IntegerType(32);
    public static final IntegerType INT64 = new IntegerType(64);

    public final int width;

    private IntegerType(int width) {
        if (width < 1) {
            throw new IllegalArgumentException("Bitwidth cannot be less than 1: " + width);
        }

        this.width = width;
    }

    public int getBitWidth() {
        return this.width;
    }

    public ImmInteger createImmediate(long value) {
        return new ImmInteger(this, value);
    }

    @Override
    protected boolean assignableFrom(Type t) {
        t = t.expandBound();
        if (t instanceof IntegerType) {
            final IntegerType it = (IntegerType) t;
            return this.width >= it.width;
        }
        return false;
    }

    @Override
    protected boolean equivalent(Type t) {
        return this.equals(t.expandBound());
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.width);
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof IntegerType) {
            return this.width == ((IntegerType) t).width;
        }
        return false;
    }

    @Override
    public String toString() {
        return "int" + this.width;
    }
}