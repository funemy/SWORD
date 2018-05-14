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

package avrora.sim.util;

import cck.text.Terminal;
import cck.util.Util;

/**
 * <code>Mem16</code> is a memory use profiler, it keeps track of the
 * set of values written to a double byte of RAM, dropping to bottom
 * when a predefined maximum number of values is exceeded.
 *
 * @author John Regehr
 */
public class Mem16 {
    public final int[] values;
    public static final int max = 16;

    // -1 = bottom, -2 = invalid
    public int count = 0;
    // 0 = waiting for either, 1 = waiting for lo, 2 = waiting for hi
    public int state = 0;
    public long deadline = 0;
    public byte saved;

    public Mem16() {
        values = new int[max];
    }

    public void add(int v) {
        for (int i = 0; i < count; i++) {
            if (values[i] == v) {
                return;
            }
        }
        // not found
        if (count == max) {
            count = -1;
        } else {
            values[count] = v;
            count++;
        }
    }

    public int btoi(byte b) {
        int i = (int) b;
        if (i < 0) {
            i += 256;
        }
        return i;
    }

    public void add_lo(byte value, long time) {
        if (count < 0) return;
        if (state == 0) {
            state = 2;
            saved = value;
            return;
        }
        if (state == 1) {
            int v = btoi(value) + (btoi(saved) << 8);
            add(v);
            state = 0;
            return;
        }
        if (state == 2) {
            count = -2;
            return;
        }
        throw Util.failure("bug in Mem16!");
    }

    public void add_hi(byte value, long time) {
        if (count < 0) return;
        if (state == 0) {
            state = 1;
            saved = value;
            return;
        }
        if (state == 2) {
            int v = btoi(saved) + (btoi(value) << 8);
            add(v);
            state = 0;
            return;
        }
        if (state == 1) {
            count = -2;
            return;
        }
        throw Util.failure("bug in Mem16!");
    }

    public void print() {
        if (count == -2) {
            Terminal.println("invalid");
            return;
        }
        if (count == -1) {
            Terminal.println("bottom");
            return;
        }
        for (int i = 0; i < count; i++) {
            Terminal.print(Integer.toString(values[i]) + " ");
        }
        Terminal.println("");
    }
}

