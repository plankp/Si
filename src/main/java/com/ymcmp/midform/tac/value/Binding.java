/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

public abstract class Binding extends Value {

    public final String name;

    private Binding(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static final class Immutable extends Binding {

        public Immutable(String name) {
            super(name);
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Immutable) {
                final Immutable lbl = (Immutable) obj;
                return this.name.equals(lbl.name);
            }
            return false;
        }
    }

    public static final class Mutable extends Binding {

        public Mutable(String name) {
            super(name);
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Mutable) {
                final Mutable lbl = (Mutable) obj;
                return this.name.equals(lbl.name);
            }
            return false;
        }
    }
}