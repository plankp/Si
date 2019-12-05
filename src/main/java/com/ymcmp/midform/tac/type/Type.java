/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.type;

public abstract class Type {

    protected abstract boolean assignableFrom(Type t);

    protected abstract boolean equivalent(Type t);

    public Type expandBound() {
        return this;
    }

    public Type substitute(Type from, Type to) {
        return this.equivalent(from) ? to : this;
    }
}