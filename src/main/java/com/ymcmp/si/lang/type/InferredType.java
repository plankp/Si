/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

import java.util.Objects;

import com.ymcmp.midform.tac.type.Type;

public final class InferredType implements ExtensionType {

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
    public boolean assignableFrom(Type t) {
        if (this.hasInferredType()) {
            return this.inner.assignableFrom(t);
        }

        this.setInferredType(t);
        return true;
    }

    @Override
    public boolean equivalent(Type t) {
        if (this.inner != null && t.equivalent(this.inner)) {
            return true;
        }
        return this.equivalent(t);
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
        return this.inner != null ? this.inner.toString() : ":";
    }
}