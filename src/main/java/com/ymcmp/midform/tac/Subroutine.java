/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.type.Types;
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
        this.validateParameters(params);
        this.params = new LinkedList<>(params);
    }

    public void validateParameters(List<Binding> params) {
        final int ps = params.size();
        final int ns = this.type.numberOfSplattedInputs();
        if (ps != ns) {
            throw new RuntimeException("Input size mismatch: expected: " + ns + " got: " + ps);
        }

        for (int i = 0; i < ps; ++i) {
            final Type expected = this.type.getSplattedInput(i);
            final Type actual = params.get(i).type;
            if (!Types.equivalent(actual, expected)) {
                throw new RuntimeException("Parameter type mismatch: expected: " + expected + " got: " + actual);
            }
        }
    }

    private void validateType() {
        for (final Block block : this.blocks) {
            block.validateType(this);
        }
    }

    private boolean analyzeReachability(boolean eliminateDeadCode) {
        // block reachability analysis
        final HashMap<Block, Integer> marked = new HashMap<>();
        // start tracing from the first block
        this.blocks.get(0).trace(marked);

        if (!eliminateDeadCode || this.blocks.size() < 2) {
            return false;
        }

        boolean mod = false;
        // then remove all unreachable blocks
        final Iterator<Block> it = this.blocks.iterator();
        while (it.hasNext()) {
            if (!marked.containsKey(it.next())) {
                // If never referenced
                mod = true;
                it.remove();
            }
        }

        // also squash blocks that are only referenced once
        for (final HashMap.Entry<Block, Integer> entry : marked.entrySet()) {
            final Block key = entry.getKey();

            if (!(key.numberOfStatements() == 1 || entry.getValue().intValue() == 1)) {
                continue;
            }

            for (final Block block : this.blocks) {
                if (block.squashJump(key)) {
                    mod = true;
                }
            }
        }
        return mod;
    }

    private boolean unfoldConstantExprs() {
        boolean mod = false;
        for (final Block block : this.blocks) {
            if (block.unfoldConstantExprs()) {
                mod = true;
            }
        }
        return mod;
    }

    private boolean dropUnreachableStatments() {
        boolean mod = false;
        for (final Block block : this.blocks) {
            if (block.dropUnreachableStatments()) {
                mod = true;
            }
        }
        return mod;
    }

    public void validate() {
        this.validateType();
        this.validateBlocks();
    }

    public void validateBlocks() {
        this.validateType();
        this.analyzeReachability(false);
    }

    public void optimize() {
        // only need to validate parameters once since no
        // optimization pass affects the function parameters
        this.validateParameters(this.params);

        while (true) {
            // Very important: want to make sure things are
            // still valid after these optimization passes!
            this.validateBlocks();

            // As soon as any change happens, restart loop
            if (this.analyzeReachability(true))     continue;
            if (this.unfoldConstantExprs())         continue;
            if (this.dropUnreachableStatments())    continue;

            break;
        }
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