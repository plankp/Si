/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.type;

public final class ReferenceType extends Type {

    public final Type referent;

    public ReferenceType(Type referent) {
        if (referent == null) {
            throw new IllegalArgumentException("Referent type cannot be null");
        }

        this.referent = referent;
    }

    public Type getReferentType() {
        return this.referent;
    }

    @Override
    protected boolean assignableFrom(Type t) {
        return this.equivalent(t);
    }

    @Override
    protected boolean equivalent(Type t) {
        if (t instanceof ReferenceType) {
            final ReferenceType rt = (ReferenceType) t;
            return this.referent.equivalent(rt.referent);
        }
        return false;
    }

    @Override
    public Type substitute(Type from, Type to) {
        if (this.equivalent(from)) {
            return to;
        }

        final Type sref = this.referent.substitute(from, to);
        return this.referent == sref ? this : new ReferenceType(sref);
    }

    @Override
    public int hashCode() {
        return this.referent.hashCode();
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof ReferenceType) {
            final ReferenceType rt = (ReferenceType) t;
            return this.referent.equals(rt.referent);
        }
        return false;
    }

    @Override
    public String toString() {
        return "ref " + this.referent;
    }
}