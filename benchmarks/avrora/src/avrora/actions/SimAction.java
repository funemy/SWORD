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

package avrora.actions;

import avrora.core.*;
import avrora.sim.*;
import avrora.sim.util.SimUtil;
import avrora.Defaults;
import avrora.monitors.Monitor;
import cck.text.*;
import cck.util.*;

import java.util.*;

/**
 * The <code>SimAction</code> is an abstract class that collects many of the options common to single node and
 * multiple-node simulations into one place.
 *
 * @author Ben L. Titzer
 */
public class SimAction extends Action {

    public static final String HELP = "The \"simulate\" action creates a simulation with the specified program(s) " +
            "for the specified node(s). The simulation type might be as simple as a single node with a single " +
            "program, or a multiple-node sensor network simulation or robotics simulation.";

    public final Option.Bool REPORT_SECONDS = newOption("report-seconds", false,
            "This option causes all times printed out by the simulator to be reported " +
            "in seconds rather than clock cycles.");
    public final Option.Long SECONDS_PRECISION = newOption("seconds-precision", 6,
            "This option sets the precision (number of decimal places) reported for " +
            "event times in the simulation.");
    public final Option.Str SIMULATION = newOption("simulation", "single",
            "The \"simulation\" option selects from the available simulation types, including a single node " +
            "simulation, a sensor network simulation, or a robotics simulation.");
    public final Option.Bool THROUGHPUT = newOption("throughput", false,
            "This option enables reporting of simulator throughput (i.e. mhz).");

    protected Simulation simulation;
    protected long startms;
    protected boolean reported;

    public SimAction() {
        super(HELP);
    }

    /**
     * The <code>run()</code> method is called by the main class.
     *
     * @param args the command line arguments after the options have been stripped out
     * @throws Exception if there is a problem loading the program, or an exception occurs during
     *                             simulation
     */
    public void run(String[] args) throws Exception {
        SimUtil.REPORT_SECONDS = REPORT_SECONDS.get();
        SimUtil.SECONDS_PRECISION = (int)SECONDS_PRECISION.get();

        simulation = Defaults.getSimulation(SIMULATION.get());
        simulation.process(options, args);

        ShutdownThread shutdownThread = new ShutdownThread();
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        printSimHeader();
        try {
            startms = System.currentTimeMillis();
            simulation.start();
            simulation.join();
        } catch (Throwable t) {
            exitSimulation(t);
            Runtime.getRuntime().removeShutdownHook(shutdownThread);
        } finally {
            exitSimulation(null);
            Runtime.getRuntime().removeShutdownHook(shutdownThread);
        }
    }

    private void exitSimulation(Throwable thrown) {
        synchronized (this) {
            if (!reported) {
                reported = true;
                report(thrown);
            }
        }
    }

    private void report(Throwable thrown) {
        long delta = System.currentTimeMillis() - startms;
        try {
            if (thrown != null) throw thrown;
        } catch (BreakPointException e) {
            Terminal.printYellow("Simulation terminated");
            Terminal.println(": breakpoint at " + StringUtil.addrToString(e.address) + " reached.");
        } catch (TimeoutException e) {
            Terminal.printYellow("Simulation terminated");
            Terminal.println(": timeout reached at pc = " + StringUtil.addrToString(e.address) + ", time = " + e.state.getCycles());
        } catch (AsynchronousExit e) {
            Terminal.printYellow("Simulation terminated asynchronously");
            Terminal.nextln();
        } catch (Util.Error e) {
            Terminal.printRed("Simulation terminated");
            Terminal.print(": ");
            e.report();
        } catch (Throwable t) {
            Terminal.printRed("Simulation terminated with unexpected exception");
            Terminal.print(": ");
            t.printStackTrace();
        } finally {
            TermUtil.printSeparator();
            reportTime(simulation, delta, THROUGHPUT.get());
            reportMonitors(simulation);
        }
    }

    /**
     * The <code>getLocationList()</code> method is to used to parse a list of program locations and turn them
     * into a list of <code>Main.Location</code> instances.
     *
     * @param program the program to look up labels in
     * @param v       the list of strings that are program locations
     * @return a list of program locations
     */
    public static List getLocationList(Program program, List v) {
        HashSet locset = new HashSet(v.size()*2);

        SourceMapping lm = program.getSourceMapping();
        Iterator i = v.iterator();

        while (i.hasNext()) {
            String val = (String)i.next();

            SourceMapping.Location l = lm.getLocation(val);
            if ( l == null )
                Util.userError("Label unknown", val);
            locset.add(l);
        }

        List loclist = Collections.list(Collections.enumeration(locset));
        Collections.sort(loclist, SourceMapping.LOCATION_COMPARATOR);

        return loclist;
    }

