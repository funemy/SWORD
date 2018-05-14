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

import avrora.Defaults;
import avrora.core.LoadableProgram;
import avrora.core.Program;
import avrora.monitors.MonitorFactory;
import avrora.sim.clock.Synchronizer;
import avrora.sim.mcu.*;
import avrora.sim.platform.*;
import avrora.sim.util.ClockCycleTimeout;
import avrora.sim.util.InterruptScheduler;
import avrora.sim.output.SimPrinter;
import avrora.sim.energy.EnergyControl;
import cck.help.HelpCategory;
import cck.util.*;
import cck.text.Verbose;

import java.util.*;
import java.io.*;

/**
 * The <code>Simulation</code> class represents a complete simulation, including
 * the nodes, the programs, the radio model (if any), the environment model, for
 * simulations of one or many nodes. This is meant as a major extension point in
 * Avrora for adding new types of simulations; for example a sensor network simulation,
 * a robotics simulation, etc.
 *
 * @author Ben L. Titzer
 */
public abstract class Simulation extends HelpCategory {

    public final Option.Str PLATFORM = newOption("platform", "",
            "This option selects the platform on which the microcontroller is built, " +
            "including the external devices such as LEDs and radio. If the platform " +
            "option is not set, the default platform is the microcontroller specified " +
            "in the \"mcu\" option, with no external devices.");
    public final Option.Long CLOCKSPEED = newOption("clockspeed", 8000000,
            "This option specifies the clockspeed of the microcontroller when the platform " +
            "is not specified. The speed is given in cycles per second, i.e. hertz.");
    public final Option.Long EXTCLOCKSPEED = newOption("external-clockspeed", 0,
            "This option specifies the clockspeed of the external clock supplied to the " +
            "microcontroller when the platform is not specified. The speed is given in cycles " +
            "per second, i.e. hertz. When this option is set to zero, the external clock is the " +
            "same speed as the main clock.");
    public final Option.Str MCU = newOption("mcu", "atmega128",
            "This option selects the microcontroller from a library of supported " +
            "microcontroller models.");
    public final Option.Long RANDOMSEED = newOption("random-seed", 0,
            "This option is used to seed a pseudo-random number generator used in the " +
            "simulation. If this option is set to non-zero, then its value is used as " +
            "the seed for reproducible simulation results. If this option is not set, " +
            "those parts of simulation that rely on random numbers will have seeds " +
            "chosen based on system parameters that vary from run to run.");
    public final Option.Double SECONDS = newOption("seconds", 0.0,
            "This option is used to terminate the " +
            "simulation after the specified number of simulated seconds have passed.");
    public final Option.List MONITORS = newOptionList("monitors", "",
            "This option specifies a list of monitors to be attached to the program. " +
            "Monitors collect information about the execution of the program while it " +
            "is running such as profiling data or timing information.");
    public final Option.Str SCHEDULE = newOption("interrupt-schedule", "",
            "This option, when specified, contains the name of a file that contains an interrupt " +
            "schedule that describes when to post interrupts (especially external interrupts) to the " +
            "program. This is useful for testing programs under different interrupt loads. For " +
            "multi-node simulations, the interrupt schedule is only applied to node 0.");
    public final Option.Str EELOADIMAGE = newOption("eeprom-load-image", "",
            "This option specifies a (binary) image file to load into EEPROM before starting " +
            "the simulation.");

    /**
     * The <code>Monitor</code> interface represents a monitor for a simulation. A monitor
     * can be attached to one or more nodes in the simulation. When the nodes are instantiated,
     * the monitor will be called to add instrumentation to the nodes in whatever way is necessary.
     * When the node finishes execution, the monitor will be called to tear down any data structures
     * and make any reports necessary to the user.
     */
    public interface Monitor {
        public void attach(Simulation sim, List nodes);
        public void construct(Simulation sim, Node n, Simulator s);
        public void destruct(Simulation sim, Node n, Simulator s);
        public void remove(Simulation sim, List nodes);
    }

