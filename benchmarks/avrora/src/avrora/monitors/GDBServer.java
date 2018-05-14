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

package avrora.monitors;

import avrora.arch.legacy.LegacyRegister;
import avrora.arch.legacy.LegacyState;
import avrora.sim.Simulator;
import avrora.sim.State;
import avrora.sim.output.SimPrinter;
import cck.text.StringUtil;
import cck.text.Terminal;
import cck.util.Option;
import cck.util.Util;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * The <code>GDBServer</code> class implements a monitor that can communicate to gdb via
 * the remote serial protocol (RSP). This allows Avrora to host a program that is executing
 * but is being remotely debugged by gdb. This can all be done without modifications to the
 * <code>Simulator</code> class, by simply using probes, watches, and events.
 *
 * NOTE: This monitor is only meant for a single node simulation!
 *
 * @author Ben L. Titzer
 */
public class GDBServer extends MonitorFactory {

    public static String HELP = "The \"gdb\" monitor implements the GNU Debugger (gdb) remote serial " +
            "protocol. The server will create a server socket which GDB can connect to in order to " +
            "send commands to Avrora. This allows gdb to be used as a front end for debugging a program " +
            "running inside of Avrora.";

    private final Option.Long PORT = newOption("port", 10001,
            "This option specifies the port on which the GDB server will listen for a connection from " +
            "the GDB front-end.");

    /**
     * The <code>GDBMonitor</code> class implements a monitor that can interactively debug
     * a program that is running in Avrora. It uses the remote serial protocol of GDB to run
     * the commands that are sent by GDB. It uses probes into the program in order to pause,
     * resume, step, break, etc.
     *
     * For information on the remote serial protocol of GDB, see:
     *
     * https://www.redhat.com/docs/manuals/enterprise/RHEL-3-Manual/gdb/remote-protocol.html
     */
    protected class GDBMonitor implements Monitor {

        final Simulator simulator;
        ServerSocket serverSocket;
        Socket socket;
        InputStream input;
        OutputStream output;
        final int port;
        BreakpointProbe BREAKPROBE = new BreakpointProbe();
        StepProbe STEPPROBE = new StepProbe();
        SimPrinter printer;
        boolean isStepping;


        GDBMonitor(Simulator s, int p) {
            simulator = s;
            port = p;
            printer = simulator.getPrinter("monitor.gdb");
            try {
                serverSocket = new ServerSocket(port);
            } catch ( IOException e ) {
                Util.userError("GDBServer could not create socket on port "+port, e.getMessage());
            }
            // insert the startup probe at the beginning of the program
            simulator.insertProbe(new StartupProbe(), 0);

            // insert the stepping probe
            isStepping=false;
            simulator.insertProbe(STEPPROBE);

            // install the ExceptionWatch
            simulator.insertErrorWatch(new ExceptionWatch("sram"));
        }

        public void report() {
            try {
                if ( socket != null )
                socket.close();
            } catch ( IOException e ) {
                throw Util.failure("Unexpected IOException: "+e);
            }
        }

        /**
         * The <code>commandLoop()</code> method reads commands from the GDB socket and executes
         * them until a command causes the simulation to resume (e.g. a continue, step, etc). This
         * method is called from within probes inserted into the simulation. Optionally, a reply
         * can be sent before the command loop is entered. For example, a trap would need to send
         * a "T05" reply to the remote gdb to signal that the simulation has stopped before waiting
         * for commands.
         * @param reply a reply to send before executing commands, if any
         */
        void commandLoop(String reply) {
            try {
                if ( reply != null ) {
                    sendPacket(reply);
                }

                while ( true ) {
                    String command = readCommand();
                    if ( command == null ) {
                        Terminal.println("GDBServer: null command, stopping simulator");
                        simulator.stop();
                        break;
                    }
                    if (printer != null) {
                        printer.println(" --> "+command);
                    }
                    // invoke the command: continue the program if the return value is true
                    if ( executeCommand(command) )
                        break;
                }
            } catch ( IOException e ) {
                throw Util.failure("Unexpected IOException: "+e);
            }
        }

