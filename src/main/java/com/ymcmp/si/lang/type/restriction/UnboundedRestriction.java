/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type.restriction;

import com.ymcmp.si.lang.type.Type;

public class UnboundedRestriction implements TypeRestriction {

    public final String name;

    public UnboundedRestriction(String name) {
        this.name = name;
    }

    @Override
    public boolean isValidType(Type t) {
        // All types are valid
        return true;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}