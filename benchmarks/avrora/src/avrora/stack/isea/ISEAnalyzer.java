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

package avrora.stack.isea;

import avrora.core.*;
import cck.text.*;
import cck.util.Util;
import java.util.*;

/**
 * The <code>ISEAnalyzer</code> class is a static analyzer for machine code. This class
 * implements intra-procedural side-effect analysis. This determines the set of registers
 * read and modified by this procedure locally; this information can be used to reduce
 * the amount of work that other static analyzers must do when they analyze code, because
 * they can use the results of this analysis to know which registers' values are used, and
 * which registers are unmodified by a procedure.
 *
 * @author Ben L. Titzer
 */
public class ISEAnalyzer implements ISEInterpreter.SummaryCache {

    protected final Program program;
    protected final SourceMapping smap;
    protected final ControlFlowGraph cfg;
    protected final ProcedureMap pmap;
    protected final HashMap procedureSummaries;
    protected final HashMap returnSummaries;
    protected final Stack stack;

    protected final Verbose.Printer printer = Verbose.getVerbosePrinter("analysis.isea");

    public ISEAnalyzer(Program p) {
        program = p;
        smap = p.getSourceMapping();
        cfg = program.getCFG();
        pmap = cfg.getProcedureMap();
        procedureSummaries = new HashMap();
        returnSummaries = new HashMap();
        stack = new Stack();
    }

    class Item {
        ControlFlowGraph.Block block;
        Item next;

        Item(ControlFlowGraph.Block b) {
            block = b;
        }
    }

    public ISEState getProcedureSummary(int start) {
        ControlFlowGraph.Block block = cfg.getBlockStartingAt(start);
        if ( block == null ) {
            throw Util.failure("cannot get procedure summary for address: "+StringUtil.addrToString(start));
        }
        analyzeProcedure(block);
        return (ISEState)procedureSummaries.get(block);
    }

    public void recordReturnSummary(int retaddr, ISEState rs) {
        ISEState ors = getReturnSummary(retaddr);
        if ( ors == null ) {
            ors = rs.dup();
            returnSummaries.put(new Integer(retaddr), ors);
        } else {
            ors.merge(rs);
        }
    }

    public ISEState getReturnSummary(int retaddr) {
        return (ISEState)returnSummaries.get(new Integer(retaddr));
    }

    public void analyze() {
        HashSet seen = new HashSet();
        Item head;
        Item tail = head = new Item(cfg.getBlockStartingAt(0x0000));

        while ( head != null ) {
            ControlFlowGraph.Block block = head.block;
            if ( printer.enabled ) {
                Terminal.println("looking at block "+getBlockName(block));
            }
            seen.add(block);
            if ( pmap.getProcedureEntrypoints().contains(block) ) {
                // this is a procedure entrypoint
                analyzeProcedure(block);
            } else {
                // otherwise, look for successor blocks
                Iterator i = block.getEdgeIterator();
                while ( i.hasNext() ) {
                    ControlFlowGraph.Edge e = (ControlFlowGraph.Edge)i.next();
                    ControlFlowGraph.Block target = e.getTarget();
                    tail = addToWorkList(seen, target, tail);
                }

                // are there any indirect edges that we should know about?
                int lastAddr = block.getLastAddress();
                List list = program.getIndirectEdges(lastAddr);
                if ( list != null ) {
                    Iterator iei = list.iterator();
                    while ( iei.hasNext() ) {
                        // add the indirect target to the work list
                        int taddr = ((Integer)iei.next()).intValue();
                        tail = addToWorkList(seen, cfg.getBlockStartingAt(taddr), tail);
                    }
                }
            }

            head = head.next;
        }
    }

    private Item addToWorkList(HashSet seen, ControlFlowGraph.Block target, Item tail) {
        if ( target == null ) return tail;
        if ( !seen.contains(target) ) {
            // if we haven't seen this block before, add to worklist
            tail.next = new Item(target);
            tail = tail.next;
        }
        return tail;
    }

    public void analyzeProcedure(ControlFlowGraph.Block start) {
        // first check the procedure summary cache
        if ( procedureSummaries.containsKey(start) ) return;
        if ( printer.enabled ) {
            printStart(start);
        }
        if ( stack.contains(start) ) {
            throw Util.failure("program contains recursion");
        }
        stack.push(start);
        ISEState rs = new ISEInterpreter(program, this).analyze(start.getAddress());
        procedureSummaries.put(start, rs);
        stack.pop();
    }

    private void printStart(ControlFlowGraph.Block start) {
        int size = stack.size();
        String indent = StringUtil.dup('=', 4*size+3);
        Terminal.print(Terminal.COLOR_MAGENTA, indent+">");
        Terminal.print(Terminal.COLOR_PURPLE, " ISE: Analyzing procedure ");
        Terminal.printBrightCyan(getBlockName(start));
        Terminal.nextln();
    }

    private String getBlockName(ControlFlowGraph.Block start) {
        int address = start.getAddress();
        return "("+smap.getName(address)+": "+ StringUtil.addrToString(address)+")";
    }

    public void analyze(int loc) {
        ISEState s = new ISEInterpreter(program, this).analyze(loc);
        TermUtil.printSeparator();
        if ( s != null ) {
            Terminal.printRed("RETURN STATE");
            Terminal.nextln();
            s.print(loc);
        } else {
            Terminal.printRed("PROCEDURE DOES NOT RETURN");
            Terminal.nextln();
        }
    }
}
