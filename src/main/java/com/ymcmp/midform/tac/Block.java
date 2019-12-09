/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ymcmp.midform.tac.statement.*;
import com.ymcmp.midform.tac.type.Types;
import com.ymcmp.midform.tac.type.UnitType;
import com.ymcmp.midform.tac.value.*;

public class Block implements Serializable {

    public final String name;

    private List<Statement> statements;

    public Block(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Block name cannot be empty");
        }

        this.name = name;
        this.statements = Collections.singletonList(new GotoStatement(this));
    }

    public void setStatements(List<Statement> statements) {
        // only be one branch statement allowed
        // and that has to be the last statement
        boolean phase = true; // true -> searching for branch statement
        final ListIterator<Statement> it = statements.listIterator();
        while (it.hasNext()) {
            final Statement stmt = it.next();
            if (phase) {
                if (stmt instanceof BranchStatement) {
                    phase = false;
                }
            } else {
                // this should not happen:
                // there are statements after the branch statement
                throw new IllegalArgumentException("A form of BranchStatement must be the last statement");
            }
        }

        if (phase) {
            // this should not happen:
            // the branch statement was never found
            throw new IllegalArgumentException("Last statement must be a form of BranchStatement");
        }

        this.statements = new LinkedList<>(statements);
    }

    public int numberOfStatements() {
        return this.statements.size();
    }

    public boolean isSelfLoop() {
        final Statement stmt = this.statements.get(this.statements.size() - 1);
        return stmt instanceof GotoStatement && ((GotoStatement) stmt).next == this;
    }

    public List<Statement> getStatements() {
        return Collections.unmodifiableList(this.statements);
    }

    public void validateType(Subroutine enclosingSub) {
        for (final Statement stmt : this.statements) {
            stmt.validateType(enclosingSub);
        }
    }

    public void trace(Map<Block, Integer> markedBlocks, Map<Binding, BindingCounter> markedBindings) {
        Integer old = markedBlocks.get(this);
        if (old == null) {
            // This means we have not traced this block yet
            // mark down this is the first time we've been referenced
            markedBlocks.put(this, 1);
            // (so we trace it...)
            for (final Statement stmt : this.statements) {
                stmt.reachBlock(markedBlocks, markedBindings);
            }
        } else {
            markedBlocks.put(this, old.intValue() + 1);
        }
    }

    public boolean dropBindingStores(final Binding binding) {
        boolean mod = false;
        final ListIterator<Statement> it = this.statements.listIterator();
        while (it.hasNext()) {
            final Statement stmt = it.next();
            if (stmt.isPure() && stmt.getResultRegister().map(binding::equals).orElse(false)) {
                it.remove();
                mod = true;
            }
        }
        return mod;
    }

    public boolean squashJump(final Block block) {
        if (this == block) {
            // The jump is necessary (we don't expand this):
            // a self-looping block will cause infinite loop if expanded
            return false;
        }

        // Only proceed if the block is not a self-looping block
        if (block.isSelfLoop()) {
            return false;
        }

        boolean mod = false;
        final ListIterator<Statement> it = this.statements.listIterator();
        while (it.hasNext()) {
            final Statement stmt = it.next();
            if (stmt instanceof GotoStatement && ((GotoStatement) stmt).next == block) {
                // Replace this goto statement with the statements in the other block
                it.remove();
                for (final Statement repl : block.statements) {
                    it.add(repl);
                }
                mod = true;
            }
        }
        return mod;
    }

    public boolean expandTemporaries() {
        final HashMap<Binding.Immutable, Value> mapping = new HashMap<>();
        for (final Statement stmt : this.statements) {
            if (stmt instanceof MoveStatement) {
                final MoveStatement move = (MoveStatement) stmt;
                if (move.isPure() && move.dst instanceof Binding.Immutable) {
                    mapping.put((Binding.Immutable) move.dst, move.src);
                }
            }
        }

        boolean mod = false;
        for (final Map.Entry<Binding.Immutable, Value> entry : mapping.entrySet()) {
            final Binding.Immutable key = entry.getKey();
            final Value value = entry.getValue();

            final ListIterator<Statement> it = this.statements.listIterator();
            while (it.hasNext()) {
                final Statement stmt = it.next();
                final Statement repl = stmt.replaceRead(key, value);
                if (repl != stmt) {
                    mod = true;
                    it.set(repl);
                }
            }
        }
        return mod;
    }

    public boolean unfoldConstantExprs() {
        boolean mod = false;
        final ListIterator<Statement> it = this.statements.listIterator();
        while (it.hasNext()) {
            final Statement stmt = it.next();
            final Statement repl = stmt.unfoldConstants();
            if (repl != stmt) {
                mod = true;
                it.set(repl);
            }
        }
        return mod;
    }

    public boolean dropUnreachableStatements() {
        // The following is only allowed because we can
        // only jump to the first statement of any block:
        //
        // - find the first branch statement
        // - then we remove everything after it

        boolean mod = false;
        boolean phase = true; // true -> searching, false -> removing
        final ListIterator<Statement> it = this.statements.listIterator();
        while (it.hasNext()) {
            final Statement stmt = it.next();
            if (phase) {
                // Search phase
                if (stmt instanceof BranchStatement) {
                    // we found the branch statment, switch to removal phase
                    phase = false;
                }
            } else {
                // Removal phase
                mod = true;
                it.remove();
            }
        }

        if (phase) {
            // sanity check: this means we never found the branch statement
            // which is impossible (unless a branch statement replaced the
            // wrong statement or was dropped)
            throw new RuntimeException("Faulty block: no branch statement found!");
        }

        return mod;
    }

    public boolean compactFunctionCalls() {
        // The following is only allowed because we can
        // only jump to the first statement of any block:
        //
        // - second to last statement must be a call
        // - last statement must be a return
        // - the return value must be same as the call result

        final int limit = this.numberOfStatements();
        if (limit < 2) return false;

        final ListIterator<Statement> it = this.statements.listIterator(limit - 2);
        final Statement wantCall = it.next(); // 2nd to last
        final Statement wantRet  = it.next(); // last

        if (!((wantCall instanceof CallStatement) && (wantRet instanceof ReturnStatement))) {
            return false;
        }

        final CallStatement callStmt = (CallStatement) wantCall;
        final ReturnStatement retStmt = (ReturnStatement) wantRet;

        if (!retStmt.value.equals(callStmt.dst)) {
            // that means the value of the function result is not returned
            // but need to account for special case:
            //    call %0, ****    where %0 is unit type
            //    ret ()
            if (!(retStmt.value == ImmUnit.INSTANCE && Types.equivalent(callStmt.dst.getType(), UnitType.INSTANCE))) {
                // all cases tested: optimization cannot be applied
                return false;
            }
        }

        // reaching here means the optimization should be applied
        // we drop the last element
        it.remove();
        // and replace the 2nd to last element with the tail call statement
        it.previous();
        it.set(new TailCallStatement(callStmt.sub, callStmt.arg));
        return true;
    }

    @Override
    public String toString() {
        final String ln = System.lineSeparator();
        final StringBuilder sb = new StringBuilder(this.name).append(':');
        for (final Statement stmt : this.statements) {
            sb.append(ln).append("    ").append(stmt);
        }
        return sb.toString();
    }
}