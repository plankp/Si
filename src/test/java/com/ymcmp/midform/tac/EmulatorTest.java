/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac;

import java.util.Arrays;
import java.util.Collections;

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
    public void testSynthSwap() {
        // function swap_chr(a, b) {
        // _entry:
        //   ldref %0, a
        //   ldref %1, b
        //   stref a, %1
        //   stref b, %0
        //   ret ()
        // }
        //
        // function caller() {
        // _entry:
        //    mov m0, 'A'
        //    mov m1, 'B'
        //    mkref %0, m0
        //    mkref %1, m1
        //    call %2, swap_chr (%0, %1)
        //    ret (m0, m1)
        // }

        final Subroutine subSwapChr = new Subroutine("swap_chr", new FunctionType(TupleType.from(ReferenceType.mutable(ImmCharacter.TYPE), ReferenceType.mutable(ImmCharacter.TYPE)), UnitType.INSTANCE));
        final Subroutine subCaller = new Subroutine("caller", new FunctionType(UnitType.INSTANCE, TupleType.from(ImmCharacter.TYPE, ImmCharacter.TYPE)));

        {
            // swap_chr function
            final Binding.Immutable a = new Binding.Immutable("a", ReferenceType.mutable(ImmCharacter.TYPE));
            final Binding.Immutable b = new Binding.Immutable("b", ReferenceType.mutable(ImmCharacter.TYPE));

            subSwapChr.setParameters(Arrays.asList(a, b));

            final Block entry = new Block("_entry");

            final Binding.Immutable t0 = new Binding.Immutable("%0", ImmCharacter.TYPE);
            final Binding.Immutable t1 = new Binding.Immutable("%1", ImmCharacter.TYPE);

            entry.setStatements(Arrays.asList(
                    new LoadRefStatement(t0, a),
                    new LoadRefStatement(t1, b),
                    new StoreRefStatement(a, t1),
                    new StoreRefStatement(b, t0),
                    new ReturnStatement(ImmUnit.INSTANCE)));

            subSwapChr.setInitialBlock(entry);
        }

        {
            // caller function
            final Block entry = new Block("_entry");

            final Binding.Mutable m0 = new Binding.Mutable("m0", ImmCharacter.TYPE);
            final Binding.Mutable m1 = new Binding.Mutable("m1", ImmCharacter.TYPE);

            final Binding.Immutable t0 = new Binding.Immutable("%0", ReferenceType.mutable(ImmCharacter.TYPE));
            final Binding.Immutable t1 = new Binding.Immutable("%1", ReferenceType.mutable(ImmCharacter.TYPE));
            final Binding.Immutable t2 = new Binding.Immutable("%2", UnitType.INSTANCE);

            entry.setStatements(Arrays.asList(
                    new MoveStatement(m0, new ImmCharacter('A')),
                    new MoveStatement(m1, new ImmCharacter('B')),
                    new MakeRefStatement(t0, m0),
                    new MakeRefStatement(t1, m1),
                    new CallStatement(t2, new FuncRef.Local(subSwapChr), Tuple.from(t0, t1)),
                    new ReturnStatement(Tuple.from(m0, m1))));

            subCaller.setInitialBlock(entry);
        }

        subSwapChr.validate();
        subCaller.validate();
        Assert.assertEquals(Tuple.from(new ImmCharacter('B'), new ImmCharacter('A')), this.emulator.callSubroutine(subCaller));

        subSwapChr.optimize();
        subCaller.optimize();
        Assert.assertEquals(Tuple.from(new ImmCharacter('B'), new ImmCharacter('A')), this.emulator.callSubroutine(subCaller));
    }
}
