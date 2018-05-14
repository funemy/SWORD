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

import avrora.arch.legacy.LegacyInstr;
import avrora.core.Program;
import avrora.sim.clock.MainClock;
import avrora.sim.mcu.Microcontroller;
import avrora.sim.output.EventBuffer;
import avrora.sim.output.SimPrinter;

/**
 * The <code>Simulator</code> class implements a full processor simulator for the AVR instruction set. It is
 * the base class of specific implementations that implement processor-specific behavior.
 *
 * @author Ben L. Titzer
 * @see Program
 * @see LegacyInstr
 * @see AtmelInterpreter
 */
public class Simulator {

    /**
     * The <code>program</code> field allows descendants of the <code>Simulator</code> class to access the
     * program that is currently loaded in the simulator.
     */
    protected final Program program;

    /**
     * The <code>microcontroller</code> field stores a reference to the microcontroller being simulated.
     */
    protected final Microcontroller microcontroller;

    /**
     * The <code>interpreter</code> field stores a reference to the instruction set interpreter.
     */
    protected Interpreter interpreter;

    /**
     * The <code>clock</code> field stores a reference to the <code>MainClock</code> instance that tracks the
     * clock cycles that have passed for this simulator.
     */
    protected MainClock clock;

    /**
     * The <code>id</code> field stores a unique identifier used to differentiate this simulator
     * from others that might be running in the same simulation.
     */
    protected final int id;

    /**
     * The simulation that this node is a part of.
     */
    protected final Simulation simulation;

    /**
     * The <code>events</code> fields stores the event buffer for this simulator.
     */
    protected EventBuffer events;

    /**
     * The constructor creates the internal data structures and initial state of the processor. It constructs
     * an instance of the simulator that is ready to have devices attached, IO registers probed, and probes
     * and events inserted. Users should not create <code>Simulator</code> instances directly, but instead
     * should get an instance of the appropriate processor and load the program into it.
     *
     * @param i the id of the simulator
     * @param sim the simulation that this node is a part of
     * @param f the interpreter factory to create the interpreter
     * @param mcu the microcontroller unit
     * @param p the program to load into the simulator
     */
    Simulator(int i, Simulation sim, InterpreterFactory f, Microcontroller mcu, Program p) {
        id = i;
        microcontroller = mcu;
        program = p;
        simulation = sim;

        // reset the state of the simulation
        clock = mcu.getClockDomain().getMainClock();
        interpreter = f.newInterpreter(this, program, microcontroller.getProperties());
        events = new EventBuffer(this);
    }

    /**
     * The <code>Simulator.Probe</code> interface represents a programmer-defined probe that can be inserted
     * at a particular instruction in the program. or at every instruction. Probes can be usedfor profiling,
     * analysis, or program understanding. The <code>fireBefore()</code> and <code>fireAfter()</code> methods
     * are called before and after the target instruction executes in simulation. Probes can also be inserted
     * in the "main loop" of the interpreter, so that the probe fires before and after every instruction
     * executed.
     *
     * @author Ben L. Titzer
     */
    public interface Probe {

        /**
         * The <code>fireBefore()</code> method is called before the probed instruction executes.
         *
         * @param state   the state of the simulation
         * @param pc the address at which this instruction resides
         */
        public void fireBefore(State state, int pc);

        /**
         * The <code>fireAfter()</code> method is called after the probed instruction executes.
         *
         * @param state   the state of the simulation
         * @param pc the address at which this instruction resides
         */
        public void fireAfter(State state, int pc);

        /**
         * The <code>Simulator.Probe.Empty</code> class is a simple base class for probes that do
         * not implement one or more methods. Deriving from this class allows shorter probes to
         * be written.
         */
        public static class Empty implements Probe {
            /**
             * The <code>fireBefore()</code> method is called before the probed instruction executes.
             * In the implementation of the <code>Empty</code> probe, this method is empty.
             *
             * @param state   the state of the simulation
             * @param pc the address at which this instruction resides
             */
            public void fireBefore(State state, int pc) {
                // do nothing
            }

