/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import com.ymcmp.midform.tac.value.Temporary;
import com.ymcmp.midform.tac.value.Value;

public class UnaryStatement implements Statement {

    // Maybe migrate to a full-blown class later?
    public enum UnaryOperator {
        NOT_I, NEG_I, POS_I,
        NOT_Z, NEG_D, POS_D,
        I2D, D2I;

        @Override
        public String toString() {
            return this.name().toLowerCase().replace("_", ".");
        }
    }

    public final UnaryOperator operator;
    public final Temporary dst;
    public final Value src;

    public UnaryStatement(UnaryOperator operator, Temporary dst, Value src) {
        this.operator = operator;
        this.dst = dst;
        this.src = src;
    }

    @Override
    public boolean isPure() {
        // All unary operators defined here are pure
        return true;
    }

    @Override
    public String toString() {
        return operator.toString() + ' ' + dst + ", " + src;
    }
}