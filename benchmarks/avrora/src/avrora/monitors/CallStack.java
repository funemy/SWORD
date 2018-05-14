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
 * Created May 3, 2006
 */
package avrora.monitors;

import cck.util.Util;
import cck.text.Terminal;
import avrora.sim.output.SimPrinter;
import avrora.core.SourceMapping;

/**
 * The <code>CallStack</code> class implements a monitor that maintains
 * a representation of the call stack of the program as it executes,
 * tracking calls, returns, interrupts, and interrupt returns. This
 * class can provide a call stack to other monitors that report errors,
 * record performance information, etc.
 *
 * @author Ben L. Titzer
 */
public class CallStack implements CallTrace.Monitor {

    public static final int MAXDEPTH = 200;

    protected int depth;
    protected final int maxDepth;
    protected final long[] stack;

    public CallStack() {
        this(MAXDEPTH);
    }

    public CallStack(int maxdepth) {
        maxDepth = maxdepth;
        stack = new long[maxDepth];
    }

    protected void push(byte inum, int site, int target) {
        stack[depth++] = makeEntry(inum, site, target);
        if ( depth >= maxDepth )
            throw Util.failure("Stack overflow: more than "+maxDepth+" calls nested");
    }

    protected void pop() {
        depth--;
        if ( depth < 0 ) depth = 0;
    }

    public void fireBeforeCall(long time, int pc, int target) {
        push((byte)-1, pc, target);
    }

    public void fireAfterReturn(long time, int pc, int retaddr) {
        pop();
    }

    public void fireBeforeInterrupt(long time, int pc, int inum) {
        // TODO: get correct address for interrupt handler
        push((byte)inum, pc, (inum - 1) * 4);
    }

    public void fireAfterInterruptReturn(long time, int pc, int retaddr) {
        pop();
    }

    public static long makeEntry(byte inum, int site, int target) {
        return ((long)inum << 48) | ((long)target << 24) | (long)site;
    }

    public int getDepth() {
        return depth;
    }

    public void clear() {
        depth = 0;
    }

    public int getSite(int indx) {
        return (int)(stack[indx] & 0xffffff);
    }

    public int getTarget(int indx) {
        return (int)((stack[indx] >> 24) & 0xffffff);
    }

    public byte getInterrupt(int indx) {
        return (byte)((stack[indx] >> 48) & 0xffffff);
    }

    public void printStack(SimPrinter printer, SourceMapping sourceMap) {
        int depth = getDepth();
        for (int cntr = depth - 1; cntr >= 0; cntr--) {
            StringBuffer buf = printer.getBuffer();
            buf.append("      @ ");
            int inum = getInterrupt(cntr);
            if ( inum >= 0 ) Terminal.append(Terminal.COLOR_RED, buf, "#"+inum + ' ');
            Terminal.append(Terminal.COLOR_GREEN, buf, sourceMap.getName(getTarget(cntr)));
            printer.printBuffer(buf);
        }
    }
}