            /**
             * The <code>fireAfter()</code> method is called after the probed instruction executes.
             * In the implementation of the <code>Empty</code> probe, this method is empty.
             *
             * @param state   the state of the simulation
             * @param pc the address at which this instruction resides
             */
            public void fireAfter(State state, int pc) {
                // do nothing
            }
        }
    }

    /**
     * The <code>InterruptProbe</code> interface represents a programmer-defined probe that can
     * be inserted on an interrupt. During simulation, when the interrupt is masked, posted, or
     * executed, the probe will be notified.
     */
    public interface InterruptProbe {

        /**
         * The <code>fireBeforeInvoke()</code> method of an interrupt probe will be called by the
         * simulator before control is transferred to this interrupt, before the microcontroller
         * has been woken from its current sleep mode.
         * @param s the state of the simulator
         * @param inum the number of the interrupt being entered
         */
        public void fireBeforeInvoke(State s, int inum);

        /**
         * The <code>fireAfterInvoke()</code> method of an interrupt probe will be called by the
         * simulator after control is transferred to this interrupt handler, i.e. after the current
         * PC is pushed onto the stack, interrupts are disabled, and the current PC is set to
         * the start of the interrupt handler.
         * @param s the state of the simulator
         * @param inum the number of the interrupt being entered
         */
        public void fireAfterInvoke(State s, int inum);

        /**
         * The <code>fireWhenDisabled()</code> method of an interrupt probe will be called by the
         * simulator when the interrupt is masked out (disabled) by the program.
         * @param s the state of the simulator
         * @param inum the number of the interrupt being masked out
         */
        public void fireWhenDisabled(State s, int inum);

        /**
         * The <code>fireWhenEnabled()</code> method of an interrupt probe will be called by the
         * simulator when the interrupt is unmasked (enabled) by the program.
         * @param s the state of the simulator
         * @param inum the number of the interrupt being unmasked
         */
        public void fireWhenEnabled(State s, int inum);

        /**
         * The <code>fireWhenPosted()</code> method of an interrupt probe will be called by the
         * simulator when the interrupt is posted. When an interrupt is posted to the simulator,
         * it will be coming pending if it is enabled (unmasked) and eventually be handled.
         * @param s the state of the simulator
         * @param inum the number of the interrupt being posted
         */
        public void fireWhenPosted(State s, int inum);

        /**
         * The <code>fireWhenUnposted()</code> method of an interrupt probe will be called by the
         * simulator when the interrupt is unposted. This can happen if the software resets the
         * flag bit of the corresponding IO register or, for most interrupts, when the pending
         * interrupt is handled.
         * @param s the state of the simulator
         * @param inum the number of the interrupt being unposted
         */
        public void fireWhenUnposted(State s, int inum);

        /**
         * The <code>Empty</code> class represents a default implementation of the
         * <code>InterruptProbe</code> interface where each fireXXX() method does nothing.
         */
        public static class Empty implements InterruptProbe {
            /**
             * The <code>fireBeforeInvoke()</code> method of an interrupt probe will be called by the
             * simulator before control is transferred to this interrupt, before the microcontroller
             * has been woken from its current sleep mode. In this implementation, the method does nothing.
             * @param s the state of the simulator
             * @param inum the number of the interrupt being entered
             */
            public void fireBeforeInvoke(State s, int inum) {
                // do nothing.
            }

            /**
             * The <code>fireAfterInvoke()</code> method of an interrupt probe will be called by the
             * simulator after control is transferred to this interrupt handler, i.e. after the current
             * PC is pushed onto the stack, interrupts are disabled, and the current PC is set to
             * the start of the interrupt handler. In this implementation, the method does nothing.
             * @param s the state of the simulator
             * @param inum the number of the interrupt being entered
             */
            public void fireAfterInvoke(State s, int inum) {
                // do nothing.
            }

            /**
             * The <code>fireWhenDisabled()</code> method of an interrupt probe will be called by the
             * simulator when the interrupt is masked out (disabled) by the program.  In this implementation,
             * the method does nothing.
             * @param s the state of the simulator
             * @param inum the number of the interrupt being masked out
             */
            public void fireWhenDisabled(State s, int inum) {
                // do nothing.
            }

