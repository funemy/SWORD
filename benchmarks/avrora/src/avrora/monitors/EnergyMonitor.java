/**
 * Created on 18. September 2004, 22:02
 *
 * Copyright (c) 2004, Olaf Landsiedel, Protocol Engineering and
 * Distributed Systems, University of Tuebingen
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
 * Neither the name of the Protocol Engineering and Distributed Systems
 * Group, the name of the University of Tuebingen nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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


package avrora.monitors;

import avrora.sim.Simulator;
import avrora.sim.State;
import avrora.sim.output.SimPrinter;
import avrora.sim.clock.Clock;
import avrora.sim.energy.*;
import avrora.sim.platform.Platform;
import cck.text.Terminal;
import cck.text.TermUtil;
import cck.util.Option;
import cck.util.Util;
import java.io.*;
import java.util.Iterator;

/**
 * energy monitor implementation this class handles logging and
 * recording of power consumption.
 *
 * Furthermore the monitor shutsdown the node, when an energy limit is exceeded.
 *
 * @author Olaf Landsiedel
 */
public class EnergyMonitor extends MonitorFactory {

    protected final Option.Double BATTERY = newOption("battery", 0.0,
            "This option specifies the number of joules in each node's battery. During " +
            "simulation, the energy consumption of each node is tracked, and if the node " +
            "runs out of battery, it will be shut down and removed from the " +
            "simulation.");
    protected final Option.Str LOG = newOption("logfile", "",
            "This option specifies whether the energy monitor should log changes to each " +
            "node's energy state. If this option is specified, then each node's energy " +
            "state transitions will be written to <option>.#, where '#' represents the " +
            "node ID.");

    /**
     * @author Olaf Landsiedel
     *
     * The <code>EnergyMonitor</code> class implements an energy monitor that provides detailed
     * feedback of the power consumption of nodes as they execute. Furthermore, the monitor shuts down
     * the node when an energy limit is exceeded.
     *
     */
    public class Monitor implements avrora.monitors.Monitor {

        // the simulator
        protected Simulator simulator;
        protected Platform platform;
        protected EnergyControl energyControl;
        // energy a node is allowed to consume (in joules)
        private double energy;
        protected BatteryCheck batteryCheck;
        protected Logger logger;

        /**
         * Create a new energy monitor. Creates a file with logging information: temp.log that contains the
         * current draw of all devices, and the state changes can be loaded into Matlab, Gnuplot, Excel... for
         * further processing and visualization.
         *
         * @param s the simulator
         */
        Monitor(Simulator s) {
            this.simulator = s;
            this.platform = s.getMicrocontroller().getPlatform();
            //activate energy monitoring....
            //so the state machine is set up for energy monitoring when needed
            energyControl = s.getSimulation().getEnergyControl();
            energyControl.activate();

            if ( (energy = BATTERY.get()) > 0 ) {
                batteryCheck = new BatteryCheck();
            }
            if ( !LOG.isBlank() ) {
                logger = new Logger();
            }
        }

        /**
         * implemenation of report of Monitor class. Called when the simulation ends and reports summaries for
         * the power consumption of all devices to the stdout
         *
         * @see avrora.monitors.Monitor#report()
         */
        public void report() {
            //simulation will end
            //provide component energy breakdown
            TermUtil.printSeparator("Energy consumption results for node "+simulator.getID());
            Clock clock = simulator.getClock();
            long cycles = clock.getCount();
            Terminal.println("Node lifetime: " + cycles + " cycles,  " + clock.cyclesToMillis(cycles) / 1000.0+ " seconds\n");
            // get energy information for each device
            Iterator it = energyControl.consumer.iterator();
            while( it.hasNext() ){
                //get energy information
                Energy en = (Energy)it.next();
                int modes = en.getModeNumber();
                Terminal.println(en.getName() + ": " + en.getTotalConsumedEnergy() + " Joule");
                // get information for each state
                for (int j = 0; j < modes; j++)
                    //when there are more than 10 modes, only print the ones the system was in
                    if (modes <= 10 || en.getCycles(j) > 0)
                        Terminal.println("   " + en.getModeName(j) + ": " + en.getConsumedEnergy(j) + " Joule, " + en.getCycles(j) + " cycles");
                Terminal.nextln();
            }
            // make sure the logger flushes the files and logs the last state
            if ( logger != null ) logger.finish();
        }


        public class BatteryCheck implements Simulator.Event {
            //check 10 times per second
            private static final int interval = 737280;

            public BatteryCheck(){
                simulator.insertEvent(this, interval);
            }

