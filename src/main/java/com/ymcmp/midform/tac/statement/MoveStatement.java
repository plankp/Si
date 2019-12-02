/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import com.ymcmp.midform.tac.value.Temporary;
import com.ymcmp.midform.tac.value.Value;

public class MoveStatement implements Statement {

    public final Temporary dst;
    public final Value src;

    public MoveStatement(Temporary dst, Value src) {
        this.dst = dst;
        this.src = src;
    }

    @Override
    public String toString() {
        return "mov " + dst + ", " + src;
    }
}