/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import static com.ymcmp.midform.tac.type.Types.equivalent;

import java.util.Optional;
import java.util.Set;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.value.Binding;
import com.ymcmp.midform.tac.type.*;
import com.ymcmp.midform.tac.value.*;

public class BinaryStatement implements Statement {

    // Maybe migrate to a full-blown class later?
    public enum BinaryOperator {
        AND_II, OR_II, XOR_II,
        ADD_II, SUB_II, MUL_II, DIV_II, MOD_II, CMP_II,
        ADD_DD, SUB_DD, MUL_DD, DIV_DD, MOD_DD, CMP_DD,
        CMP_CC,
        CMP_SS;

        public boolean isTypeValid(Type out, Type lhs, Type rhs) {
            switch (this) {
            case AND_II:
            case OR_II:
            case XOR_II:
            case ADD_II:
            case SUB_II:
            case MUL_II:
            case DIV_II:
            case MOD_II:
            case CMP_II:
                return equivalent(ImmInteger.TYPE, out)
                    && equivalent(ImmInteger.TYPE, lhs)
                    && equivalent(ImmInteger.TYPE, rhs);
            case ADD_DD:
            case SUB_DD:
            case MUL_DD:
            case DIV_DD:
            case MOD_DD:
                return equivalent(ImmDouble.TYPE, out)
                    && equivalent(ImmDouble.TYPE, lhs)
                    && equivalent(ImmDouble.TYPE, rhs);
            case CMP_DD:
                return equivalent(ImmInteger.TYPE, out)
                    && equivalent(ImmDouble.TYPE, lhs)
                    && equivalent(ImmDouble.TYPE, rhs);
            case CMP_CC:
                return equivalent(ImmInteger.TYPE, out)
                    && equivalent(ImmCharacter.TYPE, lhs)
                    && equivalent(ImmCharacter.TYPE, rhs);
            case CMP_SS:
                return equivalent(ImmInteger.TYPE, out)
                    && equivalent(ImmString.TYPE, lhs)
                    && equivalent(ImmString.TYPE, rhs);
            default:
                throw new AssertionError("Unhandled binary operator " + this.toString());
            }
        }

        @Override
        public String toString() {
            return this.name().toLowerCase().replace("_", ".");
        }
    }

    public final BinaryOperator operator;
    public final Binding dst;
    public final Value lhs;
    public final Value rhs;

    public BinaryStatement(BinaryOperator operator, Binding dst, Value lhs, Value rhs) {
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
    public void reachBlock(Set<Block> marked) {
        // No blocks to trace
    }

    @Override
    public void validateType(Subroutine s) {
        if (!this.operator.isTypeValid(this.dst.getType(), this.lhs.getType(), this.rhs.getType())) {
            throw new RuntimeException("Binary operator " + this.operator + " type mismatch");
        }
    }

    @Override
    public Optional<Statement> unfoldConstants() {
        try {
            Value result = null;
            switch (this.operator) {
            default:
                break;
            }

            if (result != null) {
                // This becomes a move statement
                return Optional.of(new MoveStatement(this.dst, result));
            }
        } catch (ClassCastException ex) {
            // Swallow it
        }

        // It might be something we don't know how to unfold
        // (but either way, it doesn't matter)
        return Optional.of(this);
    }

    @Override
    public String toString() {
        return operator.toString() + ' ' + dst + ", " + lhs + ", " + rhs;
    }
}