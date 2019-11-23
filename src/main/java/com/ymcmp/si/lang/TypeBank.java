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

public final class TypeBank {

    private Type simple;
    private List<ParametricType> parametrics;

    private synchronized List<ParametricType> ensureParametricsList() {
        List<ParametricType> local = this.parametrics;
        if (local == null) {
            local = new ArrayList<>();
            this.parametrics = local;
        }
        return local;
    }

    public static TypeBank withSimpleType(Type t) {
        final TypeBank bank = new TypeBank();
        bank.setSimpleType(t);
        return bank;
    }

    public static TypeBank withParametricType(ParametricType t) {
        final TypeBank bank = new TypeBank();
        bank.addParametricType(t);
        return bank;
    }

    public static TypeBank withParametricTypes(List<ParametricType> list) {
        final TypeBank bank = new TypeBank();
        for (final ParametricType p : list) {
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
        final List<ParametricType> local = this.parametrics;
        return local == null || local.isEmpty();
    }

    public List<ParametricType> getParametricTypes() {
        return Collections.unmodifiableList(this.ensureParametricsList());
    }

    public Type getParametrization(final List<Type> types) {
        // Error message
        final StringBuilder sb = new StringBuilder("Cannot find correct type to parametrize:");

        for (final ParametricType type : this.ensureParametricsList()) {
            try {
                return type.parametrize(types);
            } catch (TypeMismatchException ex) {
                // continue loop, try next combination
                sb.append("\n- ").append(ex.getMessage());
            }
        }
        throw new TypeMismatchException(sb.toString());
    }

    public void setSimpleType(Type t) {
        if (this.hasSimpleType()) {
            throw new DuplicateDefinitionException("Redefining type: " + this.simple);
        }
        this.simple = t;
    }

    public void addParametricType(ParametricType t) {
        final List<ParametricType> list = this.ensureParametricsList();

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
            final TypeBank bank = (TypeBank) t;
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