    /**
     * The <code>Node</code> class represents a node in a simulation, which has an ID and a program
     * to be loaded onto it. It also has a <code>PlatformFactory</code> instance that is used to create
     * the actual <code>Simulator</code> object when the simulation is begun.
     */
    public class Node {
        public final int id;
        protected final LoadableProgram path;

        protected final PlatformFactory platformFactory;
        protected final LinkedList monitors;

        protected Platform platform;
        protected Simulator simulator;
        protected SimulatorThread thread;

        /**
         * The constructor for the <code>Node</code> class creates a representation of a new node that
         * includes its id, a factory capable of creating a platform instance (i.e. microcontroller with
         * attached devices) and the program to be loaded onto the node. The constructor does not yet
         * instantiate the node, but leaves the node in an unconstructed state (i.e. the simulator and
         * microcontroller and external devices for the node have not been allocated and initialized yet).
         * @param id the unique id of the node
         * @param pf the platform factory that will be used to instantiate the node
         * @param p the program to load onto the microcontroller
         */
        protected Node(int id, PlatformFactory pf, LoadableProgram p) {
            this.id = id;
            this.platformFactory = pf;
            this.path = p;
            this.monitors = new LinkedList();
        }

        /**
         * The <code>instantiate()</code> method is called when the simulation begins. When the node is
         * created (i.e the constructor is called), it is left in an unconstructed state. The role of this
         * method is to create the simulator, the microcontroller, and external devices for the node that will
         * be actually executing in the simulation.
         */
        protected void instantiate() {
            // create the simulator object
            platform = platformFactory.newPlatform(id, Simulation.this, path.getProgram());
            simulator = platform.getMicrocontroller().getSimulator();
            processTimeout();
            processInterruptSched();
            processEepromLoad();
            synchronizer.addNode(this);
        }

        protected void addMonitors() {
            // OLD MONITOR API SUPPORT:
            // for each of the monitors in the factory list, create a new monitor
            Iterator i = monitorFactoryList.iterator();
            while ( i.hasNext() ) {
                MonitorFactory f = (MonitorFactory)i.next();
                avrora.monitors.Monitor m = f.newMonitor(simulator);
                if ( m != null ) monitors.add(m);
            }
            // NEW MONITOR API SUPPORT:
            // for each monitor attached to this node, allow them to construct data structures
            Iterator mi = monitors.iterator();
            while ( mi.hasNext() ) {
                Object o = mi.next();
                if ( o instanceof Monitor) {
                    Monitor mon = (Monitor)o;
                    mon.construct(Simulation.this, this, simulator);
                }
            }
        }

        private void processTimeout() {
            double secs = SECONDS.get();
            if ( secs > 0 ) {
                long cycles = (long)(secs * simulator.getClock().getHZ());
                simulator.insertEvent(new ClockCycleTimeout(simulator, cycles), cycles);
            }
        }

        private void processInterruptSched() {
            if ( id != 0 ) return;
            if ( !SCHEDULE.isBlank() ) {
                InterruptScheduler s = new InterruptScheduler(SCHEDULE.get(), simulator);
            }
        }

        private void processEepromLoad() {
            if ( !EELOADIMAGE.isBlank() ) {
                FileInputStream f;
                // FIXME: break of abstraction (getDevice is specific to
                // AtmelMicrocontroller)
                AtmelMicrocontroller mcu = (AtmelMicrocontroller) platform.getMicrocontroller();
                EEPROM eeprom = (EEPROM) mcu.getDevice("eeprom");
                byte[] image;

                try {
                    f = new FileInputStream(EELOADIMAGE.get());

                    if (f.available() > eeprom.getSize()) {
                        f.close();
                        Util.userError("EEPROM image too large", EELOADIMAGE.get());
                    }

                    image = new byte[f.available()];
                    int i = 0;
                    while (i < image.length) {
                        i += f.read(image, i, image.length - i);
                    }
                    f.close();

                } catch (IOException e) {
                    throw Util.unexpected(e);
                }

                eeprom.setContent(image);
            }
        }

