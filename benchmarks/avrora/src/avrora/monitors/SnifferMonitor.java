/*
 * "Copyright (c) 2009 Cork Institute of Technology, Ireland
 * All rights reserved."
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE CORK INSTITUTE OF TECHNOLOGY BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE CORK INSTITUTE
 * OF TECHNOLOGY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE CORK INSTITUTE OF TECHNOLOGY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 */

package avrora.monitors;

import avrora.sim.Simulator;
import avrora.sim.clock.Clock;
import avrora.sim.platform.Platform;
import avrora.sim.radio.*;
import avrora.sim.output.SimPrinter;
import cck.text.*;
import cck.util.Option;

import java.util.*;
import java.io.*;
import java.text.DecimalFormat;


/**
 * Sniffer monitor implementation. This class logs the number of packets, RSSI, LQI..
 * as the accepted by the Daintree Networks’ Sensor Network Analyzer (SNA) software
 * @author Rodolfo de Paz
 */
public class SnifferMonitor extends MonitorFactory {

    protected Option.Bool TRANSMITTED = newOption("Transmitted", false,
            "This option enables/disables the printing of packets as they are transmitted.");
    protected Option.Bool RECEIVED = newOption("Received", false,
            "This option enables/disables the printing of packets as they are received.");
    protected Option.Bool PRINT = newOption("Print", false,
            "This option enables/disables the printing of the output.");
    protected Option.Str FILENAME = newOption("FileName", "out.dcf",
            "This option is used to give a name to the output file. Name of output is out.dcf as default");
    protected List monitors = new LinkedList();

    protected static int packetsTotal;


    class Mon implements Monitor, Medium.Probe {
        LinkedList bytes;
        final Simulator simulator;
        final SimPrinter printer;
        final boolean showReceived;
        final boolean showTransmitted;
        final boolean Print;
        int bytesTransmitted;
        int packetsTransmitted;
        int bytesReceived;
        int packetsReceived;
        int bytesCorrupted;

        boolean matchStart;
        byte startSymbol;
        long startCycle;
        String fileName = "";

        Date now = new Date();
        double DateTime;


        Mon(Simulator s) {
            simulator = s;
            Platform platform = simulator.getMicrocontroller().getPlatform();
            Radio radio = (Radio)platform.getDevice("radio");
            radio.getTransmitter().insertProbe(this);
            radio.getReceiver().insertProbe(this);
            printer = simulator.getPrinter();
            showReceived = RECEIVED.get();
            showTransmitted = TRANSMITTED.get();
            Print = PRINT.get();
            fileName = FILENAME.get();
            bytes = new LinkedList();
            DateTime = now.getTime()/1000;//sec precision
            monitors.add(this);
        }

        public void fireBeforeTransmit(Medium.Transmitter t, byte val) {
            if (showTransmitted){
                if (bytes.size() == 0) startCycle = simulator.getClock().getCount();
                bytes.addLast(new Character((char)(0xff & val)));
                bytesTransmitted++;
            }
        }

        public void fireBeforeTransmitEnd(Medium.Transmitter t) {
            if (showTransmitted){
                packetsTransmitted++;
                synchronized ( Terminal.class) {
                    StringBuffer buf = renderTxPacket();
                    if (Print) Terminal.println(buf.toString());
                    if (showTransmitted) logfile(buf);
                }
                bytes = new LinkedList();
            }
        }

        public void fireAfterReceive(Medium.Receiver r, char val) {
            if (showReceived){
                if (bytes.size() == 0) startCycle = simulator.getClock().getCount();
                if (Medium.isCorruptedByte(val)) bytesCorrupted++;
                bytes.addLast(new Character(val));
                bytesReceived++;
            }
        }

        public void fireAfterReceiveEnd(Medium.Receiver r) {
            if (showReceived){
                packetsReceived++;
                synchronized ( Terminal.class) {
                    StringBuffer buf = renderRxPacket();
                    if (Print) Terminal.println(buf.toString());
                    if (showReceived) logfile(buf);
                }
                bytes = new LinkedList();
            }
        }

