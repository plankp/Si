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

public final class TailCallStatement extends AbstractCallStatement<TailCallStatement> implements YieldStatement<CallStatement> {

    public TailCallStatement(Value sub, Value arg) {
        super(sub, arg);
    }

    @Override
    protected TailCallStatement virtualConstructor(Value sub, Value arg) {
        return new TailCallStatement(sub, arg);
    }

    @Override
    protected Statement inlinedStatement(Statement stmt) {
        return stmt;
    }

    @Override
    public Optional<Binding> getResultRegister() {
        return Optional.empty();
    }

    @Override
    public CallStatement toNonYieldingVariant(Binding dst) {
        return new CallStatement(dst, this.sub, this.arg);
    }

    @Override
    public void validateType(Subroutine s) {
        super.validateType(s);

        final Type expected = s.type.getOutput();
        final Type actual = this.getFunctionType().getOutput();
        if (!equivalent(expected, actual)) {
            throw new RuntimeException("Call output type mismatch: expected: " + expected + " got: " + actual);
        }
    }

    @Override
    public String toString() {
        return "tailcall " + sub + ' ' + arg;
    }
}
