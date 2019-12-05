/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import static com.ymcmp.midform.tac.type.Types.equivalent;

import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.value.Value;

import java.util.Objects;

public final class ReturnStatement extends BranchStatement {

    public final Value value;

    public ReturnStatement(Value value) {
        this.value = Objects.requireNonNull(value);
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
    public String toString() {
        return "ret " + value;
    }
}