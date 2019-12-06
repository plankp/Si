/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ymcmp.midform.tac.type.TupleType;
import com.ymcmp.midform.tac.type.Type;

public final class Tuple extends Value {

    public final List<Value> values;
    public final TupleType type;

    public Tuple(List<Value> values) {
        this(values, new TupleType(values.stream().map(Value::getType).collect(Collectors.toList())));
    }

    public Tuple(List<Value> values, TupleType type) {
        if (values.size() < 2) {
            throw new IllegalArgumentException("Tuple must have length of 2: " + values.size());
        }

        this.values = Collections.unmodifiableList(values);
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public Value replaceBinding(final Binding binding, final Value t) {
        return new Tuple(values.stream().map(value -> value.replaceBinding(binding, t))
                .collect(Collectors.toList()), type);
    }

    @Override
    public int hashCode() {
        return this.values.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tuple) {
            final Tuple tpl = (Tuple) obj;
            return this.values.equals(tpl.values);
        }
        return false;
    }

    @Override
    public String toString() {
        final String str = this.values.toString();
        return '(' + str.substring(1, str.length() - 1) + ')';
    }
}