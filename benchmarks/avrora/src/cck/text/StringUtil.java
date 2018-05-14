/**
 * Copyright (c) 2004-2005, Regents of the University of California
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
 */

package cck.text;

import cck.util.Arithmetic;
import cck.util.Util;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * The <code>StringUtil</code> class implements several useful functions for dealing with strings such as
 * parsing pieces of syntax, formatting, etc.
 *
 * @author Ben L. Titzer
 */
public class StringUtil {
    public static final String QUOTE = "\"";
    public static final String SQUOTE = "'";
    public static final String LPAREN = "(";
    public static final String RPAREN = ")";
    public static final String COMMA = ",";
    public static final String COMMA_SPACE = ", ";
    public static final String[] EMPTY_STRING_ARRAY = {};
    public static final char SQUOTE_CHAR = '\'';
    public static final char BACKSLASH = '\\';
    public static final char QUOTE_CHAR = '"';

    /**
     * The <code>addToString()</code> method converts a numerical address (represented as a signed 32-bit
     * integer) and converts it to a string in the format 0xXXXX where 'X' represents a hexadecimal character.
     * The address is assumed to fit in 4 hexadecimal characters. If it does not, the string will have as many
     * characters as necessary (max 8) to represent the address.
     *
     * @param address the address value as an integer
     * @return a standard string representation of the address
     */
    public static String addrToString(int address) {
        return to0xHex(address, 4);
    }

    public static String baseFileName(String f) {
        int sind = f.lastIndexOf('/');
        if (sind >= 0) f = f.substring(sind + 1);
        int dind = f.lastIndexOf('.');
        if (dind >= 0) f = f.substring(0, dind);
        return f;
    }

    public static String readIdentifier(CharacterIterator i) {
        StringBuffer buf = new StringBuffer();

        while (true) {
            char c = i.current();

            if (!Character.isLetterOrDigit(c) && (c != '_')) break;

            buf.append(c);
            i.next();
        }

        return buf.toString();
    }

    public static String readDotIdentifier(CharacterIterator i) {
        StringBuffer buf = new StringBuffer();

        while (true) {
            char c = i.current();

            if (!Character.isLetterOrDigit(c) && (c != '_') && (c != '.')) break;

            buf.append(c);
            i.next();
        }

        return buf.toString();
    }

    public static int readHexValue(CharacterIterator i, int max_chars) {
        int accumul = 0;

        for (int cntr = 0; cntr < max_chars; cntr++) {
            char c = i.current();

            if (c == CharacterIterator.DONE) break;
            if (!isHexDigit(c)) break;

            accumul = (accumul << 4) | hexValueOf(c);
            i.next();
        }

        return accumul;
    }

    public static int readOctalValue(CharacterIterator i, int max_chars) {
        int accumul = 0;

        for (int cntr = 0; cntr < max_chars; cntr++) {
            char c = i.current();

            if (!isOctalDigit(c)) break;

            accumul = (accumul << 3) | octalValueOf(c);
            i.next();
        }

        return accumul;
    }

    public static int readBinaryValue(CharacterIterator i, int max_chars) {
        int accumul = 0;

        for (int cntr = 0; cntr < max_chars; cntr++) {
            char ch = i.current();
            i.next();
            if (ch == '0') accumul <<= 1;
            else if (ch == '1') accumul = (accumul << 1) | 1;
            else break;
        }

        return accumul;
    }

    public static int readDecimalValue(CharacterIterator i, int max_chars) {
        return Integer.parseInt(readDecimalString(i, max_chars));
    }

    public static String readDecimalString(CharacterIterator i, int max_chars) {
        StringBuffer buf = new StringBuffer();

        if (peekAndEat(i, '-')) buf.append('-');

        for (int cntr = 0; cntr < max_chars; cntr++) {
            char c = i.current();

            if (!Character.isDigit(c)) break;

            buf.append(c);
            i.next();
        }

        return buf.toString();
    }

    public static int readIntegerValue(CharacterIterator i) {
        char ch = i.current();
        if (ch == '-') return readDecimalValue(i, 10);
        if (ch == '0') {
            ch = i.next();
            if (ch == 'x' || ch == 'X') {
                i.next();
                return readHexValue(i, 8);
            } else if (ch == 'b' || ch == 'B') {
                i.next();
                return readBinaryValue(i, 32);
            } else return readOctalValue(i, 11);
        } else return readDecimalValue(i, 10);
    }

