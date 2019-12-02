/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.value.Temporary;
import com.ymcmp.midform.tac.value.Value;

import java.util.Objects;

public final class CallSubStatement implements Statement {

    public final Subroutine sub;
    public final Temporary dst;
    public final Value src;

    public CallSubStatement(Subroutine sub, Temporary dst, Value src) {
        this.sub = Objects.requireNonNull(sub);
        this.dst = Objects.requireNonNull(dst);
        this.src = Objects.requireNonNull(src);
    }

    @Override
    public boolean isPure() {
        // It depends on the function being called
        // but for now, let's assume it is not
        return false;
    }

    @Override
    public String toString() {
        return "call.sub " + dst + ", " + sub.name + ' ' + src;
    }
}
