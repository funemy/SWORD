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

package avrora.core;

import java.util.*;

/**
 * The <code>ProcedureMapBuilder</code> class is used internally in the implementation of the
 * <code>ControlFlowGraph</code> and <code>ProcedureMap</code> classes to build the mapping between basic
 * blocks and procedures.
 *
 * @author Ben L. Titzer
 * @see ProcedureMap
 */
class ProcedureMapBuilder {

    private final Program program;
    private final ControlFlowGraph cfg;

    /**
     * The constructor for the <code>ProcedureMapBuilder</code> class creates a new instance for the
     * specified program. It will first call the <code>getCFG()</code> of the program to build the
     * control flow graph if it has not already been built.
     * @param p the program to create the procedure map for
     */
    public ProcedureMapBuilder(Program p) {
        program = p;
        cfg = p.getCFG();
    }

    /**
     * The <code>buildMap()</code> method builds the procedure map for the program that was specified
     * in the constructor and returns it.
     * @return a new instance of the <code>ProcedureMap</code> class that represents the mapping of basic
     * blocks to procedures in the program
     */
    public ProcedureMap buildMap() {
        discoverProcedures();
        HashMap procMap = buildProcedureBlockLists();
        return new ProcedureMap(ENTRYPOINTS, ENTRYMAP, procMap);
    }

    private HashSet ENTRYPOINTS;
    private HashMap ENTRYMAP;

    // this object marks a block as shared between two procedures
    private final Object SHARED = new Object();

    private void discoverProcedures() {

        discoverEntrypoints();
        ENTRYMAP = new HashMap();

        Iterator iter = ENTRYPOINTS.iterator();
        while (iter.hasNext()) {
            Object block = iter.next();
            ENTRYMAP.put(block, block);
        }

        iter = ENTRYPOINTS.iterator();
        while (iter.hasNext()) {
            ControlFlowGraph.Block block = (ControlFlowGraph.Block)iter.next();
            propagate(block, block, new HashSet());
        }

    }

    private void discoverEntrypoints() {
        ENTRYPOINTS = new HashSet();

        // discover edges that have incoming call edges
        Iterator edges = cfg.getEdgeIterator();
        while (edges.hasNext()) {
            ControlFlowGraph.Edge edge = (ControlFlowGraph.Edge)edges.next();
            if ("CALL".equals(edge.getType())) {
                if (edge.getTarget() == null) {
                    addIndirectEntrypoints(edge, cfg);
                } else
                    ENTRYPOINTS.add(edge.getTarget());
            }
        }
    }

    private void addIndirectEntrypoints(ControlFlowGraph.Edge edge, ControlFlowGraph cfg) {
        List l = program.getIndirectEdges(edge.getSource().getLastAddress());
        if (l == null) return;
        Iterator i = l.iterator();
        while (i.hasNext()) {
            int target_addr = ((Integer)i.next()).intValue();
            ControlFlowGraph.Block target = cfg.getBlockStartingAt(target_addr);
            if (target != null) {
                ENTRYPOINTS.add(target);
            }
        }
    }

    private void propagate(ControlFlowGraph.Block entry, ControlFlowGraph.Block block, HashSet seen) {
        if (ENTRYMAP.get(block) == SHARED) return;

        Iterator edges = block.getEdgeIterator();
        while (edges.hasNext()) {
            ControlFlowGraph.Edge edge = (ControlFlowGraph.Edge)edges.next();
            if ("CALL".equals(edge.getType())) continue;
            ControlFlowGraph.Block target = edge.getTarget();
            if (target == null) continue;
            mark(entry, target);

            if (!seen.contains(target)) {
                seen.add(target);
                propagate(entry, target, seen);
            } else {
                seen.add(target);
            }
        }
    }

    private void mark(ControlFlowGraph.Block entry, ControlFlowGraph.Block block) {
        Object c = ENTRYMAP.get(block);
        if (c == SHARED) return;
        if (c == null) c = entry;
        if (c != entry)
            markShared(block);
        else
            ENTRYMAP.put(block, entry);
    }

    private void markShared(ControlFlowGraph.Block block) {
        if (ENTRYMAP.get(block) == SHARED) return;
        ENTRYMAP.put(block, SHARED);

        Iterator edges = block.getEdgeIterator();
        while (edges.hasNext()) {
            ControlFlowGraph.Edge edge = (ControlFlowGraph.Edge)edges.next();
            if ("CALL".equals(edge.getType())) continue;
            ControlFlowGraph.Block target = edge.getTarget();
            if (target == null) continue;
            markShared(target);
        }
    }

    private HashMap buildProcedureBlockLists() {
        // maps procedure entry to list blocks in the procedure
        HashMap procMap = new HashMap();

        // create the initial map of entry points to empty lists
        Iterator entry_iter = ENTRYPOINTS.iterator();
        while (entry_iter.hasNext()) {
            ControlFlowGraph.Block entry = (ControlFlowGraph.Block)entry_iter.next();
            procMap.put(entry, new LinkedList());
        }

        // add each block to the list of its respective procedure
        Iterator block_iter = cfg.getBlockIterator();
        while (block_iter.hasNext()) {
            ControlFlowGraph.Block block = (ControlFlowGraph.Block)block_iter.next();
            Object mark = ENTRYMAP.get(block);
            if (mark == null || !(mark instanceof ControlFlowGraph.Block)) continue;
            ControlFlowGraph.Block entry = (ControlFlowGraph.Block)mark;
            LinkedList list = (LinkedList)procMap.get(entry);
            list.add(block);
        }
        return procMap;
    }


}
