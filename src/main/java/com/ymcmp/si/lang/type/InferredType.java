/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

import java.util.Objects;

import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.type.Types;

public final class InferredType extends ExtensionType {

    private Type inner;

    public void setInferredType(Type t) {
        this.inner = t;
    }

    public boolean hasInferredType() {
        return this.inner != null;
    }

    public Type getInferredType() {
        return inner;
    }

    @Override
    public Type expandBound() {
        return this.inner != null ? this.inner : this;
    }

    @Override
    protected boolean assignableFrom(Type t) {
        if (this.hasInferredType()) {
            return Types.assignableFrom(this.inner, t);
        }

        this.setInferredType(t);
        return true;
    }

    @Override
    protected boolean equivalent(Type obj) {
        if (obj instanceof InferredType) {
            final Type other = ((InferredType) obj).inner;
            if (this.inner == other) return true;
            return Types.assignableFrom(this.inner, other);
        }
        if (this.inner != null) {
            return Types.assignableFrom(this.inner, obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.inner);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InferredType) {
            return Objects.equals(this.inner, ((InferredType) obj).inner);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.inner != null ? ':' + this.inner.toString() : ":";
    }
}