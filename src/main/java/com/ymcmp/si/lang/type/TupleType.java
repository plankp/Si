/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

public final class TupleType implements Type {

    public final List<Type> elements;

    public TupleType(List<Type> elements) {
        if (elements == null || elements.isEmpty()) {
            elements = Collections.emptyList();
        } else {
            elements = Collections.unmodifiableList(elements);
        }

        this.elements = elements;
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
            return GenericType.checkListAssignableFrom(this.elements, ((TupleType) t).elements);
        }
        return false;
    }

    @Override
    public boolean equivalent(Type t) {
        if (t instanceof TupleType) {
            return GenericType.checkListEquivalent(this.elements, ((TupleType) t).elements);
        }
        return false;
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
        switch (this.elements.size()) {
        case 0:
            return "()";
        case 1:
            return "(" + this.elements.get(0) + ",)";
        default:
            return this.elements.stream().map(Object::toString).collect(Collectors.joining(",", "(", ")"));
        }
    }
}