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

import avrora.arch.legacy.LegacyInstr;
import cck.util.Util;
import java.util.*;

/**
 * The <code>ControlFlowGraph</code> represents a control flow graph for an entire program, including all
 * basic blocks and all procedures.
 *
 * @author Ben L. Titzer
 * @see Program
 * @see ProcedureMap
 */
public class ControlFlowGraph {

    /**
     * The <code>Edge</code> represents an edge leaving a basic block and (optionally) arriving at another,
     * known basic block. When the target is not know, for example in the case of an indirect call or branch,
     * then the accessor methods to this field return <code>null</code>. Each edge has a type that is
     * represented as a string.
     */
    public class Edge {
        private final String type;
        private final Block source;
        private final Block target;

        /**
         * The constructor for the <code>Edge</code> class creates a new edge with the specified type
         * with the specified source block and specified destination block.
         * @param t the type of the edge
         * @param s the source block of the edge
         * @param b the destination block of the edge
         */
        Edge(String t, Block s, Block b) {
            type = t;
            source = s;
            target = b;
        }

        /**
         * The <code>getType()</code> method returns the string name of the type of this edge. This type
         * denotes whether it is a call, return, or regular edge.
         *
         * @return the string name of the type of the edge
         */
        public String getType() {
            return type;
        }

        /**
         * The <code>getSource()</code> method returns the basic block that is the source of this edge. The
         * edge is always from the last instruction in the basic block.
         *
         * @return the basic block that is the source of this edge
         */
        public Block getSource() {
            return source;
        }

        /**
         * The <code>getTarget()</code> method returns the known target of this control flow graph edge, if it
         * is known. In the case of indirect calls, branches, or a return, the target block is not known--in
         * that case, this method returns <code>null</code>.
         *
         * @return the basic block that is the target of this edge if it is known; null otherwise
         */
        public Block getTarget() {
            return target;
        }
    }

    /**
     * The <code>Block</code> class represents a basic block of code within the program. A basic block
     * contains a straight-line piece of code that ends with a control instruction (e.g. a skip, a jump, a
     * branch, or a call) or an implicit fall-through to the next basic block. It contains at most two
     * references to successor basic blocks.
     * <p/>
     * For <b>fallthroughs</b> (no ending control instruction), the <code>next</code> field refers to the
     * block immediately following this block, and the <code>other</code> field is null.
     * <p/>
     * For <b>jumps</b>, the <code>other</code> field refers to the block that is the target of the jump, and
     * the <code>next</code> field is null.
     * <p/>
     * For <b>skips</b>, <b>branches</b>, and <b>calls</b>, the <code>next</code> field refers to the block
     * immediately following this block (i.e. not-taken for branches, the return address for calls). The
     * <code>other</code> field refers to the target address of the branch if it is taken or the address to be
     * called.
     * <p/>
     * For <b>indirect jumps</b> both the <code>next</code> and <code>other</code> fields are null.
     * <p/>
     * For <b>indirect calls</b> the <code>next</code> field refers to the block immediately following this
     * block (i.e. the return address). The <code>other</code> field is null because the target of the call
     * cannot be known.
     */
    public class Block {

        private final int address;
        private int last_address;
        private int size;
        private int length;

        private final List instructions;
        private final List edges;

        Block(int addr) {
            address = addr;
            last_address = address;
            instructions = new LinkedList();
            edges = new LinkedList();
        }


        /**
         * The <code>addInstr()</code> method adds an instruction to the end of this basic block. It is not
         * recommended for general use: it is generally used by the <code>CFGBuilder</code> class. No
         * enforcement of invariants is made: this method does not check whether the instruction being added
         * changes the control flow or branches to another block, etc.
         *
         * @param i the instruction to add to this basic block
         */
        public void addInstr(LegacyInstr i) {
            instructions.add(i);

            last_address = address + size;
            size += i.getSize();
            length++;
        }

        /**
         * The <code>hashCode()</code> method computes the hash code of this block. In the initial
         * implementation, the hash code is simply the byte address of the block
         *
         * @return an integer value that is the hash code of this object
         */
        public int hashCode() {
            return address;
        }

