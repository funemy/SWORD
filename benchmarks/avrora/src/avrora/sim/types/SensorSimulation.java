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

package avrora.sim.types;

import avrora.Main;
import avrora.core.*;
import avrora.sim.*;
import avrora.sim.clock.RippleSynchronizer;
import avrora.sim.platform.Platform;
import avrora.sim.platform.PlatformFactory;
import avrora.sim.platform.sensors.*;
import avrora.sim.radio.*;
import avrora.sim.radio.Topology;
import avrora.sim.radio.noise;
import cck.text.StringUtil;
import cck.util.*;

import java.io.IOException;
import java.util.*;

/**
 * The <code>SensorSimulation</code> class represents a simulaion type where multiple sensor nodes,
 * each with a microcontroller, sensors, and a radio, are run in parallel. It supports options from
 * the command line that allow a simulation to be constructed with multiple nodes with multiple different
 * programs.
 *
 * @author Ben L. Titzer
 */
public class SensorSimulation extends Simulation {

    public static String HELP = "The sensor network simulation is used for simulating multiple sensor nodes " +
            "simultaneously. These nodes can communicate with each other wirelessly to exchange packets that " +
            "include sensor data and routing information for a multi-hop network. Currently, only the \"mica2\" " +
            "platform sensor nodes are supported.";

    public final Option.List NODECOUNT = newOptionList("nodecount", "1",
            "This option is used to specify the number of nodes to be instantiated. " +
            "The format is a list of integers, where each integer specifies the number of " +
            "nodes to instantiate with each program supplied on the command line. For example, " +
            "when set to \"1,2\" one node will be created with the first program loaded onto it, " +
            "and two nodes created with the second program loaded onto them.");
    public final Option.Str TOPOLOGY = newOption("topology", "",
            "This option can be used to specify the name of " +
            "a file that contains information about the topology of the network. " +
            "When this option is specified. the free space radio model will be used " +
            "to model radio propagation.");
    public final Option.Bool LOSSY_MODEL = newOption("lossy-model",false,
            "When this option is set, the radio model takes into account Noise and fadings thus" +
            "implementing in micaz platform the correlation, cca and rssi functions.");
    public final Option.Str NOISE = newOption("Noise", "",
            "This option can be used to specify the name of " +
            "a file that contains a Noise time trace. When this option is specified" +
            "the indoor radio model will be used to model radio propagation.");
    public final Option.Double RANGE = newOption("radio-range", 15.0,
            "This option, when used in conjunction with the -topology option, specifies " +
            "the maximum range for radio communication between nodes. This simple " +
            "idealized radius model will drop all communications between nodes whose " +
            "distance is greater than this threshold value.");
    public final Option.Interval RANDOM_START = newOption("random-start", 0, 0,
            "This option inserts a random delay before starting " +
            "each node in order to prevent artificial cycle-level synchronization. The " +
            "starting delay is pseudo-randomly chosen with uniform distribution over the " +
            "specified interval, which is measured in clock cycles. If the \"random-seed\" " +
            "option is set to a non-zero value, then its value is used as the seed to the " +
            "pseudo-random number generator.");
    public final Option.Long STAGGER_START = newOption("stagger-start", 0,
            "This option causes the simulator to insert a progressively longer delay " +
            "before starting each node in order to avoid artificial cycle-level " +
            "synchronization between nodes. The starting times are staggered by the specified number " +
            "of clock cycles. For example, if this option is given the " +
            "value X, then node 0 will start at time 0, node 1 at time 1*X, node 2 at " +
            "time 2*X, etc.");
    public final Option.List SENSOR_DATA = newOptionList("sensor-data", "",
            "This option accepts a list describing the input data for each sensor node. The format " +
            "for each entry in this list is $sensor:$id:$data, where $sensor is the name of " +
            "the sensor device such as \"light\", $id is the integer ID of the node, and $data is " +
            "the name of a file or the special '.' character, indicating random data. A sensor data " +
            "input file consists of an initial sensor reading which is interpreted as a 10-bit ADC " +
            "result, then a list of time value pairs separated by whitespace; the sensor will continue " +
            "returning the current value until the next (relative) time in seconds, and then the sensor " +
            "will change to the new value. ");
    public final Option.Bool UPDATE_NODE_ID = newOption("update-node-id", true,
            "When this option is set, the sensor network simulator will attempt to update " +
            "the node identifiers stored in the flash memory of the program. For TinyOS programs, " +
            "this identifier is labelled \"TOS_LOCAL_ADDRESS\". For SOS programs, this identifier is " +
            "called \"node_address\". When loading a program onto " +
            "a node, the simulator will search for these labels, and if found, will update the word " +
            "in flash with the node's ID number.");

    class SensorDataInput {
        String sensor;
        String fname;

