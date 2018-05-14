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

import avrora.sim.clock.Clock;
import avrora.sim.util.MulticastFSMProbe;
import cck.util.Util;

/**
 * The <code>FiniteStateMachine</code> class represents a model of a finite state machine that
 * allows probing and monitoring the state of a device.
 *
 * @author Ben L. Titzer
 */
public class FiniteStateMachine {

    /**
     * The <code>Probe</code> interface allows observation of the state changes of a finite
     * state machine. Probes can be inserted for a particular state so that the probe will
     * fire before and after transitions into and out of that state, as well as for every
     * state transition.
     */
    public interface Probe {
        /**
         * The <code>fireBeforeTransition()</code> method allows the probe to gain control
         * before the state machine transitions between two states. The before state and the
         * after state are passed as parameters.
         * @param beforeState the before state represented as an integer
         * @param afterState the after state represented as an integer
         */
        public void fireBeforeTransition(int beforeState, int afterState);

        /**
         * The <code>fireAfterTransition()</code> method allows the probe to gain control
         * after the state machine transitions between two states. The before state and the
         * after state are passed as parameters.
         * @param beforeState the before state represented as an integer
         * @param afterState the after state represented as an integer
         */
        public void fireAfterTransition(int beforeState, int afterState);
    }

    /**
     * The <code>LegacyState</code> class represents a state of the finite state machine, including its
     * name as a String, the transition time to each of the other states, and a list of any
     * probes attached to this state.
     */
    protected class State {
        final String name;
        final int[] transition_time;
        final MulticastFSMProbe probes;

        State(String n, int[] tt) {
            name = n;
            transition_time = tt;
            probes = new MulticastFSMProbe();
        }
    }

    /**
     * The <code>TransitionEvent</code> class is used internally by the finite state machine for transitions
     * that take 1 more more clock cycles. The machine is put in the <code>IN_TRANSITION</code> state and this
     * event is inserted into the event queue of the underlying clock. When this event fires, it will complete
     * the state transition and fire any probes as necessary.
     */
    protected class TransitionEvent implements Simulator.Event {

        protected int oldState;
        protected int newState;

        public void fire() {
            // set the current state to the new state
            curState = newState;
            // fire any probes as necessary
            fireAfter(states[oldState].probes, oldState, newState);
            fireAfter(states[newState].probes, oldState, newState);
            fireAfter(globalProbe, oldState, newState);
        }

    }

    private static void fireBefore(MulticastFSMProbe p, int oldState, int newState) {
        if ( !p.isEmpty() )
            p.fireBeforeTransition(oldState, newState);
    }

    private static void fireAfter(MulticastFSMProbe p, int oldState, int newState) {
        if ( !p.isEmpty() )
            p.fireAfterTransition(oldState, newState);
    }

    public static final int IN_TRANSITION = -1;

    protected final int numStates;
    protected final int startState;
    protected final Clock clock;
    protected final TransitionEvent transEvent = new TransitionEvent();
    protected final MulticastFSMProbe globalProbe = new MulticastFSMProbe();
    protected State[] states;

    protected int curState;

    /**
     * This constructor for the <code>FiniteStateMachine</code> class creates a new finite state machine with
     * the number of states corresponding to the length of the array containing the names of the states. The transition
     * time matrix is uniform, with all entries being initialized to the same specified value.
     * @param c the clock driving this finite state machine
     * @param ss the starting state of this machine
     * @param nm an array of strings that name each of the states in this machine
     * @param tt the transition time for any state to any other state
     */
    public FiniteStateMachine(Clock c, int ss, String[] nm, int tt) {
        clock = c;
        startState = ss;
        curState = ss;
        numStates = nm.length;
        states = new State[numStates];
        int[][] ttm = buildUniformTTM(numStates, tt);
        buildStates(nm, ttm);
    }

    /**
     * This constructor for the <code>FiniteStateMachine</code> class creates a new finite state machine with
     * the number of states corresponding to the length of the array containing the names of the states. The
     * transition time matrix is specified explicitly. Note that this constructor DOES NOT copy the
     * transition time matrix, so care should be taken not to modify the transition time matrix after the
     * creation of this finite state machine.
     * @param c the clock that drives this finite state machine
     * @param ss the starting state of this machine
     * @param nm an array of strings that name each of the states in this machine
     * @param ttm the transition time matrix for this machine; this matrix is NOT COPIED for internal use.
     */
    public FiniteStateMachine(Clock c, int ss, String[] nm, int[][] ttm) {
        clock = c;
        startState = ss;
        curState = ss;
        numStates = nm.length;
        states = new State[numStates];
        buildStates(nm, ttm);
    }

    private void buildStates(String[] nm, int[][] ttm) {
        for ( int cntr = 0; cntr < numStates; cntr++ ) {
            states[cntr] = new State(nm[cntr], ttm[cntr]);
        }
    }

