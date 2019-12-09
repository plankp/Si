/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.type;

public final class ReferenceType extends Type {

    public final Type referent;
    public final boolean immutable;

    public ReferenceType(Type referent, boolean immutable) {
        if (referent == null) {
            throw new IllegalArgumentException("Referent type cannot be null");
        }

        this.referent = referent;
        this.immutable = immutable;
    }

    public static ReferenceType mutable(Type referent) {
        return new ReferenceType(referent, false);
    }

    public static ReferenceType immutable(Type referent) {
        return new ReferenceType(referent, true);
    }

    public Type getReferentType() {
        return this.referent;
    }

    public boolean isReferentImmutable() {
        return this.immutable;
    }

    @Override
    protected boolean assignableFrom(Type t) {
        if (t instanceof ReferenceType) {
            // Only two cases are allowed:
            // 1a. ref T <: ref T           (allowed: both are equivalent)
            // 1b. ref mut T <: ref mut T   (allowed: both are equivalent)
            // 2.  ref T <: ref mut T       (allowed: immutable is more restrictive)
            final ReferenceType rt = (ReferenceType) t;
            return (!rt.immutable || this.immutable)
                && this.referent.equivalent(rt.referent);
        }
        return false;
    }

    @Override
    protected boolean equivalent(Type t) {
        if (t instanceof ReferenceType) {
            final ReferenceType rt = (ReferenceType) t;
            return this.immutable == rt.immutable
                && this.referent.equivalent(rt.referent);
        }
        return false;
    }

    @Override
    public Type substitute(Type from, Type to) {
        if (this.equivalent(from)) {
            return to;
        }

        final Type sref = this.referent.substitute(from, to);
        return this.referent == sref ? this : new ReferenceType(sref, this.immutable);
    }

    @Override
    public int hashCode() {
        return this.referent.hashCode() * 17 + (immutable ? 1 : 0);
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof ReferenceType) {
            final ReferenceType rt = (ReferenceType) t;
            return this.immutable == rt.immutable
                && this.referent.equals(rt.referent);
        }
        return false;
    }

    @Override
    public String toString() {
        return "ref " + (this.immutable ? "" : "mut ") + this.referent;
    }
}