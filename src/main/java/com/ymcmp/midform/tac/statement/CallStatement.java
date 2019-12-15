/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.statement;

import static com.ymcmp.midform.tac.type.Types.assignableFrom;
import static com.ymcmp.midform.tac.type.Types.equivalent;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.ymcmp.midform.tac.BindingCounter;
import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.type.FunctionType;
import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.value.*;

// Not a branch statement because control flow resumes after call
public final class CallStatement extends AbstractCallStatement<CallStatement> {

    public final Binding dst;

    public CallStatement(Binding dst, Value sub, Value arg) {
        super(sub, arg);
        this.dst = Objects.requireNonNull(dst);
    }

    @Override
    protected CallStatement virtualConstructor(Value sub, Value arg) {
        return new CallStatement(this.dst, sub, arg);
    }

    @Override
    protected Statement inlinedStatement(Statement stmt) {
        return ((YieldStatement<?>) stmt).toNonYieldingVariant(this.dst);
    }

    @Override
    public Optional<Binding> getResultRegister() {
        return Optional.of(this.dst);
    }

    @Override
    public void validateType(Subroutine s) {
        super.validateType(s);

        final Type actual = this.getFunctionType().getOutput();
        if (!equivalent(dst.getType(), actual)) {
            throw new RuntimeException("Call output type mismatch: expected: " + dst.getType() + " got: " + actual);
        }
    }

    @Override
    public void reachBlock(Map<Block, Integer> marked, Map<Binding, BindingCounter> bindings) {
        super.reachBlock(marked, bindings);

        Statement.bumpAssignmentCounter(bindings, this.dst);
    }

    @Override
    public String toString() {
        return "call " + dst + ", " + sub + ' ' + arg;
    }
}
