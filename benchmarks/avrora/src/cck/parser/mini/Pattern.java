/**
 * Copyright (c) 2005, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the University of California, Los Angeles nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Creation date: Jan 12, 2006
 */

package cck.parser.mini;

/**
 * The <code>Pattern</code> class represents a pattern for a token which describes
 * its structure in terms of character. An pattern might be, for example, a
 * decimally-encoding integer, an identifier, a string literal, a delimiter such
 * as a colon or comma, ec.
 *
 * @author Ben L. Titzer
 */
public abstract class Pattern {

    public static final Pattern TILDE = new DELIMITER('~');
    public static final Pattern TICK = new DELIMITER('`');
    public static final Pattern COMMA = new DELIMITER(',');
    public static final Pattern PERIOD = new DELIMITER(',');
    public static final Pattern COLON = new DELIMITER(':');
    public static final Pattern SEMICOLON = new DELIMITER(';');
    public static final Pattern EXCLAMATION = new DELIMITER('!');
    public static final Pattern AT = new DELIMITER('@');
    public static final Pattern POUND = new DELIMITER('#');
    public static final Pattern DOLLAR = new DELIMITER('$');
    public static final Pattern PERCENT = new DELIMITER('%');
    public static final Pattern CARET = new DELIMITER('^');
    public static final Pattern AMPERSAND = new DELIMITER('&');
    public static final Pattern ASTERISK = new DELIMITER('*');
    public static final Pattern LEFT_PAREN = new DELIMITER('(');
    public static final Pattern RIGHT_PAREN = new DELIMITER(')');
    public static final Pattern HYPHEN = new DELIMITER('-');
    public static final Pattern PLUS = new DELIMITER('+');
    public static final Pattern EQUAL = new DELIMITER('=');
    public static final Pattern LEFT_BRACKET = new DELIMITER('[');
    public static final Pattern RIGHT_BRACKET = new DELIMITER(']');
    public static final Pattern LEFT_BRACE = new DELIMITER('{');
    public static final Pattern RIGHT_BRACE = new DELIMITER('}');
    public static final Pattern PIPE = new DELIMITER('|');
    public static final Pattern BACKSLASH = new DELIMITER('\\');
    public static final Pattern LESSTHAN = new DELIMITER('<');
    public static final Pattern GREATERTHAN = new DELIMITER('>');
    public static final Pattern SLASH = new DELIMITER('/');
    public static final Pattern QUESTION = new DELIMITER('?');
    public static final Pattern NEWLINE = new DELIMITER('\n');

    public abstract boolean isBeginChar(char c);

    /**
     * The <code>DELIMITER</code> class represents a single character, such as a
     * comma, period, parenthesis, etc, that constitutes a token.
     */
    public static class DELIMITER extends Pattern {
        public final char ch;

        public DELIMITER(char c) {
            ch = c;
        }

        public boolean isBeginChar(char c) {
            return ch == c;
        }
    }

    /**
     * The <code>KEYWORD</code> class represents a pattern that consists of a fixed
     * string of characters (not necessarily alpha-numeric characters). For example,
     * <code>"public"</code> might be a keyword, as could be the string
     * <code>"##00$${dixie}"</code>,
     * or any other fixed sequence of characters.
     */
    public static class KEYWORD extends Pattern {
        protected final char beginChar;
        public final String keyword;

        public KEYWORD(String kw) {
            keyword = kw;
            beginChar = keyword.charAt(0);
        }

        public boolean isBeginChar(char c) {
            return beginChar == c;
        }
    }
}
