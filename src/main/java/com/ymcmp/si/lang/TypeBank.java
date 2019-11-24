/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.ymcmp.si.lang.type.ParametricType;
import com.ymcmp.si.lang.type.Type;
import com.ymcmp.si.lang.type.TypeMismatchException;

public final class TypeBank<T extends Type> {

    private T simple;
    private List<ParametricType<T>> parametrics;

    private synchronized List<ParametricType<T>> ensureParametricsList() {
        List<ParametricType<T>> local = this.parametrics;
        if (local == null) {
            local = new ArrayList<>();
            this.parametrics = local;
        }
        return local;
    }

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

    public Type getSimpleType() {
        if (!this.hasSimpleType()) {
            throw new UnboundDefinitionException("No simple type bound");
        }
        return this.simple;
    }

    public boolean hasParametricType() {
        final List<ParametricType<T>> local = this.parametrics;
        return !(local == null || local.isEmpty());
    }

    public List<ParametricType<T>> getParametricTypes() {
        return Collections.unmodifiableList(this.ensureParametricsList());
    }

    public ParametricType<T> selectParametrization(final List<Type> types) {
        if (!this.hasParametricType()) {
            throw new TypeMismatchException("Attempt to parametrize on a non-parametric type");
        }

        // Error message
        final StringBuilder sb = new StringBuilder("Cannot find correct type to parametrize:");

        for (final ParametricType<T> type : this.ensureParametricsList()) {
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
        final List<ParametricType<T>> list = this.ensureParametricsList();

        // TODO: Add more rigorous validation
        list.add(t);
    }

    public void forEach(final Consumer<? super Type> consumer) {
        consumer.accept(this.simple);
        if (this.hasParametricType()) {
            this.parametrics.forEach(consumer);
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