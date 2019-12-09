/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import static com.ymcmp.midform.tac.type.Types.equivalent;

import java.util.Map;
import java.util.Optional;

import com.ymcmp.midform.tac.BindingCounter;
import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.type.ReferenceType;
import com.ymcmp.midform.tac.value.Binding;
import com.ymcmp.midform.tac.value.Value;

public final class LoadRefStatement implements Statement {

    public final Binding dst;
    public final Binding ref;

    public LoadRefStatement(Binding dst, Binding ref) {
        this.dst = dst;
        this.ref = ref;
    }

    @Override
    public Optional<Binding> getResultRegister() {
        return Optional.of(this.dst);
    }

    @Override
    public boolean isPure() {
        // depends on the mutablility of the reference
        return ((ReferenceType) ref.getType()).isReferentImmutable();
    }

    @Override
    public void validateType(Subroutine s) {
        final Type ref = this.ref.getType();

        if (!(ref instanceof ReferenceType)) {
            throw new RuntimeException("Load ref type mismatch: expected a form of ReferenceType got: " + ref);
        }

        final Type expected = dst.getType();
        final Type actual = ((ReferenceType) ref).getReferentType();
        if (!equivalent(expected, actual)) {
            throw new RuntimeException("Load ref type mismatch: expected: " + expected + " got referent of: " + actual);
        }
    }

    @Override
    public void reachBlock(Map<Block, Integer> marked, Map<Binding, BindingCounter> bindings) {
        Statement.checkBindingDeclaration(bindings, this.ref);
        Statement.bumpAssignmentCounter(bindings, this.dst);
    }

    @Override
    public Statement replaceRead(Binding binding, Value repl) {
        // Do not replace this.ref with binding!
        // need to make sure we are loading the correct reference!
        return this;
    }

    @Override
    public Statement unfoldConstants() {
        // Nothing to unfold
        return this;
    }

    @Override
    public String toString() {
        return "ldref " + dst + ", " + ref;
    }
}