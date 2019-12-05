/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import java.util.Optional;
import java.util.Set;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;

import java.util.Objects;

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
    public void reachBlock(Set<Block> marked) {
        this.next.trace(marked);
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