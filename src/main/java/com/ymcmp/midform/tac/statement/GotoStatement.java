/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import com.ymcmp.midform.tac.Block;

import java.util.Objects;

public final class GotoStatement extends BranchStatement {

    public final Block next;

    public GotoStatement(Block next) {
        this.next = Objects.requireNonNull(next);
    }

    @Override
    public String toString() {
        return "jmp " + next.name;
    }
}