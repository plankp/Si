/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

import com.ymcmp.si.lang.type.restriction.AssignableFromRestriction;
import com.ymcmp.si.lang.type.restriction.AssignableToRestriction;
import com.ymcmp.si.lang.type.restriction.EquivalenceRestriction;
import com.ymcmp.si.lang.type.restriction.UnboundedRestriction;

public final class TypeUtils {

    private TypeUtils() {
    }

    public static NomialType name(String name) {
        return new NomialType(name);
    }

    public static TupleType group(Type... col) {
        return new TupleType(Arrays.asList(col));
    }

    public static FunctionType func(Type in, Type out) {
        return new FunctionType(in, out);
    }

    public static VariantType or(Type... types) {
        return new VariantType(Arrays.asList(types));
    }

    public static UnboundedRestriction free(String name) {
        return new UnboundedRestriction(name);
    }

    public static EquivalenceRestriction equiv(String name, Type t) {
        return new EquivalenceRestriction(name, t);
    }

    public static AssignableToRestriction convTo(String name, Type t) {
        return new AssignableToRestriction(name, t);
    }

    public static AssignableFromRestriction convFrom(String name, Type t) {
        return new AssignableFromRestriction(name, t);
    }

    public static Type unify(Type s, Type t) {
        if (s.assignableFrom(t)) {
            return s;
        }
        if (t.assignableFrom(s)) {
            return t;
        }
        throw new TypeMismatchException("Cannot unify types: " + s + " and: " + t);
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

    public static boolean checkListAssignableFrom(List<Type> lhs, List<Type> rhs) {
        return ensureListCondition(lhs, rhs, Type::assignableFrom);
    }

    public static boolean checkListEquivalent(List<Type> lhs, List<Type> rhs) {
        return ensureListCondition(lhs, rhs, Type::equivalent);
    }

    public static boolean isAssignableSubset(List<Type> subset, List<Type> superset) {
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

    public static boolean checkListEquivalentByCapture(List<Type> listA, List<Type> listB) {
        return isAssignableSubset(listA, listB) && isAssignableSubset(listB, listA);
    }
}