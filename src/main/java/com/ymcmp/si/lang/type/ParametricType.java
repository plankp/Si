/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

import static com.ymcmp.si.lang.type.TypeUtils.ensureListCondition;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.ymcmp.si.lang.type.restriction.GenericParameter;
import com.ymcmp.si.lang.type.restriction.TypeRestriction;

public final class ParametricType<T extends Type> implements Type {

    public final T base;
    public final List<TypeRestriction> restrictions;

    public ParametricType(T base, List<TypeRestriction> restrictions) {
        if (base == null) {
            throw new IllegalArgumentException("Cannot parametrize base type null");
        }
        if (restrictions == null || restrictions.isEmpty()) {
            throw new IllegalArgumentException("Cannot parametrize without boundaries conditions");
        }

        this.base = base;
        this.restrictions = Collections.unmodifiableList(restrictions);
    }

    public T parametrize(List<Type> types) {
        types = Collections.unmodifiableList(types);
        if (!ensureListCondition(this.restrictions, types, TypeRestriction::isValidType)) {
            throw new TypeMismatchException(
                    "Cannot parametrize with types: " + types + " given boundary conditions: " + this.restrictions);
        }

        T result = this.base;
        final int limit = types.size();
        for (int i = 0; i < limit; ++i) {
            @SuppressWarnings("unchecked")
            final T tmp = (T) result.substitute(this.restrictions.get(i).getAssociatedType(), types.get(i));
            result = tmp;
        }
        return result;
    }

    public T getBase() {
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
            final ParametricType<?> pt = (ParametricType<?>) t;

            // Base type must be assignable
            if (!this.base.assignableFrom(pt.base)) {
                return false;
            }

            // Type boundaries must be the same
            // TODO: This is actually incorrect, it has to follow the boundary conditions as
            // well
            return ensureListCondition(this.restrictions, pt.restrictions, Object::equals);
        }
        return false;
    }

    @Override
    public boolean equivalent(Type t) {
        if (t instanceof ParametricType) {
            final ParametricType<?> pt = (ParametricType<?>) t;

            // Base type must be equivalent
            if (!this.base.equivalent(pt.base)) {
                return false;
            }

            // Type boundaries must be the same
            return ensureListCondition(this.restrictions, pt.restrictions, Object::equals);
        }
        return false;
    }

    @Override
    public ParametricType<T> substitute(GenericParameter from, Type to) {
        // Only substitute on base
        @SuppressWarnings("unchecked")
        final T subst = (T) this.base.substitute(from, to);
        return subst == this.base ? this : new ParametricType<>(subst, restrictions);
    }

    @Override
    public int hashCode() {
        return this.base.hashCode() * 17 + this.restrictions.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ParametricType) {
            final ParametricType<?> pt = (ParametricType<?>) obj;

            // Base type must be equivalent
            if (!this.base.equals(pt.base)) {
                return false;
            }

            // Type boundaries must be the same
            return ensureListCondition(this.restrictions, pt.restrictions, Object::equals);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.restrictions.stream().map(Object::toString).collect(Collectors.joining(",", "{", "}"))
                + this.base.toString();
    }
}