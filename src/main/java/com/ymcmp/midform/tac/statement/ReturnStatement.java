/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import static com.ymcmp.midform.tac.type.Types.equivalent;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.ymcmp.midform.tac.BindingCounter;
import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.value.Binding;
import com.ymcmp.midform.tac.value.Value;

public final class ReturnStatement extends YieldStatement {

    public final Value value;

    public ReturnStatement(Value value) {
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public Optional<Binding> getResultRegister() {
        return Optional.empty();
    }

    @Override
    public Statement toNonYieldingVariant(Binding dst) {
        return new MoveStatement(dst, this.value);
    }

    @Override
    public void validateType(Subroutine s) {
        // Check if the value we are returning is equivalent
        // to the type defined by the enclosing subroutine
        final Type expected = s.type.getOutput();
        final Type actual = value.getType();
        if (!equivalent(expected, actual)) {
            throw new RuntimeException("Return type mismatch: expected: " + expected + " got: " + actual);
        }
    }

    @Override
    public void reachBlock(Map<Block, Integer> marked, Map<Binding, BindingCounter> bindings) {
        Statement.checkBindingDeclaration(bindings, this.value);
    }

    @Override
    public Statement replaceRead(Binding binding, Value repl) {
        final Value newValue = this.value.replaceBinding(binding, repl);
        if (newValue != this.value) {
            return new ReturnStatement(newValue);
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
        return "ret " + value;
    }
}