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
 * The <code>ProcedureMap</code> class represents a mapping from basic blocks to the procedures that contain
 * them. Built by the <code>ProcedureMapBuilder</code> class and accessible through the
 * <code>ControlFlowGraph.getProcedureMap()</code>, the <code>ProcedureMap</code>, the mapping is built by
 * first recognized static call sites and marking their target basic blocks as procedure entry points. Those
 * entrypoints are propagated through the control flow graph to all reachable basic blocks (ignoring call
 * edges). If a basic block is reachable through more than one procedure entry, it is considered shared.
 * <p/>
 * The result is a conservative approximation of which basic blocks are in which procedure. Given a basic
 * block, this class can look up the basic block which represents the entrypoint of that procedure. Also,
 * given the entrypoint of a procedure, the class can return a collection of the basic blocks that are
 * reachable from that entrypoint.
 * <p/>
 * Interrupt handlers are not considered the entrypoints of procedures.
 *
 * @author Ben L. Titzer
 * @see Program
 * @see ControlFlowGraph
 */
public class ProcedureMap {

    protected final HashSet entryPoints;
    protected final HashMap entryMap;
    protected final HashMap procMap;

    ProcedureMap(HashSet ep, HashMap em, HashMap pm) {
        entryPoints = ep;
        entryMap = em;
        procMap = pm;
    }

    /**
     * The <code>isInAnyProcedure()</code> method queries whether the specified basic block is reachable from
     * any procedure entry point in the program.
     *
     * @param b the basic block to query
     * @return true if this basic block is in one or more procedure; false otherwise
     */
    public boolean isInAnyProcedure(ControlFlowGraph.Block b) {
        return entryMap.get(b) != null;
    }

    /**
     * The <code>isSharedBetweenProcedures()</code> method queries whether the specified basic block is
     * reachable from more than one procedure entrypoint.
     *
     * @param b the basic block to query
     * @return true if this basic block is reachable from more than one procedure; false otherwise
     */
    public boolean isSharedBetweenProcedures(ControlFlowGraph.Block b) {
        Object o = entryMap.get(b);
        return o != null && !(o instanceof ControlFlowGraph.Block);
    }

    /**
     * The <code>getProcedureContaining()</code> method looks up the entrypoint of the procedure that contains
     * this basic block. If the block is not in any procedure, or if the block is shared by multiple
     * procedures, then this method returns null.
     *
     * @param b the basic block to find the procedure of
     * @return a reference to the unique <code>ControlFlowGraph.Block</code> instance that is the entrypoint
     *         of the procedure containing the specified basic block; null otherwise
     */
    public ControlFlowGraph.Block getProcedureContaining(ControlFlowGraph.Block b) {
        Object o = entryMap.get(b);
        if (o instanceof ControlFlowGraph.Block)
            return (ControlFlowGraph.Block)o;
        else
            return null;
    }

    /**
     * The <code>getProcedureBlocks()</code> method returns the collection of basic blocks contained in the
     * procedure with the specified entrypoint. If the block passed is not the entrypoint of any procedure,
     * this method will return null.
     *
     * @param entry the basic block representing the entrypoint of the procedure
     * @return a collection of the basic blocks contained in the procedure with the specified entrypoint
     */
    public Collection getProcedureBlocks(ControlFlowGraph.Block entry) {
        return (Collection)procMap.get(entry);
    }

    /**
     * The <code>getProcedureEntrypoints()</code> method returns a collection of basic blocks that are
     * entrypoints of procedures in the control flow graph.
     *
     * @return a collection of basic blocks that are entrypoints to procedures
     */
    public Collection getProcedureEntrypoints() {
        return entryPoints;
    }
}
