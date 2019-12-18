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

public final class TernaryOperator<E> {

    private static final class Block<E> {

        public final Type lhs;
        public final Type rhs;
        public Pair<Type, E> value;

        public Block(Type lhs, Type rhs, Type out, E mapping) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.value = new Pair<>(out, mapping);
        }

        public boolean equivalent(Type lhs, Type rhs) {
            return Types.equivalent(this.lhs, lhs)
                && Types.equivalent(this.rhs, rhs);
        }

        public boolean equivalent(Type lhs, Type rhs, Type out) {
            return this.equivalent(lhs, rhs)
                && Types.equivalent(this.value.a, out);
        }

        public Pair<Type, E> getValuePair() {
            return this.value;
        }
    }

    public final String name;

    private final ArrayList<Block<E>> list = new ArrayList<>();

    public TernaryOperator(String name) {
        this.name = name;
    }

    public void add(Type lhs, Type rhs, Type out, E mapping) {
        for (final Block<E> block : this.list) {
            if (block.equivalent(lhs, rhs, out)) {
                throw new RuntimeException("Attempt to overwrite " + this + '(' + lhs + ',' + rhs + ')' + out);
            }
        }

        this.list.add(new Block<>(lhs, rhs, out, mapping));
    }

    public void addBidi(Type lhs, Type rhs, Type out, E forward, E backward) {
        for (final Block<E> block : this.list) {
            // check forward direction
            if (block.equivalent(lhs, rhs, out)) {
                throw new RuntimeException("Attempt to overwrite " + this + '(' + lhs + ',' + rhs + ')' + out);
            }

            // check backward direction
            if (block.equivalent(rhs, lhs, out)) {
                throw new RuntimeException("Attempt to overwrite " + this + '(' + rhs + ',' + lhs + ')' + out);
            }
        }

        this.list.add(new Block<>(lhs, rhs, out, forward));
        this.list.add(new Block<>(rhs, lhs, out, backward));
    }

    public Pair<Type, E> get(Type lhs, Type rhs) {
        for (final Block<E> block : this.list) {
            if (block.equivalent(lhs, rhs)) {
                return block.getValuePair();
            }
        }

        throw new TypeMismatchException("Illegal operator " + this + " on: " + lhs + " and: " + rhs);
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