        void instantiate(Platform p) {
            try {
                Sensor s = (Sensor)p.getDevice(sensor+"-sensor");
                if ( s == null )
                    Util.userError("Sensor device does not exist", sensor);
                if ( ".".equals(fname) ) s.setSensorData(new RandomSensorData(getRandom()));
                else s.setSensorData(new ReplaySensorData(p.getMicrocontroller(), fname));
            } catch ( IOException e) {
                throw Util.unexpected(e);
            }
        }
    }

    /**
     * The <code>SensorNode</code> class extends the <code>Node</code> class of a simulation
     * by adding a reference to the radio device as well as sensor data input. It extends the
     * <code>instantiate()</code> method to create a new thread for the node and to attach the
     * sensor data input.
     */
    protected class SensorNode extends Node {
        Radio radio;
        long startup;
        List sensorInput;

        SensorNode(int id, PlatformFactory pf, LoadableProgram p) {
            super(id, pf, p);
            sensorInput = new LinkedList();
        }

        /**
         * The <code>instantiate()</code> method of the sensor node extends the default simulation node
         * by creating a new thread to execute the node as well as getting references to the radio and
         * adding it to the radio model, adding
         * an optional start up delay for each node, and connecting the node's sensor input to
         * replay or random data as specified on the command line.
         */
        protected void instantiate() {
            createNode();
            updateNodeID();
            addSensorData();
        }

        private void addSensorData() {
            // process sensor data inputs
            Iterator i = sensorInput.iterator();
            while ( i.hasNext() ) {
                SensorDataInput sdi = (SensorDataInput)i.next();
                sdi.instantiate(platform);
            }
        }

        private void createNode() {
            thread = new SimulatorThread(this);
            super.instantiate();
            // get the radio device, if it exists.
            Object dev = platform.getDevice("radio");
            if (dev instanceof CC2420Radio) {
                // connect to the cc2420 medium
                CC2420Radio radio = (CC2420Radio)dev;
                this.radio = radio;
                radio.setMedium(createCC2420Medium());
            } else if (dev instanceof CC1000Radio) {
                // connect to the cc1000 medium
                CC1000Radio radio = (CC1000Radio)dev;
                this.radio = radio;
                radio.setMedium(createCC1000Medium());
            }
            simulator.delay(startup);
            if (topology != null) {
                setNodePosition();
                return;
            }
        }

        private Medium createCC2420Medium() {
            if (cc2420_medium == null) {
                createRadioModel();
                if (LOSSY_MODEL.get()){
                    return cc2420_medium = CC2420Radio.createMedium(synchronizer, lossyModel);
                }else{
                    return cc2420_medium = CC2420Radio.createMedium(synchronizer, radiusModel);
                }
            }
            return cc2420_medium;
        }

        private Medium createCC1000Medium() {
            if (cc1000_medium == null) {
                createRadioModel();
                if (LOSSY_MODEL.get()){
                    return cc1000_medium = CC1000Radio.createMedium(synchronizer, lossyModel);
                }else{
                    return cc1000_medium = CC1000Radio.createMedium(synchronizer, radiusModel);
                }
            }
            return cc1000_medium;
        }
        private void createRadioModel() {
           if (topology == null && !TOPOLOGY.isBlank()) {
                 try {
                    if (LOSSY_MODEL.get()){
                        topology = new Topology(TOPOLOGY.get(),true);
                        lossyModel = new LossyModel();
                    }
                    else{
                        topology = new Topology(TOPOLOGY.get(),false);
                        radiusModel = new RadiusModel(1.0, RANGE.get());
                    }
                 } catch (IOException e) {
                     throw Util.unexpected(e);
                 }
           }
        }


        private void setNodePosition() {
            if (LOSSY_MODEL.get()){
                LossyModel.Position p = topology.getPosition(id);
                if (p != null && radio != null) lossyModel.setPosition(radio, p);
            }else{
                RadiusModel.Position p = topology.getPositioninRadius(id);
                if (p != null && radio != null) radiusModel.setPosition(radio, p);
            }
        }

        private void updateNodeID() {
            if ( UPDATE_NODE_ID.get() ) {
                Program p = path.getProgram();
                SourceMapping smap = p.getSourceMapping();
                if ( smap != null ) {
                    updateVariable(smap, "TOS_LOCAL_ADDRESS", id);          // TinyOS 1.1
                    updateVariable(smap, "node_address", id);               // SOS
                    updateVariable(smap, "TOS_NODE_ID", id);                // Tinyos 2.0
                    updateVariable(smap, "ActiveMessageAddressC$addr", id); // Tinyos 2.0
                }
            }
        }

        private void updateVariable(SourceMapping smap, String name, int value) {
            SourceMapping.Location location = smap.getLocation(name);
            if ( location == null ) location = smap.getLocation("node_address");
            if ( location != null ) {
                AtmelInterpreter bi = (AtmelInterpreter)simulator.getInterpreter();
                bi.writeFlashByte(location.lma_addr, Arithmetic.low(value));
                bi.writeFlashByte(location.lma_addr +1, Arithmetic.high(value));
            }
        }

