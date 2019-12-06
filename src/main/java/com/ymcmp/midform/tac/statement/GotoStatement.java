/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.value.Binding;
import com.ymcmp.midform.tac.value.Value;

public final class GotoStatement extends BranchStatement {

    public final Block next;

    public GotoStatement(Block next) {
        this.next = Objects.requireNonNull(next);
    }

    @Override
    public void validateType(Subroutine s) {
        // Nothing type related to validate, always success
    }

    @Override
    public void reachBlock(Map<Block, Integer> marked, Map<Binding, Integer> bindings) {
        this.next.trace(marked, bindings);
    }

    @Override
    public Optional<Statement> replaceRead(Binding.Immutable binding, Value value) {
        // Nothing to replace
        return Optional.of(this);
    }

    @Override
    public Optional<Statement> unfoldConstants() {
        // Nothing to unfold
        return Optional.of(this);
    }

    @Override
    public String toString() {
        return "jmp " + next.name;
    }
}