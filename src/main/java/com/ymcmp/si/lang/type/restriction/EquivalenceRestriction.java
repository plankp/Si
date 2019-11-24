/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type.restriction;

import com.ymcmp.si.lang.type.Type;

public final class EquivalenceRestriction extends TypeRestriction {

    public final Type bound;

    public EquivalenceRestriction(String name, Type bound) {
        super(name);

        if (bound == null) {
            throw new IllegalArgumentException("Cannot have restrictive bound of type null");
        }

        this.bound = bound;
    }

    @Override
    public boolean isValidType(Type t) {
        // Only equivalent types are allowed
        if (t instanceof GenericParameter) {
            final TypeRestriction r = ((GenericParameter) t).getAssociatedRestriction();
            return (r instanceof EquivalenceRestriction) && this.bound.equivalent(((EquivalenceRestriction) r).bound);
        }
        return this.bound.equivalent(t);
    }

    @Override
    public int hashCode() {
        return this.bound.hashCode();
    }

    @Override
    public boolean equals(Object k) {
        if (k instanceof EquivalenceRestriction) {
            final EquivalenceRestriction r = (EquivalenceRestriction) k;
            return this.name.equals(r.name) && this.bound.equals(r.bound);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.name + "::" + this.bound.toString();
    }
}