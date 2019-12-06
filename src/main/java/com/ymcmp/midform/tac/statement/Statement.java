/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.value.*;

public interface Statement extends Serializable {

    public default boolean isPure() {
        // Asssume all statements to not be pure
        // In other words, cannot be optimized away
        return false;
    }

    public void validateType(Subroutine enclosingSubroutine);
    public void reachBlock(Map<Block, Integer> markedBlocks, Map<Binding, Integer> markedBindings);
    public Optional<Statement> unfoldConstants();

    public static void checkBindingDeclaration(Map<Binding, Integer> bindingMap, Value src) {
        if (src instanceof Tuple) {
            // Only tuples can potentially contain other bindings
            final Tuple tuple = (Tuple) src;
            for (final Value v : tuple.values) {
                checkBindingDeclaration(bindingMap, v);
            }
        } else if (src instanceof Binding) {
            if (bindingMap.getOrDefault((Binding) src, 0) == 0) {
                throw new RuntimeException("Using an unassigned binding: " + src);
            }
        }
    }

    public static void bumpAssignmentCounter(Map<Binding, Integer> bindingMap, Binding dst) {
        final int counter = bindingMap.getOrDefault(dst, 0);
        if (dst instanceof Binding.Immutable) {
            if (counter != 0) {
                throw new RuntimeException("Immutable binding: " + dst + " is assigned more than once!");
            }
        }

        // Increment the assignment counter (since we are doing an assignment)
        bindingMap.put(dst, counter + 1);
    }
}