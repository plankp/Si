/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac;

import java.util.Arrays;
import java.util.Collections;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Emulator;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.statement.*;
import com.ymcmp.midform.tac.value.*;
import com.ymcmp.midform.tac.type.*;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

public class EmulatorTest {

    private Emulator emulator;
    private int printStrCalls;
    private int printIntCalls;

    private void resetCallCount() {
        this.printStrCalls = 0;
        this.printIntCalls = 0;
    }

    @Before
    public void setupEmulator() {
        this.resetCallCount();
        this.emulator = new Emulator();

        emulator.addExternalCallHandler("print_str", fargs -> {
            // dont print since this is tests
            this.printStrCalls++;
            return ImmUnit.INSTANCE;
        });
        emulator.addExternalCallHandler("print_int", fargs -> {
            // dont print since this is tests
            this.printIntCalls++;
            return ImmUnit.INSTANCE;
        });
    }

    @Test
    public void testSynthHelloWorld() {
        // function main() {
        // _entry:
        //   mov %0, "Hello, world!"
        //   call %1, print_str %0
        //   ret ()
        // }

        final Subroutine subMain = new Subroutine("main", new FunctionType(UnitType.INSTANCE, UnitType.INSTANCE));
        final Block entry = new Block("_entry");
        final Binding.Immutable t0 = new Binding.Immutable("%0", ImmString.TYPE);
        final Binding.Immutable t1 = new Binding.Immutable("%1", UnitType.INSTANCE);
        entry.setStatements(Arrays.asList(
                new MoveStatement(t0, new ImmString("Hello, world!")),
                new CallStatement(t1, new FuncRef.Native("print_str", new FunctionType(ImmString.TYPE, UnitType.INSTANCE)), t0),
                new ReturnStatement(ImmUnit.INSTANCE)));
        subMain.setInitialBlock(entry);

        subMain.validate();
        this.resetCallCount();
        Assert.assertEquals(ImmUnit.INSTANCE, this.emulator.callSubroutine(subMain, ImmUnit.INSTANCE));
        Assert.assertEquals(1, this.printStrCalls);
        Assert.assertEquals(0, this.printIntCalls);

        subMain.optimize();
        this.resetCallCount();
        Assert.assertEquals(ImmUnit.INSTANCE, this.emulator.callSubroutine(subMain, ImmUnit.INSTANCE));
        Assert.assertEquals(1, this.printStrCalls);
        Assert.assertEquals(0, this.printIntCalls);
    }

    @Test
    public void testSynthCounter() {
        // function counter() {
        // _entry:
        //   mov mut_i, 0
        //   jmp loop
        // loop:
        //   lt.ii incr, end, mut_i, 10
        // incr:
        //   mov %0, mut_i
        //   add.ii mut_i, mut_i, 1
        //   add.ii mut_i, %0, 1
        //   jmp loop
        // end:
        //   call %1, print_int mut_i
        //   ret mut_i
        // }

        final Subroutine subCounter = new Subroutine("counter", new FunctionType(UnitType.INSTANCE, ImmInteger.TYPE));
        final Block entry = new Block("_entry");
        final Block loop = new Block("loop");
        final Block incr = new Block("incr");
        final Block end = new Block("end");
        final Binding.Immutable t0 = new Binding.Immutable("%0", ImmInteger.TYPE);
        final Binding.Immutable t1 = new Binding.Immutable("%1", UnitType.INSTANCE);
        final Binding.Mutable m0 = new Binding.Mutable("mut_i", ImmInteger.TYPE);

        entry.setStatements(Arrays.asList(
                new MoveStatement(m0, new ImmInteger(0)),
                new GotoStatement(loop)));
        loop.setStatements(Collections.singletonList(
                new ConditionalJumpStatement(ConditionalJumpStatement.ConditionalOperator.LT_II, incr, end, m0, new ImmInteger(10))));
        incr.setStatements(Arrays.asList(
                new MoveStatement(t0, m0),
                new BinaryStatement(BinaryStatement.BinaryOperator.ADD_II, m0, m0, new ImmInteger(1)),
                new BinaryStatement(BinaryStatement.BinaryOperator.ADD_II, m0, t0, new ImmInteger(1)),
                new GotoStatement(loop)));
        end.setStatements(Arrays.asList(
                new CallStatement(t1, new FuncRef.Native("print_int", new FunctionType(ImmInteger.TYPE, UnitType.INSTANCE)), m0),
                new ReturnStatement(m0)));

        subCounter.setInitialBlock(entry);

        subCounter.validate();
        this.resetCallCount();
        Assert.assertEquals(new ImmInteger(10), this.emulator.callSubroutine(subCounter, ImmUnit.INSTANCE));
        Assert.assertEquals(0, this.printStrCalls);
        Assert.assertEquals(1, this.printIntCalls);

        subCounter.optimize();
        this.resetCallCount();
        Assert.assertEquals(new ImmInteger(10), this.emulator.callSubroutine(subCounter, ImmUnit.INSTANCE));
        Assert.assertEquals(0, this.printStrCalls);
        Assert.assertEquals(1, this.printIntCalls);
    }

