/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

public abstract class BranchStatement implements Statement {

    protected BranchStatement() {
        // Note: Disallow anonymous classes
    }

    @Override
    public boolean isPure() {
        // The act of branching alone is always pure:
        // given the same input, always the same output
        return true;
    }
}