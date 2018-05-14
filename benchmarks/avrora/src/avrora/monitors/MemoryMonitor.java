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

import avrora.actions.SimAction;
import avrora.arch.avr.AVRProperties;
import avrora.arch.legacy.LegacyState;
import avrora.core.Program;
import avrora.core.SourceMapping;
import avrora.sim.Simulator;
import avrora.sim.mcu.Microcontroller;
import avrora.sim.util.MemoryProfiler;
import cck.text.*;
import cck.util.Option;
import java.util.Iterator;
import java.util.List;

/**
 * The <code>MemoryMonitor</code> class implements a monitor that collects information about how the program
 * accesses the data memory over its execution. For each RAM address it keeps an account of the number of
 * reads and the number of writes and reports that information after the program is completed.
 *
 * @author Ben L. Titzer
 */
public class MemoryMonitor extends MonitorFactory {

    public final Option.List LOCATIONS = newOptionList("locations", "",
            "This option, when set, specifies a list of memory locations to instrument. When " +
            "this option is not specified, the monitor will instrument all reads and writes to " +
            "memory.");

    public final Option.Bool LOWER_ADDRESS = newOption("low-addresses", false,
            "When this option is enabled, the memory monitor will be inserted for lower addresses, " +
            "recording reads and writes to the general purpose registers on the AVR and also IO registers " +
            "through direct and indirect memory reads and writes.");

    public class Monitor implements avrora.monitors.Monitor {
        public final Simulator simulator;
        public final Microcontroller microcontroller;
        public final Program program;
        public final int ramsize;
        public final int memstart;
        public final MemoryProfiler memprofile;

        Monitor(Simulator s) {
            simulator = s;
            microcontroller = simulator.getMicrocontroller();
            program = simulator.getProgram();
            AVRProperties p = (AVRProperties)microcontroller.getProperties();
            ramsize = p.sram_size + p.ioreg_size + LegacyState.NUM_REGS;
            if ( LOWER_ADDRESS.get() ) {
                memstart = 0;
            } else {
                memstart = LegacyState.IOREG_BASE + p.ioreg_size;
            }
            memprofile = new MemoryProfiler(ramsize);

            insertWatches();
        }

        private void insertWatches() {

            if (!LOCATIONS.get().isEmpty() ) {
                // instrument only the locations specified
                List l = LOCATIONS.get();
                List loc = SimAction.getLocationList(program, l);
                Iterator i = loc.iterator();
                while ( i.hasNext() ) {
                    SourceMapping.Location location = (SourceMapping.Location)i.next();
                    simulator.insertWatch(memprofile, location.vma_addr - 0x800000);
                }
            } else {
                // instrument the entire memory
                for (int cntr = memstart; cntr < ramsize; cntr++) {
                    simulator.insertWatch(memprofile, cntr);
                }
            }
        }

        public void report() {
            TermUtil.printSeparator("Memory profiling results for node "+simulator.getID());
            Terminal.printGreen("   Address     Reads               Writes");
            Terminal.nextln();
            TermUtil.printThinSeparator(Terminal.MAXLINE);
            double rtotal = 0;
            long[] rcount = memprofile.rcount;
            double wtotal = 0;
            long[] wcount = memprofile.wcount;
            int imax = rcount.length;

            // compute the total for percentage calculations
            for (int cntr = 0; cntr < imax; cntr++) {
                rtotal += rcount[cntr];
                wtotal += wcount[cntr];
            }

            int zeroes = 0;

            for (int cntr = memstart; cntr < imax; cntr++) {
                long r = rcount[cntr];
                long w = wcount[cntr];

                if (r == 0 && w == 0)
                    zeroes++;
                else
                    zeroes = 0;

                // skip long runs of zeroes
                if (zeroes == 2) {
                    Terminal.println("                   .                    .");
                    continue;
                } else if (zeroes > 2) continue;

                String addr = StringUtil.addrToString(cntr);
                printLine(addr, r, rtotal, w, wtotal);

            }
            printLine("total ", (long)rtotal, rtotal, (long)wtotal, wtotal);
            Terminal.nextln();
        }

        private void printLine(String addr, long r, double rtotal, long w, double wtotal) {
            String rcnt = StringUtil.rightJustify(r, 8);
            float rpcnt = (float)(100 * r / rtotal);
            String rpercent = StringUtil.rightJustify(StringUtil.toFixedFloat(rpcnt, 4),8) + " %";

            String wcnt = StringUtil.rightJustify(w, 8);
            float wpcnt = (float)(100 * w / wtotal);
            String wpercent = StringUtil.rightJustify(StringUtil.toFixedFloat(wpcnt, 4),8) + " %";

            Terminal.printGreen("    " + addr);
            Terminal.print(": ");
            Terminal.printBrightCyan(rcnt);
            Terminal.print(' ' + ("  " + rpercent));
            Terminal.printBrightCyan(wcnt);
            Terminal.println(' ' + ("  " + wpercent));
        }

    }

    public MemoryMonitor() {
        super("The \"memory\" monitor collects information about the " +
                "memory usage statistics of the program, including the number " +
                "of reads and writes to every byte of data memory.");
    }

    public avrora.monitors.Monitor newMonitor(Simulator s) {
        return new Monitor(s);
    }
}
