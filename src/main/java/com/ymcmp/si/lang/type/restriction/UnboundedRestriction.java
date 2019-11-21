/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type.restriction;

import com.ymcmp.si.lang.type.Type;

public final class UnboundedRestriction extends TypeRestriction {

    public UnboundedRestriction(String name) {
        super(name);
    }

    @Override
    public boolean isValidType(Type t) {
        // All types are valid
        return true;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UnboundedRestriction) {
            final UnboundedRestriction r = (UnboundedRestriction) obj;
            return this.name.equals(r.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.name;
    }
}