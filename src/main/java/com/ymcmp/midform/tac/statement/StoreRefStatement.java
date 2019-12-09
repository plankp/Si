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

public final class StoreRefStatement implements Statement {

    public final Binding ref;
    public final Value src;

    public StoreRefStatement(Binding ref, Value src) {
        this.ref = ref;
        this.src = src;
    }

    @Override
    public Optional<Binding> getResultRegister() {
        // this.ref is not the result register,
        // it's the referent that is the result register
        // (which we don't know what/where it is)
        return Optional.empty();
    }

    @Override
    public boolean isPure() {
        // This changes the referent of a reference!
        return false;
    }

    @Override
    public void validateType(Subroutine s) {
        final Type ref = this.ref.getType();

        if (!(ref instanceof ReferenceType)) {
            throw new RuntimeException("Store ref type mismatch: expected a form of ReferenceType got: " + ref);
        }

        final Type expected = ((ReferenceType) ref).getReferentType();
        final Type actual = src.getType();
        if (!equivalent(expected, actual)) {
            throw new RuntimeException("Store ref type mismatch: expected referent of: " + expected + " got: " + actual);
        }
    }

    @Override
    public void reachBlock(Map<Block, Integer> marked, Map<Binding, BindingCounter> bindings) {
        // XXX: Do not bumpAssignmentCounter on ref (we are actually reading from ref!)
        Statement.checkBindingDeclaration(bindings, this.src);
        Statement.checkBindingDeclaration(bindings, this.ref);
    }

    @Override
    public Statement replaceRead(Binding binding, Value repl) {
        final Value newSrc = this.src.replaceBinding(binding, repl);
        if (newSrc != this.src) {
            return new StoreRefStatement(this.ref, newSrc);
        }
        return this;
    }

    @Override
    public Statement unfoldConstants() {
        // Nothing to unfold
        return this;
    }

    @Override
    public String toString() {
        return "stref " + ref + ", " + src;
    }
}