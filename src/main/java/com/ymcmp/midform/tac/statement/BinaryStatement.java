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
    public void reachBlock(Map<Block, Integer> marked, Map<Binding, BindingCounter> bindings) {
        Statement.checkBindingDeclaration(bindings, this.lhs);
        Statement.checkBindingDeclaration(bindings, this.rhs);
        Statement.bumpAssignmentCounter(bindings, this.dst);
    }

    @Override
    public void validateType(Subroutine s) {
        if (!this.operator.isTypeValid(this.dst.getType(), this.lhs.getType(), this.rhs.getType())) {
            throw new RuntimeException("Binary operator " + this.operator + " type mismatch");
        }
    }

    @Override
    public Optional<Statement> replaceRead(Binding.Immutable binding, Value repl) {
        final Value newLhs = this.lhs.replaceBinding(binding, repl);
        final Value newRhs = this.rhs.replaceBinding(binding, repl);
        // Check if any of the sources has been changed
        if (newLhs != this.lhs || newRhs != this.rhs) {
            return Optional.of(new BinaryStatement(this.operator, this.dst, newLhs, newRhs));
        }
        return Optional.of(this);
    }

    @Override
    public Optional<Statement> unfoldConstants() {
        try {
            Value result = null;
            switch (this.operator) {
            case AND_II:
                result = new ImmInteger(((ImmInteger) this.lhs).content & ((ImmInteger) this.rhs).content);
                break;
            case OR_II:
                result = new ImmInteger(((ImmInteger) this.lhs).content | ((ImmInteger) this.rhs).content);
                break;
            case XOR_II:
                result = new ImmInteger(((ImmInteger) this.lhs).content ^ ((ImmInteger) this.rhs).content);
                break;
            case ADD_II:
                result = new ImmInteger(((ImmInteger) this.lhs).content + ((ImmInteger) this.rhs).content);
                break;
            case SUB_II:
                result = new ImmInteger(((ImmInteger) this.lhs).content - ((ImmInteger) this.rhs).content);
                break;
            case MUL_II:
                result = new ImmInteger(((ImmInteger) this.lhs).content * ((ImmInteger) this.rhs).content);
                break;
            case DIV_II:
                result = new ImmInteger(((ImmInteger) this.lhs).content / ((ImmInteger) this.rhs).content);
                break;
            case MOD_II:
                result = new ImmInteger(((ImmInteger) this.lhs).content % ((ImmInteger) this.rhs).content);
                break;
            case CMP_II:
                result = new ImmInteger(Integer.compare(((ImmInteger) this.lhs).content, ((ImmInteger) this.rhs).content));
                break;
            case ADD_DD:
                result = new ImmDouble(((ImmDouble) this.lhs).content + ((ImmDouble) this.rhs).content);
                break;
            case SUB_DD:
                result = new ImmDouble(((ImmDouble) this.lhs).content - ((ImmDouble) this.rhs).content);
                break;
            case MUL_DD:
                result = new ImmDouble(((ImmDouble) this.lhs).content * ((ImmDouble) this.rhs).content);
                break;
            case DIV_DD:
                result = new ImmDouble(((ImmDouble) this.lhs).content / ((ImmDouble) this.rhs).content);
                break;
            case MOD_DD:
                result = new ImmDouble(((ImmDouble) this.lhs).content % ((ImmDouble) this.rhs).content);
                break;
            case CMP_DD:
                result = new ImmInteger(Double.compare(((ImmDouble) this.lhs).content, ((ImmDouble) this.rhs).content));
                break;
            case CMP_CC:
                result = new ImmInteger(Character.compare(((ImmCharacter) this.lhs).content, ((ImmCharacter) this.rhs).content));
                break;
            case CMP_SS:
                result = new ImmInteger(((ImmString) this.lhs).content.compareTo(((ImmString) this.rhs).content));
                break;
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