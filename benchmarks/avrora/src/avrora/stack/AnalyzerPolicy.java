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

/**
 * The <code>Policy</code> interface allows for more modular, composable analysis. An instance of this class
 * is passed to the <code>AbstractInterpreter</code> and that instance determines how to handle calls,
 * returns, indirect jumps, and stack operations in the abstract interpretation. Thus, multiple anlayses can
 * share the same core abstract interpreter in a clean way.
 *
 * @author Ben L. Titzer
 * @see AbstractInterpreter
 */
public interface AnalyzerPolicy {

    /**
     * The <code>call()</code> method is called by the abstract interpreter when it encounters a call
     * instruction within the program. Different policies may handle calls differently. For example, a
     * context-sensitive analysis might fork and start analysis the called method in this context, which a
     * context insensitive analsysis may just merge the current state into the entrance state for that address
     * and then reanalyze that code.
     *
     * @param s              the current calling abstract state
     * @param target_address the concrete target address of the call
     * @return the state of the program after the call, null if there is no next state
     */
    public MutableState call(MutableState s, int target_address);

    /**
     * The <code>interrupt()</code> is called by the abstract interrupt when it encounters a place in the
     * program when an interrupt might occur.
     *
     * @param s   the abstract state just before interrupt
     * @param num the interrupt number that might occur
     * @return the state of the program after the interrupt, null if there is no next state
     */
    public MutableState interrupt(MutableState s, int num);

    /**
     * The <code>ret()</code> method is called by the abstract interpreter when it encounters a return within
     * the program.
     *
     * @param s the current abstract state
     * @return the state of the program after the call, null if there is no next state
     */
    public MutableState ret(MutableState s);

    /**
     * The <code>reti()</code> method is called by the abstract interpreter when it encounters a return from
     * an interrupt within the program.
     *
     * @param s the current abstract state
     * @return the state of the program after the call, null if there is no next state
     */
    public MutableState reti(MutableState s);

    /**
     * The <code>indirectCall()</code> method is called by the abstract interpreter when it encounters an
     * indirect call within the program. The abstract values of the address are given as parameters, so that a
     * policy can choose to compute possible targets or be conservative or whatever it so chooses.
     *
     * @param s        the current abstract state
     * @param addr_low the (abstract) low byte of the address
     * @param addr_hi  the (abstract) high byte of the address
     * @return the state of the program after the call, null if there is no next state
     */
    public MutableState indirectCall(MutableState s, char addr_low, char addr_hi);

    /**
     * The <code>indirectJump()</code> method is called by the abstract interpreter when it encounters an
     * indirect jump within the program. The abstract values of the address are given as parameters, so that a
     * policy can choose to compute possible targets or be conservative or whatever it so chooses.
     *
     * @param s        the current abstract state
     * @param addr_low the (abstract) low byte of the address
     * @param addr_hi  the (abstract) high byte of the address
     * @return the state of the program after the call, null if there is no next state
     */
    public MutableState indirectJump(MutableState s, char addr_low, char addr_hi);

    /**
     * The <code>indirectCall()</code> method is called by the abstract interpreter when it encounters an
     * indirect call within the program. The abstract values of the address are given as parameters, so that a
     * policy can choose to compute possible targets or be conservative or whatever it so chooses.
     *
     * @param s        the current abstract state
     * @param addr_low the (abstract) low byte of the address
     * @param addr_hi  the (abstract) high byte of the address
     * @param ext      the (abstract) extended part of the address
     * @return the state of the program after the call, null if there is no next state
     */
    public MutableState indirectCall(MutableState s, char addr_low, char addr_hi, char ext);

    /**
     * The <code>indirectJump()</code> method is called by the abstract interpreter when it encounters an
     * indirect jump within the program. The abstract values of the address are given as parameters, so that a
     * policy can choose to compute possible targets or be conservative or whatever it so chooses.
     *
     * @param s        the current abstract state
     * @param addr_low the (abstract) low byte of the address
     * @param addr_hi  the (abstract) high byte of the address
     * @param ext      the (abstract) extended part of the address
     * @return the state of the program after the call, null if there is no next state
     */
    public MutableState indirectJump(MutableState s, char addr_low, char addr_hi, char ext);

    /**
     * The <code>push()</code> method is called by the abstract interpreter when a push to the stack is
     * encountered in the program. The policy can then choose what outgoing and/or modelling of the stack
     * needs to be done.
     *
     * @param s   the current abstract state
     * @param val the abstract value to push onto the stack
     */
    public void push(MutableState s, char val);

    /**
     * The <code>pop()</code> method is called by the abstract interpreter when a pop from the stack is
     * ecountered in the program. The policy can then choose to either return whatever information it has
     * about the stack contents, or return an UNKNOWN value.
     *
     * @param s the current abstract state
     * @return the abstract value popped from the stack
     */
    public char pop(MutableState s);

    /**
     * The <code>pushState</code> method is called by the abstract interpreter when a state is forked by the
     * abstract interpreter (for example when a branch condition is not known and both branches must be
     * taken.
     *
     * @param newState the new state created
     */
    public void pushState(MutableState newState);

}