        /**
         * The <code>executeCommand()</code> method executes a single command that has been read
         * from the socket and packaged as a string. If the command should resume the simulation,
         * this method will return <code>true</code>, and <code>false</code> otherwise.
         * @param command the command packaged as a string
         * @return true if the simulation should resume; false otherwise
         * @throws IOException if there is a problem communicating over the socket
         */
        boolean executeCommand(String command) throws IOException {
            CharacterIterator i = new StringCharacterIterator(command);
            if ( i.current() == '+' ) i.next();
            if ( !StringUtil.peekAndEat(i, '$') ) {
                commandError();
                return false;
            }

            char c = i.current();
            i.next();

            switch ( c ) {
                case 'c':
                    // CONTINUE WITH EXECUTION
                    // TODO: implement continue at address
                    sendPlus();
                    return true;
                case 'D':
                    // DISCONNECT
                    Terminal.println("GDBServer: disconnected");
                    sendPlus();
                    simulator.stop();
                    return true;
                case 'g':
                    // READ REGISTERS
                    readAllRegisters();
                    return false;
                case 'G':
                    // WRITE REGISTERS
                    // TODO: implement update of registers
                    break;
                case 'H':
                    // SET THREAD CONTEXT -- THERE IS ONLY ONE
                    sendPacketOK("OK");
                    return false;
                case 'i':
                    // STEP CYCLE
                    isStepping=true;
                    break;
                case 'k':
                    // KILL
                    Terminal.println("GDBServer: killed remotely");
                    sendPlus();
                    simulator.stop();
                    return true;
                case 'm':
                    readMemory(i);
                    return false;
                case 'M':
                    // WRITE MEMORY
                    // TODO: implement writes to memory
                    break;
                case 'p':
                    // READ SELECTED REGISTERS
                    readOneRegister(i);
                    return false;
                case 'P':
                    // WRITE SELECTED REGISTERS
                    // TODO: implement writes to selected registers
                    break;
                case 'q':
                    // QUERY A VARIABLE
                    // TODO: implement queries to variables
                    break;
                case 's':
                    // STEP INSTRUCTION
                    isStepping=true;
                    sendPlus();
                    return true;
                case 'z':
                    // REMOVE BREAKPOINT
                    setBreakPoint(i, false);
                    return false;
                case 'Z':
                    // SET BREAKPOINT
                    setBreakPoint(i, true);
                    return false;
                case '?':
                    // GET LAST SIGNAL
                    sendPacketOK("S05");
                    return false;
            }

            // didn't understand the comand
            sendPacketOK("");
            return false;
        }

        /**
         * The <code>sendPlus()</code> method is just a utility to send a plus '+' character
         * back over the socket to signal to the remote party that the command was received
         * successfully.
         * @throws IOException if there is a problem communicating over the socket
         */
        private void sendPlus() throws IOException {
            output.write((byte)'+');
        }


        /**
         * The <code>sendPlus()</code> method is just a utility to send a minus '-' character
         * back over the socket to signal to the remote party that the command was NOT received
         * successfully.
         * @throws IOException if there is a problem communicating over the socket
         */
        void commandError() throws IOException {
            output.write((byte)'-');
        }

        /**
         * The <code>setBreakPoint()</code> method inserts a breakpoint into the program. It does
         * so by inserting a special probe that will call back into the <code>commandLoop()</code>
         * before the instruction executes.
         * @param i the iterator over the characters of the command
         * @param on true if the breakpoint should be inserted; false if it should be removed
         * @throws IOException if there is a problem communicating over the socket
         */
        void setBreakPoint(CharacterIterator i, boolean on) throws IOException {
            // TODO: deal with length as well!
            char num = i.current();
            i.next();
            switch ( num ) {
                case '0':
                case '1':
                    if ( !StringUtil.peekAndEat(i, ',') ) break;
                    int addr = StringUtil.readHexValue(i, 4);
                    if ( !StringUtil.peekAndEat(i, ',') ) break;
                    int len = StringUtil.readHexValue(i, 4);
                    for ( int cntr = addr; cntr < addr+len; cntr += 2 )
                        setBreakPoint(addr, on);
                    sendPacketOK("OK");
                    return;
                case '2':
                    // TODO: other breakpoint types?
                case '3':
                    // TODO: other breakpoint types?
                default:
            }

            sendPacketOK("");
        }

        /**
         * The <code>setBreakPoint()</code> method simply inserts or removes the breakpoint probe
         * at the given location
         * @param addr the address of the breakpoint
         * @param on true if the breakpoint should be enabled, false if it should be disabled
         */
        void setBreakPoint(int addr, boolean on) {
            if ( on )
                simulator.insertProbe(BREAKPROBE, addr);
            else
                simulator.removeProbe(BREAKPROBE, addr);
        }

        /**
         * The <code>readAllRegisters()</code> method simply reads the values of all the general
         * purpose registers as well as the SREG, SP, and PC registers and sends them back over the
         * socket.
         * @throws IOException if there is a problem communicating over the socket
         */
        void readAllRegisters() throws IOException {
            StringBuffer buf = new StringBuffer(84);
            LegacyState s = (LegacyState)simulator.getState();
            for ( int cntr = 0; cntr < 32; cntr++ ) {
                appendGPR(s, cntr, buf);
            }
            appendSREG(s, buf);
            appendSP(s, buf);
            appendPC(s, buf);
            sendPacketOK(buf.toString());
        }

