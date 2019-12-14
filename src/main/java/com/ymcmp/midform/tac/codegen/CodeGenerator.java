/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.codegen;

import com.ymcmp.midform.tac.Subroutine;

public interface CodeGenerator {

    public void reset();

    public String getGenerated();

    public void visitSubroutine(Subroutine sub);

    public void addEntryPoint(Subroutine sub);
}