/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

public class Subroutine implements Serializable {

    public final String name;

    private List<Block> statements;

    public Subroutine(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Block name cannot be empty");
        }

        this.name = name;
        this.statements = Collections.singletonList(new Block("entry"));
    }

    public void setBlocks(List<Block> statements) {
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("Subroutines cannot be empty");
        }
        this.statements = new LinkedList<>(statements);
    }

    @Override
    public String toString() {
        final String ln = System.lineSeparator();
        final StringBuilder sb = new StringBuilder()
            .append("function ").append(this.name)
            .append("(...) {");
        for (final Block block : this.statements) {
            sb.append(ln).append(block);
        }
        sb.append(ln).append('}');
        return sb.toString();
    }
}