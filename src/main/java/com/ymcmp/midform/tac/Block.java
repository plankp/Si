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
import java.util.Optional;
import java.util.Set;
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
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("Blocks cannot be empty");
        }
        if (!(statements.get(statements.size() - 1) instanceof BranchStatement)) {
            throw new IllegalArgumentException("Last statement must be a form of BranchStatement");
        }

        this.statements = new LinkedList<>(statements);
    }

    public void validateType(Subroutine enclosingSub) {
        for (final Statement stmt : this.statements) {
            stmt.validateType(enclosingSub);
        }
    }

    public void trace(Set<Block> marked) {
        if (marked.add(this)) {
            // This means we have not traced this block yet
            // (so we trace it...)
            for (final Statement stmt : this.statements) {
                stmt.reachBlock(marked);
            }
        }
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
        // - if the branch statement is a goto statement
        // --- then we remove everything after the goto statement

        boolean mod = false;
        boolean phase = true; // true -> searching, false -> removing
        final ListIterator<Statement> it = this.statements.listIterator();
        while (it.hasNext()) {
            final Statement stmt = it.next();
            if (phase) {
                // Search phase
                if (stmt instanceof BranchStatement) {
                    if (!(stmt instanceof GotoStatement)) {
                        break;
                    }

                    // we found the goto statment, switch to removal phase
                    phase = false;
                    continue;
                }
            } else {
                // Removal phase
                mod = true;
                it.remove();
            }
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