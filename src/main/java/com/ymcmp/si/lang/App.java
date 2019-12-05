/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.Arrays;
import java.util.Collections;

import com.ymcmp.si.lang.grammar.SiLexer;
import com.ymcmp.si.lang.grammar.SiParser;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.statement.*;
import com.ymcmp.midform.tac.value.*;
import com.ymcmp.midform.tac.type.*;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class App {

    public static void main(String[] args) {
        // main:
        //   mov %0, "Hello, world!"
        //   call %1, print_str %0
        //   ret ()
        final Subroutine sub = new Subroutine("main", new FunctionType(UnitType.INSTANCE, UnitType.INSTANCE));
        final Block entry = new Block("_entry");
        final Binding.Immutable t0 = new Binding.Immutable("%0", ImmString.TYPE);
        entry.setStatements(Arrays.asList(
                new MoveStatement(t0, new ImmString("Hello, world!")),
                new CallStatement(new Binding.Immutable("%1", UnitType.INSTANCE), new FuncRef.Native("print_str", new FunctionType(ImmString.TYPE, UnitType.INSTANCE)), t0),
                new ReturnStatement(ImmUnit.INSTANCE)));
        sub.setBlocks(Collections.singletonList(entry));

        System.out.println(sub);
    }
}
