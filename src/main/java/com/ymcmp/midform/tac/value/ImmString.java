/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.value;

import com.ymcmp.midform.tac.type.Type;
import com.ymcmp.midform.tac.type.NomialType;

public final class ImmString extends Value {

    public static final NomialType TYPE = new NomialType("string");

    public final String content;

    public ImmString(String content) {
        this.content = java.util.Objects.requireNonNull(content);
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
        return this.content.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImmString) {
            final ImmString imm = (ImmString) obj;
            return this.content.equals(imm.content);
        }
        return false;
    }

    @Override
    public String toString() {
        return '"' + escapeCharSequence(this.content) + '"';
    }

    public static String escapeCharSequence(CharSequence cseq) {
        final int limit = cseq.length();
        final StringBuilder sb = new StringBuilder(limit);
        for (int i = 0; i < limit; ++i) {
            final char ch = cseq.charAt(i);
            if (Character.isSurrogate(ch)) {
                final char p = cseq.charAt(++i);
                sb.append(String.format("\\U%08x", Character.toCodePoint(ch, p)));
                continue;
            }
            sb.append(ImmCharacter.escapeChar(ch, false));
        }
        return sb.toString();
    }
}