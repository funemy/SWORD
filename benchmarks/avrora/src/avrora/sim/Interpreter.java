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
 * Creation date: Nov 21, 2005
 */

package avrora.sim;

import avrora.sim.clock.MainClock;

/**
 * The <code>Interpreter</code> class represents an interpreter that is capable of executing
 * instructions for a particular architecture. It contains method to start, stop, and single-step
 * the execution of the program, as well as methods to access the state of the simulation and
 * add instrumentation to the running program.
 *
 * @author Ben L. Titzer
 */
public abstract class Interpreter {

    /**
     * The <code>innerLoop</code> field is a boolean that is used internally in the implementation of the
     * interpreter. When something in the simulation changes (e.g. an interrupt is posted), this field is set
     * to false, and the execution loop (e.g. an interpretation or sleep loop) is broken out of. THIS FIELD
     * IS NOT MEANT FOR PUBLIC USE.
     */
    protected boolean innerLoop;

    /**
     * The <code>simulator</code> field stores a reference to the simulator that this interpreter instance
     * corresponds to. There should be a one-to-one mapping between instances of the <code>Simulator</code>
     * class and instances of the <code>BaseInterpreter</code> class.
     */
    protected final Simulator simulator;

    /**
     * The <code>clock</code> field stores a reference to the main clock of the simulator. This is
     * the same instance shared between the <code>Simulator</code>, <code>Microcontroller</code>, and
     * any devices that are attached directly to the main clock or to a derived clock.
     */
    protected final MainClock clock;

    /**
     * The <code>interrupts</code> field stores a reference to the interrupt table that contains
     * information about the interrupt vectors of the microcontroller; which are enabled, disabled,
     * posted, etc. The interrupt table also allows instrumenting interrupts, allowing probes to be
     * inserted on individual interrupts that are notified when the status changes, such as posting,
     * unposting, enabling, or invoking.
     */
    protected InterruptTable interrupts;

    /**
     * The <code>start()</code> method starts execution of the interpreter. The interpreter will begin
     * passing simulation time in the current mode (e.g. executing instructions or sleeping). This
     * method will not return until the program terminates with a BREAK (or equivalent) instruction or
     * the <code>stop()</code> method is called by this thread or another thread.
     */
    public abstract void start();

    /**
     * The <code>step()</code> method steps this node forward one instruction or one clock cycle. The node may
     * execute an instruction, execute events, wake from sleep, take an interrupt, etc. In the case of multi-cycle
     * instructions, the node will execute until the end of the instruction. The number of cycles consumed is
     * returned by this method.
     * @return the number of cycles consumed in executing one instruction or waking from an interrupt, sleeping,
     * delay, etc
     */
    public abstract int step();

    /**
     * The <code>stop()</code> method terminates the execution of the simulation. This method is asynchronous;
     * it runs immediately, and the simulator is not guaranteed to be stopped. This call will cause the
     * call to start() to eventually return in the thread that started the simulator, but there is not guarantee
     * as to how much simulation time will pass before the method returns.
     */
    public abstract void stop();

    /**
     * The <code>getSimulator()</code> method gets a reference to the simulator which encapsulates this
     * interpreter.
     * @return a reference to the simulator containing to this interpreter
     */
    public Simulator getSimulator() {
        return simulator;
    }

    /**
     * The <code>getMainClock()</code> method returns a reference to the main clock for this interpreter.
     * The main clock keeps track of time for this microcontroller and contains an event queue that allows
     * events to be inserted to be executed in the future.
     * @return a reference to the main clock for this interpreter.
     */
    public MainClock getMainClock() {
        return clock;
    }

    /**
     * The <code>getState()</code> method returns a reference to a <code>State</code> implementation
     * that represents the state of the simulation, including register files, memories, program counter,
     * stack pointer, etc.
     * @return a reference to an implementation of the <code>State</code> interface appropriate for
     * this interpreter
     */
    public abstract State getState();

    protected Interpreter(Simulator sim) {
        // set up the reference to the simulator
        this.simulator = sim;
        this.clock = sim.clock;
    }

    /**
     * The <code>getInterruptTable()</code> method returns a reference to the interrupt table for this
     * interpreter. The interrupt table contains the status information about what interrupts are posted,
     * enabled, disabled, etc.
     * @return a reference to the interrupt table for this interpreter
     */
    public InterruptTable getInterruptTable() {
        return interrupts;
    }

    /**
     * The <code>insertProbe()</code> method is used internally to insert a probe on a particular instruction.
     * @param p the probe to insert on an instruction
     * @param addr the address of the instruction on which to insert the probe
     */
    protected abstract void insertProbe(Simulator.Probe p, int addr);

    /**
     * The <code>insertExceptionWatch()</code> method registers an </code>ExceptionWatch</code> to listen for
     * exceptional conditions in the machine.
     *
     * @param watch The <code>ExceptionWatch</code> instance to add.
     */
    protected abstract void insertErrorWatch(Simulator.Watch watch);

    /**
     * The <code>insertProbe()</code> method allows a probe to be inserted that is executed before and after
     * every instruction that is executed by the simulator
     *
     * @param p the probe to insert
     */
    protected abstract void insertProbe(Simulator.Probe p);

    /**
     * The <code>removeProbe()</code> method is used internally to remove a probe from a particular instruction.
     * @param p the probe to remove from an instruction
     * @param addr the address of the instruction from which to remove the probe
     */
    protected abstract void removeProbe(Simulator.Probe p, int addr);

    /**
     * The <code>removeProbe()</code> method removes a probe from the global probe table (the probes executed
     * before and after every instruction). The comparison used is reference equality, not
     * <code>.equals()</code>.
     *
     * @param b the probe to remove
     */
    public abstract void removeProbe(Simulator.Probe b);

    /**
     * The <code>insertWatch()</code> method is used internally to insert a watch on a particular memory location.
     * @param p the watch to insert on a memory location
     * @param data_addr the address of the memory location on which to insert the watch
     */
    protected abstract void insertWatch(Simulator.Watch p, int data_addr);

    /**
     * The <code>removeWatch()</code> method is used internally to remove a watch from a particular memory location.
     * @param p the watch to remove from the memory location
     * @param data_addr the address of the memory location from which to remove the watch
     */
    protected abstract void removeWatch(Simulator.Watch p, int data_addr);

    /**
     * The <code>delay()</code> method is used to add some delay cycles before the next instruction is executed.
     * This is necessary because some devices such as the EEPROM actually delay execution of instructions while
     * they are working
     * @param cycles the number of cycles to delay the execution
     */
    protected abstract void delay(long cycles);
}
