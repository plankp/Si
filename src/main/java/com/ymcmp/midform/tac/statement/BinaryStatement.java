/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import com.ymcmp.midform.tac.value.Temporary;
import com.ymcmp.midform.tac.value.Value;

public class BinaryStatement implements Statement {

    // Maybe migrate to a full-blown class later?
    public enum BinaryOperator {
        AND_II, OR_II, XOR_II,
        ADD_II, SUB_II, MUL_II, DIV_II, MOD_II, CMP_II,
        ADD_DD, SUB_DD, MUL_DD, DIV_DD, MOD_DD, CMP_DD,
        CMP_CC,
        CMP_SS;

        @Override
        public String toString() {
            return this.name().toLowerCase().replace("_", ".");
        }
    }

    public final BinaryOperator operator;
    public final Temporary dst;
    public final Value lhs;
    public final Value rhs;

    public BinaryStatement(BinaryOperator operator, Temporary dst, Value lhs, Value rhs) {
        this.operator = operator;
        this.dst = dst;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public boolean isPure() {
        // All binary operators defined here are pure
        return true;
    }

    @Override
    public String toString() {
        return operator.toString() + ' ' + dst + ", " + lhs + ", " + rhs;
    }
}