            /**
             * The <code>fireWhenEnabled()</code> method of an interrupt probe will be called by the
             * simulator when the interrupt is unmasked (enabled) by the program. In this implementation, the
             * method does nothing.
             * @param s the state of the simulator
             * @param inum the number of the interrupt being unmasked
             */
            public void fireWhenEnabled(State s, int inum) {
                // do nothing.
            }

            /**
             * The <code>fireWhenPosted()</code> method of an interrupt probe will be called by the
             * simulator when the interrupt is posted. When an interrupt is posted to the simulator,
             * it will be coming pending if it is enabled (unmasked) and eventually be handled. In this
             * implementation, the method does nothing.
             * @param s the state of the simulator
             * @param inum the number of the interrupt being posted
             */
            public void fireWhenPosted(State s, int inum) {
                // do nothing.
            }

            /**
             * The <code>fireWhenUnposted()</code> method of an interrupt probe will be called by the
             * simulator when the interrupt is unposted. This can happen if the software resets the
             * flag bit of the corresponding IO register or, for most interrupts, when the pending
             * interrupt is handled. In this implementation, the method does nothing.
             * @param s the state of the simulator
             * @param inum the number of the interrupt being unposted
             */
            public void fireWhenUnposted(State s, int inum) {
                // do nothing.
            }

        }
    }

    /**
     * The <code>Simulator.Event</code> interface represents an event that is fired when a timed event occurs
     * within the simulator. Users of the simulator can insert timed events that model environmental factors,
     * implement timeouts, timers, or any other type of functionality that is simulation-time dependent.
     */
    public interface Event {
        /**
         * The <code>fire()</code> method is called when the event to which it is tied happens with in the
         * simulator.
         */
        public void fire();
    }

    /**
     * The <code>Watch</code> interface represents a user watch that is fired when a watchpoint detects
     * an access to an address where this watch has been inserted.
     */
    public interface Watch {

        /**
         * The <code>fireBeforeRead()</code> method is called before the data address is read by the program.
         *
         * @param state     the state of the simulation
         * @param data_addr the address of the data being referenced
         */
        public void fireBeforeRead(State state, int data_addr);

        /**
         * The <code>fireBeforeWrite()</code> method is called before the data address is written by the
         * program.
         *
         * @param state     the state of the simulation
         * @param data_addr the address of the data being referenced
         * @param value     the value being written to the memory location
         */
        public void fireBeforeWrite(State state, int data_addr, byte value);

        /**
         * The <code>fireAfterRead()</code> method is called after the data address is read by the program.
         *
         * @param state     the state of the simulation
         * @param data_addr the address of the data being referenced
         * @param value     the value of the memory location being read
         */
        public void fireAfterRead(State state, int data_addr, byte value);

        /**
         * The <code>fireAfterWrite()</code> method is called after the data address is written by the
         * program.
         *
         * @param state     the state of the simulation
         * @param data_addr the address of the data being referenced
         * @param value     the value being written to the memory location
         */
        public void fireAfterWrite(State state, int data_addr, byte value);

        /**
         * The <code>Simulator.Watch.Empty</code> class acts as a base class with empty methods for
         * each fireXXX() method. This makes it easier to write much shorter simple watches because
         * empty methods are simply inherited.
         */
        public static class Empty implements Watch {

            /**
             * The <code>fireBeforeRead()</code> method is called before the data address is read by the program.
             * In the implementation of the Empty watch, this method does nothing.
             *
             * @param state     the state of the simulation
             * @param data_addr the address of the data being referenced
             */
            public void fireBeforeRead(State state, int data_addr) {
                // do nothing.
            }

            /**
             * The <code>fireBeforeWrite()</code> method is called before the data address is written by the
             * program.
             * In the implementation of the Empty watch, this method does nothing.
             *
             * @param state     the state of the simulation
             * @param data_addr the address of the data being referenced
             * @param value     the value being written to the memory location
             */
            public void fireBeforeWrite(State state, int data_addr, byte value) {
                // do nothing.
            }

