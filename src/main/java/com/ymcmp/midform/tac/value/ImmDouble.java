/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.type.NomialType;

public final class ImmDouble extends Value {

    public static final NomialType TYPE = new NomialType("double");

    public final double content;

    public ImmDouble(double content) {
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
    public boolean containsLocalBinding() {
        return false;
    }

    @Override
    public boolean isCompileTimeConstant() {
        return true;
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