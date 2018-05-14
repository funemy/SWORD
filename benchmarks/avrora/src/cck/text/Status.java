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

import cck.util.Util;
import cck.util.TimeUtil;

import java.util.Stack;

/**
 * The <code>Status</code> class is a utility that allows printing of the status (or progress) of a computation,
 * which might involve several steps. For example, executing a simulation might require loading several programs,
 * finding and initializing the device platform, creating a network topology, etc.
 *
 * @author Ben L. Titzer
 */
public class Status {

    static class Item {
        String title;
        long starttime;

        Item(String t) {
            title = t;
            starttime = System.currentTimeMillis();
        }
    }

    static Stack stack = new Stack();
    static boolean inside = false;

    public static boolean ENABLED = true;
    public static boolean TIMING = true;

    /**
     * The <code>begin()</code> method prints a new line with the new status. For example,
     * <code>begin("Loading program");</code> prints the status line and an ellipsis <code>"..."</code>,
     * as well as recording the time at which the method was called, which can be used to recover the time
     * spent in each stage. When the operation is completed, the <code>success()</code> or <code>error()</code>
     * methods should be called.
     *
     * @param s the current operation being performed, as a string
     */
    public static void begin(String s) {
        if (!ENABLED) return;

        if (inside) {
            Terminal.nextln();
        }
        Terminal.print(StringUtil.space(stack.size() * 4));
        Terminal.print(Terminal.COLOR_BROWN, s);
        Terminal.print("...");
        Terminal.flush();
        stack.push(new Item(s));
        inside = true;
    }

    /**
     * The <code>success()</code> method simply prints out "OK" in a stylized fashion as well as the time since
     * the last <code>begin()</code> call.
     */
    public static void success() {
        if (!ENABLED) return;
        print(Terminal.COLOR_GREEN, "OK");
    }

    private static void print(int color, String s) {
        long time = -1;
        Item i = (Item) stack.pop();
        if (i != null) {
            time = System.currentTimeMillis() - i.starttime;
        }
        if (TIMING) {
            Terminal.print("[");
        }
        Terminal.print(color, s);
        if (TIMING) {
            if (time >= 0) {
                Terminal.print(": ");
                Terminal.print(TimeUtil.milliToSecs(time) + " seconds");
            }
            Terminal.print("]");
        }
        Terminal.nextln();
        inside = false;
    }

    /**
     * The <code>success()</code> method simply prints out the success string in a stylized fashion as well as the
     * time since the last <code>begin()</code> call.
     *
     * @param s the success string to print out (instead of the default, "OK")
     */
    public static void success(String s) {
        if (!ENABLED) return;
        print(Terminal.COLOR_GREEN, s);
    }

    /**
     * The <code>error()</code> method simply prints out "ERROR" in a stylized fashion was well as the time since
     * the last <code>begin()</code> call.
     */
    public static void error() {
        if (!ENABLED) return;
        print(Terminal.COLOR_RED, "ERROR");
    }

    /**
     * The <code>error()</code> method simply prints out an error string in a stylized fashion was well as the time since
     * the last <code>begin()</code> call.
     *
     * @param s the string to report as an error instead of "ERROR", which is the default
     */
    public static void error(String s) {
        if (!ENABLED) return;
        print(Terminal.COLOR_RED, s);
    }

    /**
     * The <code>error()</code> method simply prints out "ERROR" in a stylized fashion was well as the time since
     * the last <code>begin()</code> call, and then reports the exception.
     *
     * @param t a throwable that was caught since the last begin()
     */
    public static void error(Throwable t) {
        if (!ENABLED) return;
        print(Terminal.COLOR_RED, "UNEXPECTED EXCEPTION");
    }

    /**
     * The <code>error()</code> method simply prints out "ERROR" in a stylized fashion was well as the time since
     * the last <code>begin()</code> call, and then reports the exception.
     *
     * @param e an error that was caught since the last begin()
     */
    public static void error(Util.Error e) {
        if (!ENABLED) return;
        print(Terminal.COLOR_RED, "ERROR");
    }
}
