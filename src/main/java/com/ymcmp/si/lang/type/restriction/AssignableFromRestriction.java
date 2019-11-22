/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type.restriction;

import com.ymcmp.si.lang.type.Type;

public class AssignableFromRestriction extends TypeRestriction {

    public final Type bound;

    public AssignableFromRestriction(String name, Type bound) {
        super(name);

        if (bound == null) {
            throw new IllegalArgumentException("Cannot have restrictive bound of type null");
        }

        this.bound = bound;
    }

    @Override
    public boolean isValidType(Type t) {
        // Only if new type is assignable from boundary
        if (t instanceof GenericType) {
            final TypeRestriction r = ((GenericType) t).getAssociatedRestriction();
            if (r instanceof AssignableFromRestriction) {
                // if T <: List and S <: Collection, then T <: S
                return ((AssignableFromRestriction) r).bound.assignableFrom(this.bound);
            }
            if (r instanceof EquivalenceRestriction) {
                // if T <: List and S :: Collection, then T <: S
                return ((EquivalenceRestriction) r).bound.assignableFrom(this.bound);
            }
            return false;
        }
        return t.assignableFrom(this.bound);
    }

    @Override
    public int hashCode() {
        return this.bound.hashCode();
    }

    @Override
    public boolean equals(Object k) {
        if (k instanceof AssignableFromRestriction) {
            final AssignableFromRestriction r = (AssignableFromRestriction) k;
            return this.name.equals(r.name) && this.bound.equals(r.bound);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.name + "<:" + this.bound.toString();
    }
}
