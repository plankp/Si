/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

import com.ymcmp.midform.tac.statement.*;
import com.ymcmp.midform.tac.value.*;

public final class Emulator {

    private static final Value[] EMPTY_VAL_ARRAY = new Value[0];

    private final HashMap<String, Function<Value[], ? extends Value>> extHandlers = new HashMap<>();

    public void addExternalCallHandler(String name, Function<Value[], ? extends Value> f) {
        this.extHandlers.put(name, Objects.requireNonNull(f));
    }

    public void removeExternalCallHandler(String name) {
        this.extHandlers.remove(name);
    }

    public void reset() {
        this.extHandlers.clear();
    }

    public Value callSubroutine(Subroutine routine, Value arg) {
        final HashMap<Binding, Value> locals = new HashMap<>();
        final Value[] splatted = argsToArray(arg);
        final Iterator<Binding> params = routine.getParameters().iterator();
        for (int i = 0; i < splatted.length; ++i) {
            locals.put(params.next(), splatted[i]);
        }
        return this.execute(locals, blockToIterator(routine.getInitialBlock()));
    }

    public Value callExternal(String name, Value arg) {
        return this.extHandlers.get(name).apply(argsToArray(arg));
    }

    private Value performCall(FuncRef fptr, Value arg) {
        if (fptr instanceof FuncRef.Native) {
            return this.callExternal(((FuncRef.Native) fptr).name, arg);
        }
        if (fptr instanceof FuncRef.Local) {
            return this.callSubroutine(((FuncRef.Local) fptr).sub, arg);
        }

        throw new RuntimeException("Unrecognized FuncRef type: " + fptr.getClass().getSimpleName() + "::" + fptr);
    }

    public Value execute(Map<Binding, Value> locals, Iterator<Statement> pc) {
        while (true) {
            Statement stmt = pc.next();

            // substitute the variables
            for (final Map.Entry<Binding, Value> entry : locals.entrySet()) {
                stmt = stmt.replaceRead(entry.getKey(), entry.getValue());
            }

            // quasi-execute it by unfolding the constants
            stmt = stmt.unfoldConstants();

            // then check if this statement is one of the few
            // *must-be-implemented-by-runtime* types
            if (stmt instanceof MoveStatement) {
                // add (or update it) into the current locals
                final MoveStatement moveStmt = (MoveStatement) stmt;
                locals.put(moveStmt.dst, moveStmt.src);
            } else if (stmt instanceof ReturnStatement) {
                // we return
                return ((ReturnStatement) stmt).value;
            } else if (stmt instanceof GotoStatement) {
                // we jump by changing the current program counter
                pc = blockToIterator(((GotoStatement) stmt).next);
            } else if (stmt instanceof CallStatement) {
                // It depends if it is a native call or a local call
                final CallStatement callStmt = (CallStatement) stmt;
                final Value ret = this.performCall((FuncRef) callStmt.sub, callStmt.arg);
                locals.put(callStmt.dst, ret);
            } else if (stmt instanceof TailCallStatement) {
                // See CallStatement + ReturnStatement
                final TailCallStatement callStmt = (TailCallStatement) stmt;
                return this.performCall((FuncRef) callStmt.sub, callStmt.arg);
            } else {
                throw new RuntimeException("Unrecognized statement pattern: " + stmt);
            }
        }
    }

    private static Value[] argsToArray(final Value arg) {
        if (arg == ImmUnit.INSTANCE)    return EMPTY_VAL_ARRAY;
        if (arg instanceof Tuple)       return ((Tuple) arg).values.toArray(new Value[0]);
        return new Value[] { arg };
    }

    private static Iterator<Statement> blockToIterator(Block block) {
        return block.getStatements().iterator();
    }
}