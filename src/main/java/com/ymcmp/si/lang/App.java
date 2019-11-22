/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import com.ymcmp.si.lang.grammar.SiLexer;
import com.ymcmp.si.lang.grammar.SiParser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class App {

    public static void main(String[] args) {
        System.out.println("Hi!");
    }

    public static void typeCheck(CharStream stream) {
        final SiLexer lexer = new SiLexer(stream);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final SiParser parser = new SiParser(tokens);

        final TypeChecker visitor = new TypeChecker();
        visitor.visit(parser.file());
    }
}