        /**
         * The <code>getSimulator()</code> method returns the simulator instance for this node while it is
         * executing. When the node is not currently executing (i.e. the simulation has not started yet),
         * this method will return null.
         * @return the simulator instance for this node if the simulation has started; null otherwise
         */
        public Simulator getSimulator() {
            return simulator;
        }

        /**
         * The <code>getSimulation()</code> method returns a reference to the simulation instance which this
         * node is a part of.
         * @return a reference to the simulation instance that this node is a part of
         */
        public Simulation getSimulation() {
            return Simulation.this;
        }

        /**
         * The <code>addMonitor()</code> method is called by a Monitor when it attaches itself to this node. More
         * specifically, the model is that a Monitor is attached to a list of nodes; the monitor itself decides
         * which of those nodes to attach to and, for each one, calls this method.
         * @param f the monitor being added to this node
         */
        public void addMonitor(Monitor f) {
            monitors.add(f);
        }

        /**
         * The <code>removeMonitor()</code> method is called by a Monitor when it removes itself from this node.
         * @param f the monitor being removed from this node
         */
        public void removeMonitor(Monitor f) {
            monitors.remove(f);
        }

        /**
         * The <code>getMonitors()</code> method gets a list of monitors that are attached to this node.
         * @return a list of monitors currently attached to this node
         */
        public List getMonitors() {
            return monitors;
        }

        /**
         * The <code>getProgram()</code> method return a reference to the loadable program for this node.
         * @return a reference to the loadable program for this node
         */
        public LoadableProgram getProgram() {
            return path;
        }

        /**
         * The <code>remove()<code> method is called on a node when it is being removed from the simulation.
         */
        protected void remove() {
            Iterator i = monitors.iterator();
            while ( i.hasNext() ) {
                Monitor f = (Monitor)i.next();
                f.destruct(Simulation.this, this, simulator);
            }
        }

        public SimulatorThread getThread() {
            return thread;
        }
    }

    protected int num_nodes;
    protected Node[] nodes;

    protected boolean running;
    protected boolean paused;
    protected Random random;
    protected LinkedList monitorFactoryList;
    protected EnergyControl energyControl;

    protected Synchronizer synchronizer;

    /**
     * The construcotr for the <code>Simulation</code> class creates a new simulation. This is intended to be
     * called only by subclasses of Simulation, and expects a short string representing the name of the simulation
     * type, a String representing the help item, as well as a node factory (if null, the
     * <code>StandardNodeFactory</code> will be used), and a <code>Synchronizer</code> instance used to synchronize
     * the starting and stopping of multiple nodes.
     * @param str the name of the simulation as a short string
     * @param h the help item for this simulation
     * @param s the synchronizer instance used to start and stop all of the nodes
     */
    protected Simulation(String str, String h, Synchronizer s) {
        super(str, h);
        energyControl = new EnergyControl();
        nodes = new Node[16];
        synchronizer = s;
        monitorFactoryList = new LinkedList();
    }

    /**
     * The <code>process()</code> method is called when the simulation is created from the command line.
     * This gives the simulation instance a chance to read in options from the command line, instantiate nodes,
     * and configure the simulation from the user's input.
     * @param o the options processed so far from the command line
     * @param args the command line arguments from the user
     * @throws Exception if any type of exception occurs during this processing (e.g. FileNotFound)
     */
    public abstract void process(Options o, String[] args) throws Exception;

    public Simulator createSimulator(int id, InterpreterFactory f, Microcontroller mcu, Program p) {
        return new Simulator(id, this, f, mcu, p);
    }

    public SimPrinter getPrinter(Simulator s, String category) {
        if (Verbose.isVerbose(category)) {
            return new SimPrinter(s, category);
        }
        return null;
    }

    public SimPrinter getPrinter(Simulator s) {
        return new SimPrinter(s, "");
    }

