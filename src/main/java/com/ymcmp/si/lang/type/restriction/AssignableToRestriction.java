/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type.restriction;

import com.ymcmp.si.lang.type.Type;

public final class AssignableToRestriction extends TypeRestriction {

    public final Type bound;

    public AssignableToRestriction(String name, Type bound) {
        super(name);

        if (bound == null) {
            throw new IllegalArgumentException("Cannot have restrictive bound of type null");
        }

        this.bound = bound;
    }

    @Override
    public boolean isValidType(Type t) {
        // Only if boundary is assignable from new type
        if (t instanceof GenericParameter) {
            final TypeRestriction r = ((GenericParameter) t).getAssociatedRestriction();
            if (r instanceof AssignableToRestriction) {
                // if T :> (int|char) and S :> (int|char|string), then T :> S
                return this.bound.assignableFrom(((AssignableToRestriction) r).bound);
            }
            if (r instanceof EquivalenceRestriction) {
                // if T :: (int|char) and S :> (int|char|string), then T :> S
                return this.bound.assignableFrom(((EquivalenceRestriction) r).bound);
            }
            return false;
        }
        return this.bound.assignableFrom(t);
    }

    @Override
    public int hashCode() {
        return this.bound.hashCode();
    }

    @Override
    public boolean equals(Object k) {
        if (k instanceof AssignableToRestriction) {
            final AssignableToRestriction r = (AssignableToRestriction) k;
            return this.name.equals(r.name) && this.bound.equals(r.bound);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.name + ":>" + this.bound.toString();
    }
}
