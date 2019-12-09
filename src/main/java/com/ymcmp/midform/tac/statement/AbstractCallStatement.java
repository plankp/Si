/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.ymcmp.midform.tac.BindingCounter;
import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.type.FunctionType;
import com.ymcmp.midform.tac.value.*;

public abstract class AbstractCallStatement<T extends AbstractCallStatement<T>> implements Statement {

    public final Value sub;
    public final Value arg;

    public AbstractCallStatement(Value sub, Value arg) {
        this.sub = Objects.requireNonNull(sub);
        this.arg = Objects.requireNonNull(arg);
    }

    protected abstract T virtualConstructor(Value sub, Value arg);
    protected abstract Statement inlinedStatement(Statement stmt);

    @Override
    public boolean isPure() {
        // It depends on the function being called
        // but for now, let's assume it is not
        return false;
    }

    @Override
    public void validateType(Subroutine s) {
        // Check if the function type accepts the correct inputs
        // and returns an output acceptable by the binding (destination)
        final FunctionType f = (FunctionType) sub.getType();
        if (!f.canApply(arg.getType())) {
            throw new RuntimeException("Call input type mismatch: expected: " + f.getInput() + " got: " + arg.getType());
        }
    }

    @Override
    public void reachBlock(Map<Block, Integer> marked, Map<Binding, BindingCounter> bindings) {
        // No blocks to trace (we only care about blocks in the same function)

        Statement.checkBindingDeclaration(bindings, this.sub);
        Statement.checkBindingDeclaration(bindings, this.arg);
    }

    @Override
    public Statement replaceRead(Binding binding, Value repl) {
        final Value newSub = this.sub.replaceBinding(binding, repl);
        final Value newArg = this.arg.replaceBinding(binding, repl);
        if (newSub != this.sub || newArg != this.arg) {
            return this.virtualConstructor(newSub, newArg);
        }
        return this;
    }

    @Override
    public Statement unfoldConstants() {
        // Try to inline simple functions
        if (this.sub instanceof FuncRef.Local) {
            final Subroutine callsite = ((FuncRef.Local) this.sub).sub;
            final Block initialBlock = callsite.getInitialBlock();
            if (initialBlock.numberOfStatements() == 1) {
                // need to account for parameters:
                // function (a) {
                // _entry:
                //   ret a
                // }
                Statement repl = initialBlock.getStatements().get(0);
                if (repl instanceof YieldStatement<?>) {
                    final Iterator<Value> splatted = Subroutine.splatterArguments(this.arg).iterator();
                    final Iterator<Binding> params = callsite.getParameters().iterator();
                    while (splatted.hasNext() || params.hasNext()) {
                        // which is nice: if size mismatch, iterator will throw error!
                        repl = repl.replaceRead(params.next(), splatted.next());
                    }

                    // then we replace this with the substituted statement
                    return this.inlinedStatement(repl);
                }
            }
        }

        return this;
    }
}