        /**
         * The <code>equals()</code> method computes object equality for basic blocks. It simply compares the
         * addresses of the basic blocks and returns true if they match.
         *
         * @param o the other object to compare to
         * @return true if these two basic blocks are equivalent; false otherwise
         */
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Block)) return false;
            return ((Block)o).address == this.address;
        }

        /**
         * The <code>getAddress()</code> method gets the starting byte address of this basic block.
         *
         * @return the starting address of this basic block
         */
        public int getAddress() {
            return address;
        }

        /**
         * The <code>getLastAddress()</code> gets the last address that this block covers.
         * @return the last address that this block covers
         */
        public int getLastAddress() {
            return last_address;
        }

        /**
         * The <code>getSize()</code> method returns the size of the basic block in bytes.
         *
         * @return the number of bytes in this basic block
         */
        public int getSize() {
            return size;
        }

        /**
         * The <code>getLength()</code> returns the length of this basic block in terms of the number of
         * instructions
         *
         * @return the number of instructions in this basic block
         */
        public int getLength() {
            return length;
        }

        /**
         * The <code>getInstrIterator()</code> method returns an iterator over the instructions in this basic
         * block. The resulting iterator can be used to iterate over the instructions in the basic block in
         * order.
         *
         * @return an iterator over the instructions in this block.
         */
        public Iterator getInstrIterator() {
            return instructions.iterator();
        }

        public Iterator getEdgeIterator() {
            return edges.iterator();
        }
    }

    private static class BlockComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Block b1 = (Block)o1;
            Block b2 = (Block)o2;

            return b1.address - b2.address;
        }
    }

    /**
     * The <code>blocks</code> field contains a reference to a map from <code>Integer</code> to
     * <code>Block</code> this map is used to lookup the basic block that starts at a particular address.
     */
    protected final HashMap blocks;

    /**
     * The <code>edges</code> field contains a reference to the list of edges (instances of class
     * <code>Edge</code>) within this control flow graph.
     */
    protected final List allEdges;

    /**
     * The <code>program</code> field stores a reference to the program to which this control flow graph
     * corresponds.
     */
    protected final Program program;

    /**
     * The <code>COMPARATOR</code> field stores a comparator that is used in sorting basic blocks by program
     * order.
     */
    public static final Comparator COMPARATOR = new BlockComparator();


    /**
     * The constructor for the <code>ControlFlowGraph</code> initializes this control flow graph with the
     * given program. It does not build the actual control flow graph-- a <code>CFGBuilder</code> instance
     * does that.
     *
     * @param p the program to create the control flow graph for
     */
    ControlFlowGraph(Program p) {
        program = p;
        blocks = new HashMap();
        allEdges = new LinkedList();
    }

    /**
     * The <code>newBlock()</code> method creates a new block within the control flow graph, starting at the
     * specified address. No checking is done by this method as to whether the address overlaps with another
     * block. This is primarily intended for use within the <code>CFGBuilder</code> class.
     *
     * @param address the byte address at which this block begins
     * @return an instance of <code>Block</code> representing the new block
     */
    public Block newBlock(int address) {
        Block b = new Block(address);
        blocks.put(new Integer(address), b);
        return b;
    }

    /**
     * The <code>addEdge()</code> method adds an edge between two blocks with a given type. If the destination
     * block is null, then the edge has an unknown target.
     *
     * @param s    the source block of the edge
     * @param t    the target block of the edge
     * @param type the string name of the type of the edge, e.g. CALL or RETURN
     */
    public void addEdge(Block s, Block t, String type) {
        Edge edge = new Edge(type, s, t);
        s.edges.add(edge);
        allEdges.add(edge);
    }

    /**
     * The <code>addEdge()</code> method adds an edge between two blocks. If the destination block is null,
     * then the edge has an unknown target.
     *
     * @param s the source block of the edge
     * @param t the target block of the edge
     */
    public void addEdge(Block s, Block t) {
        Edge edge = new Edge("", s, t);
        s.edges.add(edge);
        allEdges.add(edge);
    }

    /**
     * The <code>getBlockStartingAt()</code> method looks up a basic block based on its starting address. If a
     * basic block contains the address, but does not begin at that address, that basic block is ignored.
     *
     * @param address the byte address at which the block begins
     * @return a reference to the <code>Block</code> instance that starts at the address specified, if such a
     *         block exists; null otherwise
     */
    public Block getBlockStartingAt(int address) {
        return (Block)blocks.get(new Integer(address));
    }

    /**
     * The <code>getBlockContaining()</code> method looks up the basic block that contains the address
     * specified. The basic blocks are assumed to not overlap.
     *
     * @return a reference to the <code>Block</code> instance that contains the address specified, if such a
     *         block exists; null otherwise
     */
    public Block getBlockContaining(int address) {
        throw Util.unimplemented();
    }

    /**
     * The <code>getBlockIterator()</code> method constructs an interator over all of the blocks in the
     * control flow graph, regardless of connectivity. No order is guaranteed.
     *
     * @return an instance of <code>Iterator</code> that can be used to iterate over all blocks in the control
     *         flow graph
     */
    public Iterator getBlockIterator() {
        return blocks.values().iterator();
    }

    /**
     * The <code>getBlockIterator()</code> method constructs an interator over all of the blocks in the
     * control flow graph, regardless of connectivity. The order is guaranteed to be in ascending order.
     *
     * @return an instance of <code>Iterator</code> that can be used to iterate over all blocks in the control
     *         flow graph in ascending order
     */
    public Iterator getSortedBlockIterator() {
        List l = Collections.list(Collections.enumeration(blocks.values()));
        Collections.sort(l, COMPARATOR);
        return l.iterator();
    }

    /**
     * The <code>getEdgeIterator()</code> method returns an interator over all edges between all blocks within
     * this control flow graph.
     *
     * @return an instance of <code>Iterator</code> that iterates over the edges of this control flow graph.
     */
    public Iterator getEdgeIterator() {
        return allEdges.iterator();
    }

    private ProcedureMap pmap;

    /**
     * The <code>getProcedureMap()</code> method returns a reference to a <code>ProcedureMap</code> instance
     * that maps basic blocks to the procedures in which they are contained
     *
     * @return a reference to a <code>ProcedureMap</code> instance for this control flow graph
     */
    public synchronized ProcedureMap getProcedureMap() {
        if (pmap == null) {
            pmap = new ProcedureMapBuilder(program).buildMap();
        }
        return pmap;
    }
}
