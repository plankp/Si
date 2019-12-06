/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import static com.ymcmp.midform.tac.type.Types.equivalent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.function.Supplier;

import com.ymcmp.midform.tac.BindingCounter;
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
    public final Block ifTrue;
    public final Block ifFalse;
    public final Value lhs;
    public final Value rhs;

    public ConditionalJumpStatement(ConditionalOperator operator, Block ifTrue, Block ifFalse, Value lhs, Value rhs) {
        this.operator = operator;
        this.ifTrue = Objects.requireNonNull(ifTrue);
        this.ifFalse = Objects.requireNonNull(ifFalse);
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
    public void reachBlock(Map<Block, Integer> marked, final Map<Binding, BindingCounter> bindings) {
        Statement.checkBindingDeclaration(bindings, this.lhs);
        Statement.checkBindingDeclaration(bindings, this.rhs);

        // since the control flow diverges at this point,
        // duplicate the maps and perform trace on it

        // Note: the binding counter needs to be deep copied
        final Supplier<Map<Binding, BindingCounter>> dupMap = () ->
                bindings.entrySet().stream().collect(Collectors
                        .toMap(Map.Entry::getKey, e -> new BindingCounter(e.getValue())));

        final Map<Binding, BindingCounter> bmap1 = dupMap.get();
        final Map<Binding, BindingCounter> bmap2 = dupMap.get();

        final Map<Block, Integer> mmap1 = new HashMap<>(marked);
        final Map<Block, Integer> mmap2 = new HashMap<>(marked);

        this.ifTrue.trace(mmap1, bmap1);
        this.ifFalse.trace(mmap2, bmap2);

        // Take the union of the two maps
        // if duplicate, then we take the upper bound

        for (final Map.Entry<Binding, BindingCounter> entry : bmap2.entrySet()) {
            final Binding key = entry.getKey();
            final BindingCounter merged = bmap1.getOrDefault(key, new BindingCounter());
            merged.takeMaximum(entry.getValue());
            bmap1.put(key, merged);
        }

        for (final Map.Entry<Block, Integer> entry : mmap2.entrySet()) {
            final Block key = entry.getKey();
            mmap1.put(key, Math.max(mmap1.getOrDefault(key, 0), entry.getValue()));
        }

        // replace marked with the union

        bindings.clear();
        bindings.putAll(bmap1);

        marked.clear();
        marked.putAll(mmap1);
    }

    @Override
    public Optional<Statement> replaceRead(Binding.Immutable binding, Value repl) {
        final Value newLhs = this.lhs.replaceBinding(binding, repl);
        final Value newRhs = this.rhs.replaceBinding(binding, repl);
        // Check if any of the sources has been changed
        if (newLhs != this.lhs || newRhs != this.rhs) {
            return Optional.of(new ConditionalJumpStatement(this.operator, this.ifTrue, this.ifFalse, newLhs, newRhs));
        }
        return Optional.of(this);
    }

    @Override
    public Optional<Statement> unfoldConstants() {
        try {
            Boolean boxed = null;
            switch (this.operator) {
            case EQ_II:
                boxed = ((ImmInteger) this.lhs).content == ((ImmInteger) this.rhs).content;
                break;
            case NE_II:
                boxed = ((ImmInteger) this.lhs).content != ((ImmInteger) this.rhs).content;
                break;
            case LT_II:
                boxed = ((ImmInteger) this.lhs).content < ((ImmInteger) this.rhs).content;
                break;
            case LE_II:
                boxed = ((ImmInteger) this.lhs).content <= ((ImmInteger) this.rhs).content;
                break;
            case GE_II:
                boxed = ((ImmInteger) this.lhs).content >= ((ImmInteger) this.rhs).content;
                break;
            case GT_II:
                boxed = ((ImmInteger) this.lhs).content > ((ImmInteger) this.rhs).content;
                break;
            case EQ_DD:
                boxed = ((ImmDouble) this.lhs).content == ((ImmDouble) this.rhs).content;
                break;
            case NE_DD:
                boxed = ((ImmDouble) this.lhs).content != ((ImmDouble) this.rhs).content;
                break;
            case LT_DD:
                boxed = ((ImmDouble) this.lhs).content < ((ImmDouble) this.rhs).content;
                break;
            case LE_DD:
                boxed = ((ImmDouble) this.lhs).content <= ((ImmDouble) this.rhs).content;
                break;
            case GE_DD:
                boxed = ((ImmDouble) this.lhs).content >= ((ImmDouble) this.rhs).content;
                break;
            case GT_DD:
                boxed = ((ImmDouble) this.lhs).content > ((ImmDouble) this.rhs).content;
                break;
            case EQ_CC:
                boxed = ((ImmCharacter) this.lhs).content == ((ImmCharacter) this.rhs).content;
                break;
            case NE_CC:
                boxed = ((ImmCharacter) this.lhs).content != ((ImmCharacter) this.rhs).content;
                break;
            case LT_CC:
                boxed = ((ImmCharacter) this.lhs).content < ((ImmCharacter) this.rhs).content;
                break;
            case LE_CC:
                boxed = ((ImmCharacter) this.lhs).content <= ((ImmCharacter) this.rhs).content;
                break;
            case GE_CC:
                boxed = ((ImmCharacter) this.lhs).content >= ((ImmCharacter) this.rhs).content;
                break;
            case GT_CC:
                boxed = ((ImmCharacter) this.lhs).content > ((ImmCharacter) this.rhs).content;
                break;
            case EQ_SS:
                boxed = ((ImmString) this.lhs).content.equals(((ImmString) this.rhs).content);
                break;
            case NE_SS:
                boxed = !((ImmString) this.lhs).content.equals(((ImmString) this.rhs).content);
                break;
            case LT_SS:
                boxed = ((ImmString) this.lhs).content.compareTo(((ImmString) this.rhs).content) < 0;
                break;
            case LE_SS:
                boxed = ((ImmString) this.lhs).content.compareTo(((ImmString) this.rhs).content) <= 0;
                break;
            case GE_SS:
                boxed = ((ImmString) this.lhs).content.compareTo(((ImmString) this.rhs).content) >= 0;
                break;
            case GT_SS:
                boxed = ((ImmString) this.lhs).content.compareTo(((ImmString) this.rhs).content) > 0;
                break;
            case EQ_UU:
                // unit value is singleton
                boxed = ((ImmUnit) this.lhs) == ((ImmUnit) this.rhs);
                break;
            case NE_UU:
                // unit value is singleton
                boxed = ((ImmUnit) this.lhs) != ((ImmUnit) this.rhs);
                break;
            case EQ_ZZ:
                boxed = ((ImmBoolean) this.lhs).content == ((ImmBoolean) this.rhs).content;
                break;
            case NE_ZZ:
                boxed = ((ImmBoolean) this.lhs).content != ((ImmBoolean) this.rhs).content;
                break;
            default:
                break;
            }

            if (boxed != null) {
                // then we change to direct jump (goto) depending on result
                return Optional.of(new GotoStatement(boxed.booleanValue() ? ifTrue : ifFalse));
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
        return operator.toString() + ' ' + ifTrue.name + ", " + ifFalse.name + ", " + lhs + ", " + rhs;
    }
}