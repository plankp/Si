/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

import static com.ymcmp.si.lang.type.TypeUtils.ensureListCondition;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public final class ParametricType<T extends Type> implements Type {

    public final T base;
    public final List<FreeType> restrictions;

    public ParametricType(T base, List<FreeType> restrictions) {
        if (base == null) {
            throw new IllegalArgumentException("Cannot parametrize base type null");
        }
        if (restrictions == null || restrictions.isEmpty()) {
            throw new IllegalArgumentException("Cannot parametrize without boundaries conditions");
        }

        this.base = base;
        this.restrictions = Collections.unmodifiableList(restrictions);
    }

    public void checkParametrization(final List<Type> types) {
        final int limit = types.size();
        if (limit != this.restrictions.size()) {
            throw new TypeMismatchException(
                    "Cannot parametrize with types: " + types + " given boundary conditions: " + this.restrictions);
        }

        // We also handle type inferences:
        // is_zero(val i:) := i == 0;
        // the only valid parametrization on operator== will be for i to be int

        // Consider the following snippet:
        // is_space(val chr:) := chr == ' ';
        // it takes multiple tries to get chr to be type char
        // (that's just how type bank works...)
        //
        // this structure saves state before we try to do inference
        final LinkedList<Integer> saveInferredState = new LinkedList<>();
        boolean shouldRestore = false;

        for (int i = 0; i < limit; ++i) {
            final FreeType restriction = this.restrictions.get(i);
            final Type expected = types.get(i).expandBound();

            if (restriction.assignableFrom(expected)) {
                // This is clearly valid
                continue;
            }

            // Otherwise we might be matching against an inferred type
            if (!(expected instanceof InferredType)) {
                // Only inferred types can have a chance of being matched correctly now
                // since it is not, then we request method to restore the previously
                // made inferences and throw type mismatch exception
                shouldRestore = true;
                break;
            }

            final InferredType inf = (InferredType) expected;
            saveInferredState.addLast(i);

            inf.setInferredType(restriction.expandBound());
        }

        if (shouldRestore) {
            // restore to previous state for inferred types
            Integer idx;
            while ((idx = saveInferredState.pollFirst()) != null) {
                ((InferredType) types.get(idx)).setInferredType(null);
            }

            throw new TypeMismatchException(
                    "Cannot parametrize with types: " + types + " given boundary conditions: " + this.restrictions);
        }

        // Reaching here means that type resolution was successful
    }

    public T parametrize(List<Type> types) {
        this.checkParametrization(types);

        Type result = this.base;
        final int limit = types.size();
        for (int i = 0; i < limit; ++i) {
            result = result.substitute(this.restrictions.get(i), types.get(i));
        }

        @SuppressWarnings("unchecked")
        final T casted = (T) result;
        return casted;
    }

    public T getBase() {
        return base;
    }

    public List<FreeType> getTypeRestrictions() {
        return this.restrictions;
    }

    public FreeType getTypeRestrictionAt(int idx) {
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

            // Type boundaries must be equivalent
            return ensureListCondition(this.restrictions, pt.restrictions, Type::equivalent);
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
            return ensureListCondition(this.restrictions, pt.restrictions, Type::equivalent);
        }
        return false;
    }

    @Override
    public Type substitute(final Type from, Type to) {
        if (this.equivalent(from)) {
            return to;
        }

        // Only substitute on base
        final Type subst = this.base.substitute(from, to);
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