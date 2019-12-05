/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import static com.ymcmp.midform.tac.type.Types.equivalent;

import java.util.Set;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.value.Binding;
import com.ymcmp.midform.tac.type.*;
import com.ymcmp.midform.tac.value.*;

public class UnaryStatement implements Statement {

    // Maybe migrate to a full-blown class later?
    public enum UnaryOperator {
        NOT_I, NEG_I, POS_I,
        NOT_Z, NEG_D, POS_D,
        I2D, D2I;

        public boolean isTypeValid(Type out, Type src) {
            switch (this) {
            case NOT_I:
            case NEG_I:
            case POS_I:
                return equivalent(ImmInteger.TYPE, out)
                    && equivalent(ImmInteger.TYPE, src);
            case NOT_Z:
                return equivalent(ImmBoolean.TYPE, out)
                    && equivalent(ImmBoolean.TYPE, src);
            case NEG_D:
            case POS_D:
                return equivalent(ImmDouble.TYPE, out)
                    && equivalent(ImmDouble.TYPE, src);
            case I2D:
                return equivalent(ImmDouble.TYPE, out)
                    && equivalent(ImmInteger.TYPE, src);
            case D2I:
                return equivalent(ImmInteger.TYPE, out)
                    && equivalent(ImmDouble.TYPE, src);
            default:
                throw new AssertionError("Unhandled unary operator " + this.toString());
            }
        }

        @Override
        public String toString() {
            return this.name().toLowerCase().replace("_", ".");
        }
    }

    public final UnaryOperator operator;
    public final Binding dst;
    public final Value src;

    public UnaryStatement(UnaryOperator operator, Binding dst, Value src) {
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
    public void validateType(Subroutine s) {
        if (!this.operator.isTypeValid(this.dst.getType(), this.src.getType())) {
            throw new RuntimeException("Unary operator " + this.operator + " type mismatch");
        }
    }

    @Override
    public void reachBlock(Set<Block> marked) {
        // No blocks to trace
    }

    @Override
    public String toString() {
        return operator.toString() + ' ' + dst + ", " + src;
    }
}