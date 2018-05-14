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

package avrora.test.sim;

import avrora.core.Program;
import avrora.sim.Simulator;
import cck.util.Arithmetic;
import cck.util.Util;
import java.util.*;

/**
 * The <code>StateAccessor</code> class separates the simulation test engine from the details of
 * the state implementations for each architecture. The basic model is that this class provides
 * methods that allow getting and setting single variables, which may correspond to registers,
 * the program counter, stack pointer, etc, as well as getting and setting elements of arrays
 * indexed by an integer, such as memory and flash.
 *
 * Each architecture that would like to have automated tests needs to provide an implementation
 * of this class that maps variables and arrays to the internal state of the simulation.
 *
 * @author Ben L. Titzer
 */
public abstract class StateAccessor {

    protected final Program program;
    protected final Simulator simulator;
    protected final HashMap accessors;

    /**
     * The <code>Accessor</code> class is exposed for subclasses of the <code>StateAccess</code> class
     * in order to provide the access for a single variable or array. This allows instances of this class
     * to be added to an internal hashmap that is used by the default implementation of the <code>StateAccessor</code>
     * <code>.get()</code>, <code>.set()</code>, etc. methods.
     */
    protected abstract class Accessor {
        protected abstract int get();
        protected abstract void set(int val);
        protected int getIndex(int ind) {
            return Arithmetic.getBit(get(), ind) ? 1 : 0;
        }
        protected void setIndex(int ind, int val) {
            int value = Arithmetic.setBit(get(), ind, (val & 1) != 0);
            set(value);
        }
    }

    /**
     * The default constructor of the <code>StateAccessor</code> class stores a reference to the
     * program instance and the simulator. The simulator, in turn, allows access to the <code>State</code>
     * object, interpreter, event queue, etc.
     * @param p the program
     * @param sim the simulator
     */
    protected StateAccessor(Program p, Simulator sim) {
        program = p;
        simulator = sim;
        accessors = new HashMap();
    }

    /**
     * The <code>get()</code> method provides access to single variables that are distinguished
     * by their names as a string. The string need not have any particular format--it need only
     * be unique. For example, the string may include special characters such as '$', '.', etc.
     * @param name the name of the variable represented as a string
     * @return the value of the variable represented as an integer
     */
    public int get(String name) {
        Accessor a = (Accessor)accessors.get(name);
        if ( a == null ) throw Util.failure("unknown variable "+name);
        return a.get();
    }

    /**
     * The <code>getIndex()</code> method provides access to arrays that are distinguished by
     * their name and indexed by an integer. Arrays might represent the memory, IO register space,
     * flash memory, EEPROM, etc.
     * @param name the name of the array
     * @param ind the index into the array which to read
     * @return the value of the array at the specified position represented as an integer
     */
    public int getIndex(String name, int ind) {
        Accessor a = (Accessor)accessors.get(name);
        if ( a == null ) throw Util.failure("unknown variable "+name);
        return a.getIndex(ind);
    }

    /**
     * The <code>set()</code> method sets the value of the specified variable. This method is provided
     * so that the simulation framework can parse the values of an initial state from the test file
     * and use the accessor to initialize the state accordingly.
     * @param name the name of the variable to set
     * @param val the value to assign to the variable
     */
    public void set(String name, int val) {
        Accessor a = (Accessor)accessors.get(name);
        if ( a == null ) throw Util.failure("unknown variable "+name);
        a.set(val);
    }

    /**
     * The <code>setIndex()</code> methods sets the value of the specified array at the specified index.
     * This method is provided so that the simulation framework can parse the values of the initial state
     * from the test file and use the accessor to initialize the state accordingly.
     * @param name the name of the array as a string
     * @param ind the index into the array where to assign
     * @param val the value to assign to the element of the array
     */
    public void setIndex(String name, int ind, int val) {
        Accessor a = (Accessor)accessors.get(name);
        if ( a == null ) throw Util.failure("unknown variable "+name);
        a.setIndex(ind, val);
    }

    public Program getProgram() {
        return program;
    }

    public Simulator getSimulator() {
        return simulator;
    }

    public void init(List inits) {
        Iterator i = inits.iterator();
        while ( i.hasNext() ) {
            Predicate p = (Predicate)i.next();
            p.init(this);
        }
    }
}
