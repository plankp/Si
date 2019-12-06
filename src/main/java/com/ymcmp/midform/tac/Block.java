/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac;

import com.ymcmp.midform.tac.statement.Statement;
import com.ymcmp.midform.tac.statement.BranchStatement;
import com.ymcmp.midform.tac.statement.GotoStatement;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public void validateType(Subroutine enclosingSub) {
        for (final Statement stmt : this.statements) {
            stmt.validateType(enclosingSub);
        }
    }

    public void trace(Map<Block, Integer> marked) {
        Integer old = marked.get(this);
        if (old == null) {
            // This means we have not traced this block yet
            // mark down this is the first time we've been referenced
            marked.put(this, 1);
            // (so we trace it...)
            for (final Statement stmt : this.statements) {
                stmt.reachBlock(marked);
            }
        } else {
            marked.put(this, old.intValue() + 1);
        }
    }

    public boolean squashJump(final Block block) {
        if (this == block) {
            // The jump is necessary (we don't expand this):
            // a self-looping block will cause infinite loop if expanded
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

    public boolean unfoldConstantExprs() {
        boolean mod = false;
        final ListIterator<Statement> it = this.statements.listIterator();
        while (it.hasNext()) {
            final Statement stmt = it.next();
            final Optional<Statement> unfolded = stmt.unfoldConstants();
            if (unfolded.isPresent()) {
                final Statement repl = unfolded.get();
                if (repl != stmt) {
                    mod = true;
                    it.set(repl);
                }
            } else {
                mod = true;
                it.remove();
            }
        }
        return mod;
    }

    public boolean dropUnreachableStatments() {
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