        private StringBuffer renderRxPacket() {
            //create string buffer
            StringBuffer buf = new StringBuffer(3 * bytes.size() + 45);
            //Append Sequence number
            packetsTotal++;
            buf.append(String.valueOf(packetsTotal)).append(" ");
            //Append Timestamp in us
            Clock clk = simulator.getClock();
            double seconds = (double)clk.getCount() / (double)clk.getHZ();
            DecimalFormat SixDecimals = new DecimalFormat("0.000000");
            buf.append(String.valueOf(SixDecimals.format(seconds + DateTime))).append(" ");
            //Iterate bytes from the packet and parse them
            Iterator i = bytes.iterator();
            int cntr = 0;
            int len = 0;
            int power_received = 0;
            int lqi = 0;
            int fcs=0;
            //Skip SHR (Preamble &SFD) and append length,data payload,LQI..
            while ( i.hasNext() ) {
                cntr++;
                char t = ((Character)i.next()).charValue();
                //length field
                if (cntr == 6){
                    len = (int)t;
                    buf.append(StringUtil.toDecimal((long)t,0));
                    buf.append(" ");
                }
                //data payload
                if (cntr > 6 && (cntr-6) <= (len)) buf.append(StringUtil.toHex((byte)t, 2));
                //LQI, FCS, Power
                if (cntr == (7 + len - 2)) power_received = (int)t-255-45;
                if (cntr == (7 + len - 1)){
                    lqi = (0x7f & (int)t);
                    fcs = (0x80 & (int)t)>>>7;
                }
            }
            //Format 4 Daintree sniffer
            //lqi+fcs+power_received+channel+SN in this channel+duplicate packet = always false
            //+timestamp synchronized = always true+Device id = "unknown"
            buf.append(" ").append(StringUtil.toDecimal((byte) lqi, 0))
                    .append(" ").append(StringUtil.toHex((byte) fcs, 1))
                    .append(" ").append(StringUtil.toDecimal((byte) power_received, 0))
                    .append(" 26 ").append(String.valueOf(packetsTotal))
                    .append(" 0 1 32767");
        return buf;
        }
        private StringBuffer renderTxPacket() {
            //create string buffer
            StringBuffer buf = new StringBuffer(3 * bytes.size() + 45);
            //Append Sequence number
            packetsTotal++;
            Terminal.append(Terminal.COLOR_DEFAULT, buf, String.valueOf(packetsTotal)+" ");
            //Append Timestamp in us
            Clock clk = simulator.getClock();
            double seconds = (double)clk.getCount() / (double)clk.getHZ();
            DecimalFormat SixDecimals = new DecimalFormat("0.000000");
            buf.append(SixDecimals.format(seconds + DateTime)).append(" ");
            //Iterate bytes from the packet and parse them
            Iterator i = bytes.iterator();
            int cntr = 0;
            int len = 0;
            //Skip SHR (Preamble &SFD) and append length,data payload,LQI..
            while ( i.hasNext() ) {
                cntr++;
                char t = ((Character)i.next()).charValue();
                //length field
                if (cntr == 6){
                    len = (int)t;
                    buf.append(StringUtil.toDecimal((long) t, 0)).append(" ");
                }
                //data payload
                if (cntr > 6 && (cntr-6) <= (len)) buf.append(StringUtil.toHex((byte)t, 2));
            }
            //Format 4 Daintree sniffer
            //lqi not supported + fcs + power_received not supported+channel+SN in this channel
            //+duplicate packet = always false+timestamp synchronized = always true+Device id = "unknown"
            buf.append(" 0 1 32767 26 ").append(packetsTotal).append(" 0 1 32767");
        return buf;
        }
        private void logfile(StringBuffer buf){
            if (showReceived || showTransmitted){
                try{
                    // Create file
                    if (packetsTotal == 1){
                        BufferedWriter out = new BufferedWriter( new FileWriter(fileName));
                        out.write("#Format=4");
                        out.write('\n');
                        out.write(buf.toString());
                        out.write('\n');
                        out.close();
                    } else{
                        BufferedWriter out = new BufferedWriter( new FileWriter(fileName,true));
                        out.write(buf.toString());
                        out.write('\n');
                        out.close();
                    }

                }catch (Exception e){//Catch exception if any
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }

        public void report() {
            if (monitors != null) {
                monitors = null;
            }
            Terminal.nextln();
            }
        }
    /**
     * create a new monitor
     */
    public SnifferMonitor() {
        super("The \"sniffer\" monitor logs packets in the format accepted by the Daintree Networks’ Sensor Network Analyzer (SNA) software.");
    }


    /**
     * create a new monitor, calls the constructor
     *
     * @see MonitorFactory#newMonitor(Simulator)
     */
    public Monitor newMonitor(Simulator s) {
        return new Mon(s);
    }

}