            /**
             * The <code>fireAfterRead()</code> method is called after the data address is read by the program.
             * In the implementation of the Empty watch, this method does nothing.
             *
             * @param state     the state of the simulation
             * @param data_addr the address of the data being referenced
             * @param value     the value of the memory location being read
             */
            public void fireAfterRead(State state, int data_addr, byte value) {
                // do nothing.
            }

            /**
             * The <code>fireAfterWrite()</code> method is called after the data address is written by the
             * program.
             * In the implementation of the Empty watch, this method does nothing.
             *
             * @param state     the state of the simulation
             * @param data_addr the address of the data being referenced
             * @param value     the value being written to the memory location
             */
            public void fireAfterWrite(State state, int data_addr, byte value) {
                // do nothing.
            }
        }
    }

    /**
     * The <code>getMicrocontroller()</code> method gets a reference to the microcontroller being simulated.
     *
     * @return a reference to the microcontroller being simulated
     */
    public Microcontroller getMicrocontroller() {
        return microcontroller;
    }

    /**
     * The <code>getProgram()</code> method gets a reference to the program that has been loaded onto this
     * simulator.
     *
     * @return a reference to the <code>Program</code> instance representing the program loaded onto this
     *         <code>Simulator</code> object
     */
    public Program getProgram() {
        return program;
    }

    /**
     * The <code>getClock()</code> method gets a reference to the <code>Clock</code> that this
     * simulator is driving.
     * @return a reference to the clock for this simulator
     */
    public MainClock getClock() {
        return clock;
    }

    /**
     * The <code>getEventBuffer()</code> method retrieves the event buffer associated with
     * this simulator.
     * @return a reference to the event buffer
     */
    public EventBuffer getEventBuffer() {
        return events;
    }

    /**
     * The <code>getID()</code> method simply returns this node's unique ID.
     * @return the unique ID of this node
     */
    public int getID() {
        return id;
    }

    /**
     * The <code>getInterpreter()</code> method returns the interpreter that is currently attached
     * to this simulator.
     * @return the current interpreter
     */
    public Interpreter getInterpreter() {
        return interpreter;
    }

    /**
     * Gets the simulation that this node is a part of.
     * @return the simulation
     */
    public Simulation getSimulation() {
        return simulation;
    }

    /**
     * Gets a printer for this simulator, which automatically attaches
     * the time and node number to all output and provides globally ordered
     * output.
     * @param category the category for printing
     * @return a new printer for the category, if the category is enabled; <code>null</code>
     * if the category is not enabled and no output should be generated
     */
    public SimPrinter getPrinter(String category) {
        return simulation.getPrinter(this, category);
    }

    public SimPrinter getPrinter() {
        return simulation.getPrinter(this);
    }

    /**
     * The <code>getState()</code> retrieves a reference to the current state of the simulation, including the
     * values of all registers, the SRAM, the IO register, the program memory, program counter, etc. This
     * state is mutable.
     *
     * @return a reference to the current state of the simulation
     */
    public State getState() {
        return interpreter.getState();
    }

    /**
     * The <code>start()</code> method begins the simulation. It causes the simulator to invoke a runLoop that
     * executes instructions, firing probes and events as it executes. The <code>start()</code> method returns
     * normally when the </code>break</code> AVR instruction is executed, when a
     * <code>BreakPointException</code> is thrown, when a <code> TimeoutException</code> is thrown, or when
     * the <code>stop()</code> method on this simulator instance is called.
     */
    public void start() {
        interpreter.start();
    }

    /**
     * The <code>step()</code> method steps the simulation one instruction or cycle.
     * @return the number of cycles advanced; 1 in the case of sleeping, delaying,
     * 1 in the case of handling an interrupt, and for all other multi-cycle instructions, the
     * number of cycles consumed by executing the instruction
     */
    public int step() {
        return interpreter.step();
    }

    /**
     * The <code>stop()</code> method stops the simulation if it is running. This method can be called from
     * within a probe or event or from another thread.
     */
    public void stop() {
        interpreter.stop();
    }

