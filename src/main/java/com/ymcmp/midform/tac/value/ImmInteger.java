/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

public final class ImmInteger extends Value {

    public final int content;

    public ImmInteger(int content) {
        this.content = content;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.content);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImmInteger) {
            final ImmInteger imm = (ImmInteger) obj;
            return this.content == imm.content;
        }
        return false;
    }

    @Override
    public String toString() {
        return Integer.toString(this.content);
    }
}