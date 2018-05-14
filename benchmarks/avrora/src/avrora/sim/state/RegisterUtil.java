/**
 * Copyright (c) 2006, Regents of the University of California
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
 * Creation date: Sep 20, 2006
 */

package avrora.sim.state;

import avrora.sim.clock.Clock;
import avrora.sim.*;
import avrora.sim.output.SimPrinter;
import cck.util.Arithmetic;
import cck.text.StringUtil;

/**
 * The <code>RegisterUtil</code> class implements a number of utilities for
 * constructing new views of register state. For example, a view of a bit as
 * a boolean, a view of a subrange of bits, a stacked view of multiple registers,
 * etc.
 *
 * @author Ben L. Titzer
 */
public class RegisterUtil {

    public static class Buffer {
        protected final Register r1;
        protected final Register r2;

        public Buffer(Register a, Register b) {
            r1 = a;
            r2 = b;
        }

        public void flush() {
            r2.write(r1.getValue());
        }
    }

    public static class BoolView implements BooleanView {
        protected final RegisterView reg;
        protected final byte low;

        public BoolView(RegisterView r, byte l) {
            reg = r;
            low = l;
        }

        public boolean getValue() {
            return (reg.getValue() >> low & 1) == 1;
        }

        public void setValue(boolean v) {
            if ( v ) reg.setValue(reg.getValue() | 1 << low);
            else reg.setValue(reg.getValue() & ~(1 << low));
        }
    }

    public static class ByteArrayView implements RegisterView {
        protected final byte[] values;
        protected final int index;

        public ByteArrayView(byte[] v, int i) {
            values = v;
            index = i;
        }
        public int getWidth() {
            return 8;
        }
        public int getValue() {
            return values[index];
        }

        public void setValue(int val) {
            values[index] = (byte)val;
        }
    }

    public static class CharArrayView implements RegisterView {
        protected final char[] values;
        protected final int index;

        public CharArrayView(char[] v, int i) {
            values = v;
            index = i;
        }
        public int getWidth() {
            return 16;
        }
        public int getValue() {
            return values[index];
        }

        public void setValue(int val) {
            values[index] = (char)val;
        }
    }

    public static class BitRangeView implements RegisterView {
        protected final RegisterView reg;
        protected final byte low;
        protected final byte width;
        protected final int mask;

        public BitRangeView(RegisterView r, byte l, byte h) {
            low = l;
            mask = Arithmetic.getBitRangeMask(l, h);
            width = (byte)(h - l + 1);
            reg = r;
        }

        public int getWidth() {
            return width;
        }

        public int getValue() {
            return (reg.getValue() & mask) >> low;
        }

        public void setValue(int val) {
            reg.setValue(reg.getValue() & ~mask | ((val << low) & mask));
        }
    }

    public static class PermutedView implements RegisterView {
        protected final RegisterView reg;
        protected final byte[] bits;

        public PermutedView(RegisterView r, byte[] b) {
            bits = b;
            reg = r;
        }

        public int getWidth() {
            return bits.length;
        }

        public int getValue() {
            int val = reg.getValue();
            int res = 0;
            for ( int cntr = 0; cntr < bits.length; cntr++ ) {
                int bit = (val >> bits[cntr]) & 1;
                res = res | bit << cntr;
            }
            return res;
        }

        public void setValue(int val) {
            int res = reg.getValue();
            for ( int cntr = 0; cntr < bits.length; cntr++ ) {
                int nbit = (val >> cntr) & 1;
                res = res & ~(1 << bits[cntr]) | nbit << bits[cntr];
            }
            reg.setValue(res);
        }
    }

    public static class StackedView implements RegisterView {
        protected final RegisterView[] regs;
        protected final int width;

        public StackedView(RegisterView[] r) {
            regs = r;
            int w = 0;
            for ( int i = 0; i < r.length; i++ ) w += regs[i].getWidth();
            width = w;
        }

        public int getWidth() {
            return width;
        }

        public int getValue() {
            int val = 0;
            for ( int i = 0, p = 0; i < regs.length; i++ ) {
                RegisterView r = regs[i];
                val = val | r.getValue() << p;
                p += r.getWidth();
            }
            return val;
        }

        public void setValue(int val) {
            for ( int i = 0, p = 0; i < regs.length; i++ ) {
                RegisterView r = regs[i];
                r.setValue(val); // by design, setValue() will ignore high-order bitss
                p += r.getWidth();
                val = val >> p;
            }
        }
    }

    /**
     * The <code>TimedBuffer</code> class implements a buffer for writes from one register
     * to another. Each write to the first register will be intercepted and forwarded to
     * the second register after a specified number of ticks of the specified clock.
     * This class implements the notifications and events necessary to accomplish the
     * functionality.
     */
    public static class TimedBuffer implements Register.Watch, Simulator.Event {
        protected final Clock clock;
        protected final Register r1;
        protected final Register r2;
        protected int value;
        protected long delay;

        public TimedBuffer(Clock c, Register a, Register b, long d) {
            clock = c;
            r1 = a;
            r2 = b;
            delay = d;
            r1.addWatch(this);
        }

        public void fireAfterWrite(Register r, int oldv, int newv) {
            value = newv;
            clock.insertEvent(this, delay);
        }

        public void fireAfterRead(Register r, int oldv, int newv) {
            // do nothing.
        }

        public void setDelay(long cycles) {
            delay = cycles;
        }

        public void fire() {
            r2.write(value);
        }
    }

    public static avrora.sim.state.BooleanView booleanView(RegisterView sup, int low) {
        return new BoolView(sup, (byte)low);
    }

    public static RegisterView bitView(RegisterView sup, int low) {
        return new BitRangeView(sup, (byte)low, (byte)low);
    }

    public static RegisterView bitRangeView(RegisterView sup, int low, int high) {
        return new BitRangeView(sup, (byte)low, (byte)high);
    }

    public static RegisterView permutedView(RegisterView sup, byte[] perm) {
        return new PermutedView(sup, perm);
    }

    public static RegisterView stackedView(RegisterView a, RegisterView b) {
        return new StackedView(new RegisterView[] { a, b });
    }

    public static RegisterView stackedView(RegisterView[] a) {
        return new StackedView(a);
    }

    public static class RegisterPrinter implements Register.Watch {
        protected final SimPrinter printer;
        protected final String name;

        public RegisterPrinter(SimPrinter p, String n) {
            printer = p;
            name = n;
        }
        public void fireAfterWrite(Register r, int oldv, int newv) {
            printer.println(name+"    <=   "+ StringUtil.toMultirepString(newv, r.width));
        }

        public void fireAfterRead(Register r, int oldv, int newv) {
            printer.println(name+"    ->   "+ StringUtil.toMultirepString(oldv, r.width));
        }
    }

    public static void instrumentRegister(SimPrinter sp, Register reg, String name) {
        if (sp != null) reg.addWatch(new RegisterPrinter(sp, name));
    }

    public static class ConstantBehavior extends VolatileBehavior {
        public final int value;
        public ConstantBehavior(int val) {
            value = val;
        }
        public int read(int cur) {
            return value;
        }
        public int write(int cur, int nv) {
            return value;
        }
    }

    public static class ReadonlyBehavior extends VolatileBehavior {
        public int write(int cur, int nv) {
            return cur;
        }
    }
}
