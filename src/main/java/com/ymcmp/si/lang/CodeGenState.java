/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.statement.*;
import com.ymcmp.midform.tac.type.*;
import com.ymcmp.midform.tac.value.*;

public final class CodeGenState {

    private final LinkedList<Statement> statements = new LinkedList<>();

    private Value temporary;
    private int temporaryCounter;

    private Block currentBlock;
    private int blockCounter;

    public CodeGenState() {
        this.reset();
    }

    public void reset() {
        this.statements.clear();

        this.temporary = null;
        this.temporaryCounter = -1;

        this.currentBlock = null;
        this.blockCounter = -1;
    }

    public Binding.Immutable makeTemporary(Type type) {
        final Binding.Immutable temp = new Binding.Immutable("%t" + ++this.temporaryCounter, type);
        return temp;
    }

    public Binding.Immutable makeAndSetTemporary(Type type) {
        final Binding.Immutable temp = this.makeTemporary(type);
        this.setTemporary(temp);
        return temp;
    }

    public void setTemporary(Value val) {
        this.temporary = val;
    }

    public Value getTemporary() {
        return this.temporary;
    }

    public Block makeBlock() {
        return this.makeBlock(this.nextGeneratedBlockName());
    }

    public Block makeBlock(String name) {
        return new Block(name);
    }

    public Block makeAndSetBlock() {
        return this.makeAndSetBlock(this.nextGeneratedBlockName());
    }

    public Block makeAndSetBlock(String name) {
        final Block b = this.makeBlock(name);
        this.currentBlock = b;
        return b;
    }

    public Block getCurrentBlock() {
        return this.currentBlock;
    }

    public void setCurrentBlock(Block b) {
        this.currentBlock = Objects.requireNonNull(b);
    }

    public void addStatement(Statement s) {
        this.statements.addLast(s);
    }

    public void setStatements(List<Statement> stmts) {
        this.statements.clear();
        this.statements.addAll(stmts);
    }

    public List<Statement> clearStatements() {
        final LinkedList<Statement> copy = new LinkedList<>(this.statements);
        this.statements.clear();
        return copy;
    }

    public int numberOfStatements() {
        return this.statements.size();
    }

    public Block buildCurrentBlock() {
        final Block b = this.currentBlock;
        this.currentBlock = null;

        b.setStatements(this.statements);
        this.statements.clear();

        return b;
    }

    private String nextGeneratedBlockName() {
        return "%b" + ++this.blockCounter;
    }
}