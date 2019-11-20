/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.ymcmp.si.lang.type.restriction.TypeRestriction;

public final class ParametricType implements Type {

    public final Type base;
    public final List<TypeRestriction> restrictions;

    public ParametricType(Type base, List<TypeRestriction> restrictions) {
        if (base == null) {
            throw new IllegalArgumentException("Cannot parametrize base type null");
        }
        if (restrictions == null || restrictions.isEmpty()) {
            throw new IllegalArgumentException("Cannot parametrize without boundaries conditions");
        }

        this.base = base;
        this.restrictions = Collections.unmodifiableList(restrictions);
    }

    public GenericType parametrize(List<Type> types) {
        types = Collections.unmodifiableList(types);
        if (!GenericType.ensureListCondition(this.restrictions, types, TypeRestriction::isValidType)) {
            throw new IllegalArgumentException("Cannot parametrize with types: " + types);
        }

        return new GenericType(this.base, types);
    }

    public Type getBase() {
        return base;
    }

    public List<TypeRestriction> getTypeRestrictions() {
        return this.restrictions;
    }

    public TypeRestriction getTypeRestrictionAt(int idx) {
        return this.restrictions.get(idx);
    }

    public int numberOfTypeRestrictions() {
        return this.restrictions.size();
    }

    @Override
    public boolean assignableFrom(Type t) {
        if (t instanceof ParametricType) {
            final ParametricType pt = (ParametricType) t;

            // Base type must be assignable
            if (!this.base.assignableFrom(pt.base)) {
                return false;
            }

            // Type boundaries must be the same
            return GenericType.ensureListCondition(this.restrictions, pt.restrictions, Object::equals);
        }
        return false;
    }

    @Override
    public boolean equivalent(Type t) {
        if (t instanceof ParametricType) {
            final ParametricType pt = (ParametricType) t;

            // Base type must be equivalent
            if (!this.base.equivalent(pt.base)) {
                return false;
            }

            // Type boundaries must be the same
            return GenericType.ensureListCondition(this.restrictions, pt.restrictions, Object::equals);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.base.hashCode() * 17 + this.restrictions.hashCode();
    }

    @Override
    public String toString() {
        // return "<>";
        return this.restrictions.stream().map(Object::toString).collect(Collectors.joining(",", "<", ">"))
                + this.base.toString();
    }
}