/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

import com.ymcmp.midform.tac.type.FunctionType;
import com.ymcmp.midform.tac.value.Binding;

public class Subroutine implements Serializable {

    public final String name;
    public final FunctionType type;

    private List<Binding> params;
    private List<Block> blocks;

    public Subroutine(String name, FunctionType type) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Block name cannot be empty");
        }

        this.name = name;
        this.type = type;
        this.params = Collections.emptyList();
        this.blocks = Collections.singletonList(new Block("entry"));
    }

    public void setBlocks(List<Block> blocks) {
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("Subroutines cannot be empty");
        }
        this.blocks = new LinkedList<>(blocks);
    }

    public void setParameters(List<Binding> params) {
        this.params = new LinkedList<>(params);
    }

    @Override
    public String toString() {
        final String ln = System.lineSeparator();
        final StringBuilder sb = new StringBuilder()
            .append("function ").append(this.name)
            .append("(");

        if (!this.params.isEmpty()) {
            for (final Binding param : this.params) {
                sb.append(param).append(',');
            }
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append(") {");
        for (final Block block : this.blocks) {
            sb.append(ln).append(block);
        }
        sb.append(ln).append('}');
        return sb.toString();
    }
}