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

public class ValidationTest {

    @Test(expected = RuntimeException.class)
    public void testStoreToImmutableReference() {
        // function swap_chr(a, b) {
        // _entry:
        //   ldref %0, a
        //   ldref %1, b
        //   stref a, %1
        //   stref b, %0
        //   ret ()
        // }

        final Subroutine subSwapChr = new Subroutine("swap_chr", new FunctionType(TupleType.from(ReferenceType.immutable(ImmCharacter.TYPE), ReferenceType.immutable(ImmCharacter.TYPE)), UnitType.INSTANCE));

        {
            // swap_chr function
            final Binding.Immutable a = new Binding.Immutable("a", ReferenceType.immutable(ImmCharacter.TYPE));
            final Binding.Immutable b = new Binding.Immutable("b", ReferenceType.immutable(ImmCharacter.TYPE));

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

        subSwapChr.validate();
    }
}