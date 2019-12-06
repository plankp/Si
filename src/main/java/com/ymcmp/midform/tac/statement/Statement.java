/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import com.ymcmp.midform.tac.BindingCounter;
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
    public void reachBlock(Map<Block, Integer> markedBlocks, Map<Binding, BindingCounter> markedBindings);
    public Optional<Statement> replaceRead(Binding.Immutable binding, Value value);
    public Optional<Statement> unfoldConstants();

    public static void checkBindingDeclaration(Map<Binding, BindingCounter> bindingMap, Value src) {
        if (src instanceof Tuple) {
            // Only tuples can potentially contain other bindings
            final Tuple tuple = (Tuple) src;
            for (final Value v : tuple.values) {
                checkBindingDeclaration(bindingMap, v);
            }
        } else if (src instanceof Binding) {
            final BindingCounter counter = bindingMap.computeIfAbsent((Binding) src, k -> new BindingCounter());
            if (counter.getWrites() == 0) {
                throw new RuntimeException("Using an unassigned binding: " + src);
            }

            // Issue a read command
            counter.newRead();
        }
    }

    public static void bumpAssignmentCounter(Map<Binding, BindingCounter> bindingMap, Binding dst) {
        final BindingCounter counter = bindingMap.computeIfAbsent(dst, k -> new BindingCounter());
        if (dst instanceof Binding.Immutable) {
            if (counter.getWrites() != 0) {
                throw new RuntimeException("Immutable binding: " + dst + " is assigned more than once!");
            }
        }

        // Issue a write command
        counter.newWrite();
    }
}