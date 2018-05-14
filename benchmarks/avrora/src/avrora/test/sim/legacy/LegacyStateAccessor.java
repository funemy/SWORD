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
 * Creation date: Nov 29, 2005
 */

package avrora.test.sim.legacy;

import avrora.arch.legacy.*;
import avrora.core.Program;
import avrora.sim.Simulator;
import avrora.test.sim.StateAccessor;
import cck.util.Arithmetic;

/**
 * The <code>LegacyStateAccessor</code> class provides access to the state of the AVR simulator
 * for the purpose of the automated simulation tester. It provides a number of names such
 * "r1" and "pc" that correspond to values in the simulation. Test cases can specify
 * an initial state, a sequence of instructions to execute, and the expected final state.
 *
 * @author Ben L. Titzer
 */
public class LegacyStateAccessor extends StateAccessor {

    final LegacyInterpreter interpreter;

    public LegacyStateAccessor(Program p, Simulator s) {
        super(p, s);
        interpreter = (LegacyInterpreter)simulator.getInterpreter();

        // install the general purpose registers
        for ( int cntr = 0; cntr < 32; cntr++ ) {
            accessors.put("r"+cntr, newRegister(cntr));
        }
        // install the word registers
        for ( int cntr = 0; cntr < 32; cntr += 2 ) {
            WordRegister register = new WordRegister();
            register.reg = LegacyRegister.getRegisterByNumber(cntr);
            accessors.put("r"+cntr+":r"+(cntr+1), register);
        }

        // install the address registers
        installAR("x");
        installAR("y");
        installAR("z");

        // install accessors for PC, SRAM, SP, and FLASH
        accessors.put("sram", new SRAM());
        accessors.put("pc", new PC());
        accessors.put("sp", new SP());
        accessors.put("flash", new FLASH());
        accessors.put("cycles", new Cycles());

        // install accessors for I, T, H, S, V, N, Z, C
        accessors.put("flags.i", new Flag(LegacyState.SREG_I));
        accessors.put("flags.t", new Flag(LegacyState.SREG_T));
        accessors.put("flags.h", new Flag(LegacyState.SREG_H));
        accessors.put("flags.s", new Flag(LegacyState.SREG_S));
        accessors.put("flags.v", new Flag(LegacyState.SREG_V));
        accessors.put("flags.n", new Flag(LegacyState.SREG_N));
        accessors.put("flags.z", new Flag(LegacyState.SREG_Z));
        accessors.put("flags.c", new Flag(LegacyState.SREG_C));
    }

    private void installAR(String name) {
        WordRegister register = new WordRegister();
        register.reg = LegacyRegister.getRegisterByName(name);
        accessors.put(name, register);
    }

    private Register newRegister(int cntr) {
        Register register = new Register();
        register.reg = LegacyRegister.getRegisterByNumber(cntr);
        return register;
    }

    class Register extends Accessor {
        LegacyRegister reg;
        protected int get() {
            return interpreter.getRegisterByte(reg);
        }
        protected void set(int val) {
            interpreter.writeRegisterByte(reg.getNumber(), (byte)val);
        }
    }

    class WordRegister extends Accessor {
        LegacyRegister reg;
        protected int get() {
            return interpreter.getRegisterWord(reg);
        }
        protected void set(int val) {
            interpreter.writeRegisterWord(reg.getNumber(), val);
        }
    }

    class PC extends Accessor {
        protected int get() {
            return interpreter.getState().getPC();
        }
        protected void set(int val) {
            interpreter.setBootPC(val);
        }
    }

    class SP extends Accessor {
        protected int get() {
            return interpreter.getState().getSP();
        }
        protected void set(int val) {
            interpreter.setSP(val);
        }
    }

    class SRAM extends Accessor {
        protected int get() {
            return 0;
        }
        protected void set(int val) {
            // do nothing.
        }
        protected int getIndex(int ind) {
            return interpreter.getDataByte(ind);
        }
        protected void setIndex(int ind, int val) {
            interpreter.writeDataByte(ind, (byte)val);
        }
    }

    class FLASH extends Accessor {
        protected int get() {
            return 0;
        }
        protected void set(int val) {
            // do nothing.
        }
        protected int getIndex(int ind) {
            return interpreter.getFlashByte(ind);
        }
        protected void setIndex(int ind, int val) {
            interpreter.writeFlashByte(ind, (byte)val);
        }
    }

    class Flag extends Accessor {
        final int bit;
        Flag(int b) {
            bit = b;
        }
        protected int get() {
            return interpreter.getFlag(bit) ? 1 : 0;
        }
        protected void set(int val) {
            interpreter.setFlag(bit, val != 0);
        }
    }

    class Cycles extends Accessor {
        protected int get() {
            return (int)interpreter.getState().getCycles();
        }
        protected void set(int val) {
            interpreter.getMainClock().advance(val);
        }
    }
}
