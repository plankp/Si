/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import static com.ymcmp.midform.tac.type.Types.equivalent;

import java.util.Map;
import java.util.Optional;

import com.ymcmp.midform.tac.BindingCounter;
import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
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
    public Optional<Binding> getResultRegister() {
        return Optional.of(this.dst);
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
    public void reachBlock(Map<Block, Integer> marked, Map<Binding, BindingCounter> bindings) {
        Statement.checkBindingDeclaration(bindings, this.src);
        Statement.bumpAssignmentCounter(bindings, this.dst);
    }

    @Override
    public Statement replaceRead(Binding binding, Value repl) {
        final Value newSrc = this.src.replaceBinding(binding, repl);
        if (newSrc != this.src) {
            return new UnaryStatement(this.operator, this.dst, newSrc);
        }
        return this;
    }

    @Override
    public Statement unfoldConstants() {
        try {
            Value result = null;
            switch (this.operator) {
            case NOT_I:
                result = new ImmInteger(~((ImmInteger) this.src).content);
                break;
            case NEG_I:
                result = new ImmInteger(-((ImmInteger) this.src).content);
                break;
            case POS_I: // +k yields k
                result = (ImmInteger) this.src;
                break;
            case NOT_Z:
                result = new ImmBoolean(!((ImmBoolean) this.src).content);
                break;
            case NEG_D:
                result = new ImmDouble(-((ImmDouble) this.src).content);
                break;
            case POS_D: // +k yields k
                result = (ImmDouble) this.src;
                break;
            case I2D:
                result = new ImmDouble(((ImmInteger) this.src).content);
                break;
            case D2I:
                result = new ImmInteger((int) ((ImmDouble) this.src).content);
                break;
            default:
                break;
            }

            if (result != null) {
                // This becomes a move statement
                return new MoveStatement(this.dst, result);
            }
        } catch (ClassCastException ex) {
            // Swallow it
        }

        // It might be something we don't know how to unfold
        // (but either way, it doesn't matter)
        return this;
    }

    @Override
    public String toString() {
        return operator.toString() + ' ' + dst + ", " + src;
    }
}