            public void fire(){
                double totalEnergy = 0.0d;
                Iterator it = energyControl.consumer.iterator();
                //for (int i = 0; i < consumer.size(); ++i) {
                while(it.hasNext()){
                    //get energy information
                    totalEnergy += ((Energy)it.next()).getTotalConsumedEnergy();
                }
                if( totalEnergy <= energy ){
                    //lets go on
                    simulator.insertEvent(this, interval);
                } else {
                    //shutdown this node
                    SimPrinter printer = simulator.getPrinter();
                    StringBuffer buf = printer.getBuffer();
                    Terminal.append(Terminal.COLOR_YELLOW, buf, "energy limit exceeded: "+ totalEnergy+" joules");
                    printer.printBuffer(buf);

                    // TODO: remove the node from simulation.
                    //stop loop
                    simulator.stop();
                }

            }
        }

        /**
         * The <code>EnergyMonitorLog</code>
         * energy monitor implementation
         * Creates a file with logging information: energyNODEIS.log that contains the
         * current draw of all devices, and the state changes can be loaded into
         * Matlab, Gnuplot, Excel... for further processing and visualization.
         *
         * @author Olaf Landsiedel
         */
        public class Logger implements EnergyObserver {

            // file for data logging
            private BufferedWriter file;
            // the simulator state
            protected State state;

            /**
             * Create a new logger monitor. Creates a file with logging information: temp.log that contains the
             * current draw of all devices, and the state changes can be loaded into Matlab, Gnuplot, Excel... for
             * further processing and visualization.
             */
            Logger() {
                this.state = simulator.getState();
                // subscribe the monitor to the energy  control
                energyControl.subscribe(this);

                //open file for logging, currently with fixed path and file name
                String fileName = LOG.get() + simulator.getID();
                try {
                    this.file = new BufferedWriter(new FileWriter(fileName));
                } catch (IOException e) {
                    throw Util.unexpected(e);
                }

                //write headlines
                //first: cycle
                write("cycle ");
                //and than all consumers names
                Iterator it = energyControl.consumer.iterator();
                while( it.hasNext() ){
                    //for (int i = 0; i < consumer.size(); ++i) {
                    Energy en = (Energy)it.next();
                    write(en.getName() + " ");
                }
                write("total");
                newLine();

                //log the startup state
                logCurrentState();
            }

            /**
             * write text or data to the log file
             *
             * @param text data or text to write
             */
            private void write(String text) {
                try {
                    file.write(text);
                } catch (IOException e) {
                    throw Util.unexpected(e);
                }
            }

            /**
             * add new line to the log file
             */
            private void newLine() {
                try {
                    file.newLine();
                } catch (IOException e) {
                    throw Util.unexpected(e);
                }
            }

            public void finish() {

                //simulation will end
                //update log file
                logCurrentState();

                //close log file
                try {
                    file.flush();
                    file.close();
                } catch (IOException e) {
                    throw Util.unexpected(e);
                }
            }


            /**
             * called when the state of the device changes this component logs these state changes
             *
             * @see EnergyObserver#stateChange(Energy)
             */
            public void stateChange(Energy energy) {
                logOldState(energy);
                logCurrentState();
            }


            /**
             * log the current state
             */
            private void logCurrentState() {
                //write new state
                //first: current cycles
                write(state.getCycles() + " ");
                //and than all consumers
                double total = 0.0f;
                Iterator it = energyControl.consumer.iterator();
                while(it.hasNext()){
                    Energy en = (Energy)it.next();
                    double ampere = en.getCurrentAmpere();
                    total += ampere;
                    write(ampere + " ");
                }
                write(total + "");
                newLine();
            }


            /**
             * log the old state
             *
             * @param energy device, which state changed
             */
            private void logOldState(Energy energy) {
                //write old state
                //first: old cycles
                write((state.getCycles() - 1) + " ");
                //and than all consumers
                double total = 0.0f;
                Iterator it = energyControl.consumer.iterator();
                //for (int i = 0; i < consumer.size(); ++i) {
                while( it.hasNext() ){
                    Energy en = (Energy)it.next();
                    double ampere = (en == energy) ? en.getOldAmpere() : en.getCurrentAmpere();

                    total += ampere;
                    write(ampere + " ");
                }
                write(total + "");
                newLine();
            }
        }
    }


    /**
     * create a new monitor
     */
    public EnergyMonitor() {
        super("The \"energy\" is a monitor to trace energy consumption.");
    }

    public EnergyMonitor(String s2) {
        super(s2);
    }

    /**
     * create a new monitor, calls the constructor
     *
     * @see MonitorFactory#newMonitor(Simulator)
     */
    public avrora.monitors.Monitor newMonitor(Simulator s) {
        return new Monitor(s);
    }

}

