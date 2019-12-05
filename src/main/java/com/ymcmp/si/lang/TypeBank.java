/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import com.ymcmp.midform.tac.type.Type;

import com.ymcmp.si.lang.type.ParametricType;
import com.ymcmp.si.lang.type.TypeMismatchException;

public final class TypeBank<T extends Type> {

    private T simple;
    private final LinkedHashMap<ParametricType<T>, Object> parametrics = new LinkedHashMap<>();

    public static <T extends Type> TypeBank<T> withSimpleType(T t) {
        final TypeBank<T> bank = new TypeBank<>();
        bank.setSimpleType(t);
        return bank;
    }

    public static <T extends Type> TypeBank<T> withParametricType(ParametricType<T> t) {
        final TypeBank<T> bank = new TypeBank<>();
        bank.addParametricType(t);
        return bank;
    }

    public static <T extends Type> TypeBank<T> withParametricTypes(List<ParametricType<T>> list) {
        final TypeBank<T> bank = new TypeBank<>();
        for (final ParametricType<T> p : list) {
            bank.addParametricType(p);
        }
        return bank;
    }

    public boolean hasSimpleType() {
        return this.simple != null;
    }

    public T getSimpleType() {
        if (!this.hasSimpleType()) {
            throw new UnboundDefinitionException("No simple type bound");
        }
        return this.simple;
    }

    public boolean hasParametricType() {
        return !this.parametrics.isEmpty();
    }

    public Set<ParametricType<T>> getParametricTypes() {
        return Collections.unmodifiableSet(this.parametrics.keySet());
    }

    public ParametricType<T> selectParametrization(final List<Type> types) {
        if (!this.hasParametricType()) {
            throw new TypeMismatchException("Attempt to parametrize on a non-parametric type");
        }

        // Error message
        final StringBuilder sb = new StringBuilder("Cannot find correct type to parametrize:");

        for (final ParametricType<T> type : this.parametrics.keySet()) {
            try {
                type.checkParametrization(types);
                return type;
            } catch (TypeMismatchException ex) {
                // continue loop, try next combination
                sb.append("\n- ").append(ex.getMessage());
            }
        }
        throw new TypeMismatchException(sb.toString());
    }

    public void setSimpleType(T t) {
        if (this.hasSimpleType()) {
            throw new DuplicateDefinitionException("Redefining type: " + this.simple);
        }
        this.simple = t;
    }

    public void addParametricType(ParametricType<T> t) {
        this.addParametricType(t, null);
    }

    public void addParametricType(ParametricType<T> t, Object mapping) {
        // TODO: Add more rigorous validation
        if (this.parametrics.containsKey(t)) {
            throw new IllegalArgumentException("Duplicate parametric type: " + t);
        }

        this.parametrics.put(t, mapping);
    }

    public Object getMapping(ParametricType<T> t) {
        return this.parametrics.get(t);
    }

    public void forEach(final Consumer<? super Type> consumer) {
        consumer.accept(this.simple);
        if (this.hasParametricType()) {
            this.parametrics.keySet().forEach(consumer);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.simple, this.parametrics);
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof TypeBank) {
            final TypeBank<?> bank = (TypeBank<?>) t;
            if (!Objects.equals(this.simple, bank.simple)) {
                return false;
            }
            if (!this.hasParametricType() && !bank.hasParametricType()) {
                return true;
            }
            return Objects.equals(this.parametrics, bank.parametrics);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Simple type: " + this.simple + " Parametric type(s): " + this.parametrics;
    }
}