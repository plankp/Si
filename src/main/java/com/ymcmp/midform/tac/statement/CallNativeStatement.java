/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.value.Temporary;
import com.ymcmp.midform.tac.value.Value;

import java.util.Objects;

public final class CallNativeStatement implements Statement {

    public final String name;
    public final Temporary dst;
    public final Value src;

    public CallNativeStatement(String name, Temporary dst, Value src) {
        this.name = Objects.requireNonNull(name);
        this.dst = Objects.requireNonNull(dst);
        this.src = Objects.requireNonNull(src);
    }

    @Override
    public boolean isPure() {
        // Since there is no way for us to get this information,
        // better assume it is not pure
        return false;
    }

    @Override
    public String toString() {
        return "call.native " + dst + ", " + name + ' ' + src;
    }
}
