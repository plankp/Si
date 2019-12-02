/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

public final class Temporary extends Value {

    public final String name;

    public Temporary(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Temporary) {
            final Temporary lbl = (Temporary) obj;
            return this.name.equals(lbl.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.name;
    }
}