        private void appendPC(State s, StringBuffer buf) {
            int pc = s.getPC();
            buf.append(StringUtil.toLowHex(pc & 0xff, 2));
            buf.append(StringUtil.toLowHex((pc >> 8) & 0xff, 2));
            buf.append(StringUtil.toLowHex((pc >> 16) & 0xff, 2));
            buf.append(StringUtil.toLowHex((pc >> 24) & 0xff, 2));
        }

        private void appendSP(State s, StringBuffer buf) {
            buf.append(StringUtil.toLowHex(s.getSP() & 0xff, 2));
            buf.append(StringUtil.toLowHex((s.getSP() >> 8) & 0xff, 2));
        }

        private void appendSREG(LegacyState s, StringBuffer buf) {
            buf.append(StringUtil.toLowHex(s.getSREG() & 0xff, 2));
        }

        private void appendGPR(LegacyState s, int cntr, StringBuffer buf) {
            byte value = s.getRegisterByte(LegacyRegister.getRegisterByNumber(cntr));
            buf.append(StringUtil.toLowHex(value & 0xff, 2));
        }

        /**
         * The <code>readOneRegister()</code> method simply reads the value of one register
         * given its number.
         * @throws IOException if there is a problem communicating over the socket
         */
        void readOneRegister(CharacterIterator i) throws IOException {
            StringBuffer buf = new StringBuffer(8);
            LegacyState s = (LegacyState)simulator.getState();

            int num = StringUtil.readHexValue(i, 2);

            if ( num < 32 ) {
                // general purpose register
                appendGPR(s, num, buf);
            } else if ( num == 32 ) {
                // SREG
                appendSREG(s, buf);
            } else if ( num == 33 ) {
                // SP
                appendSP(s, buf);
            } else if ( num == 34 ) {
                // PC
                appendPC(s, buf);
            } else {
                // unknown register
                buf.append("ERR");
            }

            sendPacketOK(buf.toString());
        }

        private static final int MEMMASK = 0xf00000;
        private static final int MEMBEGIN = 0x800000;

        /**
         * The <code>readMemory()</code> method implements the memory read command that can read
         * the values of SRAM, the registers, and the flash, depending on the linear address.
         * @param i the iterator over the characters of the command
         * @throws IOException if there is a problem communicating over the socket
         */
        void readMemory(CharacterIterator i) throws IOException {
            // read the address in memory
            int addr = StringUtil.readHexValue(i, 8);
            // read the length if it exists
            int length = 1;
            if ( StringUtil.peekAndEat(i, ',') )
                length = StringUtil.readHexValue(i, 8);
            LegacyState s = (LegacyState)simulator.getState();
            StringBuffer buf = new StringBuffer(length*2);

            if ( (addr & MEMMASK) == MEMBEGIN ) {
                // reading from SRAM
                addr = addr & (~MEMMASK);
                for ( int cntr = 0; cntr < length; cntr++ ) {
                    byte value = s.getDataByte(addr+cntr);
                    buf.append(StringUtil.toLowHex(value & 0xff, 2));
                }
            } else {
                // reading from program memory
                for ( int cntr = 0; cntr < length; cntr++ ) {
                    byte value = s.getProgramByte(addr+cntr);
                    buf.append(StringUtil.toLowHex(value & 0xff, 2));
                }
            }

            sendPacketOK(buf.toString());
        }

        /**
         * The <code>sendPacketOK()</code> method sends a string packet with a preceding
         * plus '+' character.
         * @param s the string packet to send
         * @throws IOException if there is a problem communicating over the socket
         */
        void sendPacketOK(String s) throws IOException {
            sendPlus();
            sendPacket(s);
        }

        /**
         * The <code>sendPacket()</code> method sends a string packet over the socket, prepending
         * the '$' and computing the checksum of the packet
         * @param packet the string packet to send
         * @throws IOException if there is a problem communicating over the socket
         */
        void sendPacket(String packet) throws IOException {
            byte[] bytes = packet.getBytes();

            int cksum = 0;
            int cntr = 0;
            while (cntr < bytes.length) {
                cksum += bytes[cntr++];
            }

            String np = '$' +packet+ '#' +StringUtil.toLowHex(cksum & 0xff, 2);
            if (printer != null) {
                printer.println("   <-- "+np+"");
            }

            output.write(np.getBytes());
        }

