/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

public final class ImmCharacter extends Value {

    // stores as UTF16 codepoint (like Java)
    public final char content;

    public ImmCharacter(char content) {
        this.content = content;
    }

    @Override
    public int hashCode() {
        return Character.hashCode(this.content);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImmCharacter) {
            final ImmCharacter imm = (ImmCharacter) obj;
            return this.content == imm.content;
        }
        return false;
    }

    @Override
    public String toString() {
        if (this.content == '\'') {
            return "'\\''";
        }
        return "'" + this.content + "'";
    }
}