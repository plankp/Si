/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.type.NomialType;

public final class ImmBoolean extends Value {

    public static final NomialType TYPE = new NomialType("bool");

    public final boolean content;

    public ImmBoolean(boolean content) {
        this.content = content;
    }

    @Override
    public Type getType() {
        return TYPE;
    }

    @Override
    public Value replaceBinding(Binding binding, Value t) {
        return this;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(this.content);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImmBoolean) {
            final ImmBoolean imm = (ImmBoolean) obj;
            return this.content == imm.content;
        }
        return false;
    }

    @Override
    public String toString() {
        return Boolean.toString(this.content);
    }
}