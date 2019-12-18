/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.operator;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import com.ymcmp.midform.tac.type.*;

import com.ymcmp.si.lang.Pair;
import com.ymcmp.si.lang.type.TypeMismatchException;

public final class UnaryOperator<E> {

    public final String name;

    private final ArrayList<Pair<Type, E>> list = new ArrayList<>();

    public UnaryOperator(String name) {
        this.name = name;
    }

    public void add(Type input, E mapping) {
        for (final Pair<Type, E> pair : this.list) {
            if (Types.equivalent(pair.a, input)) {
                throw new RuntimeException("Attempt to overwrite " + this + '(' + input + ')' + input);
            }
        }

        this.list.add(new Pair<>(input, mapping));
    }

    public E get(Type input) {
        for (final Pair<Type, E> pair : this.list) {
            if (Types.equivalent(pair.a, input)) {
                return pair.b;
            }
        }

        throw new TypeMismatchException("Illegal operator " + this + " on: " + input);
    }

    public void clear() {
        this.list.clear();
        this.list.trimToSize();
    }

    @Override
    public String toString() {
        return "operator " + this.name;
    }
}