    public static void skipWhiteSpace(CharacterIterator i) {
        while (true) {
            char c = i.current();
            if (c != ' ' && c != '\n' && c != '\t') break;
            i.next();
        }
    }

    public static char peek(CharacterIterator i) {
        return i.current();
    }

    public static boolean peekAndEat(CharacterIterator i, char c) {
        char r = i.current();
        if (r == c) {
            i.next();
            return true;
        }
        return false;
    }

    public static boolean peekAndEat(CharacterIterator i, String s) {
        int ind = i.getIndex();
        for (int cntr = 0; cntr < s.length(); cntr++) {
            if (i.current() == s.charAt(cntr)) i.next();
            else {
                i.setIndex(ind);
                return false;
            }
        }
        return true;
    }

    public static void expectChar(CharacterIterator i, char c) throws Exception {
        char r = i.current();
        i.next();
        if (r != c) Util.failure("parse error at " + i.getIndex() + ", expected character " + squote(c));
    }

    public static void expectChars(CharacterIterator i, String s) throws Exception {
        for (int cntr = 0; cntr < s.length(); cntr++) expectChar(i, s.charAt(cntr));
    }

    public static void expectKeyword(CharacterIterator i, String kw) {
        String str = readIdentifier(i);
        if (!str.equals(kw))
            Util.failure("parse error at " + i.getIndex() + ", expected keyword " + quote(kw));
    }


    /**
     * The <code>isHex()</code> method checks whether the specifed string represents a hexadecimal
     * integer. This method only checks the first two characters. If they match "0x" or "0X", then
     * this method returns true, otherwise, it returns false.
     *
     * @param s the string to check whether it begins with a hexadecimal sequence
     * @return true if the string begins with "0x" or "0X"; false otherwise
     */
    public static boolean isHex(String s) {
        if (s.length() < 2) return false;
        char c = s.charAt(1);
        return s.charAt(0) == '0' && (c == 'x' || c == 'X');
    }

    /**
     * The <code>isBin()</code> method checks whether the specifed string represents a binary
     * integer. This method only checks the first two characters. If they match "0b" or "0B", then
     * this method returns true, otherwise, it returns false.
     *
     * @param s the string to check whether it begins with a hexadecimal sequence
     * @return true if the string begins with "0b" or "0B"; false otherwise
     */
    public static boolean isBin(String s) {
        if (s.length() < 2) return false;
        char c = s.charAt(1);
        return s.charAt(0) == '0' && (c == 'b' || c == 'B');
    }

    /**
     * The <code>isHexDigit()</code> method tests whether the given character corresponds to one of the
     * characters used in the hexadecimal representation (i.e. is '0'-'9' or 'a'-'b', case insensitive. This
     * method is generally used in parsing and lexing of input.
     *
     * @param c the character to test
     * @return true if this character is a hexadecimal digit; false otherwise
     */
    public static boolean isHexDigit(char c) {
        return CharUtil.isHexDigit(c);
    }

    public static int hexValueOf(char c) {
        return CharUtil.hexValueOf(c);
    }

    public static int octalValueOf(char c) {
        return CharUtil.octValueOf(c);
    }

    public static boolean isOctalDigit(char c) {
        return CharUtil.isOctDigit(c);
    }

    /**
     * The <code>justify()</code> method justifies a string to either the right or left margin
     * by inserting spaces to pad the string to a specific width. This is useful in printing out
     * values in a columnar (aligned) format. This version of the method accepts a string buffer
     * into which to put the string.
     * @param right a parameter determining whether to justify to the right margin. If this parameter
     * is true, the padding spaces will be inserted on the left, before the string.
     * @param buf the string buffer into which to write the padded string
     * @param s the string to justify
     * @param width the width (in characters) to which to justify the string
     */
    public static void justify(boolean right, StringBuffer buf, String s, int width) {
        int pad = width - s.length();
        if ( right ) {
            space(buf, pad);
            buf.append(s);
        } else {
            buf.append(s);
            space(buf, pad);
        }
    }

    public static void justify(boolean right, StringBuffer buf, long l, int width) {
        justify(right, buf, Long.toString(l), width);
    }

    public static void justify(boolean right, StringBuffer buf, float f, int width) {
        justify(right, buf, Float.toString(f), width);
    }

