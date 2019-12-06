/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac;

public final class BindingCounter {

    private int reads;
    private int writes;

    public BindingCounter() {
        // Default constructor
    }

    public BindingCounter(BindingCounter counter) {
        // Copy constructor
        this.reads = counter.reads;
        this.writes = counter.writes;
    }

    public void newRead() {
        ++this.reads;
    }

    public void newWrite() {
        ++this.writes;
    }

    public int getReads() {
        return this.reads;
    }

    public int getWrites() {
        return this.writes;
    }
}