        /**
         * The <code>readCommand()</code> method reads a command from the socket. The command
         * consists of an optional plus '+' signalling correct reception of the previous packet,
         * followed by a '$', the packet, then '#' and the one byte checksum as a two-character
         * hex value.
         * @return a string representation of the command
         * @throws IOException if there is a problem communicating over the socket
         */
        String readCommand() throws IOException {
                int i = input.read();
                if ( i < 0 ) return null;

                StringBuffer buf = new StringBuffer(32);
                buf.append((char)i);

                while (true) {
                    i = input.read();
                    if ( i < 0 ) return buf.toString();

                    buf.append((char)i);
                    if ( i == '#') {
                        int i2 = input.read();
                        int i3 = input.read();

                        if ( i2 >= 0 ) buf.append((char)i2);
                        if ( i3 >= 0 ) buf.append((char)i3);
                        return buf.toString();
                    }
                }
        }

        /**
         * The <code>ExceptionWatch</code> halts execution and signals GDB when an exceptional event occurs.
         *
         * @author Jey Kottalam (kottalam@cs.ucdavis.edu)
         */
        protected class ExceptionWatch extends Simulator.Watch.Empty {
            protected final String segment;

            protected ExceptionWatch(String s) {
                segment = s;
            }

            public void fireBeforeRead(State s, int address) {
                if (printer != null) {
                    printer.println("GDB caught invalid read of " + segment + " at " + address);
                }

                // send a SIGSEGV and halt execution
                commandLoop("T11");
            }

            public void fireBeforeWrite(State s, int address, byte val) {
                if (printer != null) {
                    printer.println("GDB caught invalid write of " + segment + " at " + address);
                }

                // send a SIGSEGV and halt execution
                commandLoop("T11");
            }
        }

        /**
         * The <code>StartupProbe</code> is a probe inserted at the beginning of the program that
         * will stop the simulation in order to wait for GDB to connect to Avrora.
         */
        protected class StartupProbe implements Simulator.Probe {
            public void fireBefore(State s, int pc) {
                if (printer != null) {
                    printer.println("--IN STARTUP PROBE @ "+StringUtil.addrToString(pc)+"--");
                }
                Terminal.println("GDBServer listening on port "+port+"...");
                Terminal.flush();
                try {
                    socket = serverSocket.accept();
                    input = socket.getInputStream();
                    output = socket.getOutputStream();
                    if ( printer != null )
                        printer.println("Connection established with: "+socket.getInetAddress().getCanonicalHostName());
                    serverSocket.close();
                } catch ( IOException e ) {
                    throw Util.failure("Unexpected IOException: "+e);
                }

                commandLoop(null);
            }

            public void fireAfter(State s, int pc) {
                // remove ourselves from the beginning of the program after it has started
                simulator.removeProbe(this, pc);
            }
        }

        /**
         * The <code>BreakpointProbe</code> is a probe inserted at a breakpoint that calls
         * the <code>commandLoop()</code> method before the target instruction is executed, thus
         * implementing a breakpoint.
         */
        protected class BreakpointProbe extends Simulator.Probe.Empty {
            public void fireBefore(State s, int pc) {
                if (printer != null)
                    printer.println("--IN BREAKPOINT PROBE @ "+StringUtil.addrToString(pc)+"--");
                // if we already hit a breakpoint then we dont need to hit the step probe too
                isStepping=false;
                commandLoop("T05");
            }
        }

        /**
         * The <code>StepProbe</code> class implements a probe that is used to step by a single
         * instruction. It calls the <code>commandLoop()</code> method after the target instruction
         * executes, thus stepping by only a single instruction.
         */
        protected class StepProbe implements Simulator.Probe {
            public void fireBefore(State s, int pc) {
                if (printer != null)
                    printer.println("--IN STEP PROBE @ "+StringUtil.addrToString(pc)+"--");
                if (isStepping){
                    isStepping=false;
                    commandLoop("T05");
                }
            }

            public void fireAfter(State s, int pc) {
                if (printer != null)
                    printer.println("--AFTER STEP PROBE @ "+StringUtil.addrToString(pc)+"--");
            }
        }

    }

    /**
     * The constructor for the <code>GDBServer</code> class simply creates a new instance that
     * is capable of creating monitors for simulators.
     */
    public GDBServer() {
        super(HELP);
    }

    /**
     * The <code>newMonitor()</code> method creates a new monitor for the given simulator. The
     * monitor waits for a connection from GDB and then implements the remote serial protocol,
     * allowing GDB to control the Avrora simulation.
     * @param s the simulator to create a monitor for
     * @return a new <code>Monitor</code> instance for the specified simulator
     */
    public Monitor newMonitor(Simulator s) {
        return new GDBMonitor(s, (int)PORT.get());
    }
}
