/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import java.io.Serializable;

import com.ymcmp.midform.tac.value.Value;

public interface Statement extends Serializable {

    public default boolean isPure() {
        // Asssume all statements to not be pure
        // In other words, cannot be optimized away
        return false;
    }
}