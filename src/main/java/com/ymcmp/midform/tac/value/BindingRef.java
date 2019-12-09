/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import java.util.Objects;

import com.ymcmp.midform.tac.type.ReferenceType;
import com.ymcmp.midform.tac.type.Type;

public abstract class BindingRef extends Value {

    public final Binding referent;
    public final ReferenceType type;

    public BindingRef(Binding referent) {
        this.referent = Objects.requireNonNull(referent);
        this.type = new ReferenceType(this.referent.getType());
    }

    public abstract void storeValue(Value value);
    public abstract Value loadValue();

    @Override
    public final Type getType() {
        return this.type;
    }

    @Override
    public final Value replaceBinding(Binding binding, Value t) {
        return this;
    }

    @Override
    public final int hashCode() {
        return this.referent.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof BindingRef) {
            // due to the way this.type is constructed,
            // we only need to check if the referent is equals
            final BindingRef br = (BindingRef) obj;
            return this.referent.equals(br.referent);
        }
        return false;
    }

    @Override
    public final String toString() {
        return "ref " + this.referent;
    }
}