/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

public interface Type {

    public boolean assignableFrom(Type t);

    public boolean equivalent(Type t);

    public default Type substitute(Type from, Type to) {
        return this.equivalent(from) ? to : this;
    }
}