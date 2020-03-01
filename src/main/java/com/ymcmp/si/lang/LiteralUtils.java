/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

public final class LiteralUtils {

    private LiteralUtils() {
    }

    public static int convertIntLiteral(String raw) {
        if (raw.length() >= 3) {
            if (raw.charAt(0) == '0') {
                switch (raw.charAt(1)) {
                    case 'b':   return Integer.parseInt(raw.substring(2), 2);
                    case 'c':   return Integer.parseInt(raw.substring(2), 8);
                    case 'd':   return Integer.parseInt(raw.substring(2), 10);
                    case 'x':   return Integer.parseInt(raw.substring(2), 16);
                    default:    throw new AssertionError("Illegal base changer 0" + raw.charAt(1));
                }
            }
        }

        // parse as base 10
        return Integer.parseInt(raw);
    }

    public static double convertDoubleLiteral(String raw) {
        return Double.parseDouble(raw);
    }

    public static boolean convertBoolLiteral(String raw) {
        switch (raw) {
            case "true":    return true;
            case "false":   return false;
            default:        throw new AssertionError("Illegal boolean " + raw);
        }
    }

    public static char convertCharLiteral(String raw) {
        if (raw.charAt(0) == '\'' && raw.charAt(raw.length() - 1) == '\'') {
            final String s = stringLiteralHelper(raw.toCharArray(), 1, raw.length() - 1);
            if (s.length() != 1) {
                // This happens either when s is empty
                // or s contains more than one UTF16 codepoint
                throw new AssertionError("Illegal char (UTF16 codepoint) " + raw);
            }
            return s.charAt(0);
        }

        throw new AssertionError("Illegal char (UTF16 codepoint) " + raw);
    }

    public static String convertStringLiteral(String raw) {
        if (raw.charAt(0) == '"' && raw.charAt(raw.length() - 1) == '"') {
            return stringLiteralHelper(raw.toCharArray(), 1, raw.length() - 1);
        }

        throw new AssertionError("Illegal string " + raw);
    }

    private static String stringLiteralHelper(char[] array, final int start, final int end) {
        final StringBuilder sb = new StringBuilder(end - start);

        for (int i = start; i < end; ++i) {
            final char ch = array[i];
            if (ch != '\\') {
                sb.append(ch);
                continue;
            }

            // Assumes string is escaped properly!
            final char next = array[++i];
            switch (next) {
            case 'a':   sb.append((char) 0x07); break;
            case 'b':   sb.append((char) 0x08); break;
            case 'f':   sb.append('\f'); break;
            case 'n':   sb.append('\n'); break;
            case 'r':   sb.append('\r'); break;
            case 't':   sb.append('\t'); break;
            case 'v':   sb.append((char) 0x0B); break;
            case '\"':  sb.append('\"'); break;
            case '\'':  sb.append('\''); break;
            case '\\':  sb.append('\\'); break;
            case 'u':
                sb.append((char) Integer.parseInt("" + array[++i] + array[++i] + array[++i] + array[++i], 16));
                break;
            case 'U': {
                sb.append(Character.toChars((int) Long.parseLong("" + array[++i] + array[++i] + array[++i] + array[++i] + array[++i] + array[++i] + array[++i] + array[++i], 16)));
                break;
            }
            default:
                throw new AssertionError("Illegal escape \\" + next);
            }
        }

        return sb.toString();
    }
}