/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.type;

public abstract class CoreType implements Type {

    /* package */ CoreType() {
        // Note: Use package since we want to restrict
        // the amount of subclasses (sort of like enums)
    }
}