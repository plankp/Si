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

public final class MakeRefStatement implements Statement {

    public final Binding dst;
    public final Binding src;

    public MakeRefStatement(Binding dst, Binding src) {
        this.dst = dst;
        this.src = src;
    }

    @Override
    public Optional<Binding> getResultRegister() {
        return Optional.of(this.dst);
    }

    @Override
    public boolean isPure() {
        return true;
    }

    @Override
    public void validateType(Subroutine s) {
        final Type ref = dst.getType();

        if (!(ref instanceof ReferenceType)) {
            throw new RuntimeException("Make ref type mismatch: expected a form of ReferenceType got: " + ref);
        }

        final Type expected = ((ReferenceType) ref).getReferentType();
        final Type actual = src.getType();
        if (!equivalent(expected, actual)) {
            throw new RuntimeException("Make ref type mismatch: expected referent of: " + ref + " got: " + actual);
        }
    }

    @Override
    public void reachBlock(Map<Block, Integer> marked, Map<Binding, BindingCounter> bindings) {
        Statement.checkBindingDeclaration(bindings, this.src);
        Statement.bumpAssignmentCounter(bindings, this.dst);
    }

    @Override
    public Statement replaceRead(Binding binding, Value repl) {
        // Do not replace this.src with binding!
        // need to make sure we are taking the reference of the correct thing!
        return this;
    }

    @Override
    public Statement unfoldConstants() {
        // Nothing to unfold
        return this;
    }

    @Override
    public String toString() {
        return "mkref " + dst + ", " + src;
    }
}