    /**
     * The <code>insertProbe()</code> method allows a probe to be inserted that is executed before and after
     * every instruction that is executed by the simulator
     *
     * @param p the probe to insert
     */
    public void insertProbe(Probe p) {
        interpreter.insertProbe(p);
    }

    /**
     * The <code>insertProbe()</code> method allows a probe to be inserted at a particular address in the
     * program that corresponds to an instruction. The probe is then fired before and after that particular
     * instruction is executed.
     *
     * @param p    the probe to insert
     * @param addr the address at which to insert the probe
     */
    public void insertProbe(Probe p, int addr) {
        interpreter.insertProbe(p, addr);
    }

    /**
     * The <code>removeProbe()</code> method removes a probe from the global probe table (the probes executed
     * before and after every instruction). The comparison used is reference equality, not
     * <code>.equals()</code>.
     *
     * @param b the probe to remove
     */
    public void removeProbe(Probe b) {
        interpreter.removeProbe(b);
    }

    /**
     * The <code>removeProbe()</code> method removes a probe from the instruction at the specified the
     * address. The comparison used is reference equality, not <code>.equals()</code>.
     *
     * @param p    the probe to remove
     * @param addr the address from which to remove the probe
     */
    public void removeProbe(Probe p, int addr) {
        interpreter.removeProbe(p, addr);
    }

    /**
     * The <code>insertWatch()</code> method allows a watch to be inserted at a memory location. The probe
     * will be executed before every read or write to that memory location.
     *
     * @param p         the probe to insert
     * @param data_addr the data address at which to insert the probe
     */
    public void insertWatch(Watch p, int data_addr) {
        interpreter.insertWatch(p, data_addr);
    }


    /**
     * The <code>removeWatch()</code> method removes a given watch from the memory location. Reference
     * equality is used to check for equality when removing probes, not <code>.equals()</code>.
     *
     * @param p         the probe to remove
     * @param data_addr the data address from which to remove the probe
     */
    public void removeWatch(Watch p, int data_addr) {
        interpreter.removeWatch(p, data_addr);
    }

    /**
     * The <code>forceInterrupt()</code> method forces the simulator to post the specified interrupt
     * regardless of the normal source of the interrupt. If there is a flag register associated with the
     * specified interrupt, then the flag register's value will be set as if the original source of the
     * interrupt (e.g. a timer) had posted the interrupt. As with a normal post of the interrupt, if the
     * interrupt is masked out via a mask register or the master interrupt enable bit, the interrupt will not
     * be delivered. The main reason that this interface exists is for forcing programs to handle interrupts
     * and observe their behavior.
     *
     * @param num the interrupt number to force
     */
    public void forceInterrupt(int num) {
        interpreter.getInterruptTable().force(num);
    }

    /**
     * The <code>insertEvent()</code> method inserts an event into the event queue of the simulator with the
     * specified delay in clock cycles. The event will then be executed at the future time specified
     *
     * @param e      the event to be inserted
     * @param cycles the number of cycles in the future at which to fire
     */
    public void insertEvent(Event e, long cycles) {
        clock.insertEvent(e, cycles);
    }

    /**
     * The <code>removeEvent()</code> method removes an event from the event queue of the simulator. The
     * comparison used is reference equality, not <code>.equals()</code>.
     *
     * @param e the event to remove
     */
    public void removeEvent(Event e) {
        clock.removeEvent(e);
    }


    /**
     * The <code>insertExceptionWatch()</code> method registers an <code>ExceptionWatch</code> instance.
     *
     * @param watch The <code>ExceptionWatch</code> instance.
     */
    public void insertErrorWatch(Watch watch) {
        interpreter.insertErrorWatch(watch);
    }

    /**
     * The <code>delay()</code> method introduces a delay in the execution of the instructions of the program.
     * This is used by some devices for correct timing behavior. For example, the EEPROM, when written, causes
     * a small delay in which no instructions are executed.
     *
     * @param cycles the number of cycles to delay the simulation
     */
    public void delay(long cycles) {
        interpreter.delay(cycles);
    }

}
