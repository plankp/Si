/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ymcmp.midform.tac.statement.*;
import com.ymcmp.midform.tac.value.*;
import com.ymcmp.midform.tac.type.*;

import org.junit.Assert;
import org.junit.Test;

public class OptimizationTest {

    @Test
    public void testNoTCOWhenTakingLocalReference() {
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
        //    call %2, swap_chr (%0, %1)    <-- stays a call,
        //    ret ()                        <-- otherwise corrupted stack
        // }

        final Subroutine subSwapChr = new Subroutine("", "swap_chr", new FunctionType(TupleType.from(ReferenceType.mutable(ImmCharacter.TYPE), ReferenceType.mutable(ImmCharacter.TYPE)), UnitType.INSTANCE));
        final Subroutine subCaller = new Subroutine("", "caller", new FunctionType(UnitType.INSTANCE, UnitType.INSTANCE));

        {
            // swap_chr function
            final Binding.Parameter a = new Binding.Parameter("a", ReferenceType.mutable(ImmCharacter.TYPE));
            final Binding.Parameter b = new Binding.Parameter("b", ReferenceType.mutable(ImmCharacter.TYPE));

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
                    new ReturnStatement(ImmUnit.INSTANCE)));

            subCaller.setInitialBlock(entry);
        }

        subSwapChr.optimize();
        subCaller.optimize();

        // check last statement must be an ordinary return:
        // since TCO not applied, shouldn't be a tailcall
        final List<Statement> stmts = subCaller.getInitialBlock().getStatements();
        Assert.assertTrue(stmts.get(stmts.size() - 1) instanceof ReturnStatement);
    }
}
