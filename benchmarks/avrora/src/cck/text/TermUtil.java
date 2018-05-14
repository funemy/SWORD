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

/**
 * @author Ben L. Titzer
 */
public class TermUtil {
    /**
     * The <code>reportQuantity()</code> method is a simply utility to print out a quantity's name
     * (such as "Number of instructions executed", the value (such as 2002), and the units (such as
     * cycles) in a colorized and standardized way.
     *
     * @param name  the name of the quantity as a string
     * @param val   the value of the quantity as a long integer
     * @param units the name of the units as a string
     */
    public static void reportQuantity(String name, long val, String units) {
        reportQuantity(name, Long.toString(val), units);
    }

    /**
     * The <code>reportProportion()</code> method is a simply utility to print out a quantity's name
     * (such as "Number of instructions executed", the value (such as 2002), and the units (such as
     * cycles) in a colorized and standardized way.
     *
     * @param name  the name of the quantity as a string
     * @param val   the value of the quantity as a long integer
     * @param units the name of the units as a string
     */
    public static void reportProportion(String name, long val, long total, String units) {
        String sval = Long.toString(val);
        Terminal.printGreen(name);
        Terminal.print(": ");
        Terminal.printBrightCyan(sval);
        if (units != null && units.length() > 0) Terminal.print(' ' + units + ' ');
        else Terminal.print(" ");
        float pcnt = (100 * (float) val / total);

        Terminal.printBrightCyan(StringUtil.toFixedFloat(pcnt, 4));
        Terminal.println(" %");
    }

    /**
     * The <code>reportQuantity()</code> method is a simply utility to print out a quantity's name
     * (such as "Number of instructions executed", the value (such as 2002), and the units (such as
     * cycles) in a colorized and standardized way.
     *
     * @param name  the name of the quantity as a string
     * @param val   the value of the quantity as a floating point number
     * @param units the name of the units as a string
     */
    public static void reportQuantity(String name, float val, String units) {
        reportQuantity(name, Float.toString(val), units);
    }

    /**
     * The <code>reportQuantity()</code> method is a simply utility to print out a quantity's name
     * (such as "Number of instructions executed", the value (such as 2002), and the units (such as
     * cycles) in a colorized and standardized way.
     *
     * @param name  the name of the quantity as a string
     * @param val   the value of the quantity as a string
     * @param units the name of the units as a string
     */
    public static void reportQuantity(String name, String val, String units) {
        Terminal.printGreen(name);
        Terminal.print(": ");
        Terminal.printBrightCyan(val);
        Terminal.println(' ' + units);
    }

    /**
     * The <code>printSeparator()</code> method prints a horizontal bar on the terminal
     * that helps to separate different sections of textual output. This implementation
     * uses the '=' character, providing a double-thick separator line.
     *
     * @param width the width of the horizontal separator bar
     */
    public static void printSeparator(int width) {
        Terminal.println(StringUtil.dup('=', width));
    }

    /**
     * The <code>printSeparator()</code> method prints a horizontal bar on the terminal
     * that helps to separate different sections of textual output. This implementation
     * uses the '=' character, providing a double-thick separator line.
     */
    public static void printSeparator() {
        Terminal.println(StringUtil.dup('=', Terminal.MAXLINE));
    }

    /**
     * The <code>printThinSeparator()</code> method prints a horizontal bar on the terminal
     * that helps to separate different sections of textual output. This implementation
     * uses the '-' character, providing a thinner separator line for separating subsections
     * of text output.
     *
     * @param width the width of the horizontal separator bar
     */
    public static void printThinSeparator(int width) {
        Terminal.println(StringUtil.dup('-', width));
    }

    /**
     * The <code>printThinSeparator()</code> method prints a horizontal bar on the terminal
     * that helps to separate different sections of textual output. This implementation
     * uses the '-' character, providing a thinner separator line for separating subsections
     * of text output.
     */
    public static void printThinSeparator() {
        Terminal.println(StringUtil.dup('-', Terminal.MAXLINE));
    }

    /**
     * The <code>printSeparator()</code> method prints a horizontal bar on the terminal
     * that helps to separate different sections of textual output. This implementation
     * uses the '=' character, providing a double-thick separator line and embeds a
     * header string within the separating line.
     *
     * @param width  the width of the horizontal separator bar
     * @param header the string to embed in the separator line
     */
    public static void printSeparator(int width, String header) {
        Terminal.print("=={ ");
        Terminal.print(header);
        Terminal.print(" }");
        Terminal.print(StringUtil.dup('=', width - 6 - header.length()));
        Terminal.nextln();
    }

    public static void printSeparator(String header) {
        printSeparator(Terminal.MAXLINE, header);
    }

}