    /**
     * The <code>justify()</code> method justifies a string to either the right or left margin
     * by inserting spaces to pad the string to a specific width. This is useful in printing out
     * values in a columnar (aligned) format.
     * @param right a parameter determining whether to justify to the right margin. If this parameter
     * is true, the padding spaces will be inserted on the left, before the string.
     * @param s the string to justify
     * @param width the width (in characters) to which to justify the string
     * @return a new string with padding inserted
     */
    public static String justify(boolean right, String s, int width) {
        // if the string is too wide, return the original
        if ( width - s.length() <= 0 ) return s;
        // otherwise, adjust with padding
        StringBuffer buf = new StringBuffer(width);
        justify(right, buf, s, width);
        return buf.toString();
    }

    public static String justify(boolean right, long l, int width) {
        return justify(right, Long.toString(l), width);
    }

    public static String justify(boolean right, float f, int width) {
        return justify(right, Float.toString(f), width);
    }

    /**
     * The <code>leftJustify()</code> method pads a string to a specified length by adding spaces on the
     * right, thus justifying the string to the left margin. This is extremely useful in generating columnar
     * output in textual tables.
     *
     * @param v     a long value to convert to a string and justify
     * @param width the number of characters to pad the string to
     * @return a string representation of the input, padded on the right with spaces to achieve the desired
     *         length.
     */
    public static String leftJustify(long v, int width) {
        return justify(false, v, width);
    }

    /**
     * The <code>leftJustify()</code> method pads a string to a specified length by adding spaces on the
     * right, thus justifying the string to the left margin. This is extremely useful in generating columnar
     * output in textual tables.
     *
     * @param v     a floating point value to convert to a string and justify
     * @param width the number of characters to pad the string to
     * @return a string representation of the input, padded on the right with spaces to achieve the desired
     *         length.
     */
    public static String leftJustify(float v, int width) {
        return justify(false, v, width);
    }

    /**
     * The <code>leftJustify()</code> method pads a string to a specified length by adding spaces on the
     * right, thus justifying the string to the left margin. This is extremely useful in generating columnar
     * output in textual tables.
     *
     * @param s     a string to justify
     * @param width the number of characters to pad the string to
     * @return a string representation of the input, padded on the right with spaces to achieve the desired
     *         length.
     */
    public static String leftJustify(String s, int width) {
        return justify(false, s, width);
    }

    /**
     * The <code>rightJustify()</code> method pads a string to a specified length by adding spaces on the
     * left, thus justifying the string to the right margin. This is extremely useful in generating columnar
     * output in textual tables.
     *
     * @param v     a long value to convert to a string and justify
     * @param width the number of characters to pad the string to
     * @return a string representation of the input, padded on the left with spaces to achieve the desired
     *         length.
     */
    public static String rightJustify(long v, int width) {
        return justify(true, v, width);
    }

    /**
     * The <code>rightJustify()</code> method pads a string to a specified length by adding spaces on the
     * left, thus justifying the string to the right margin. This is extremely useful in generating columnar
     * output in textual tables.
     *
     * @param v     a floating point value to convert to a string and justify
     * @param width the number of characters to pad the string to
     * @return a string representation of the input, padded on the left with spaces to achieve the desired
     *         length.
     */
    public static String rightJustify(float v, int width) {
        return justify(true, v, width);
    }

    /**
     * The <code>rightJustify()</code> method pads a string to a specified length by adding spaces on the
     * left, thus justifying the string to the right margin. This is extremely useful in generating columnar
     * output in textual tables.
     *
     * @param s     a string to justify
     * @param width the number of characters to pad the string to
     * @return a string representation of the input, padded on the left with spaces to achieve the desired
     *         length.
     */
    public static String rightJustify(String s, int width) {
        return justify(true, s, width);
    }

    /**
     * The <code>toHex()</code> converts the specified long value into a hexadecimal string of the given with.
     * The value will be padded on the left with zero values to achieve the desired with.
     *
     * @param value the long value to convert to a string
     * @param width the desired length of the string
     * @return a hexadecimal string representation of the given value, padded on the left with zeroes to the
     *         length specified
     */
    public static String toHex(long value, int width) {
        return convertToHex(value, width, 0, new char[width], CharUtil.HEX_CHARS);
    }

    public static String toLowHex(long value, int width) {
        return convertToHex(value, width, 0, new char[width], CharUtil.LOW_HEX_CHARS);
    }

