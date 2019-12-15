/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.type;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

public final class Types {

    private Types() {
    }

    public static boolean equivalent(Type s, Type t) {
        return s.expandBound().equivalent(t.expandBound());
    }

    public static boolean assignableFrom(Type s, Type t) {
        return s.expandBound().assignableFrom(t.expandBound());
    }

    public static Optional<Type> unify(Type s, Type t) {
        if (s.assignableFrom(t)) {
            return Optional.of(s);
        }
        if (t.assignableFrom(s)) {
            return Optional.of(t);
        }
        return Optional.empty();
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

    public static boolean checkListAssignableFrom(List<? extends Type> lhs, List<? extends Type> rhs) {
        return ensureListCondition(lhs, rhs, Types::assignableFrom);
    }

    public static boolean checkListEquivalent(List<? extends Type> lhs, List<? extends Type> rhs) {
        return ensureListCondition(lhs, rhs, Types::equivalent);
    }

    public static boolean isAssignableSubset(List<? extends Type> subset, List<? extends Type> superset) {
        // forall e1 in subset:
        // | there exists e2 in superset:
        // | | e2 assignableFrom e1

        for (final Type e1 : subset) {
            if (superset.stream().noneMatch(e2 -> e2.assignableFrom(e1))) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkListEquivalentByCapture(List<? extends Type> listA, List<? extends Type> listB) {
        return isAssignableSubset(listA, listB) && isAssignableSubset(listB, listA);
    }

    public static boolean checkListDisjoint(List<? extends Type> list) {
        // idea: list=[a, b, c, ..., y, z]
        //
        // a checks with b, c, ..., y, z
        // b checks with c, d, ..., y, z
        // ...
        // y checks with z

        final int limit = list.size();
        for (int i = 0; i < limit - 1; ++i) {
            final Type lhs = list.get(i);
            for (int j = i + 1; j < limit; ++j) {
                if (Types.equivalent(lhs, list.get(j))) {
                    // found overlapping (non-disjoint) element
                    return false;
                }
            }
        }

        // list is disjoint
        return true;
    }
}