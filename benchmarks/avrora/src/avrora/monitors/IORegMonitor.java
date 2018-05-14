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

import avrora.arch.avr.AVRProperties;
import avrora.arch.legacy.LegacyState;
import avrora.sim.Simulator;
import avrora.sim.State;
import avrora.sim.output.SimPrinter;
import avrora.sim.mcu.Microcontroller;
import cck.text.StringUtil;
import cck.util.Option;
import java.util.Iterator;

/**
 * The <code>IORegMonitor</code> is a simple tracing mechanism that allows reads and writes of IO registers
 * to be displayed to the user as the program performs them.
 *
 * @author Ben L. Titzer
 */
public class IORegMonitor extends MonitorFactory {

    final Option.List IOREGS = newOptionList("ioregs", "all",
            "This option accepts a list of IO register names which will be monitored during the " +
            "simulation. For example, specifying \"PORTA,DDR\" as this option's value will enable monitoring " +
            "of reads and writes to the PORTA and DDRA IO registers. " +
            "If the string \"all\" is specified as one of the items of the list, then all " +
            "IO registers will be monitored.");

    class Monitor implements avrora.monitors.Monitor {
        SimPrinter printer;

        Monitor(Simulator s) {
            printer = s.getPrinter();
            insertWatches(s);
        }

        private void insertWatches(Simulator s) {
            Microcontroller m = s.getMicrocontroller();
            if ( IOREGS.get().contains("all"))
                insertAllWatches(m, s);
            else
                insertSingleWatches(m, s);
        }

        private void insertAllWatches(Microcontroller m, Simulator s) {
            AVRProperties props = (AVRProperties)m.getProperties();
            int size = props.ioreg_size;
            for ( int cntr = 0; cntr < size; cntr++ ) {
                String name = props.getIORegName(cntr);
                if ( name == null ) name = StringUtil.to0xHex(cntr,2);
                s.insertWatch(new Watch(name), cntr + LegacyState.IOREG_BASE);
            }
        }

        private void insertSingleWatches(Microcontroller m, Simulator s) {
            Iterator i = IOREGS.get().iterator();
            while ( i.hasNext() ) {
                String str = (String)i.next();
                int ior;
                if ( StringUtil.isHex(str) )
                    ior = StringUtil.evaluateIntegerLiteral(str) + LegacyState.IOREG_BASE;
                else
                    ior = m.getProperties().getIORegAddr(str);
                s.insertWatch(new Watch(str), ior);
            }
        }

        public void report() {
            // we don't need to generate a report.
        }

        class Watch extends Simulator.Watch.Empty {
            String name;

            Watch(String name) {
                this.name = StringUtil.leftJustify(name, 6);
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
                printer.println(name+"    <=   "+render(value));
            }

            public void fireAfterRead(State state, int data_addr, byte val) {
                printer.println(name+"      -> "+render(val));
            }

            private String render(byte value) {
                return StringUtil.toHex(value, 2)+" "+StringUtil.toBin(value, 8);
            }
        }
    }

    public IORegMonitor() {
        super("This \"ioregs\" monitor tracks the updates to IO registers on the microcontroller, " +
                "including IO registers corresponding to devices such as the timer, UART, SPI, etc.");
    }

    public avrora.monitors.Monitor newMonitor(Simulator s) {
        return new Monitor(s);
    }
}
