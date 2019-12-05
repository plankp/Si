/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import static com.ymcmp.midform.tac.type.Types.equivalent;

import java.util.Set;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.type.FunctionType;
import com.ymcmp.midform.tac.value.Binding;
import com.ymcmp.midform.tac.value.Value;

import java.util.Objects;

public final class CallStatement implements Statement {

    public final Binding dst;
    public final Value sub;
    public final Value arg;

    public CallStatement(Binding dst, Value sub, Value arg) {
        this.dst = Objects.requireNonNull(dst);
        this.sub = Objects.requireNonNull(sub);
        this.arg = Objects.requireNonNull(arg);
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
        if (!equivalent(dst.getType(), f.getOutput())) {
            throw new RuntimeException("Call output type mismatch: expected: " + dst.getType() + " got: " + f.getInput());
        }
    }

    @Override
    public void reachBlock(Set<Block> marked) {
        // No blocks to trace (we only care about blocks in the same function)
    }

    @Override
    public String toString() {
        return "call " + dst + ", " + sub + ' ' + arg;
    }
}
