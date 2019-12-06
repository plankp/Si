/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import java.io.Serializable;

import com.ymcmp.midform.tac.type.Type;

public abstract class Value implements Serializable {

    /* package */ Value() {
        // Note: Use package since we want to restrict
        // the amount of subclasses (sort of like enums)
    }

    public abstract Type getType();

    public abstract Value replaceBinding(Binding binding, Value t);
}