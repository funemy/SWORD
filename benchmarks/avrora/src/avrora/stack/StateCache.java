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

import avrora.core.Program;
import cck.text.StringUtil;
import cck.util.Util;
import java.util.*;

/**
 * The <code>StateSpace</code> class represents the reachable state space as it is explored by the
 * <code>Analyzer</code> class. It stores reachable states and the outgoing edges that connect them.
 *
 * @author Ben L. Titzer
 */
public class StateCache {

    private long uidCount;

    /**
     * The <code>LegacyState</code> class represents an immutable state within the state space of the program. Such
     * a state is cached and cannot be modified. It contains a unique identifier, a mark for graph traversals,
     * and a list of outgoing edges.
     */
    public class State extends AbstractState implements IORegisterConstants {

        private final int hashCode;
        private int type;
        public final long UID;

        boolean isExplored;
        boolean onFrontier;

        State(MutableState s) {
            pc = s.pc;
            av_SREG = s.av_SREG;
            av_EIMSK = s.av_EIMSK;
            av_TIMSK = s.av_TIMSK;
            av_REGISTERS = new char[NUM_REGS];
            System.arraycopy(s.av_REGISTERS, 0, av_REGISTERS, 0, NUM_REGS);
            hashCode = computeHashCode();
            UID = uidCount++;
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof State)) return false;
            return deepCompare((State)o);
        }

        /**
         * The <code>mark</code> field is used by graph traversal algorithms to detect cycles and terminate
         * traversals. Concurrent traversal is not supported.
         */
        public Object mark;

        public StateTransitionGraph.StateInfo info;

        /**
         * The <code>getUniqueName()</code> gets a string that uniquely identifies this state. For immutable
         * states, this is simply the UID. For special states, this is the name of the special state.
         *
         * @return a unique identifying string for this state
         */
        public String getUniqueName() {
            return StringUtil.toHex(UID, 10);
        }

        public void setType(int t) {
            type = t;
        }

        public int getType() {
            return type;
        }

    }

    public class Set {

        private State oneState;
        private HashSet delegate;

        private boolean delegating = false;
        private boolean empty = true;

        private class OptionalIterator implements Iterator {
            boolean done;

            public boolean hasNext() {
                return !done && oneState != null;
            }

            public Object next() {
                done = true;
                return oneState;
            }

            public void remove() {
                throw Util.unimplemented();
            }

        }

        public int size() {
            if (delegating)
                return delegate.size();
            else {
                return oneState == null ? 0 : 1;
            }
        }

        public boolean isEmpty() {
            if (delegating) {
                return false;
            } else {
                return (oneState != null);
            }
        }

        public boolean contains(Object o) {
            if (delegating) {
                return delegate.contains(o);
            } else {
                return oneState == o;
            }
        }

        public Iterator iterator() {
            if (delegating)
                return delegate.iterator();
            else
                return new OptionalIterator();
        }

        public boolean add(StateCache.State ns) {
            empty = false;

            if (delegating)
                return delegate.add(ns);
            else {
                if (oneState == null) {
                    oneState = ns;
                    return false;
                }
                if (ns == oneState) return true;
                beginDelegation();
                return delegate.add(ns);
            }
        }

        private void beginDelegation() {
            delegate = new HashSet();
            if (oneState != null)
                delegate.add(oneState);
            else
                oneState = null;
            delegating = true;
        }

        public boolean containsAll(Set oset) {
            if (oset.empty) return true;
            if (empty) return false;

            if (delegating) {
                if (oset.delegating)
                    return delegate.containsAll(oset.delegate);
                else
                    return delegate.contains(oset.oneState);
            } else {
                if (oset.delegating)
                    return false;
                else {
                    return oneState == oset.oneState || oset.oneState == null;
                }
            }
        }

        public boolean addAll(Set oset) {
            if (oset.empty) return true;
            empty = false;

            if (delegating) {
                if (oset.delegating)
                    return delegate.addAll(oset.delegate);
                else
                    return delegate.add(oset.oneState);
            } else {
                beginDelegation();
                if (oset.delegating)
                    return delegate.addAll(oset.delegate);
                else
                    return delegate.add(oset.oneState);
            }
        }

    }


    private HashMap stateMap;
    private final State edenState;
    private final Program program;
    private long totalStateCount;

    /**
     * The constructor for the <code>StateSpace</code> accepts a program as a parameter. This is currently
     * unused, but is reserved for use later.
     *
     * @param p the program to create the state space for
     */
    public StateCache(Program p) {
        stateMap = new HashMap(p.program_end * 5);
        edenState = getStateFor(new MutableState());
        program = p;
    }

    /**
     * The <code>getEdenState()</code> method gets the starting state of the abstract interpretation.
     *
     * @return the initial state to begin abstract interpretation
     */
    public State getEdenState() {
        return edenState;
    }

    /**
     * The <code>getCachedState()</code> method searches the state cache for an immutable state that
     * corresponds to the given mutable state. If no immutable state exists in the cache, one will be created
     * and inserted.
     *
     * @param s the state to search for
     * @return an instance of the <code>StateSpace.LegacyState</code> immutable state that corresponds to the given
     *         mutable state
     */
    public State getStateFor(MutableState s) {
        State is = new State(s);
        State cs = (State)stateMap.get(is);

        if (cs != null) {
            // if the state is already in the state map, return original
            return cs;
        } else {
            totalStateCount++;
            // the state is new, put it in the map and return it.
            stateMap.put(is, is);
            return is;
        }
    }

    /**
     * The <code>getTotalStateCount()</code> method returns the internally recorded number of states created
     * in this state space. This is mainly used for reporting purposes.
     *
     * @return the total number of states in the state cache
     */
    public long getTotalStateCount() {
        return totalStateCount;
    }

    public Iterator getStateIterator() {
        return stateMap.values().iterator();
    }

    public Set newSet() {
        return new Set();
    }
}
