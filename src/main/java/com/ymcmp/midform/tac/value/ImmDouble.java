/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

public final class ImmDouble extends Value {

    public final double content;

    public ImmDouble(double content) {
        this.content = content;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(this.content);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImmDouble) {
            final ImmDouble imm = (ImmDouble) obj;
            return this.content == imm.content;
        }
        return false;
    }

    @Override
    public String toString() {
        return Double.toString(this.content);
    }
}