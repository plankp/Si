/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import com.ymcmp.si.lang.type.Type;
import com.ymcmp.midform.tac.value.Binding;

public final class LocalVar {

    public final Type type;
    public final Binding binding;

    public LocalVar(Type type, Binding binding) {
        this.type = type;
        this.binding = binding;
    }

    @Override
    public int hashCode() {
        return this.type.hashCode() * 17 + this.binding.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LocalVar) {
            final LocalVar lv = (LocalVar) obj;
            return this.type.equals(lv.type)
                && this.binding.equals(lv.binding);
        }
        return false;
    }

    @Override
    public String toString() {
        return binding.toString() + ' ' + type.toString();
    }
}