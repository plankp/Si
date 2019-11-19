/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.Collections;

public final class GenericType implements Type {

    public final Type base;
    public final List<Type> params;

    public GenericType(Type base, List<Type> params) {
        if (base == null) {
            throw new IllegalArgumentException("Generic base type cannot be null");
        }
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Generic type parameter cannot be empty or null");
        }

        this.base = base;
        this.params = Collections.unmodifiableList(params);
    }

    public Type getBase() {
        return this.base;
    }

    public List<Type> getTypeParameters() {
        return this.params;
    }

    public Type getTypeParameterAt(int idx) {
        return this.params.get(idx);
    }

    public int numberOfTypeParameters() {
        return this.params.size();
    }

    @Override
    public boolean assignableFrom(Type t) {
        if (t instanceof GenericType) {
            final GenericType gt = (GenericType) t;

            // Base type must be assignable
            if (!this.base.assignableFrom(gt.base)) {
                return false;
            }

            // Type parameters must be equivalent!
            //
            // The following must NOT be allowed
            // a <Object>Array <: b <Integer>Array
            // Assigning into $a causes $b to hold Object, not Integer
            // which is inconsistent!
            return checkListEquivalent(this.params, gt.params);
        }
        return false;
    }

    @Override
    public boolean equivalent(Type t) {
        if (t instanceof GenericType) {
            final GenericType gt = (GenericType) t;

            // Base type must be equivalent
            if (!this.base.equivalent(gt.base)) {
                return false;
            }

            // Type parameters must be equivalent
            return checkListEquivalent(this.params, gt.params);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.base.hashCode() * 17 + this.params.hashCode();
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof GenericType) {
            final GenericType gt = (GenericType) t;
            return this.base.equals(gt.base) && this.params.equals(gt.params);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.params.stream().map(Object::toString).collect(Collectors.joining(",", "<", ">"))
                + this.base.toString();
    }

    public static boolean checkListEquivalent(List<? extends Type> lhs, List<? extends Type> rhs) {
        return ensureListCondition(lhs, rhs, Type::equivalent);
    }

    public static boolean checkListAssignableFrom(List<? extends Type> lhs, List<? extends Type> rhs) {
        return ensureListCondition(lhs, rhs, Type::assignableFrom);
    }

    public static <S, T> boolean ensureListCondition(List<S> lhs, List<T> rhs, BiPredicate<S, T> test) {
        final int limit = lhs.size();
        if (limit != rhs.size()) {
            return false;
        }

        for (int i = 0; i < limit; ++i) {
            if (!test.test(lhs.get(i), rhs.get(i))) {
                return false;
            }
        }

        return true;
    }
}