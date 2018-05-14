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

import avrora.Main;
import avrora.core.Program;
import avrora.stack.Analyzer;
import cck.util.Option;

/**
 * The <code>AnalyzeStackAction</code> class is an extension of the <code>Main.Action</code> class that allows
 * the stack tool to be reached from the command line.
 *
 * @author Ben L. Titzer
 */
public class AnalyzeStackAction extends Action {

    public static final String HELP = "The \"analyze-stack\" option invokes the built-in " +
            "stack analysis tool on the specified program. It uses an abstract interpretation " +
            "of the program to determine the possible interrupt masks at each program point " +
            "and determines the worst-case stack depth in the presence of interrupts.";

    public final Option.Bool MONITOR_STATES = newOption("monitor-states", false,
            "This option is used to monitor the progress of a long-running stack analysis problem. " +
            "The analyzer will report the count of states, edges, and propagation information " +
            "produced every 5 seconds. ");
    public final Option.Bool USE_ISEA = newOption("use-isea", false,
            "This option enables the use of information from inter-procedural side effect analysis " +
            "that may help in reducing the memory usage during state exploration, without affecting " +
            "stack analysis precision. When this option is enabled, the stack analyzer will consult the " +
            "ISEA analysis subsystem for each procedure call that it encounters in the program.");
    public final Option.Bool TRACE_SUMMARY = newOption("trace-summary", true,
            "This option is used to reduce the amount of output by summarizing the error trace" +
            "that yields the maximal stack depth. When true, the analysis will shorten the error " +
            "trace by not reporting edges between states of adjacent instructions that do not " +
            "change the stack height.");
    public final Option.Bool TRACE = newOption("trace", false,
            "This option causes the stack analyzer to print a trace of each abstract state " +
            "produced, every edge between states that is inserted, and all propagations " +
            "performed during the analysis. ");
    public final Option.Bool DUMP_STATE_SPACE = newOption("dump-state-space", false,
            "This option causes the stack analyzer to print a dump of all " +
            "the reachable abstract states in the state space, as well as all " +
            "edges between states. This can be used for a post-mortem analysis.");
    public final Option.Bool SHOW_PATH = newOption("show-path", false,
            "This option causes the stack analyzer to print out the execution path corresponding " +
            "to the maximal stack depth.");
    public final Option.Long RESERVE = newOption("reserve", 0,
            "This option is used for reserving a small portion of memory before the " +
            "analysis begins, in case the Java heap space is exhausted. This can happen " +
            "with very large analyses. By reserving some space up front, there is space " +
            "left so that post mortem graph analysis can be run. The units given are " +
            "megabytes.");

    /**
     * The default constructor of the <code>AnalyzeStackAction</code> class simply creates an empty instance
     * with the appropriate name and help string.
     */
    public AnalyzeStackAction() {
        super(HELP);
    }

    public static final int MEGABYTES = 1024 * 1024;

    /**
     * The <code>run()</code> method runs the stack analysis by loading the program from the command line
     * options specified, creating an instance of the <code>Analyzer</code> class, and running the analysis.
     *
     * @param args the string arguments that are the files containing the program
     * @throws Exception if the program cannot be loaded correctly
     */
    public void run(String[] args) throws Exception {
        Program p = Main.loadProgram(args);

        Analyzer.TRACE_SUMMARY = TRACE_SUMMARY.get();
        Analyzer.MONITOR_STATES = MONITOR_STATES.get();
        Analyzer.TRACE = TRACE.get();
        Analyzer.USE_ISEA = USE_ISEA.get();
        Analyzer.SHOW_PATH = SHOW_PATH.get();
        Analyzer.reserve = new byte[(int)(RESERVE.get() * MEGABYTES)];
        Analyzer a = new Analyzer(p);

        a.run();
        a.report();

        if (DUMP_STATE_SPACE.get())
            a.dump();
    }

}
