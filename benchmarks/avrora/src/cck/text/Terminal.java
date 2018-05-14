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

import java.io.PrintStream;

/**
 * The <code>Terminal</code> class provides the ability to print color on the terminal by using
 * control characters. The portability of these particular control sequences is not guaranteed, but seem to
 * work most places a color terminal is supported.
 *
 * @author Ben L. Titzer
 */
public class Terminal {

    public static boolean useColors = true;
    public static boolean htmlColors;

    public static final int MAXLINE = 78;

    private static PrintStream out = System.out;

    public static final int COLOR_BLACK = 0;
    public static final int COLOR_RED = 1;
    public static final int COLOR_GREEN = 2;
    public static final int COLOR_BROWN = 3;
    public static final int COLOR_BLUE = 4;
    public static final int COLOR_PURPLE = 5;
    public static final int COLOR_CYAN = 6;
    public static final int COLOR_LIGHTGRAY = 7;

    public static final int COLOR_DARKGRAY = 8;
    public static final int COLOR_BRIGHT_RED = 9;
    public static final int COLOR_BRIGHT_GREEN = 10;
    public static final int COLOR_YELLOW = 11;
    public static final int COLOR_BRIGHT_BLUE = 12;
    public static final int COLOR_MAGENTA = 13;
    public static final int COLOR_BRIGHT_CYAN = 14;
    public static final int COLOR_WHITE = 15;
    public static final int COLOR_DEFAULT = 16;

    public static final int MAXCOLORS = 16;

    private static final String CTRL_BLACK = "\u001b[0;30m";
    private static final String CTRL_RED = "\u001b[0;31m";
    private static final String CTRL_GREEN = "\u001b[0;32m";
    private static final String CTRL_BROWN = "\u001b[0;33m";
    private static final String CTRL_BLUE = "\u001b[0;34m";
    private static final String CTRL_PURPLE = "\u001b[0;35m";
    private static final String CTRL_CYAN = "\u001b[0;36m";
    private static final String CTRL_LIGHTGRAY = "\u001b[0;37m";

    private static final String CTRL_DARKGRAY = "\u001b[1;30m";
    private static final String CTRL_BRIGHT_RED = "\u001b[1;31m";
    private static final String CTRL_BRIGHT_GREEN = "\u001b[1;32m";
    private static final String CTRL_YELLOW = "\u001b[1;33m";
    private static final String CTRL_BRIGHT_BLUE = "\u001b[1;34m";
    private static final String CTRL_MAGENTA = "\u001b[1;35m";
    private static final String CTRL_BRIGHT_CYAN = "\u001b[1;36m";
    private static final String CTRL_WHITE = "\u001b[1;37m";

    private static final String CTRL_DEFAULT = "\u001b[1;00m";

    private static final String[] COLORS = {CTRL_BLACK, CTRL_RED, CTRL_GREEN, CTRL_BROWN, CTRL_BLUE, CTRL_PURPLE, CTRL_CYAN, CTRL_LIGHTGRAY, CTRL_DARKGRAY, CTRL_BRIGHT_RED, CTRL_BRIGHT_GREEN, CTRL_YELLOW, CTRL_BRIGHT_BLUE, CTRL_MAGENTA, CTRL_BRIGHT_CYAN, CTRL_WHITE, CTRL_DEFAULT};

    private static final String[] HTML_COLORS = {"black", /* black */
            "red", "green", "brown", "blue", "purple", "cyan", /* cyan */
            "lightgray", "gray", "pink", "green", "yellow", /* yellow */
            "blue", "magenta", "cyan", "white"};

    private static final String[] HTML_STRINGS;

    static {
        HTML_STRINGS = new String[HTML_COLORS.length];
        for (int cntr = 0; cntr < HTML_STRINGS.length; cntr++)
            HTML_STRINGS[cntr] = "<font color=" + HTML_COLORS[cntr] + '>';
    }

    public static final int ERROR_COLOR = COLOR_RED;
    public static final int WARN_COLOR = COLOR_YELLOW;

    public static void print(int[] colors, String[] s) {
        for (int cntr = 0; cntr < s.length; cntr++) {
            if (cntr < colors.length) print(colors[cntr], s[cntr]);
            else print(s[cntr]);
        }
    }

    public static void print(int color, String s) {
        if (color >= MAXCOLORS || color < 0) print(s);
        else outputColor(color, s);
    }

    public static void println(int color, String s) {
        print(color, s);
        out.print('\n');
    }

    public static void append(int color, StringBuffer buf, String s) {
        if (useColors && color < MAXCOLORS && color >= 0) {
            if (htmlColors) {
                buf.append(HTML_STRINGS[color]);
                buf.append(s);
                buf.append("</font>");
                return;
            } else if (color != COLOR_DEFAULT) {
                buf.append(COLORS[color]);
                buf.append(s);
                buf.append(COLORS[COLOR_DEFAULT]);
                return;
            }
        }
        buf.append(s);

    }

    public static void print(String s) {
        out.print(s);
    }

    public static void println(String s) {
        out.println(s);
    }

    public static void nextln() {
        out.print("\n");
    }

    public static void setOutput(PrintStream s) {
        out = s;
    }

    public static void printRed(String s) {
        outputColor(COLOR_RED, s);
    }

    public static void printBlue(String s) {
        outputColor(COLOR_BLUE, s);
    }

    public static void printGreen(String s) {
        outputColor(COLOR_GREEN, s);
    }

    public static void printYellow(String s) {
        outputColor(COLOR_YELLOW, s);
    }

    public static void printCyan(String s) {
        outputColor(COLOR_CYAN, s);
    }

    public static void printBrightRed(String s) {
        outputColor(COLOR_BRIGHT_RED, s);
    }

    public static void printBrightBlue(String s) {
        outputColor(COLOR_BRIGHT_BLUE, s);
    }

    public static void printBrightGreen(String s) {
        outputColor(COLOR_BRIGHT_GREEN, s);
    }

    public static void printBrightCyan(String s) {
        outputColor(COLOR_BRIGHT_CYAN, s);
    }

    public static void flush() {
        out.flush();
    }

    public static void printPair(int c1, int c2, String s1, String sep, String s2) {
        print(c1, s1);
        print(sep);
        print(c2, s2);
    }

    public static void printTriple(int c1, int c2, int c3, String s1, String sep1, String s2, String sep2, String s3) {
        print(c1, s1);
        print(sep1);
        print(c2, s2);
        print(sep2);
        print(c3, s3);
    }

    private static void outputColor(int color, String s) {
        if (color < 0) color = COLOR_DEFAULT;

        if (useColors) {
            if (htmlColors) {
                out.print(HTML_STRINGS[color]);
                out.print(s);
                out.print("</font>");
                return;
            } else if (color != COLOR_DEFAULT) {
                out.print(COLORS[color]);
                out.print(s);
                out.print(COLORS[COLOR_DEFAULT]);
                return;
            }
        }

        out.print(s);
    }

}
