/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.type.Types;

public final class VariantType implements Type {

    public final List<Type> bases;

    public VariantType(List<Type> bases) {
        if (bases == null || bases.isEmpty()) {
            throw new IllegalArgumentException("Variant type must compose of at least one base type");
        }

        this.bases = Collections.unmodifiableList(bases);
    }

    public List<Type> getBases() {
        return this.bases;
    }

    public Type getBasesAt(int idx) {
        return this.bases.get(idx);
    }

    public int numberOfBases() {
        return this.bases.size();
    }

    public boolean containsBaseType(final Type t) {
        for (final Type base : this.bases) {
            if (base.equivalent(t)) {
                return true;
            }
            if (base instanceof VariantType && ((VariantType) base).containsBaseType(t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean assignableFrom(Type t) {
        // Either t is same as this
        // - this type (s1 | s2 | ... | sn) <- type t (s1 | s2 | ... | sn)
        // or t is a base type of this
        // - this type (s1 | s2 | ... | sn) <- type t (si), 1<=i<=n
        // or t captures a subset of this
        // - this type (s1 | s2 | ... | sn) <- type t (si | sj | ...), 1<=i,j,...<=n

        if (this.equivalent(t) || this.containsBaseType(t)) {
            return true;
        }

        if (t instanceof VariantType) {
            final VariantType vt = (VariantType) t;
            return Types.isAssignableSubset(vt.bases, this.bases);
        }
        return false;
    }

    @Override
    public boolean equivalent(Type t) {
        if (t instanceof VariantType) {
            final VariantType vt = (VariantType) t;

            // All base types must be equivalent
            return Types.checkListEquivalent(this.bases, vt.bases);
        }
        return false;
    }

    @Override
    public Type expandBound() {
        return new VariantType(this.bases.stream().map(Type::expandBound).collect(Collectors.toList()));
    }

    @Override
    public Type substitute(final Type from, final Type to) {
        if (this.equivalent(from)) {
            return to;
        }
        return new VariantType(this.bases.stream().map(e -> e.substitute(from, to)).collect(Collectors.toList()));
    }

    @Override
    public int hashCode() {
        return this.bases.hashCode();
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof VariantType) {
            return Types.ensureListCondition(this.bases, ((VariantType) t).bases, Object::equals);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.bases.stream().map(Object::toString).collect(Collectors.joining("|"));
    }
}