    private static String convertToHex(long value, int width, int start, char[] result, char[] hexChars) {
        if (value > (long) 1 << width * 4) {
            StringBuffer buf = new StringBuffer();
            for (int cntr = 0; cntr < start; cntr++) buf.append(result[cntr]);
            buf.append(Long.toHexString(value).toUpperCase());
            return buf.toString();
        }

        int i = start + width - 1;
        for (int cntr = 0; cntr < width; cntr++) {
            result[i - cntr] = hexChars[(int) (value >> (cntr * 4)) & 0xf];
        }

        return new String(result);
    }

    public static String to0xHex(long value, int width) {
        char[] result = new char[width + 2];
        result[0] = '0';
        result[1] = 'x';
        return convertToHex(value, width, 2, result, CharUtil.HEX_CHARS);
    }

    public static String toBin(long value, int width) {
        char[] result = new char[width];

        for (int cntr = 0; cntr < width; cntr++)
            result[width - cntr - 1] = (value & (0x1 << cntr)) == 0 ? '0' : '1';

        return new String(result);
    }

    public static void toHex(StringBuffer buf, long value, int width) {
        if (value > (long) 1 << width * 4) {
            buf.append(Long.toHexString(value).toUpperCase());
            return;
        }

        for (int cntr = width - 1; cntr >= 0; cntr--)
            buf.append(CharUtil.HEX_CHARS[(int) (value >> (cntr * 4)) & 0xf]);
    }

    public static String splice(String[] a, String[] b) {
        StringBuffer buf = new StringBuffer();
        int cntr = 0;
        for (; cntr < a.length; cntr++) {
            buf.append(a[cntr]);
            if (cntr < b.length) buf.append(b[cntr]);
        }

        for (; cntr < b.length; cntr++) {
            buf.append(b[cntr]);
        }
        return buf.toString();
    }

    /**
     * The <code>quote()</code> method simply adds double quotes around a string.
     *
     * @param s the string to add double quotes to
     * @return a new string that is the result of concatenating the double quote character, the specified
     *         string, and another double quote character in sequence
     */
    public static String quote(Object s) {
        return QUOTE + s + QUOTE;
    }

    /**
     * The <code>squote()</code> method simply adds single quotes around a character.
     *
     * @param c the character to add double quotes to
     * @return a new string that is the result of concatenating the double quote character, the specified
     *         string, and another double quote character in sequence
     */
    public static String squote(char c) {
        return SQUOTE + c + SQUOTE;
    }

    /**
     * The <code>embed()</code> method simply adds parentheses around a string.
     *
     * @param s the string to add parentheses to
     * @return a new string that is the result of concatenating the parenthesis character, the specified
     *         string, and another parenthesis character in sequence
     */
    public static String embed(Object s) {
        return LPAREN + s + RPAREN;
    }

    public static String embed(Object lead, Object arg) {
        return lead + LPAREN + arg + RPAREN;
    }

    public static String embed(Object lead, Object arg1, Object arg2) {
        return lead + LPAREN + arg1 + COMMA_SPACE + arg2 + RPAREN;
    }

    public static String embed(Object lead, Object arg1, Object arg2, Object arg3) {
        return lead + LPAREN + arg1 + COMMA_SPACE + arg2 + COMMA_SPACE + arg3 + RPAREN;
    }

    public static String embed(Object lead, Object arg1, Object arg2, Object arg3, Object arg4) {
        return lead + LPAREN + arg1 + COMMA_SPACE + arg2 + COMMA_SPACE + arg3 + COMMA_SPACE + arg4 + RPAREN;
    }

    public static String commalist(List l) {
        StringBuffer buf = new StringBuffer();
        commalist(l, buf);
        return buf.toString();
    }

    public static void commalist(List l, StringBuffer buf) {
        Iterator i = l.iterator();
        while (i.hasNext()) {
            buf.append(i.next().toString());
            if (i.hasNext()) buf.append(", ");
        }
    }

    public static String commalist(Object[] o) {
        StringBuffer buf = new StringBuffer();
        commalist(o, buf);
        return buf.toString();
    }

    public static void commalist(Object[] o, StringBuffer buf) {
        for ( int cntr = 0; cntr < o.length; cntr++ ) {
            if (cntr > 0) buf.append(", ");
            buf.append(o[cntr].toString());
        }
    }

    public static void commalist(Iterator i, StringBuffer buf) {
        for ( int cntr = 0; i.hasNext(); cntr++ ) {
            if (cntr > 0) buf.append(", ");
            buf.append(i.next().toString());
        }
    }

