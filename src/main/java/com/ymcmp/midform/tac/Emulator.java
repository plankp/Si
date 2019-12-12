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

    private static final class Continuation extends Exception {

        public final FuncRef func;
        public final Value arg;

        public Continuation(FuncRef func, Value arg) {
            super("Calling " + func + ' ' + arg);

            this.func = func;
            this.arg = arg;
        }
    }

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

    public Value callSubroutine(Subroutine routine) {
        return this.callSubroutine(routine, ImmUnit.INSTANCE);
    }

    public Value callSubroutine(Subroutine routine, Value arg) {
        try {
            return this.internalCallSubroutine(routine, arg);
        } catch (Continuation ex1) {
            // trampoline
            Continuation continuation = ex1;
            while (true) {
                try {
                    return this.performCall(continuation.func, continuation.arg);
                } catch (Continuation ex2) {
                    continuation = ex2;
                }
            }
        }
    }

    private Value internalCallSubroutine(Subroutine routine, Value arg) throws Continuation {
        final HashMap<Binding, Value> locals = new HashMap<>();
        final Iterator<Value> splatted = Subroutine.splatterArguments(arg).iterator();
        final Iterator<Binding.Parameter> params = routine.getParameters().iterator();
        while (splatted.hasNext() || params.hasNext()) {
            // which is nice: if size mismatch, iterator will throw error!
            locals.put(params.next(), splatted.next());
        }
        return this.execute(locals, blockToIterator(routine.getInitialBlock()));
    }

    public Value callExternal(String name, Value arg) {
        return this.extHandlers.get(name).apply(Subroutine.splatterArguments(arg).toArray(new Value[0]));
    }

    private Value performCall(FuncRef fptr, Value arg) throws Continuation {
        if (fptr instanceof FuncRef.Native) {
            return this.callExternal(((FuncRef.Native) fptr).name, arg);
        }
        if (fptr instanceof FuncRef.Local) {
            return this.internalCallSubroutine(((FuncRef.Local) fptr).sub, arg);
        }

        throw new RuntimeException("Unrecognized FuncRef type: " + fptr.getClass().getSimpleName() + "::" + fptr);
    }

    public Value execute(final Map<Binding, Value> locals, Iterator<Statement> pc) throws Continuation {
        while (true) {
            Statement stmt = pc.next();

            // quasi-execute it by unfolding the constants
            stmt = stmt.unfoldConstants();

            // substitute the variables
            for (final Map.Entry<Binding, Value> entry : locals.entrySet()) {
                stmt = stmt.replaceRead(entry.getKey(), entry.getValue());
            }

            // then unfold the constants again!
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
                // See CallStatement, but we throw a continuation (and let the trampoline deal with it)
                final TailCallStatement callStmt = (TailCallStatement) stmt;
                throw new Continuation((FuncRef) callStmt.sub, callStmt.arg);
            } else if (stmt instanceof MakeRefStatement) {
                final MakeRefStatement mkref = (MakeRefStatement) stmt;
                locals.put(mkref.dst, new BindingRef(mkref.src) {
                    @Override
                    public void storeValue(Value value) {
                        locals.put(this.referent, value);
                    }

                    @Override
                    public Value loadValue() {
                        return locals.get(this.referent);
                    }
                });
            } else if (stmt instanceof LoadRefStatement) {
                final LoadRefStatement ldref = (LoadRefStatement) stmt;
                final BindingRef ref = (BindingRef) locals.get(ldref.ref);
                locals.put(ldref.dst, ref.loadValue());
            } else if (stmt instanceof StoreRefStatement) {
                final StoreRefStatement stref = (StoreRefStatement) stmt;
                final BindingRef ref = (BindingRef) locals.get(stref.ref);
                ref.storeValue(stref.src);
            } else {
                throw new RuntimeException("Unrecognized statement pattern: " + stmt);
            }
        }
    }

    private static Iterator<Statement> blockToIterator(Block block) {
        return block.getStatements().iterator();
    }
}