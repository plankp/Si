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
import com.ymcmp.midform.tac.codegen.*;
import com.ymcmp.midform.tac.statement.*;
import com.ymcmp.midform.tac.value.*;
import com.ymcmp.midform.tac.type.*;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class App {

    public static final FunctionType ENTRY_SIG = new FunctionType(UnitType.INSTANCE, IntegerType.INT8);

    public static void main(String[] args) {
        boolean emitTAC = false;
        boolean emitC99 = false;
        boolean optimize = false;
        String outName = "out";
        String entryName = null;
        LinkedList<String> inName = new LinkedList<>();

        boolean previewTC = false;

        boolean readOutFile = false;
        boolean readEntryPoint = false;
        for (int i = 0; i < args.length; ++i) {
            final String arg = args[i];

            if (readOutFile) {
                outName = arg;
                readOutFile = false;
                continue;
            }

            if (readEntryPoint) {
                entryName = arg;
                readEntryPoint = false;
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
                    case "-e":
                        readEntryPoint = true;
                        break;
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
                    case "--only-tc":
                        previewTC = true;
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

        if (previewTC) {
            final TypeChecker compiler = new TypeChecker();
            String name;
            while ((name = inName.pollFirst()) != null) {
                compiler.loadSource(name);
            }

            compiler.processLoadedModules();
            return;
        }

        final LegacyTypeChecker compiler = new LegacyTypeChecker();
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

        final LinkedList<CodeGenerator> codegens = new LinkedList<>();

        if (emitTAC || !emitTAC && !emitC99) {
            codegens.addLast(new TACGenerator());
        }
        if (emitC99) {
            codegens.addLast(new C99Generator());
        }

        // then we check our entry point:
        if (entryName != null) {
            final Subroutine entry = ifuncs.get(entryName);
            if (entry == null) {
                System.err.println("error: unknown entry point: '" + entryName + "'");
                return;
            }
            if (!Types.equivalent(entry.type, ENTRY_SIG)) {
                System.err.println("error: illegal signature for entry point: '" + entryName + "'");
                return;
            }

            // Register the entry point onto each code generator
            for (final CodeGenerator codegen : codegens) {
                codegen.addEntryPoint(entry);
            }
        }

        try (final PrintStream pw = outName == null ? System.out : new PrintStream(outName)) {
            CodeGenerator codegen;
            while ((codegen = codegens.pollFirst()) != null) {
                for (final Subroutine sub : ifuncs.values()) {
                    codegen.visitSubroutine(sub);
                }

                pw.println(codegen.getGenerated());
                codegen.reset();
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
        System.out.println(" --stdout           Write output to standard output stream");
        System.out.println(" --emit-ir          Emit internal representation (default)");
        System.out.println(" --emit-c99         Emit C99 code");
        System.out.println(" -e <func>          Specifies the entry point, must have signature " + ENTRY_SIG);
        System.out.println(" -t                 Premature optimize code");
        System.out.println();
        System.out.println(" --only-tc          Use the experimental type checker");
    }
}
