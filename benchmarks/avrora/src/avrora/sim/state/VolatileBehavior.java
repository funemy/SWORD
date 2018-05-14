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
 * Created Oct 12, 2007
 */
package avrora.sim.state;

/**
 * The <code>VolatileBehavior</code> class represents the behavior associated
 * with a volatile register. This class does not actually store the state of
 * the register, but is intended to implement the behavior associated with
 * a state query or update.
 *
 * @author Ben L. Titzer
 */
public class VolatileBehavior {

    /**
     * The <code>read()</code> method implements the behavior associated
     * with reading this volatile variable. The current state of the
     * register is supplied, and this method should return the new state
     * of the register.
     * @param cur the current state of the register
     * @return the new state of the register; the default behavior is to
     * return the current state
     */
    public int read(int cur) {
        return cur;
    }

    /**
     * The <code>write()</code> method implements the behavior associated
     * with writing this volatile variable to a new state. The current state
     * of the register is supplied with the new (written) value. This method
     * therefore takes the appropriate actions and returns the new state
     * of the register or variable.
     * @param cur the current state of the register or variable
     * @param newv the new (written) value to the register or variable
     * @return the new state of the register; the default behavior is to
     * return the written value.
     */
    public int write(int cur, int newv) {
        return newv;
    }
}
