/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import static com.ymcmp.midform.tac.type.Types.equivalent;

import java.util.Set;

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
    public void validateType(Subroutine s) {
        // Check if the value we are assigning is equivalent
        // to the type defined by the binding (destination)
        final Type expected = dst.getType();
        final Type actual = src.getType();
        if (!equivalent(expected, actual)) {
            throw new RuntimeException("Return type mismatch: expected: " + expected + " got: " + actual);
        }
    }

    @Override
    public void reachBlock(Set<Block> marked) {
        // No blocks to trace
    }

    @Override
    public String toString() {
        return "mov " + dst + ", " + src;
    }
}