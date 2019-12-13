/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class AppTest {

    @Test(expected = CompileTimeException.class)
    public void testInexistentInputFile() {
        final String file = "www.xxx.yyy.zzz";
        Assert.assertFalse(new File(file).exists());
        App.main(new String[]{ file });
    }

    @Test
    public void testEmitTACOnAllSpecFiles() {
        final File path = new File("./spec/");
        final File[] proclist = path.listFiles((file, name) -> name.endsWith(".si"));

        final File outdir = new File("./spec/tacgen/");
        outdir.mkdir();

        // generate the files then delete them
        for (final File file : proclist) {
            final String name = file.getAbsolutePath();
            final String output = outdir.getAbsolutePath() + '/' + file.getName() + ".tac";

            App.main(new String[] { "--emit-ir", "-t", "-o", output, name });
        }
    }

    @Test
    public void testEmitC99OnAllSpecFiles() {
        final File path = new File("./spec/");
        final File[] proclist = path.listFiles((file, name) -> name.endsWith(".si"));

        final File outdir = new File("./spec/c99gen/");
        outdir.mkdir();

        // generate the files then delete them
        for (final File file : proclist) {
            final String name = file.getAbsolutePath();
            final String output = outdir.getAbsolutePath() + '/' + file.getName() + ".c";

            // this one we intentionally *not* do any optimizations
            // to see if the code was generated correctly!
            App.main(new String[] { "--emit-c99", "-o", output, name });
        }
    }
}