    public EnergyControl getEnergyControl() {
        return energyControl;
    }

    /**
     * The <code>createNode()</code> method creates a new node in the simulation with the specified
     * platform, with the specified program loaded onto it.
     * @param pf the platform factory used to create the platform for the node
     * @param pp the program for the node
     * @return a new instance of the <code>Node</code> class representing the node
     */
    public synchronized Node createNode(PlatformFactory pf, LoadableProgram pp) {
        if ( running ) return null;
        int id = num_nodes++;
        Node n = newNode(id, pf, pp);
        if ( id >= nodes.length ) grow();
        nodes[id] = n;
        return n;
    }

    /**
     * The <code>newNode()</code> method is intended to be overridden by subclasses of Simulation. Since
     * some simulations may have special types of nodes with more information attached (e.g. a sensor
     * simulation where radios are attached to nodes), overriding this method allows new types of nodes
     * to be instantiated.
     * @param id the id number of the new node
     * @param pf the platform factory for the new node
     * @param pp the the loadable program for the new node
     * @return a reference to a new node instance for this new node
     */
    protected Node newNode(int id, PlatformFactory pf, LoadableProgram pp) {
        return new Node(id, pf, pp);
    }

    /**
     * The <code>getNumberOfNodes()</code> method returns the number of nodes in this simulation.
     * @return the number of nodes in this simulation
     */
    public int getNumberOfNodes() {
        return num_nodes;
    }

    /**
     * The <code>getRandom()</code> method returns a reference to a random number generator that is used
     * in the simulation. The random number generator may be used to randomly perturb node start times,
     * drop packets, etc. This random number generator has a user-selectable random seed for reproducibility.
     * @return a reference to the random number generator for this simulation
     */
    public Random getRandom() {
        if ( random == null ) {
            long seed = RANDOMSEED.get();
            if ( seed != 0 ) random = new Random(seed);
            else random = new Random();
        }
        return random;
    }

    private void grow() {
        Node[] nnodes = new Node[nodes.length*2];
        System.arraycopy(nodes, 0, nnodes, 0, nodes.length);
        nodes = nnodes;
    }

    /**
     * The <code>getNode()</code> method gets a reference to the node with the specified id number.
     * @param node_id the node's id number as an integer
     * @return a reference to the node if it exists; null otherwise
     */
    public synchronized Node getNode(int node_id) {
        if ( node_id >= nodes.length ) return null;
        return nodes[node_id];
    }

    /**
     * The <code>removeNode()</code> method removes a node from this simulation. This operation can only
     * be performed when the simulation is not running.
     * @param node_id the node's id number specifying which node to remove
     */
    public synchronized void removeNode(int node_id) {
        if ( running ) return;
        if ( nodes[node_id] != null ) {
            Node node = nodes[node_id];
            nodes[node_id] = null;
            num_nodes--;
            synchronizer.removeNode(node);
            node.remove();
        }
    }

    /**
     * The <code>start()</code> method starts the simulation execution. This method will return immediately
     * and the simulation will continue to run in the background in one or more other threads. The simulation
     * can be paused and stopped with the corresponding methods.
     */
    public synchronized void start() {
        // if we are already running, do nothing
        if ( running ) return;

        instantiateNodes();
        synchronizer.start();
        running = true;
    }

    protected void instantiateNodes() {
        // instantiate all of the nodes (and create threads)
        for ( int cntr = 0; cntr < nodes.length; cntr++ ) {
            Node n = nodes[cntr];
            if ( n == null ) continue;

            n.instantiate(); // create the simulator and simulator thread
            n.addMonitors();
        }
    }

    /**
     * The <code>pause()</code> method pauses the simulation. This method is synchronous in the sense that it will
     * not return until all nodes in the simulation are guaranteed to be paused (i.e. no longer making progress).
     */
    public synchronized void pause() {
        if ( !running ) return;
        synchronizer.pause();
        paused = true;
    }

