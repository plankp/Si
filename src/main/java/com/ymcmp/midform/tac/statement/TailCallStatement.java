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
import com.ymcmp.midform.tac.type.FunctionType;
import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.value.Binding;
import com.ymcmp.midform.tac.value.Value;

public final class TailCallStatement extends BranchStatement {

    public final Value sub;
    public final Value arg;

    public TailCallStatement(Value sub, Value arg) {
        this.sub = Objects.requireNonNull(sub);
        this.arg = Objects.requireNonNull(arg);
    }

    @Override
    public Optional<Binding> getResultRegister() {
        return Optional.empty();
    }

    @Override
    public boolean isPure() {
        // It depends on the function being called
        // but for now, let's assume it is not
        return false;
    }

    @Override
    public void validateType(Subroutine s) {
        // Check if the function type accepts the correct inputs
        // and returns an output acceptable by the binding (destination)
        final FunctionType f = (FunctionType) sub.getType();
        if (!equivalent(f.getInput(), arg.getType())) {
            throw new RuntimeException("Call input type mismatch: expected: " + f.getInput() + " got: " + arg.getType());
        }

        final Type expected = s.type.getOutput();
        final Type actual = f.getOutput();
        if (!equivalent(expected, actual)) {
            throw new RuntimeException("Call output type mismatch: expected: " + expected + " got: " + actual);
        }
    }

    @Override
    public void reachBlock(Map<Block, Integer> marked, Map<Binding, BindingCounter> bindings) {
        // No blocks to trace (we only care about blocks in the same function)

        Statement.checkBindingDeclaration(bindings, this.sub);
        Statement.checkBindingDeclaration(bindings, this.arg);
    }

    @Override
    public Optional<Statement> replaceRead(Binding.Immutable binding, Value repl) {
        final Value newSub = this.sub.replaceBinding(binding, repl);
        final Value newArg = this.arg.replaceBinding(binding, repl);
        if (newSub != this.sub || newArg != this.arg) {
            return Optional.of(new TailCallStatement(newSub, newArg));
        }
        return Optional.of(this);
    }

    @Override
    public Optional<Statement> unfoldConstants() {
        // For now there is nothing to unfold
        // inline or compile time functions will need to change this
        return Optional.of(this);
    }

    @Override
    public String toString() {
        return "tailcall " + sub + ' ' + arg;
    }
}