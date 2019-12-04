/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import com.ymcmp.midform.tac.value.Binding;
import com.ymcmp.midform.tac.value.Value;

import java.util.Objects;

public final class CallStatement implements Statement {

    public final Binding dst;
    public final Value sub;
    public final Value src;

    public CallStatement(Binding dst, Value sub, Value src) {
        this.dst = Objects.requireNonNull(dst);
        this.sub = Objects.requireNonNull(sub);
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
        return "call " + dst + ", " + sub + ' ' + src;
    }
}
