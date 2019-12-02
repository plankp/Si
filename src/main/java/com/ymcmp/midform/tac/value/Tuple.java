/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import java.util.Arrays;

public final class Tuple extends Value {

    public final Value[] values;

    public Tuple(Value... values) {
        this.values = values;
    }

    @Override
    public int hashCode() {
        return this.values.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tuple) {
            final Tuple tpl = (Tuple) obj;
            return Arrays.equals(this.values, tpl.values);
        }
        return false;
    }

    @Override
    public String toString() {
        final String str = Arrays.toString(this.values);
        return '(' + str.substring(1, str.length() - 1) + ')';
    }
}