    /**
     * The <code>printSimHeader()</code> method simply prints the first line of output that names
     * the columns for the events outputted by the rest of the simulation.
     */
    protected static void printSimHeader() {
        TermUtil.printSeparator(Terminal.MAXLINE, "Simulation events");
        Terminal.printGreen("Node          Time   Event");
        Terminal.nextln();
        TermUtil.printThinSeparator(Terminal.MAXLINE);
    }

    protected static void reportMonitors(Simulation sim) {
        Iterator i = sim.getNodeIterator();
        while (i.hasNext()) {
            Simulation.Node n = (Simulation.Node)i.next();
            Iterator im = n.getMonitors().iterator();
            while ( im.hasNext() ) {
                Monitor m = (Monitor)im.next();
                m.report();
            }
        }
    }

    protected static void reportTime(Simulation sim, long diff, boolean throughput) {
        // calculate total throughput over all threads
        Iterator i = sim.getNodeIterator();
        long aggCycles = 0;
        long maxCycles = 0;
        while ( i.hasNext() ) {
            Simulation.Node n = (Simulation.Node)i.next();
            Simulator simulator = n.getSimulator();
            if ( simulator == null ) continue;
            long count = simulator.getClock().getCount();
            aggCycles += count;
            if ( count > maxCycles ) maxCycles = count;
        }
        TermUtil.reportQuantity("Simulated time", maxCycles, "cycles");
        if (throughput) {
            TermUtil.reportQuantity("Time for simulation", TimeUtil.milliToSecs(diff), "seconds");
            int nn = sim.getNumberOfNodes();
            double thru = ((double)aggCycles) / (diff * 1000);
            TermUtil.reportQuantity("Total throughput", (float)thru, "mhz");
            if ( nn > 1 )
                TermUtil.reportQuantity("Throughput per node", (float)(thru / nn), "mhz");
        }
    }

    /**
     * The <code>BreakPointException</code> is an exception that is thrown by the simulator before it executes
     * an instruction which has a breakpoint. When this exception is thrown within the simulator, the
     * simulator is left in a state where it is ready to be resumed where it left off by the
     * <code>start()</code> method. When resuming, the breakpointed instruction will not cause a second
     * <code>BreakPointException</code> until the the instruction is executed a second time.
     *
     * @author Ben L. Titzer
     */
    public static class BreakPointException extends RuntimeException {
        /**
         * The <code>address</code> field stores the address of the instruction that caused the breakpoint.
         */
        public final int address;

        /**
         * The <code>state</code> field stores a reference to the state of the simulator when the breakpoint
         * occurred, before executing the instruction.
         */
        public final State state;

        public BreakPointException(int a, State s) {
            super("breakpoint @ " + StringUtil.addrToString(a) + " reached");
            address = a;
            state = s;
        }
    }

    /**
     * The <code>TimeoutException</code> is thrown by the simulator when a timeout reaches zero. Timeouts can
     * be used to ensure termination of the simulator during testing, and implementing timestepping in
     * surrounding tools such as interactive debuggers or visualizers.
     * <p/>
     * When the exception is thrown, the simulator is left in a state that is safe to be resumed by a
     * <code>start()</code> call.
     *
     * @author Ben L. Titzer
     */
    public static class TimeoutException extends RuntimeException {

        /**
         * The <code>address</code> field stores the address of the next instruction to be executed after the
         * timeout.
         */
        public final int address;

        /**
         * The <code>state</code> field stores the state of the simulation at the point at which the timeout
         * occurred.
         */
        public final State state;

        /**
         * The <code>timeout</code> field stores the value (in clock cycles) of the timeout that occurred.
         */
        public final long timeout;

        public TimeoutException(int a, State s, long t, String l) {
            super("timeout @ " + StringUtil.addrToString(a) + " reached after " + t + ' ' + l);
            address = a;
            state = s;
            timeout = t;
        }
    }

    public static class AsynchronousExit extends RuntimeException {
    }

    public class ShutdownThread extends Thread {
        public void run() {
            exitSimulation(new AsynchronousExit());
        }
    }
}
