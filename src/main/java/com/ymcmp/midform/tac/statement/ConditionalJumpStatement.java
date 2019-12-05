/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import static com.ymcmp.midform.tac.type.Types.equivalent;

import java.util.Set;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.type.*;
import com.ymcmp.midform.tac.value.*;

import java.util.Objects;

public final class ConditionalJumpStatement extends BranchStatement {

    // Maybe migrate to a full-blown class later?
    public enum ConditionalOperator {
        EQ_II, NE_II, LT_II, LE_II, GE_II, GT_II,
        EQ_DD, NE_DD, LT_DD, LE_DD, GE_DD, GT_DD,
        EQ_CC, NE_CC, LT_CC, LE_CC, GE_CC, GT_CC,
        EQ_SS, NE_SS, LT_SS, LE_SS, GE_SS, GT_SS,
        EQ_UU, NE_UU,
        EQ_ZZ, NE_ZZ;

        public boolean isTypeValid(Type lhs, Type rhs) {
            switch (this) {
            case EQ_II:
            case NE_II:
            case LT_II:
            case LE_II:
            case GE_II:
            case GT_II:
                return equivalent(ImmInteger.TYPE, lhs)
                    && equivalent(ImmInteger.TYPE, rhs);
            case EQ_DD:
            case NE_DD:
            case LT_DD:
            case LE_DD:
            case GE_DD:
            case GT_DD:
                return equivalent(ImmDouble.TYPE, lhs)
                    && equivalent(ImmDouble.TYPE, rhs);
            case EQ_CC:
            case NE_CC:
            case LT_CC:
            case LE_CC:
            case GE_CC:
            case GT_CC:
                return equivalent(ImmCharacter.TYPE, lhs)
                    && equivalent(ImmCharacter.TYPE, rhs);
            case EQ_SS:
            case NE_SS:
            case LT_SS:
            case LE_SS:
            case GE_SS:
            case GT_SS:
                return equivalent(ImmString.TYPE, lhs)
                    && equivalent(ImmString.TYPE, rhs);
            case EQ_UU:
            case NE_UU:
                return equivalent(UnitType.INSTANCE, lhs)
                    && equivalent(UnitType.INSTANCE, rhs);
            case EQ_ZZ:
            case NE_ZZ:
                return equivalent(ImmBoolean.TYPE, lhs)
                    && equivalent(ImmBoolean.TYPE, rhs);
            default:
                throw new AssertionError("Unhandled conditional jump operator " + this.toString());
            }
        }
    
        @Override
        public String toString() {
            return this.name().toLowerCase().replace("_", ".");
        }
    }

    public final ConditionalOperator operator;
    public final Block next;
    public final Value lhs;
    public final Value rhs;

    public ConditionalJumpStatement(ConditionalOperator operator, Block next, Value lhs, Value rhs) {
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
    public void validateType(Subroutine s) {
        if (!this.operator.isTypeValid(this.lhs.getType(), this.rhs.getType())) {
            throw new RuntimeException("Conditional jump operator " + this.operator + " type mismatch");
        }
    }

    @Override
    public void reachBlock(Set<Block> marked) {
        this.next.trace(marked);
    }

    @Override
    public String toString() {
        return operator.toString() + ' ' + next.name + ", " + lhs + ", " + rhs;
    }
}