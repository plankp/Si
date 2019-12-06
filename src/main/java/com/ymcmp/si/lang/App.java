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
        // function main() {
        // _entry:
        //   mov %0, "Hello, world!"
        //   call %1, print_str %0
        //   ret ()
        // }
        {
            final Subroutine subMain = new Subroutine("main", new FunctionType(UnitType.INSTANCE, UnitType.INSTANCE));
            final Block entry = new Block("_entry");
            final Binding.Immutable t0 = new Binding.Immutable("%0", ImmString.TYPE);
            entry.setStatements(Arrays.asList(
                    new MoveStatement(t0, new ImmString("Hello, world!")),
                    new CallStatement(new Binding.Immutable("%1", UnitType.INSTANCE), new FuncRef.Native("print_str", new FunctionType(ImmString.TYPE, UnitType.INSTANCE)), t0),
                    new ReturnStatement(ImmUnit.INSTANCE)));
            subMain.setBlocks(Collections.singletonList(entry));

            subMain.validate();
            subMain.optimize();

            System.out.println(subMain);
        }

        // function counter() {
        // _entry:
        //   mov mut_i, 0
        //   jmp loop
        // loop:
        //   lt.ii incr, end, mut_i, 10
        // incr:
        //   add.ii mut_i, mut_i, 1
        //   jmp loop
        // end:
        //   ret ()
        // }
        {
            final Subroutine subCounter = new Subroutine("counter", new FunctionType(UnitType.INSTANCE, UnitType.INSTANCE));
            final Block entry = new Block("_entry");
            final Block loop = new Block("loop");
            final Block incr = new Block("incr");
            final Block end = new Block("end");
            final Binding.Mutable m0 = new Binding.Mutable("mut_i", ImmInteger.TYPE);

            entry.setStatements(Arrays.asList(
                    new MoveStatement(m0, new ImmInteger(0)),
                    new GotoStatement(loop)));
            loop.setStatements(Collections.singletonList(
                    new ConditionalJumpStatement(ConditionalJumpStatement.ConditionalOperator.LT_II, incr, end, m0, new ImmInteger(10))));
            incr.setStatements(Arrays.asList(
                    new BinaryStatement(BinaryStatement.BinaryOperator.ADD_II, m0, m0, new ImmInteger(1)),
                    new GotoStatement(loop)));
            end.setStatements(Collections.singletonList(
                    new ReturnStatement(ImmUnit.INSTANCE)));

            subCounter.setBlocks(Arrays.asList(entry, loop, incr, end));

            subCounter.validate();
            subCounter.optimize();

            System.out.println(subCounter);
        }
    }
}
