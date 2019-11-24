/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type.restriction;

import com.ymcmp.si.lang.type.Type;

public abstract class TypeRestriction {

    public final String name;

    private final GenericParameter type;

    TypeRestriction(String name) {
        this.name = name;
        this.type = new GenericParameter(this);
    }

    public final String getName() {
        return this.name;
    }

    public final GenericParameter getAssociatedType() {
        return this.type;
    }

    public abstract boolean isValidType(Type t);
}
