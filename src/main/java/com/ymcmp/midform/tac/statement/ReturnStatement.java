/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import com.ymcmp.midform.tac.value.Value;

import java.util.Objects;

public final class ReturnStatement extends BranchStatement {

    public final Value value;

    public ReturnStatement(Value value) {
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public String toString() {
        return "ret " + value;
    }
}