        /**
         * The <code>remove()</code> method removes this node from the simulation. This method extends the
         * default simulation remove method by removing the node from the radio air implementation.
         */
        protected void remove() {
            synchronizer.removeNode(this);
        }
    }

    Topology topology;
    noise noise;
    LossyModel lossyModel;
    RadiusModel radiusModel;
    Medium cc2420_medium;
    Medium cc1000_medium;
    long stagger;

    public SensorSimulation() {
        super("sensor-network", HELP, null);
        addSection("SENSOR NETWORK SIMULATION OVERVIEW", help);
        addOptionSection("This simulation type supports simulating multiple sensor network nodes that communicate " +
                "with each other over radios. There are options to specify how many of each type of sensor node to " +
                "instantiate, as well as the program to be loaded onto each node, and an optional topology file " +
                "that describes the physical layout of the sensor network. Also, each node's sensors can be " +
                "supplied with random or replay sensor data through the \"sensor-data\" option.", options);

        PLATFORM.setNewDefault("micaz");       // set the new default monitors
        MONITORS.setNewDefault("leds,packet"); // set the new default monitors
    }

    /**
     * The <code>newNode()</code> method creates a new node in the simulation. In this implementation,
     * a <code>WiredNode</code> is created that contains, in addition to the simulator, ID, program, etc,
     * a reference to the <code>Radio</code> instance for the node and a <code>SimulatorThread</code> for
     * the node.
     * @param id the integer identifier for the node
     * @param pf the platform factory to use to instantiate the node
     * @param p the program to load onto the node
     * @return a new instance of the <code>SensorNode</code> class for the node
     */
    public Node newNode(int id, PlatformFactory pf, LoadableProgram p) {
        return new SensorNode(id, pf, p);
    }

    /**
     * The <code>process()</code> method processes options and arguments from the command line. In this
     * implementation, this method accepts multiple programs from the command line as arguments as well
     * as options that describe how many of each type of node to instantiate.
     * @param o the options from the command line
     * @param args the arguments from the command line
     * @throws Exception if there is a problem loading any of the files or instantiating the simulation
     */
    public void process(Options o, String[] args) throws Exception {
        options.process(o);
        processMonitorList();

        if ( args.length == 0 )
            Util.userError("Simulation error", "No program specified");
        Main.checkFilesExist(args);
        PlatformFactory pf = getPlatform();

        // build the synchronizer
        synchronizer = new RippleSynchronizer(100000, null);

        // create the nodes based on arguments
        createNodes(args, pf);

        // process the sensor data input option
        processSensorInput();

        //create Noise time trace
        createNoise();
    }

    private void createNodes(String[] args, PlatformFactory pf) throws Exception {
        Iterator i = NODECOUNT.get().iterator();
        for ( int arg = 0; arg < args.length; arg++ ) {
            int count = i.hasNext() ? StringUtil.evaluateIntegerLiteral((String)i.next()) : 1;
            LoadableProgram lp = new LoadableProgram(args[arg]);
            lp.load();

            // create a number of nodes with the same program
            for (int node = 0; node < count; node++) {
                SensorNode n = (SensorNode)createNode(pf, lp);
                long r = processRandom();
                long s = processStagger();
                n.startup = r + s;
            }
        }
    }
        private void createNoise() throws Exception {
            if (noise == null && !NOISE.isBlank()) {
                    noise = new noise(NOISE.get());
            }else if (noise == null && NOISE.isBlank()){
                    noise = new noise();
            }
    }

    private void processSensorInput() {
        Iterator i = SENSOR_DATA.get().iterator();
        while ( i.hasNext() ) {
            String str = (String)i.next();
            int ind = str.indexOf(':');
            if ( ind <= 0 )
                Util.userError("Sensor data format error", str);
            String sensor = str.substring(0, ind);
            String rest = str.substring(ind+1);
            int ind2 = rest.indexOf(':');
            if ( ind2 <= 0 )
                Util.userError("Sensor data format error", str);
            String id = rest.substring(0, ind2);
            String file = rest.substring(ind2+1);

            addSensorData(id, file, sensor);
        }
    }

    private void addSensorData(String id, String file, String sensor) {
        int num = StringUtil.evaluateIntegerLiteral(id);
        SensorNode node = (SensorNode)getNode(num);
        if ( node != null ) {
            SensorDataInput sdi = new SensorDataInput();
            sdi.fname = file;
            sdi.sensor = sensor;
            node.sensorInput.add(sdi);
            if (! ".".equals(file) )
                Main.checkFileExists(file);
        }
    }

    long processRandom() {
        long low = RANDOM_START.getLow();
        long size = RANDOM_START.getHigh() - low;
        long delay = 0;
        if (size > 0) {
            Random r = getRandom();
            delay = r.nextLong();
            if (delay < 0) delay = -delay;
            delay = delay % size;
        }

        return (low + delay);
    }

    long processStagger() {
        long st = stagger;
        stagger += STAGGER_START.get();
        return st;
    }

}