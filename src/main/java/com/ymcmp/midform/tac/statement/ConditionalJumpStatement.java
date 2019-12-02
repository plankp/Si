/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.value.Value;

import java.util.Objects;

public final class ConditionalJumpStatement extends BranchStatement {

    // Maybe migrate to a full-blown class later?
    public enum CondionalOperator {
        EQ_II, NE_II, LT_II, GT_II,
        EQ_DD, NE_DD, LT_DD, GT_DD,
        EQ_CC, NE_CC, LT_CC, GT_CC,
        EQ_SS, NE_SS, LT_SS, GT_SS,
        EQ_UU,
        EQ_ZZ;

        @Override
        public String toString() {
            return this.name().toLowerCase().replace("_", ".");
        }
    }

    public final CondionalOperator operator;
    public final Block next;
    public final Value lhs;
    public final Value rhs;

    public ConditionalJumpStatement(CondionalOperator operator, Block next, Value lhs, Value rhs) {
        this.operator = operator;
        this.next = Objects.requireNonNull(next);
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public boolean isPure() {
        // All conditional operators defined here are pure
        return true;
    }

    @Override
    public String toString() {
        return operator.toString() + ' ' + next.name;
    }
}