    public static String linelist(List l) {
        StringBuffer buf = new StringBuffer();
        linelist(buf, l);
        return buf.toString();
    }

    public static void linelist(StringBuffer buf, List l) {
        Iterator i = l.iterator();
        while (i.hasNext()) {
            buf.append(i.next().toString());
            buf.append('\n');
        }
    }

    public static String commalist(Object o1, Object o2) {
        return o1 + COMMA + o2;
    }

    public static String commalist(Object o1, Object o2, Object o3) {
        return o1 + COMMA + o2 + COMMA + o3;
    }

    public static String commalist(Object o1, Object o2, Object o3, Object o4) {
        return o1 + COMMA + o2 + COMMA + o3 + COMMA + o4;
    }

    public static String interval(int low, int high) {
        return "[" + low + ", " + high + ']';
    }

    public static char alpha(int num) {
        return (char) ('a' + num - 1);
    }

    public static String qembed(String s1, String s2, String s3) {
        return s1 + ' ' + quote(s2) + ' ' + s3;
    }

    public static int evaluateIntegerLiteral(String val) {
        return readIntegerValue(new StringCharacterIterator(val));
    }

    public static String evaluateStringLiteral(String literal) throws Exception {
        StringBuffer buffer = new StringBuffer(literal.length());
        CharacterIterator i = new StringCharacterIterator(literal);

        expectChar(i, QUOTE_CHAR);
        while (true) {
            if (peekAndEat(i, QUOTE_CHAR)) break;
            char c = i.current();
            i.next();

            if (c == CharacterIterator.DONE) break;
            if (c == BACKSLASH) c = escapeChar(i);

            buffer.append(c);
        }

        expectChar(i, CharacterIterator.DONE);

        return buffer.toString();
    }

    public static char evaluateCharLiteral(String literal) throws Exception {
        CharacterIterator i = new StringCharacterIterator(literal);

        expectChar(i, SQUOTE_CHAR);

        char ch;
        if (peekAndEat(i, BACKSLASH)) {
            ch = escapeChar(i);
        } else {
            ch = i.current();
            i.next();
        }

        expectChar(i, SQUOTE_CHAR);
        expectChar(i, CharacterIterator.DONE);

        return ch;
    }

    private static char escapeChar(CharacterIterator i) {
        char c = i.current();
        switch (c) {
            case 'f':
                i.next();
                return '\f';
            case 'b':
                i.next();
                return '\b';
            case 'n':
                i.next();
                return '\n';
            case 'r':
                i.next();
                return '\r';
            case BACKSLASH:
                i.next();
                return BACKSLASH;
            case SQUOTE_CHAR:
                i.next();
                return SQUOTE_CHAR;
            case QUOTE_CHAR:
                i.next();
                return QUOTE_CHAR;
            case 't':
                i.next();
                return '\t';
            case 'x':
                return (char) readHexValue(i, 4);
            case '0': // fall through
            case '1': // fall through
            case '2': // fall through
            case '3': // fall through
            case '4': // fall through
            case '5': // fall through
            case '6': // fall through
            case '7':
                return (char) readOctalValue(i, 3);

        }
        return c;
    }

    private static IllegalArgumentException invalidCharLiteral(String lit) {
        return new IllegalArgumentException("Invalid character literal: " + lit);
    }

    public static String trimquotes(String s) {
        if (s.length() == 0) return s;

        int start = 0, end = s.length();
        if (s.charAt(start) == '\"') start++;
        if (s.charAt(end - 1) == '\"') end--;

        if (start < end) return s.substring(start, end);
        else return "";
    }

    public static String formatParagraphs(String s, int leftJust, int indent, int width) {
        int len = s.length();
        indent += leftJust;
        int consumed = indent + leftJust;
        String indstr = space(indent);
        String ljstr = space(leftJust);
        StringBuffer buf = new StringBuffer(s.length() + 50);
        buf.append(indstr);
        int lastSp = -1;
        for (int cntr = 0; cntr < len; cntr++) {
            char c = s.charAt(cntr);
            if (c == '\n') {
                buf.append('\n');
                consumed = indent;
                buf.append(indstr);
                continue;
            } else if (Character.isWhitespace(c)) {
                lastSp = buf.length();
            }
            buf.append(c);
            consumed++;

            if (consumed > width) {
                if (lastSp >= 0) {
                    buf.setCharAt(lastSp, '\n');
                    buf.insert(lastSp + 1, ljstr);
                    consumed = buf.length() - lastSp + leftJust - 1;
                }
            }
        }
        return buf.toString();
    }

