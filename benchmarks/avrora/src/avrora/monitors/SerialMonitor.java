/**
 * Created on 09.11.2004
 * 
 * Copyright (c) 2004-2005, Olaf Landsiedel, Protocol Engineering and 
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

package avrora.monitors;

import avrora.sim.Simulator;
import avrora.sim.mcu.AtmelMicrocontroller;
import avrora.sim.mcu.USART;
import avrora.sim.platform.SerialForwarder;
import cck.util.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;


/**
 * The <code>SerialMonitor</code> class is a monitor that that is capable of setting up a virtual
 * usart connection to the pc. You can connect the TinyOS serial forwarder to the port 2390.
 *
 * @author Olaf Landsiedel
 * @author Torsten Landschoff
 */
public class SerialMonitor extends MonitorFactory {

    protected final Option.List PORTS = newOptionList("ports", "0:0:2390",
            "The \"ports\" option specifies a list of server ports that the simulator will listen on " +
            "to connect to the serial forwarder for each node. The format is to first give " +
            "the node number, the UART number, and then the port number " +
            "($node:$uart:$port,$node:$uart:$port).");
    protected final Option.List DEVICE = newOptionList("devices", "",
            "The \"device\" option can be used to specify the devices (represented as file names) " +
            "to connect to each of the nodes' serial port. The format is to first give " +
            "the node number, the UART number, and then a file name for the input file, and (optionally) " +
            "a file name for the output ($node:$uart:$in[:$out],$node:$uart:$in[:$out]).");
    protected final Option.Str COMMAND = newOption("command", "",
            "The \"command\" option defines an external command to connect to the serial " +
            "port of the simulated system.");

    HashMap portMap;

    abstract class Connection {
        int usart;
        abstract void connect(USART usart);
    }

    class SocketConnection extends Connection {
        int port;
        void connect(USART usart) {
            new SerialForwarder(usart, port);
        }
    }

    class FileConnection extends Connection {
        String infile;
        String outfile;
        void connect(USART usart) {
            new SerialForwarder(usart, infile, outfile);
        }
    }

    class CommandConnection extends Connection {
        String[] command;
        void connect(USART usart) {
            new SerialForwarder(usart, command);
        }
    }

    /**
     * The <code>SerialMonitor</code> class is a monitor that connects the USART of a node to a socket that allows data
     * to be read and written from the simulation.
     */
    public class Monitor implements avrora.monitors.Monitor {

        /**
         * construct a new monitor
         *
         * @param s Simulator
         */
        Monitor(Simulator s) {
            Set conns = (Set)portMap.get(new Integer(s.getID()));
            if ( conns != null ) {
                Iterator i = conns.iterator();
                while ( i.hasNext() ) {
                    Connection conn = (Connection)i.next();
                    AtmelMicrocontroller mcu = (AtmelMicrocontroller)s.getMicrocontroller();
                    USART usart = (USART)mcu.getDevice("usart" + conn.usart);
                    if ( usart == null && conn.usart == 0 )
                        usart = (USART)mcu.getDevice("usart");
                    conn.connect(usart);
                }
            }
        }

        public void report() {
            //no report
        }

    }

    /**
     * The constructor for the <code>SerialMonitor</code> class builds a new <code>MonitorFactory</code> capable of
     * creating monitors for each <code>Simulator</code> instance passed to the <code>newMonitor()</code> method.
     */
    public SerialMonitor() {
        super("The \"serial\" monitor allows the serial port (UART) of a node in the simulation to be " +
                "connected to a socket so that data from the program running in the simulation can be " +
                "outputted, and external data can be fed into the serial port of the simulated node.");
        portMap = new HashMap();
    }

    public void processOptions(Options o) {
        super.processOptions(o);
        processSocketConnections();
        processDeviceConnections();
    }

    private void processSocketConnections() {
        Iterator i = PORTS.get().iterator();
        while ( i.hasNext() ) {
            String pid = (String)i.next();
            String[] str = pid.split(":");
            if ( str.length < 3 ) Util.userError("Format error in \"ports\" option");
            int nid = Integer.parseInt(str[0]);
            int uart = Integer.parseInt(str[1]);
            int port = Integer.parseInt(str[2]);
            SocketConnection conn = new SocketConnection();
            conn.usart = uart;
            conn.port = port;
            addConnection(nid, conn);
        }
    }

    private void processDeviceConnections() {
        Iterator i = DEVICE.get().iterator();
        while ( i.hasNext() ) {
            String pid = (String)i.next();
            String[] str = pid.split(":");
            if ( str.length < 3 ) Util.userError("Format error in \"device\" option");
            int nid = Integer.parseInt(str[0]);
            int uart = Integer.parseInt(str[1]);
            String inf = str[2];
            String outf = (str.length > 3) ? str[3] : inf;
            FileConnection conn = new FileConnection();
            conn.usart = uart;
            conn.infile = inf;
            conn.outfile = outf;
            addConnection(nid, conn);
        }
    }

    private void addConnection(int nid, Connection ucon) {
        Integer nidI = new Integer(nid);
        Set set = (Set)portMap.get(nidI);
        if (set == null) {
            set = new HashSet();
            portMap.put(nidI, set);
        }
        set.add(ucon);
    }

    /**
     * The <code>newMonitor()</code> method creates a new monitor that is capable of setting up a virtual usart
     * connection to the pc. You can connect the TinyOS serial forwarder to the port 2390.
     *
     * @param s the simulator to create a monitor for
     * @return an instance of the <code>Monitor</code> interface for the specified simulator
     */
    public avrora.monitors.Monitor newMonitor(Simulator s) {
        return new Monitor(s);
    }

}