    /**
     * The <code>resume()</code> method resumes the simulation after it has been paused.
     */
    public synchronized void resume() {
        if ( !running ) return;
        throw Util.unimplemented();
    }

    /**
     * The <code>stop()</code> method stops the simulation. This method will terminate the simulation, causing
     * each node and its monitor's states to be deconstructed. After calling stop(), subsequent calls to
     * start() will result in the creation of a new simulation run.
     */
    public synchronized void stop() {
        if ( !running ) return;
        synchronizer.stop();
        paused = false;
        running = false;
    }

    /**
     * The <code>join()</code> method waits for the simulation to terminate before returning. After this method
     * returns, the nodes are all guaranteed to be have terminated.
     * @throws InterruptedException if the thread is interrupt
     */
    public synchronized void join() throws InterruptedException {
        synchronizer.join();
    }

    /**
     * The <code>stopNode()</code> method can be used to stop (and remove) a single node from the simulation
     * while the simulation is running. This may be useful for simulating node failure or reconfiguring the
     * simulation while it is running. This method has no effect when the simulation is not running.
     * @param id the node_id of the node to stop
     */
    public synchronized void stopNode(int id) {
        if ( !running ) return;
        throw Util.unimplemented();
    }

    /**
     * The <code>isPaused()</code> method checks whether the simulation is currently paused.
     * @return true if the simulation is currently paused; false otherwise
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * The <code>isRunning()</code> method checks whether the simulation is currently running.
     * @return true if the simulation is currently running; false otherwise.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * The <code>Iter</code> class implements java.util.Iterator for the node iterator.
     */
    class Iter implements Iterator {
        int cursor;

        Iter() {
            scan();
        }

        public boolean hasNext() {
            return cursor < nodes.length;
        }

        public Object next() {
            if ( cursor >= nodes.length ) throw new NoSuchElementException();
            Object o = nodes[cursor];
            cursor++;
            scan();
            return o;
        }

        private void scan() {
            while ( cursor < nodes.length ) {
                if ( nodes[cursor] != null ) return;
                cursor++;
            }
        }

        public void remove() {
            throw Util.unimplemented();
        }
    }

    /**
     * The <code>getNodeIterator()</code> method returns an iterator over all of the nodes of the simulation.
     * @return an iterator that can traverse all of the nodes of the simulation
     */
    public Iterator getNodeIterator() {
        return new Iter();
    }

    /**
     * The <code>getPlatform()</code> method is a helper method for extensions of the <code>Simulation</code>
     * class. This method will consult the value of the \"platform\" command line option and construct an
     * appropriate <code>PlatformFactory</code> instance that can be used for a node.
     * @return an instance of the <code>PlatformFactory</code> interface
     */
    protected PlatformFactory getPlatform() {
        if ( PLATFORM.isBlank() ) {
            long hz = CLOCKSPEED.get();
            long exthz = EXTCLOCKSPEED.get();
            if ( exthz == 0 ) exthz = hz;
            if ( exthz > hz )
                Util.userError("External clock is greater than main clock speed", exthz+"hz");
            MicrocontrollerFactory mcf = Defaults.getMicrocontroller(MCU.get());
            return new DefaultPlatform.Factory(hz, exthz, mcf);
        } else {
            String pfs = PLATFORM.get();
            return Defaults.getPlatform(pfs);
        }
    }

    /**
     * The <code>processMonitorList()</code> method builds a list of <code>MonitorFactory</code> instances
     * from the list of strings given as an option at the command line. The list of
     * <code>MonitorFactory</code> instances is used to create monitors for each simulator as it is created.
     */
    protected void processMonitorList() {
        List l = MONITORS.get();
        Iterator i = l.iterator();
        while (i.hasNext()) {
            String clname = (String)i.next();
            MonitorFactory mf = Defaults.getMonitor(clname);
            mf.processOptions(options);
            monitorFactoryList.addLast(mf);
        }
    }

}
