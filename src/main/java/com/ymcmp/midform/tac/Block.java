/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac;

import com.ymcmp.midform.tac.statement.Statement;
import com.ymcmp.midform.tac.statement.BranchStatement;
import com.ymcmp.midform.tac.statement.GotoStatement;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
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