    public static List trimLines(String s, int indent, int width) {
        LinkedList list = new LinkedList();
        int len = s.length();
        int consumed = indent;
        String indstr = space(indent);
        StringBuffer buf = new StringBuffer(s.length());
        buf.append(indstr);
        int lastSp = -1;
        for (int cntr = 0; cntr < len; cntr++) {
            char c = s.charAt(cntr);
            if (c == '\n') {
                buf = newBuffer(indstr, buf, list);
                consumed = buf.length();
                continue;
            } else if (Character.isWhitespace(c)) {
                lastSp = consumed;
            }
            buf.append(c);
            consumed++;

            if (consumed > width) {
                if (lastSp >= 0) {
                    String leftover = buf.substring(lastSp + 1); // get leftover already in the buffer
                    buf.setLength(lastSp); // trim off any stuff after the last space
                    buf = newBuffer(leftover, buf, list); // create new buffer and add last to list
                    consumed = buf.length();
                }
            }
        }
        if (buf.length() > 0) list.add(buf.toString());
        return list;
    }

    static StringBuffer newBuffer(String n, StringBuffer old, List strs) {
        strs.add(old.toString());
        return new StringBuffer(n);
    }

    /**
     * The <code>dup()</code> method takes a character and a count and returns a string where that character
     * has been duplicated the specified number of times.
     *
     * @param c   the character to duplicate
     * @param len the number of times to duplicate the character
     * @return a string representation of the particular character duplicated the specified number of times
     */
    public static String dup(char c, int len) {
        StringBuffer buf = new StringBuffer(len);
        for (int cntr = 0; cntr < len; cntr++) {
            buf.append(c);
        }
        return buf.toString();
    }

    protected static final String[] spacers = {
            "",            // 0
            " ",           // 1
            "  ",          // 2
            "   ",         // 3
            "    ",        // 4
            "     ",       // 5
            "      ",      // 6
            "       ",     // 7
            "        ",    // 8
            "         ",    // 9
            "          ",  // 10
    };

    public static String space(int len) {
        if ( len <= 0 ) return "";
        if ( len < spacers.length ) return spacers[len];
        return dup(' ', len);
    }

    public static void space(StringBuffer buf, int len) {
        // PERF: consider using spacer[] array of character or string
        while ( len-- > 0 ) buf.append(' ');
    }

    // TODO: test this routine with negative numbers!
    public static String toFixedFloat(float fval, int places) {
        if ( Float.isInfinite(fval) ) return "(inf)";
        if ( Float.isNaN(fval) ) return "(NaN)";

        StringBuffer buf = new StringBuffer(places + 5);
        // append the whole part
        long val = (long) fval;
        buf.append(val);
        // append the fractional part
        float fract = fval >= 0 ? fval - val : val - fval;
        appendFract(buf, fract, places);

        return buf.toString();
    }

    public static String toDecimal(long val, int places) {
        StringBuffer buf = new StringBuffer(10 + places);
        while (places > 0) {
            buf.append(val % 10);
            places--;
            val = val / 10;
            if (places == 0) buf.append('.');
        }
        buf.reverse();
        return val + buf.toString();

    }

    public static String toMultirepString(int value, int bits) {
        StringBuffer buf = new StringBuffer(bits * 3 + 8);

        buf.append("0x");
        int hexdigs = (bits + 3) / 4;
        toHex(buf, value, hexdigs);

        buf.append(" [");
        // append each of the bits
        for (int bit = bits - 1; bit >= 0; bit--)
            buf.append(Arithmetic.getBit(value, bit) ? '1' : '0');

        buf.append("] (");
        buf.append(value);
        buf.append(") ");
        if (bits < 9) {
            appendChar(value, buf);
        }
        return buf.toString();
    }

    private static void appendChar(int value, StringBuffer buf) {
        switch (value) {
            case '\n':
                buf.append("'\\n'");
                break;
            case '\r':
                buf.append("'\\r'");
                break;
            case '\b':
                buf.append("'\\b'");
                break;
            case '\t':
                buf.append("'\\t'");
                break;
            default:
                if (value >= 32) {
                    buf.append(SQUOTE);
                    buf.append((char) value);
                    buf.append(SQUOTE);
                }
        }
    }

