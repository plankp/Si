/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.ymcmp.midform.tac.BindingCounter;
import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.value.Binding;
import com.ymcmp.midform.tac.value.Value;

public final class GotoStatement implements BranchStatement {

    public final Block next;

    public GotoStatement(Block next) {
        this.next = Objects.requireNonNull(next);
    }

    @Override
    public Optional<Binding> getResultRegister() {
        return Optional.empty();
    }

    @Override
    public void validateType(Subroutine s) {
        // Nothing type related to validate, always success
    }

    @Override
    public void reachBlock(Map<Block, Integer> marked, Map<Binding, BindingCounter> bindings) {
        this.next.trace(marked, bindings);
    }

    @Override
    public Statement replaceRead(Binding binding, Value value) {
        // Nothing to replace
        return this;
    }

    @Override
    public Statement unfoldConstants() {
        // Nothing to unfold
        return this;
    }

    @Override
    public String toString() {
        return "jmp " + next.name;
    }
}