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

package avrora.stack;

import cck.text.StringUtil;
import cck.text.Terminal;

/**
 * @author Ben L. Titzer
 */
public class StatePrinter {
    public static void printEdge(StateCache.State s, int type, int weight, StateCache.State t) {
        printStateName(s);
        Terminal.print(" --(");
        Terminal.print(Analyzer.EDGE_NAMES[type]);
        if (weight > 0) Terminal.print("+");
        Terminal.print(weight + ")--> ");
        printStateName(t);
        Terminal.nextln();
    }

    public static void printEdge(int type, int weight, StateCache.State t) {
        Terminal.print("--(");
        Terminal.print(Analyzer.EDGE_NAMES[type]);
        if (weight > 0) Terminal.print("+");
        Terminal.print(weight + ")--> ");
        printStateName(t);
        Terminal.nextln();
    }

    public static void printStateName(StateCache.State t) {
        Terminal.print("[");
        Terminal.printBrightGreen(StringUtil.toHex(t.getPC(), 4));
        Terminal.print("|");
        Terminal.printBrightCyan(t.getUniqueName());
        Terminal.print("] ");
    }

    public static void printState(String beg, StateCache.State s) {
        Terminal.printBrightRed(beg);

        printStateName(s);

        for (int cntr = 0; cntr < 8; cntr++) {
            Terminal.print(toString(s.getRegisterAV(cntr)));
            Terminal.print(" ");
        }
        Terminal.nextln();

        printStateLine(s, "SREG", IORegisterConstants.SREG, 8);
        printStateLine(s, "EIMSK", IORegisterConstants.EIMSK, 16);
        printStateLine(s, "TIMSK", IORegisterConstants.TIMSK, 24);
    }

    public static void printStateLine(StateCache.State s, String ior_name, int ior_num, int cntr) {
        int max = cntr + 8;
        printIOReg(ior_name, s.getIORegisterAV(ior_num));
        for (; cntr < max; cntr++) {
            Terminal.print(toString(s.getRegisterAV(cntr)));
            Terminal.print(" ");
        }
        Terminal.nextln();
    }

    public static void printIOReg(String name, char val) {
        String l = StringUtil.rightJustify("[", 21 - name.length());
        Terminal.print(l);
        Terminal.printBrightGreen(name);
        Terminal.print(":");
        Terminal.print(AbstractArithmetic.toString(val));
        Terminal.print("] ");
    }

    public static String toString(char av) {
        if (av == AbstractArithmetic.ZERO)
            return "       0";
        else if (av == AbstractArithmetic.UNKNOWN) return "       .";
        return AbstractArithmetic.toString(av);
    }
}
