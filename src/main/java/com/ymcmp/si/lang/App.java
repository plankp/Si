/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

import com.ymcmp.si.lang.grammar.SiLexer;
import com.ymcmp.si.lang.grammar.SiParser;

public class App {

    public static void main(String[] args) {
        System.out.println("Hi!");
    }

    public static void compile(CharStream stream) {
        SiLexer lexer = new SiLexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SiParser parser = new SiParser(tokens);

        GlobalSymbolVisitor visitor = new GlobalSymbolVisitor();
        visitor.visit(parser.file());
    }
}
