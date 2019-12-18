/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.operator;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.function.BiFunction;

import com.ymcmp.midform.tac.type.*;

import com.ymcmp.si.lang.Pair;
import com.ymcmp.si.lang.type.TypeMismatchException;

public final class BinaryOperator<E> {

    private static final class Block<E> {

        public final Type input;
        public final Type output;
        public E value;

        public Block(Type input, Type output, E mapping) {
            this.input = input;
            this.output = output;
            this.value = mapping;
        }

        public boolean equivalent(Type input, Type output) {
            return Types.equivalent(this.input, input)
                && Types.equivalent(this.output, output);
        }
    
        public E getValue() {
            return this.value;
        }
    }

    public final String name;

    private final ArrayList<Block<E>> list = new ArrayList<>();
    private final BiFunction<Type, Type, String> msg;

    public BinaryOperator(String name) {
        this(name, null);
    }

    public BinaryOperator(String name, BiFunction<Type, Type, String> builder) {
        this.name = name;
        this.msg = builder == null ? BinaryOperator::defaultMsgBuilder : builder;
    }

    public void add(Type input, Type output, E mapping) {
        for (final Block<E> block : this.list) {
            if (block.equivalent(input, output)) {
                throw new RuntimeException("Attempt to overwrite " + this + this.msg.apply(input, output));
            }
        }

        this.list.add(new Block<>(input, output, mapping));
    }

    public void addBidi(Type input, Type output, E forward, E backward) {
        for (final Block<E> block : this.list) {
            // check forward direction
            if (block.equivalent(input, output)) {
                throw new RuntimeException("Attempt to overwrite " + this + this.msg.apply(input, output));
            }

            // check backward direction
            if (block.equivalent(output, input)) {
                throw new RuntimeException("Attempt to overwrite " + this + this.msg.apply(input, output));
            }
        }

        this.list.add(new Block<>(input, output, forward));
        this.list.add(new Block<>(output, input, backward));
    }

    public E get(Type input, Type output) {
        for (final Block<E> block : this.list) {
            if (block.equivalent(input, output)) {
                return block.getValue();
            }
        }

        throw new TypeMismatchException("Illegal operator " + this + " on: " + input + " and: " + output);
    }

    public void clear() {
        this.list.clear();
        this.list.trimToSize();
    }

    @Override
    public String toString() {
        return "operator " + this.name;
    }

    private static String defaultMsgBuilder(Type input, Type output) {
        return '(' + input.toString() + ')' + output;
    }
}