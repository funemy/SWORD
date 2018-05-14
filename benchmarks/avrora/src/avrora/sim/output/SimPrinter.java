/**
 * Copyright (c) 2007, Regents of the University of California
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
 * Created Oct 20, 2007
 */
package avrora.sim.output;

import avrora.sim.Simulator;
import avrora.sim.util.SimUtil;
import cck.text.Verbose;
import cck.text.Terminal;
import cck.util.Util;

/**
 * The <code>Simulator.Printer</code> class is a printer that is tied to a specific <code>Simulator</code>
 * instance. Being tied to this instance, it will always report the node ID and time before printing
 * anything. This simple mechanism allows the output to be much cleaner to track the output
 * of multiple nodes at once.
 */
public class SimPrinter {

    private Simulator simulator;

    public SimPrinter(Simulator simulator, String category) {
        this.simulator = simulator;
    }

    /**
     * The <code>println()</code> method prints the node ID, the time, and a message to the
     * console, synchronizing with other threads so that output is not interleaved. This method
     * SHOULD ONLY BE CALLED WHEN <code>enabled</code> IS TRUE! This is done to prevent
     * performance bugs created by string construction inside printing (and debugging code).
     * @param s the string to print
     */
    public void println(String s) {
        synchronized ( Terminal.class ) {
            // synchronize on the terminal to prevent interleaved output
            StringBuffer buf = new StringBuffer(s.length() + 30);
            SimUtil.getIDTimeString(buf, simulator);
            buf.append(s);
            Terminal.println(buf.toString());
        }
    }

    public void printBuffer(StringBuffer buffer) {
        synchronized ( Terminal.class ) {
            // synchronize on the terminal to prevent interleaved output
            Terminal.println(buffer.toString());
        }
    }

    public StringBuffer getBuffer() {
        StringBuffer buf = new StringBuffer(70);
        SimUtil.getIDTimeString(buf, simulator);
        return buf;
    }

    public StringBuffer getBuffer(int size) {
        StringBuffer buf = new StringBuffer(30 + size);
        SimUtil.getIDTimeString(buf, simulator);
        return buf;
    }
}
