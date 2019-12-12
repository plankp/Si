/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.type.NomialType;

public final class ImmCharacter extends Value {

    public static final NomialType TYPE = new NomialType("char");

    // stores as UTF16 codepoint (like Java)
    public final char content;

    public ImmCharacter(char content) {
        this.content = content;
    }

    @Override
    public Type getType() {
        return TYPE;
    }

    @Override
    public Value replaceBinding(Binding binding, Value t) {
        return this;
    }

    @Override
    public boolean containsLocalBinding() {
        return false;
    }

    @Override
    public boolean isCompileTimeConstant() {
        return true;
    }

    @Override
    public int hashCode() {
        return Character.hashCode(this.content);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImmCharacter) {
            final ImmCharacter imm = (ImmCharacter) obj;
            return this.content == imm.content;
        }
        return false;
    }

    @Override
    public String toString() {
        return "'" + escapeChar(this.content, true) + "'";
    }

    public static String escapeChar(char ch, boolean singleQuotes) {
        if (singleQuotes && ch == '\'') {
            return "\\'";
        }
        if (!singleQuotes && ch == '\"') {
            return "\\\"";
        }

        switch (ch) {
        case 0x07:  return "\\a";
        case 0x08:  return "\\b";
        case '\f':  return "\\f";
        case '\n':  return "\\n";
        case '\r':  return "\\r";
        case '\t':  return "\\t";
        case 0x0B:  return "\\v";
        case '\\':  return "\\\\";
        default:    break;
        }

        if (ch > 0xff || Character.isISOControl(ch)) {
            // we want to escape these as codepoints
            return String.format("\\u%04x", (int) ch);
        }
        return Character.toString(ch);
    }
}