    @Test
    public void testSynthEvenOdd() {
        // function is_odd(n) {
        // _entry:
        //   eq.ii %b0, %b1, n, 0
        // %b0:
        //   ret false
        // %b1:
        //   sub.ii %t0, n, 1
        //   tailcall is_even %t0
        // }
        //
        // function is_even(n) {
        // _entry:
        //   eq.ii %b0, %b1, n, 0
        // %b0:
        //   ret true
        // %b1:
        //   sub.ii %t0, n, 1
        //   tailcall is_odd %t0
        // }

        final Subroutine subIsOdd = new Subroutine("is_odd", new FunctionType(ImmInteger.TYPE, ImmBoolean.TYPE));
        final Subroutine subIsEven = new Subroutine("is_even", new FunctionType(ImmInteger.TYPE, ImmBoolean.TYPE));

        {
            // is_odd function
            final Binding.Immutable n = new Binding.Immutable("n", ImmInteger.TYPE);
            subIsOdd.setParameters(Collections.singletonList(n));

            final Block entry = new Block("_entry");
            final Block b0 = new Block("%b0");
            final Block b1 = new Block("%b1");


            final Binding.Immutable t0 = new Binding.Immutable("%t0", ImmInteger.TYPE);

            entry.setStatements(Collections.singletonList(
                    new ConditionalJumpStatement(ConditionalJumpStatement.ConditionalOperator.EQ_II, b0, b1, n, new ImmInteger(0))));

            b0.setStatements(Collections.singletonList(
                    new ReturnStatement(new ImmBoolean(false))));

            b1.setStatements(Arrays.asList(
                    new BinaryStatement(BinaryStatement.BinaryOperator.SUB_II, t0, n, new ImmInteger(1)),
                    new TailCallStatement(new FuncRef.Local(subIsEven), t0)));

            subIsOdd.setInitialBlock(entry);
        }

        {
            // is_even function
            final Binding.Immutable n = new Binding.Immutable("n", ImmInteger.TYPE);
            subIsEven.setParameters(Collections.singletonList(n));

            final Block entry = new Block("_entry");
            final Block b0 = new Block("%b0");
            final Block b1 = new Block("%b1");


            final Binding.Immutable t0 = new Binding.Immutable("%t0", ImmInteger.TYPE);

            entry.setStatements(Collections.singletonList(
                    new ConditionalJumpStatement(ConditionalJumpStatement.ConditionalOperator.EQ_II, b0, b1, n, new ImmInteger(0))));

            b0.setStatements(Collections.singletonList(
                    new ReturnStatement(new ImmBoolean(true))));

            b1.setStatements(Arrays.asList(
                    new BinaryStatement(BinaryStatement.BinaryOperator.SUB_II, t0, n, new ImmInteger(1)),
                    new TailCallStatement(new FuncRef.Local(subIsOdd), t0)));

            subIsEven.setInitialBlock(entry);
        }

        subIsOdd.validate();
        subIsEven.validate();
        Assert.assertEquals(new ImmBoolean(false), this.emulator.callSubroutine(subIsOdd, new ImmInteger(6)));
        Assert.assertEquals(new ImmBoolean(true), this.emulator.callSubroutine(subIsEven, new ImmInteger(6)));
        Assert.assertEquals(new ImmBoolean(true), this.emulator.callSubroutine(subIsOdd, new ImmInteger(5)));
        Assert.assertEquals(new ImmBoolean(false), this.emulator.callSubroutine(subIsEven, new ImmInteger(5)));

        subIsOdd.optimize();
        subIsEven.optimize();
        Assert.assertEquals(new ImmBoolean(false), this.emulator.callSubroutine(subIsOdd, new ImmInteger(6)));
        Assert.assertEquals(new ImmBoolean(true), this.emulator.callSubroutine(subIsEven, new ImmInteger(6)));
        Assert.assertEquals(new ImmBoolean(true), this.emulator.callSubroutine(subIsOdd, new ImmInteger(5)));
        Assert.assertEquals(new ImmBoolean(false), this.emulator.callSubroutine(subIsEven, new ImmInteger(5)));
    }
}
