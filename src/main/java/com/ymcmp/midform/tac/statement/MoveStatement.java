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
import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.value.Binding;
import com.ymcmp.midform.tac.value.Value;

public class MoveStatement implements Statement {

    public final Binding dst;
    public final Value src;

    public MoveStatement(Binding dst, Value src) {
        this.dst = dst;
        this.src = src;
    }

    @Override
    public Optional<Binding> getResultRegister() {
        return Optional.of(this.dst);
    }

    @Override
    public boolean isPure() {
        return !(src instanceof Binding.Mutable);
    }

    @Override
    public void validateType(Subroutine s) {
        // Check if the value we are assigning is equivalent
        // to the type defined by the binding (destination)
        final Type expected = dst.getType();
        final Type actual = src.getType();
        if (!equivalent(expected, actual)) {
            throw new RuntimeException("Move type mismatch: expected: " + expected + " got: " + actual);
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
            return new MoveStatement(this.dst, newSrc);
        }
        return this;
    }

    @Override
    public Statement unfoldConstants() {
        // Nothing to unfold
        return this;
    }

    @Override
    public String toString() {
        return "mov " + dst + ", " + src;
    }
}