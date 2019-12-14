/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.codegen;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.statement.*;
import com.ymcmp.midform.tac.value.*;
import com.ymcmp.midform.tac.type.*;

public final class TACGenerator implements CodeGenerator {

    private final StringBuilder code = new StringBuilder();

    public TACGenerator() {
        this.reset();
    }

    @Override
    public void reset() {
        this.code.setLength(0);
    }

    @Override
    public String getGenerated() {
        return "# Note: This form does not support entry points, hence none were generated" + this.code.toString();
    }

    @Override
    public void visitSubroutine(Subroutine sub) {
        this.code.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append(sub);
    }

    @Override
    public void addEntryPoint(Subroutine sub) {
        // does nothing: TAC form does not
        // have the concept of entry points
    }
}