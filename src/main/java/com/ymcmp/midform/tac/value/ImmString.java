/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

public final class ImmString extends Value {

    public final String content;

    public ImmString(String content) {
        this.content = java.util.Objects.requireNonNull(content);
    }

    @Override
    public int hashCode() {
        return this.content.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImmString) {
            final ImmString imm = (ImmString) obj;
            return this.content.equals(imm.content);
        }
        return false;
    }

    @Override
    public String toString() {
        return '"' + this.content.replace("\"", "\\\"") + '"';
    }
}