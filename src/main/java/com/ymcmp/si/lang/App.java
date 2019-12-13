/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.io.PrintStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

import com.ymcmp.si.lang.grammar.SiLexer;
import com.ymcmp.si.lang.grammar.SiParser;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Emulator;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.codegen.C99Generator;
import com.ymcmp.midform.tac.statement.*;
import com.ymcmp.midform.tac.value.*;
import com.ymcmp.midform.tac.type.*;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class App {

    public static void main(String[] args) {
        boolean emitTAC = false;
        boolean emitC99 = false;
        boolean optimize = false;
        String outName = "out";
        LinkedList<String> inName = new LinkedList<>();

        boolean readOutFile = false;
        for (int i = 0; i < args.length; ++i) {
            final String arg = args[i];

            if (readOutFile) {
                outName = arg;
                readOutFile = false;
                continue;
            }

            if (arg.charAt(0) == '-') {
                switch (arg) {
                    case "-h":
                    case "--help":
                        help();
                        return;
                    case "-o":
                        readOutFile = true;
                        break;
                    case "-":
                    case "--stdout":
                        outName = null;
                        break;
                    case "--emit-ir":
                        emitTAC = true;
                        break;
                    case "--emit-c99":
                        emitC99 = true;
                        break;
                    case "-t":
                        optimize = true;
                        break;
                    default:
                        System.err.println("error: unknown argument: '" + arg + "'");
                        return;
                }
                continue;
            }

            inName.add(arg);
        }

        if (inName.isEmpty()) {
            System.err.println("error: no input files");
            return;
        }

        final TypeChecker compiler = new TypeChecker();
        String name;
        while ((name = inName.pollFirst()) != null) {
            compiler.loadSource(name);
        }

        compiler.processLoadedModules();

        final Map<String, Subroutine> ifuncs = compiler.getAllInstantiatedFunctions();
        if (optimize) {
            boolean restart = true;
            while (restart) {
                restart = false;
                for (final Subroutine sub : ifuncs.values()) {
                    restart |= sub.optimize();
                }
            }
        }

        try (final PrintStream pw = outName == null ? System.out : new PrintStream(outName)) {
            if (emitTAC || !emitTAC && !emitC99) {
                for (final Subroutine sub : ifuncs.values()) {
                    pw.println(sub.toString());
                    pw.println();                   // add blank line
                }
            }

            if (emitC99) {
                final C99Generator codegen = new C99Generator();
                for (final Subroutine sub : ifuncs.values()) {
                    codegen.visitSubroutine(sub);
                }

                pw.println(codegen.getGenerated());
            }
        } catch (IOException ex) {
            System.err.println("error: " + ex.getMessage());
        }
    }

    public static void help() {
        System.out.println("usage: Si [options...] file");
        System.out.println("options:");
        System.out.println(" -h, --help         Print this help message");
        System.out.println(" -o <file>          Write output to <file>");
        System.out.println(" -, --stdout        Write output to standard output stream");
        System.out.println(" --emit-ir          Emit internal representation (default)");
        System.out.println(" --emit-c99         Emit C99 code");
        System.out.println(" -t                 Premature optimize code");
    }
}