    /**
     * The <code>insertProbe()</code> method allows the insertion of a probe for each state transition of this
     * finite state machine.
     * @param p the probe to insert that will be called before and after each transition, including
     * self-transitions (transitions from one state to the same state)
     */
    public void insertProbe(Probe p) {
        globalProbe.add(p);
    }

    /**
     * The <code>removeProbe()</code> method removes a probe that has been inserted for all state transitions.
     * @param p the probe to remove
     */
    public void removeProbe(Probe p) {
        globalProbe.remove(p);
    }

    /**
     * The <code>insertProbe()</code> method allows the insertion of a probe for transitions that involve a
     * particular state, either transitioning from this state or from this state.
     * @param p the probe to insert that will be called before and after each transition to or from the
     * specified state, including self-transitions (transitions from this state to the same state)
     * @param state the state for which to insert the probe
     */
    public void insertProbe(Probe p, int state) {
        states[state].probes.add(p);
    }

    /**
     * The <code>removeProbe()</code> method removes a probe that has been inserted for particular state transitions.
     * @param p the probe to remove
     * @param state the state for which to remove this probe
     */
    public void removeProbe(Probe p, int state) {
        states[state].probes.remove(p);
    }

    /**
     * The <code>getNumberOfStates()</code> method returns the total number of states that this machine
     * has.
     * @return the number of states in this machine
     */
    public int getNumberOfStates() {
        return numStates;
    }

    /**
     * The <code>getStartState()</code> method returns the state in which the machine starts operation.
     * @return an integer that represents the start state for this machine
     */
    public int getStartState() {
        return startState;
    }

    /**
     * The <code>getCurrentState()</code> method returns an integer that represents the state that the machine
     * is currently in. If the machine is current in transition between two states, this method will return
     * the value <code>IN_TRANSITION</code>
     * @return the current the state of the machine
     */
    public int getCurrentState() {
        return curState;
    }

    /**
     * The <code>transition()</code> method transitions the machine from the current state to a new state.
     * This transition is only legal if the corresponding entry in the transition time matrix is non-negative.
     * This method should only be called by the "controller" of the machine, and not by clients interested
     * in probing the state of the device. If the transition time matrix for this transition is greater than
     * zero, then this method will change the state to <code>IN_TRANSITION</code> and insert a transition
     * event into the queue of the underlying clock for this device. That event will complete the transition.
     * This method will not allow any transitions when the machine is already in transition.
     * @param newState the new state to transition to
     * @throws Util.InternalError if it is illegal to transition between the current state and the new state
     * according to the transition time matrix; or if the machine is already in a transitional state
     */
    public void transition(int newState) {
        // are we currently in a transition already?
        if ( curState == IN_TRANSITION ) {
            throw Util.failure("cannot transition to state "
                    +newState+" while in transition: "
                    +transEvent.oldState+" -> "+transEvent.newState);
        }

        // get transition time
        int ttime = states[curState].transition_time[newState];
        if ( ttime < 0 ) // valid transition ?
            throw Util.failure("cannot transition from state "
                    +curState+" -> "+newState);

        // fire probes before transition
        fireBefore(globalProbe, curState, newState);
        fireBefore(states[curState].probes, curState, newState);
        fireBefore(states[newState].probes, curState, newState);

        if ( ttime == 0 ) {
            // transition is instantaneous
            int oldState = curState;
            curState = newState;
            fireAfter(states[oldState].probes, oldState, newState);
            fireAfter(states[newState].probes, oldState, newState);
            fireAfter(globalProbe, oldState, newState);
        }
        else {
            // transition will complete in the future
            transEvent.oldState = curState;
            transEvent.newState = newState;
            clock.insertEvent(transEvent, ttime);
        }
    }

    /**
     * The <code>getTransitionTime()</code> method retrieves the transition time between the two states
     * specified from the transition time matrix.
     * @param beforeState the state transitioning from
     * @param afterState the state transitioning to
     * @return the number of clock cycles required to transition between these two states (0 or more) if the
     * transition is legal; a negative number if the transition is not legal
     */
    public int getTransitionTime(int beforeState, int afterState) {
        return states[beforeState].transition_time[afterState];
    }

    /**
     * The <code>getStateName()</code> method retrieves the name for the specified state.
     * @param state the state to get the string name for
     * @return a string representation of the specified state
     */
    public String getStateName(int state) {
        return states[state].name;
    }

    /**
     * The <code>getCurrentStateName()</code> method retrieves the name for the current state.
     * @return a string representation of the name of the current state
     */
    public String getCurrentStateName() {
        return states[curState].name;
    }

    /**
     * The <code>getClock()</code> method gets the underlying clock driving the device.
     * @return the clock driving this device
     */
    public Clock getClock() {
        return clock;
    }