    public static char toBit(boolean f) {
        return f ? '1' : '0';
    }

    public static void appendFract(StringBuffer buf, double val, int digits) {
        int cntr = 0;
        for (int radix = 10; cntr < digits; radix = radix * 10, cntr++) {
            if (cntr == 0) buf.append('.');
            int digit = (int) (val * radix) % 10;
            buf.append((char) (digit + '0'));
        }
    }

    public static String stringReplace(String template, Properties p, Object o1) {
        p.setProperty("1", o1.toString());
        return stringReplace(template, p);
    }

    public static String stringReplace(String template, Properties p, Object[] strs) {
        for ( int cntr = 0; cntr < strs.length; cntr++ ) {
            p.setProperty(String.valueOf((cntr + 1)), strs[cntr].toString());
        }
        return stringReplace(template, p);
    }

    public static String stringReplace(String template, Properties p, String[] strs) {
        for ( int cntr = 0; cntr < strs.length; cntr++ ) {
            p.setProperty(String.valueOf((cntr + 1)), strs[cntr]);
        }
        return stringReplace(template, p);
    }

    public static String stringReplace(String template, Properties p, Object o1, Object o2) {
        p.setProperty("1", o1.toString());
        p.setProperty("2", o2.toString());
        return stringReplace(template, p);
    }

    public static String stringReplace(String template, Properties p, Object o1, Object o2, Object o3) {
        p.setProperty("1", o1.toString());
        p.setProperty("2", o2.toString());
        p.setProperty("3", o3.toString());
        return stringReplace(template, p);
    }

    public static String stringReplace(String template, Properties p) {
        int max = template.length();
        StringBuffer buf = new StringBuffer(max);
        for (int pos = 0; pos < max; pos++) {
            char ch = template.charAt(pos);
            if (ch == '$') {
                pos = replaceVar(pos, max, template, buf, p);
            } else if (ch == '%') {
                pos = replaceVarQuote(pos, max, template, buf, p);
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    private static int replaceVar(int pos, int max, String template, StringBuffer buf, Properties p) {
        StringBuffer var = new StringBuffer(10);
        pos = scanAhead(pos, '$', max, template, buf, var);
        String result = getProperty(var, p);
        buf.append(result);
        return pos;
    }

    private static int scanAhead(int pos, char ch, int max, String template, StringBuffer buf, StringBuffer var) {
        for (pos++; pos < max; pos++) {
            char vch = template.charAt(pos);
            if (!Character.isLetterOrDigit(vch)) {
                pos--;
                break;
            }
            if (vch == ch) {
                buf.append(ch);
                break;
            }
            var.append(vch);
        }
        return pos;
    }

    private static String getProperty(StringBuffer var, Properties p) {
        String varname = var.toString();
        String result = p.getProperty(varname);
        if (result == null) throw Util.failure("stringReplace(): unknown variable " + quote(varname));
        return result;
    }

    private static int replaceVarQuote(int pos, int max, String template, StringBuffer buf, Properties p) {
        StringBuffer var = new StringBuffer(10);
        pos = scanAhead(pos, '%', max, template, buf, var);
        String result = getProperty(var, p);
        buf.append(QUOTE_CHAR);
        buf.append(result);
        buf.append(QUOTE_CHAR);
        return pos;
    }

    public static char[] getStringChars(String str) {
        char[] val = new char[str.length()];
        str.getChars(0, val.length, val, 0);
        return val;
    }

    public static List toList(String val) {
        LinkedList list = new LinkedList();
        if ("".equals(val)) return list;

        CharacterIterator i = new StringCharacterIterator(val);
        StringBuffer buf = new StringBuffer(32);
        while (i.current() != CharacterIterator.DONE) {
            if (i.current() == ',') {
                list.add(buf.toString().trim());
                buf = new StringBuffer(32);
            } else {
                buf.append(i.current());
            }
            i.next();
        }
        list.add(buf.toString().trim());
        return list;
    }

    public static String getShortName(Class clazz) {
        String nm = clazz.getName();
        int dollar = nm.lastIndexOf('$');
        int dot = nm.lastIndexOf('.');
        if ( dot > 0 || dollar > 0 ) {
            if ( dot > dollar ) nm = nm.substring(dot + 1, nm.length());
            else nm = nm.substring(dollar + 1, nm.length());
        }
        return nm;
    }
}
