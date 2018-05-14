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
import avrora.sim.mcu.SPI;
import avrora.sim.platform.SPIForwarder;
import cck.util.*;
import java.util.HashMap;
import java.util.Iterator;


/**
 * The <code>SerialMonitor</code> class is a monitor that that is capable of setting up a virtual
 * usart connection to the pc. You can connect the TinyOS serial forwarder to the port 2390.
 *
 * @author Olaf Landsiedel
 * @author Torsten Landschoff
 */
public class SPIMonitor extends MonitorFactory {

    protected final Option.List PORTS = newOptionList("ports", "0:2391",
            "The \"ports\" option specifies a list of server ports that the simulator will listen on " +
            "to connect to the serial forwarder for each node. The format is to first give " +
            "the node number, and then the port number " +
            "($node:$port,$node:$port).");

    protected final Option.Long BPS = newOption("bps", 2400,
            "This option controls the bit rate of the controlling SPI transfer device.");

    protected final Option.Bool MASTER = newOption("master", true,
            "This option controls whether the SPI forwarder device will act as the " +
            "master or the slave in the SPI connection.");

    HashMap portMap;

    abstract class Connection {
        //int spi;
        abstract void connect(SPI spi);
    }

    class SocketConnection extends Connection {
        int port;
        void connect(SPI spi) {
            new SPIForwarder(spi, port, (int)BPS.get(), MASTER.get());
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
            Connection conn = (Connection)portMap.get(new Integer(s.getID()));
            if ( conn != null ) {
                AtmelMicrocontroller mcu = (AtmelMicrocontroller)s.getMicrocontroller();
                SPI spi = (SPI)mcu.getDevice("spi");
                conn.connect(spi);
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
    public SPIMonitor() {
        super("The \"spi\" monitor allows the SPI of a node in the simulation to be " +
                "connected to a socket so that data from the program running in the simulation can be " +
                "outputted, and external data can be fed into the SPI of the simulated node.");
        portMap = new HashMap();
    }

    public void processOptions(Options o) {
        super.processOptions(o);
        processSocketConnections();
        //processDeviceConnections();
    }

    private void processSocketConnections() {
        Iterator i = PORTS.get().iterator();
        while ( i.hasNext() ) {
            String pid = (String)i.next();
            String[] str = pid.split(":");
            if ( str.length < 2 ) Util.userError("Format error in \"ports\" option");
            int nid = Integer.parseInt(str[0]);
            int port = Integer.parseInt(str[1]);
            SocketConnection conn = new SocketConnection();
            conn.port = port;
            portMap.put(new Integer(nid), conn);
        }
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



