/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac;

import static com.ymcmp.midform.tac.statement.Statement.bumpAssignmentCounter;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.type.Types;
import com.ymcmp.midform.tac.type.FunctionType;
import com.ymcmp.midform.tac.value.Binding;

public class Subroutine implements Serializable {

    public final String name;
    public final FunctionType type;

    private List<Binding> params;
    private Block initialBlock;

    public Subroutine(String name, FunctionType type) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Block name cannot be empty");
        }

        this.name = name;
        this.type = type;
        this.params = Collections.emptyList();
        this.initialBlock = new Block("entry");
    }

    public void setInitialBlock(Block block) {
        if (block == null) {
            throw new IllegalArgumentException("Subroutines cannot be empty");
        }
        this.initialBlock = block;
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
        for (final Block block : this.traceAllBlocks()) {
            block.validateType(this);
        }
    }

    private HashMap<Binding, BindingCounter> createBindingMap() {
        final HashMap<Binding, BindingCounter> bindings = new HashMap<>();
        for (final Binding param : this.params) {
            final BindingCounter counter = bumpAssignmentCounter(bindings, param);
            // Read it twice (since parameters are never temporary)
            counter.newRead();
            counter.newRead();
        }
        return bindings;
    }

    private boolean inlineSimpleBlocks() {
        // block reachability analysis
        final HashMap<Block, Integer> marked = new HashMap<>();
        final HashMap<Binding, BindingCounter> bindings = this.createBindingMap();

        final List<Block> blocks = this.traceAllBlocks(marked, bindings);

        boolean mod = false;

        // squash blocks that are only referenced once
        for (final HashMap.Entry<Block, Integer> entry : marked.entrySet()) {
            final Block key = entry.getKey();

            if (!(key.numberOfStatements() == 1 || entry.getValue().intValue() == 1)) {
                continue;
            }

            for (final Block block : blocks) {
                if (block.squashJump(key)) {
                    mod = true;
                }
            }
        }

        // drop all bindings that are not being read
        for (final HashMap.Entry<Binding, BindingCounter> entry : bindings.entrySet()) {
            final BindingCounter counter = entry.getValue();
            if (counter.getReads() == 0) {
                final Binding binding = entry.getKey();
                for (final Block block : blocks) {
                    if (block.dropBindingStores(binding)) {
                        mod = true;
                    }
                }
            }
        }

        return mod;
    }


    public List<Block> traceAllBlocks() {
        // block reachability analysis
        final HashMap<Block, Integer> marked = new HashMap<>();
        final HashMap<Binding, BindingCounter> bindings = this.createBindingMap();

        return this.traceAllBlocks(marked, bindings);
    }

    private List<Block> traceAllBlocks(Map<Block, Integer> marked, Map<Binding, BindingCounter> bindings) {
        // start tracing from the first block
        this.initialBlock.trace(marked, bindings);

        final LinkedList<Block> list = new LinkedList<>();

        // Initial block is the first element
        list.add(this.initialBlock);
        for (final Block traced : marked.keySet()) {
            if (traced != this.initialBlock) {
                list.add(traced);
            }
        }
        return list;
    }

    private boolean unfoldConstantExprs() {
        boolean mod = false;
        for (final Block block : this.traceAllBlocks()) {
            if (block.unfoldConstantExprs())    mod = true;
            if (block.expandTemporaries())      mod = true;
        }
        return mod;
    }

    private boolean dropUnreachableStatments() {
        boolean mod = false;
        for (final Block block : this.traceAllBlocks()) {
            if (block.dropUnreachableStatments()) {
                mod = true;
            }
        }
        return mod;
    }

    public void validate() {
        this.validateParameters(this.params);
        this.validateBlocks();
    }

    public void validateBlocks() {
        this.validateType();
        this.traceAllBlocks();
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
            if (this.inlineSimpleBlocks())          continue;
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
        for (final Block block : this.traceAllBlocks()) {
            sb.append(ln).append(block);
        }
        sb.append(ln).append('}');
        return sb.toString();
    }
}