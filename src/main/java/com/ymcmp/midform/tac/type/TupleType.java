/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.type;

import static com.ymcmp.midform.tac.type.TypeUtils.checkListAssignableFrom;
import static com.ymcmp.midform.tac.type.TypeUtils.checkListEquivalent;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class TupleType extends CoreType {

    public final List<Type> elements;

    public TupleType(List<Type> elements) {
        if (elements == null || elements.size() < 2) {
            throw new IllegalArgumentException("Tuples cannot have less than two elments");
        }

        this.elements = Collections.unmodifiableList(elements);
    }

    public List<Type> getElements() {
        return this.elements;
    }

    public Type getElementAt(int idx) {
        return this.elements.get(idx);
    }

    public int numberOfElements() {
        return this.elements.size();
    }

    @Override
    public boolean assignableFrom(Type t) {
        if (t instanceof TupleType) {
            return checkListAssignableFrom(this.elements, ((TupleType) t).elements);
        }
        return false;
    }

    @Override
    public boolean equivalent(Type t) {
        if (t instanceof TupleType) {
            return checkListEquivalent(this.elements, ((TupleType) t).elements);
        }
        return false;
    }

    @Override
    public Type substitute(final Type from, final Type to) {
        if (this.equivalent(from)) {
            return to;
        }
        return new TupleType(this.elements.stream().map(e -> e.substitute(from, to)).collect(Collectors.toList()));
    }

    @Override
    public int hashCode() {
        return this.elements.hashCode();
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof TupleType) {
            return this.elements.equals(((TupleType) t).elements);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.elements.stream().map(Object::toString).collect(Collectors.joining(","));
    }
}