    /**
     * The <code>buildUniformTTM()</code> method builds a transition time
     * matrix that is uniform; the machine can transition from any state to any other
     * state with the given transition time.
     * @param size the size of the transition time matrix
     * @param tt the transition time for each edge
     * @return a new transition time matrix where each entry is the given transition time
     */
    public static int[][] buildUniformTTM(int size, int tt) {
        int[][] ttm = new int[size][size];
        if ( tt != 0 ) {
            for ( int cntr = 0; cntr < size; cntr++ ) {
                for ( int loop = 0; loop < size; loop++ ) {
                    ttm[cntr][loop] = tt;
                }
            }
        }
        return ttm;
    }

    /**
     * The <code>buildSparseTTM()</code> method builds a transition time matrix
     * that is uniform but sparse; the machine can transition from any state to any
     * other state with the given transition time. However, the internal representation
     * shares the underlying integer arrays, avoiding large space overhead. Due to this
     * storage sharing, DO NOT ATTEMPT TO UPDATE THE SPARSE TTM AFTER CONSTRUCTING IT
     * UNLESS YOU UNDERSTAND THAT ALL ROWS WILL BE THE SAME.
     * @param size the size of the matrix
     * @param tt the transition time for each edge.
     * @return a new transition time matrix where each entry is the given transition time
     * and the representation is space efficient.
     */
    public static int[][] buildSparseTTM(int size, int tt) {
        int[][] ttm = new int[size][];
        int[] row = new int[size];
        // initialize the matrix with one loop
        for ( int cntr = 0; cntr < size; cntr++ ) {
            ttm[cntr] = row;
            row[cntr] = tt;
        }
        return ttm;
    }

    /**
     * The <code>buildBimodalTTM()</code> method builds a transition time matrix
     * that corresponds to a finite state machine with two modes. One special state
     * is the "default" state. The machine can transition from the default state to
     * any other state, and from any other state back to the default state, but not
     * between any other two states.
     * @param size the size of the transition time matrix
     * @param ds the default state
     * @param tf the transition times from each state back to the default state
     * @param tt the transition times from the default state to each other state
     * @return a square transition time matrix that represents a bimodal state machine
     */
    public static int[][] buildBimodalTTM(int size, int ds, int[] tf, int[] tt) {
        int[][] ttm = newTTM(size);

        for ( int cntr = 0; cntr < size; cntr++ ) {
            for ( int loop = 0; loop < size; loop++ ) {
                ttm[cntr][ds] = tf[cntr];
                ttm[ds][cntr] = tt[cntr];
            }
        }
        return ttm;
    }

    /**
     * The <code>setCircularTTM()</code> method builds a transition time matrix
     * that represents a finite state machine arranged in a ring; each state can transition
     * to one other state, wrapping around. For example, a finite state machine of consisting
     * of states S1, S2, and S3 could have a cycle S1 -> S2 -> S3 -> S1.
     * @param ttm the original transition time matrix
     * @param perm an array of integers representing the order of the state transitions in
     * the ring
     * @param tt the transition time between the corresponding states in the ring
     * @return a square
     */
    public static int[][] setCircularTTM(int[][] ttm, int[] perm, int[] tt) {
        int size = ttm.length;

        for ( int cntr = 0; cntr < size-1; cntr++ ) {
            ttm[perm[cntr]][perm[cntr+1]] = tt[perm[cntr+1]];
        }
        ttm[perm[size-1]][perm[0]] = tt[perm[0]];
        return ttm;
    }

    /**
     * The <code>newTTM()</code> method is a utility function for building a new
     * transition time matrix. It will create a new transition time matrix (TTM) where
     * each entry is <code>-1</code>, indicating that there are no legal state transitions.
     * @param size the size of matrix, i.e. the number of rows, which is equal to the
     * number of columns
     * @return a square matrix of the given size where each entry is set to -1
     */
    public static int[][] newTTM(int size) {
        int[][] ttm = new int[size][size];

        for ( int cntr = 0; cntr < size; cntr++ ) {
            for ( int loop = 0; loop < size; loop++ ) {
                ttm[cntr][loop] = -1;
            }
        }
        return ttm;
    }

    /**
     * The <code>setDiagonal()</code> method sets the diagonal of the given transition
     * time matrix to the specified value. This is useful for finite state machines where
     * transitions from one state to the same state is either impossible or a no-op.
     * @param ttm the original transition time matrix
     * @param diag the value to set the diagonal entries to
     * @return the original transition time matrix with the diagonal entries appropriately
     * set
     */
    public static int[][] setDiagonal(int[][] ttm, int diag) {
        for ( int cntr = 0; cntr < ttm.length; cntr++ )
            ttm[cntr][cntr] = diag;
        return ttm;
    }
}
