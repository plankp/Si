/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import org.antlr.v4.runtime.CharStreams;

import org.junit.Test;
import org.junit.Assert;

public class AppTest {

    // @Test
    // public void testCompile() {
    //     try {
    //         // Don't use this file. It has lot's of unimplemented features
    //         App.compile(CharStreams.fromStream(this.getClass().getResourceAsStream("/sample.si")));
    //     } catch (java.io.IOException ex) {
    //         Assert.fail("Wut!? IOException should not happen: " + ex.getMessage());
    //     }
    // }

    @Test
    public void testTypesSi() {
        try {
            App.compile(CharStreams.fromStream(this.getClass().getResourceAsStream("/types.si")));
        } catch (java.io.IOException ex) {
            Assert.fail("Wut!? IOException should not happen: " + ex.getMessage());
        }
    }
}
