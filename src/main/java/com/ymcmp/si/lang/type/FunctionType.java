/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

public final class FunctionType implements Type {

    public final Type input;
    public final Type output;

    public FunctionType(Type input, Type output) {
        if (input == null) {
            throw new IllegalArgumentException("Function input type cannot be null");
        }
        if (output == null) {
            throw new IllegalArgumentException("Function output type cannot be null");
        }

        this.input = input;
        this.output = output;
    }

    public Type getInput() {
        return this.input;
    }

    public Type getOutput() {
        return this.output;
    }

    @Override
    public boolean assignableFrom(Type t) {
        if (t instanceof FunctionType) {
            final FunctionType ft = (FunctionType) t;

            // input needs to be less restrictive
            // output needs to be more restrictive
            //
            // Assuming a hierarchy like:
            // Student <: Person <: WorldEntity
            // Then this should be allowed:
            // (Person)Person <: (WorldEntity)Student
            return ft.input.assignableFrom(this.input) && this.output.assignableFrom(ft.output);
        }
        return false;
    }

    @Override
    public boolean equivalent(Type t) {
        if (t instanceof FunctionType) {
            final FunctionType ft = (FunctionType) t;
            return this.input.equivalent(ft.input) && this.output.equivalent(ft.output);
        }
        return false;
    }

    @Override
    public Type substitute(Type from, Type to) {
        final Type sin = this.input.substitute(from, to);
        final Type sout = this.output.substitute(from, to);
        return this.input == sin && this.output == sout ? this : new FunctionType(sin, sout);
    }

    @Override
    public int hashCode() {
        return this.input.hashCode() * 17 + this.output.hashCode();
    }

    @Override
    public boolean equals(Object t) {
        if (t instanceof FunctionType) {
            final FunctionType ft = (FunctionType) t;
            return this.input.equals(ft.input) && this.output.equals(ft.output);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.input.toString() + "->" + this.output.toString();
    }
}
