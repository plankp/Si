/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

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
    public void testHelpMessage() {
        final PrintStream oldOut = System.out;
        try {
            final ByteArrayOutputStream expected = new ByteArrayOutputStream();
            System.setOut(new PrintStream(expected));
            App.help();

            final ByteArrayOutputStream actual1 = new ByteArrayOutputStream();
            System.setOut(new PrintStream(actual1));
            App.main(new String[]{ "-h" });

            final ByteArrayOutputStream actual2 = new ByteArrayOutputStream();
            System.setOut(new PrintStream(actual2));
            App.main(new String[]{ "--help" });

            Assert.assertEquals(expected.toString(), actual1.toString());
            Assert.assertEquals(expected.toString(), actual2.toString());
        } finally {
            System.setOut(oldOut);
        }
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