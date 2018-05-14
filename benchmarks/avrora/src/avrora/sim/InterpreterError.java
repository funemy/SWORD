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

package avrora.sim;

import cck.text.StringUtil;
import cck.util.Util;

/**
 * The <code>InterpreterError</code> class is a collection point for all of the error classes that
 * correspond to errors that can happen during the interpretation of a program.
 *
 * @author Ben L. Titzer
 */
public class InterpreterError {

    /**
     * The <code>NoSuchInstructionException()</code> is thrown when the program attempts to execute
     * an instruction that does not exist (i.e. a section of the flash that is not initialized).
     */
    public static class NoSuchInstructionException extends Util.Error {
        public final int badPc;

        protected NoSuchInstructionException(int pc) {
            super("Program error", "attempt to execute non-existant instruction at " + StringUtil.addrToString(pc));
            this.badPc = pc;
        }
    }

    /**
     * The <code>PCOutOfBoundsException</code> is thrown when the program attempts to execute
     * an instruction outside the bounds of the flash.
     */
    public static class PCOutOfBoundsException extends Util.Error {
        public final int badPc;

        protected PCOutOfBoundsException(int pc) {
            super("Program error", "PC out of bounds at " + StringUtil.addrToString(pc));
            this.badPc = pc;
        }
    }

    /**
     * The <code>PCAlignmentException</code> is thrown if the program counter somehow becomes misaligned.
     * This should not happen during normal execution, but is included to guard against interpreter
     * bugs.
     */
    public static class PCAlignmentException extends Util.Error {
        public final int badPc;

        protected PCAlignmentException(int pc) {
            super("Program error", "PC misaligned at " + StringUtil.addrToString(pc));
            this.badPc = pc;
        }
    }

    /**
     * The <code>AddressOutOfBoundsException</code> is thrown when the user attempts to access out of
     * bounds memory through the state interface.
     */
    public static class AddressOutOfBoundsException extends Util.Error {
        public final String segment;
        public final int data_addr;
        public final int badPc;

        protected AddressOutOfBoundsException(String s, int pc, int da) {
            super("Program error", "at pc = " + StringUtil.addrToString(pc) + ", illegal access of " + StringUtil.quote(s) + " at " + StringUtil.addrToString(da));
            this.data_addr = da;
            this.segment = s;
            this.badPc = pc;
        }
    }
}
