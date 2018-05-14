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

package avrora.stack;

import avrora.arch.legacy.LegacyInstr;
import avrora.arch.legacy.LegacyRegister;
import avrora.core.Program;
import avrora.stack.isea.*;
import cck.stat.Distribution;
import cck.text.*;
import cck.util.Util;
import cck.util.TimeUtil;
import java.util.*;

/**
 * The <code>Analyzer</code> class implements the analysis phase that determines the transition relation
 * between the states in the abstract state space. It is modelled on the simulator, but only does abstract
 * interpretation rather than executing the entire program.
 *
 * @author Ben L. Titzer
 */
public class Analyzer {

    public static boolean MONITOR_STATES;
    public static boolean TRACE_SUMMARY;
    public static boolean USE_ISEA;
    public static boolean SHOW_PATH;

    protected final Verbose.Printer printer = Verbose.getVerbosePrinter("analyzer.stack");

    protected final Program program;
    protected final StateTransitionGraph graph;
    final ContextSensitivePolicy policy;
    final AbstractInterpreter interpreter;
    protected int retCount;
    protected int retiCount;
    protected int newRetCount;
    protected int newEdgeCount;

    protected StateTransitionGraph.StateList newReturnStates;
    protected StateTransitionGraph.EdgeList newEdges;

    long buildTime;
    long traverseTime;
    boolean unbounded;
    Path maximalPath;
    ISEAnalyzer isea;

    public static final int NORMAL_EDGE = 0;
    public static final int PUSH_EDGE = 1;
    public static final int POP_EDGE = 2;
    public static final int CALL_EDGE = 3;
    public static final int INT_EDGE = 4;
    public static final int RET_EDGE = 5;
    public static final int RETI_EDGE = 6;
    public static final int SPECIAL_EDGE = 7;

    public static final int NORMAL_STATE = 0;
    public static final int RET_STATE = 1;
    public static final int RETI_STATE = 2;

    public static final String[] EDGE_NAMES = {"", "PUSH", "POP", "CALL", "INT", "RET", "RETI", "SPECIAL"};
    public static final int[] EDGE_DELTA = {0, 1, -1, 2, 2, 0, 0, 0};

    public static boolean TRACE;
    public static boolean running;

    public static byte[] reserve;

    public Analyzer(Program p) {
        program = p;
        graph = new StateTransitionGraph(p);
        policy = new ContextSensitivePolicy();
        interpreter = new AbstractInterpreter(program, policy);
        if ( USE_ISEA ) {
            isea = new ISEAnalyzer(program);
            isea.analyze();
        }
    }

    /**
     * The <code>run()</code> method begins the analysis. The entrypoint of the program with an initial
     * default state serves as the first state to start exploring.
     */
    public void run() {
        running = true;
        MonitorThread t = null;
        if (MONITOR_STATES) {
            t = new MonitorThread();
            t.start();
        }

        long start = System.currentTimeMillis();
        try {
            buildReachableStateSpace();
            long check = System.currentTimeMillis();
            buildTime = check - start;
            findMaximalPath();
            traverseTime = System.currentTimeMillis() - check;
        } catch (OutOfMemoryError ome) {
            // free the reserved memory
            reserve = null;
            long check = System.currentTimeMillis();
            buildTime = check - start;
            outOfMemory();
            graph.deleteStateSets();
        } finally {
            running = false;
        }

    }

    private void outOfMemory() {
        running = false;

        Terminal.nextln();
        Terminal.printRed("Stack Analyzer Error");
        Terminal.println(": out of memory");

        printStatHeader();
        printStats();

        analyzeAggregationPoints();
        analyzeStates();
    }

    private void analyzeAggregationPoints() {
        Iterator i = graph.getStateCache().getStateIterator();
        Distribution sizeDist = new Distribution("Set Size Statistics", "Number of sets",
                "Aggregate size", "Distribution of Set Size");
        while (i.hasNext()) {
            StateCache.State state = (StateCache.State)i.next();
            StateCache.Set stateSet = state.info.stateSet;
            int size = stateSet == null ? 0 : stateSet.size();
            sizeDist.record(size);
        }
        sizeDist.process();
        sizeDist.print(printer);
    }

    private void analyzeStates() {
        Iterator i = graph.getStateCache().getStateIterator();
        Distribution pcDist = new Distribution("Distribution of program states over PC", "Number of unique instructions", null, "Distribution");
        while (i.hasNext()) {
            StateCache.State s = (StateCache.State)i.next();
            pcDist.record(s.getPC());

        }
        pcDist.process();
        pcDist.print(printer);
    }

    /**
     * The <code>MonitorThread</code> class represents a thread instance that constantly monitors the progress
     * of the stack analysis and reports on the number of states explored, edges inserted, states on the
     * frontier, as well statistics about the propagation phase.
     */
    protected class MonitorThread extends Thread {

        /**
         * The <code>run()</code> method simply loops while the analysis is running. Every five seconds it
         * reports the number of states, edges, frontier states, propagations, etc.
         */
        public void run() {
            int cntr = 0;
            try {
                while (running) {
                    sleep(5000);
                    if (!running) break;
                    if (cntr % 10 == 0) {
                        printStatHeader();
                    }

                    printStats();

                    cntr++;
                }
            } catch (InterruptedException e) {
                throw Util.unexpected(e);
            }
        }

    }

    private long numSets;
    private long numElems;

    private void printStats() {
        print_just_9(graph.getFrontierCount());
        print_just_9(graph.getExploredCount());
        print_just_9(graph.getEdgeCount());
        countAggregElems();
        print_just_9(numSets);
        print_just_12(numElems);
        String s = Long.toString(retCount) + '/' + Long.toString(retiCount);
        Terminal.print(StringUtil.rightJustify(s, 12));
        print_just_9(newRetCount);
        print_just_9(newEdgeCount);
        Terminal.nextln();
    }

    private void countAggregElems() {
        numElems = 0;
        numSets = 0;
    }

    private void printStatHeader() {
        Terminal.printBrightGreen(" Frontier Explored    Edges   Aggreg       Elems      ret(i)   Pend-R   Pend-E");
        Terminal.nextln();
        TermUtil.printSeparator(88);
    }

    private void print_just_9(long val) {
        String s = StringUtil.rightJustify(val, 9);
        Terminal.print(s);
    }

    private void print_just_12(long val) {
        String s = StringUtil.rightJustify(val, 12);
        Terminal.print(s);
    }

    /**
     * The <code>buildReachableStateSpace()</code> method starts at the eden state of the analysis,
     * maintaining a list of frontier states. It then builds the state space using the frontier states as a
     * work list. When the work list becomes null, it propagates calling states to their reachable return
     * states and inserts return edges to (possibly unexplored) states. After the propagation phase, it
     * processes any new frontier states. The analysis continues until there are no new frontier states and
     * there are no new propagations to be performed.
     */
    protected void buildReachableStateSpace() {
        StateCache.State s = graph.getNextFrontierState();

        while (true) {
            if (s != null) {
                processFrontierState(s);
            } else if (newReturnStates != null) {
                processNewReturns();
            } else if (newEdges != null) {
                processNewEdges();
            } else {
                break;
            }
            s = graph.getNextFrontierState();
        }
    }

    /**
     * The <code>processPropagationList()</code> method walks through a list of target/caller state pairs,
     * propagating callers to return states. When return states are encountered, return edges between the
     * caller and new return state are inserted and any new return states are pushed onto the frontier.
     */
    protected void processNewReturns() {
        while (newReturnStates != null) {
            StateCache.State state = newReturnStates.state;
            propagateOneBackwards(state, state, state.copy(), new Object());
            newReturnStates = newReturnStates.next;
            newRetCount--;
        }
    }

    protected void processNewEdges() {
        while (newEdges != null) {
            StateTransitionGraph.Edge edge = newEdges.edge;
            newEdges = newEdges.next;
            StateCache.Set set = edge.target.info.stateSet;
            if (set != null && !set.isEmpty())
                propagateSetBackwards(edge.source, edge.target.info.stateSet, new Object());
            newEdgeCount--;
        }
    }

    // propagate return state back through the graph to callers
    private void propagateOneBackwards(StateCache.State t, StateCache.State rt, MutableState copy, Object mark) {
        // visited this node already?
        if (t.mark == mark) return;
        t.mark = mark;

        StateTransitionGraph.StateInfo info = t.info;
        if (info.stateSet == null)
            info.stateSet = graph.newSet();
        else if (info.stateSet.contains(rt))
            return;

        info.stateSet.add(rt);

        for (StateTransitionGraph.Edge edge = info.backwardEdges; edge != null; edge = edge.backwardLink) {

            if (edge.type == CALL_EDGE) {
                // found a call edge: we need to connect this caller to a new return state
                insertReturnEdge(edge.source, copy, rt.getType() == RETI_STATE);
            } else {
                // propagate this return state backwards
                propagateOneBackwards(edge.source, rt, copy, mark);
            }
        }
    }

    // propagate return state back through the graph to callers
    private void propagateSetBackwards(StateCache.State t, StateCache.Set rset, Object mark) {
        // visited this node already?
        if (t.mark == mark) return;
        t.mark = mark;

        StateTransitionGraph.StateInfo info = t.info;
        if (info.stateSet == null)
            info.stateSet = graph.newSet();
        else if (info.stateSet.containsAll(rset))
            return;

        for (StateTransitionGraph.Edge edge = info.backwardEdges; edge != null; edge = edge.backwardLink) {

            if (edge.type == CALL_EDGE) {
                // found a call edge: we need to connect this caller to the new return states
                insertReturnEdges(edge.source, edge.target.info.stateSet, rset);
            } else {
                // propagate these calls to all children
                propagateSetBackwards(edge.source, rset, mark);
            }
        }

        info.stateSet.addAll(rset);
    }

    private void insertReturnEdges(StateCache.State caller, StateCache.Set prev, StateCache.Set rset) {
        Iterator i = rset.iterator();
        while (i.hasNext()) {
            Object o = i.next();
            if (!prev.contains(o)) {
                StateCache.State rs = (StateCache.State)o;
                insertReturnEdge(caller, rs.copy(), rs.getType() == RETI_STATE);
            }
        }
    }

    private void insertReturnEdge(StateCache.State caller, MutableState rstate, boolean reti) {
        int cpc = caller.getPC();
        int npc;

        if ( isea != null ) {
            ISEState rs = isea.getReturnSummary(rstate.getPC());
            if ( rs != null ) {
                mergeReturnStateIntoCaller(caller, rstate, rs);
            }
        }

        if (reti) {
            npc = cpc;
            rstate.setFlag_I(AbstractArithmetic.TRUE);
        } else {
            npc = program.getNextPC(cpc);
        }

        rstate.setPC(npc);

        int type = reti ? RETI_EDGE : RET_EDGE;
        policy.addEdge(caller, type, rstate);
    }

    private void mergeReturnStateIntoCaller(StateCache.State caller, MutableState rstate, ISEState rs) {
        for ( int cntr = 0; cntr < MutableState.NUM_REGS; cntr++ ) {
            LegacyRegister reg = LegacyRegister.getRegisterByNumber(cntr);
            // get the value that is in the return state
            char retval = rstate.getRegisterAV(reg);
            // interpret the ISE abstract value: is it a new value or an old value?
            char av = interpret(caller, rs.getRegister(reg), retval);
            rstate.setRegisterAV(reg, av);
        }

        mergeReturnIORegister(IORegisterConstants.SREG, caller, rstate, rs);
        mergeReturnIORegister(IORegisterConstants.EIMSK, caller, rstate, rs);
        mergeReturnIORegister(IORegisterConstants.TIMSK, caller, rstate, rs);
    }

    private void mergeReturnIORegister(int ior, StateCache.State caller, MutableState rstate, ISEState rs) {
        char retval = rstate.getIORegisterAV(ior);
        char av = interpret(caller, rs.readIORegister(ior), retval);
        rstate.setIORegisterAV(ior, av);
    }

    private char interpret(StateCache.State caller, byte rsval, char defval) {
        // if the ISE value refers to the value of a register before the call, return it from the caller state
        LegacyRegister reg = ISEValue.asRegister(rsval);
        if ( reg != null ) return caller.getRegisterAV(reg);
        // if the ISE value refers to the value of an IO register before the call, return it from the caller state
        int ior = ISEValue.asIORegister(rsval);
        if ( ior > 0 ) return caller.getIORegisterAV(ior);
        // otherwise, return the default (value in the return state)
        return defval;
    }

    private void postNewEdge(StateTransitionGraph.Edge edge) {
        newEdges = new StateTransitionGraph.EdgeList(edge, newEdges);
        newEdgeCount++;
    }

    private void processFrontierState(StateCache.State s) {
        traceState(s);

        // get the frontier information (call sites)
        policy.frontierState = s;
        policy.edgeType = NORMAL_EDGE;

        // add this to explored states
        graph.setExplored(s);

        // compute the possible next states
        interpreter.computeNextStates(s);
    }

    private void findMaximalPath() {
        // the stack hashmap contains a mapping between states on the stack
        // and the current stack depth at which they are being explored
        HashMap stack = new HashMap(1000);
        StateCache.State state = graph.getEdenState();

        try {
            maximalPath = findMaximalPath(state, stack, 0);
        } catch (UnboundedStackException e) {
            unbounded = true;
            maximalPath = e.path;
        }
    }

    private class Path {
        final int depth;
        final int length;
        final StateTransitionGraph.Edge edge;
        final Path tail;

        Path(int d, StateTransitionGraph.Edge e, Path p) {
            depth = d;
            edge = e;
            tail = p;
            length = p == null ? 1 : 1 + p.length;
        }
    }

    /**
     * The <code>findMaximalPath()</code> method is a recursive procedure that discovers the maximal weight
     * path in the state graph. If there is a non-zero weight cycle, this method will throw a
     * <code>UnboundedStackException</code> which contains as a field the path leading out of the specified
     * state back to a state on the stack.
     *
     * @param s     the state to explore
     * @param stack the states on the traversal stack
     * @param depth the current stack depth
     * @return a path leading out of the current state that adds the maximum height to the stack of any path
     *         leading out of this state
     * @throws UnboundedStackException if a non-zero weight cycle exists in the graph
     */
    protected Path findMaximalPath(StateCache.State s, HashMap stack, int depth) throws UnboundedStackException {
        // record this node and the stack depth at which we first encounter it
        stack.put(s, new Integer(depth));

        int maxdepth = 0;
        int minlength = Integer.MAX_VALUE;
        Path maxtail = null;
        StateTransitionGraph.Edge maxedge = null;
        for (StateTransitionGraph.Edge edge = s.info.forwardEdges; edge != null; edge = edge.forwardLink) {

            StateCache.State t = edge.target;
            if (stack.containsKey(t)) {
                // cycle detected. check that the depth when reentering is the same
                int prevdepth = ((Integer)stack.get(t)).intValue();
                if (depth + edge.weight != prevdepth) {
                    stack.remove(s);
                    throw new UnboundedStackException(new Path(depth + edge.weight, edge, null));
                }
            } else {
                Path tail;

                if (edge.target.mark instanceof Path) {
                    // node has already been visited and marked with the
                    // maximum amount of stack depth that it can add.
                    tail = ((Path)edge.target.mark);
                } else {
                    // node has not been seen before, traverse it
                    try {
                        tail = findMaximalPath(edge.target, stack, depth + edge.weight);
                    } catch (UnboundedStackException e) {
                        // this node is part of an unbounded cycle, add it to the path
                        // and rethrow the exception
                        e.path = new Path(depth + edge.weight, edge, e.path);
                        stack.remove(s);
                        throw e;
                    }
                }

                // compute maximum added stack depth by following this edge
                int extra = edge.weight + tail.depth;

                // remember the shortest path (in number of links) to the
                // maximum depth stack from following any of the links
                if (extra > maxdepth || (tail.length < minlength && extra == maxdepth)) {
                    maxdepth = extra;
                    maxtail = tail;
                    maxedge = edge;
                    minlength = tail.length;
                }
            }
        }
        // we are finished with this node, remember how much deeper it can take us
        stack.remove(s);
        Path maxpath = new Path(maxdepth, maxedge, maxtail);
        s.mark = maxpath;
        return maxpath;
    }


    /**
     * The <code>UnboundedStackException</code> class is an exception thrown when traversing the graph to find
     * the maximal stack depth. It is thrown if a cycle in the graph is found to have non-zero weight; such a
     * cycle means that the stack is unbounded.
     */
    private class UnboundedStackException extends Exception {
        Path path;

        UnboundedStackException(Path p) {
            path = p;
        }
    }

    /**
     * The <code>report()</code> method generates a textual report after the analysis has been completed. It
     * reports information about states, reachable states, time for analysis, and of course, the maximum stack
     * size for the program.
     */
    public void report() {
        printStatHeader();
        printStats();

        printQuantity("Time to build graph   ", TimeUtil.milliToSecs(buildTime));
        if (maximalPath == null) {
            Terminal.printRed("No maximal path data.");
            Terminal.nextln();
            return;
        }

        printQuantity("Time to traverse graph", TimeUtil.milliToSecs(traverseTime));
        if (unbounded)
            printQuantity("Maximum stack depth   ", "unbounded");
        else
            printQuantity("Maximum stack depth   ", "" + maximalPath.depth + " bytes");

        if ( SHOW_PATH ) {
            printPath(maximalPath);
        }
    }

    public void dump() {
        graph.dump(Printer.STDOUT);
    }

    private void printPath(Path p) {
        int depth = 0;
        int cntr = 1;
        for (Path path = p; path != null && path.edge != null; path = path.tail) {

            StateTransitionGraph.Edge edge = path.edge;

            if (cntr > 1 && TRACE_SUMMARY && edge.weight == 0) {
                int pc = edge.source.getPC();
                if (edge.target.getPC() == program.getNextPC(pc)) {
                    cntr++;
                    continue;
                }
            }

            printFullState("[" + cntr + "] Depth: " + depth, edge.source);
            Terminal.print("    ");
            StatePrinter.printEdge(edge.type, edge.weight, edge.target);
            depth += edge.weight;
            cntr++;
        }
    }

    private void printQuantity(String q, String v) {
        Terminal.printBrightGreen(q);
        Terminal.println(": " + v);
    }

    private void postReturnState(StateCache.State rs) {
        newReturnStates = new StateTransitionGraph.StateList(rs, newReturnStates);
        newRetCount++;
    }

    private void maskIrrelevantState(MutableState s, ISEState rs) {
        for ( int cntr = 0; cntr < MutableState.NUM_REGS; cntr++ ) {
            LegacyRegister reg = LegacyRegister.getRegisterByNumber(cntr);
            if ( !rs.isRegisterRead(reg) )
                s.setRegisterAV(reg, AbstractArithmetic.UNKNOWN);
        }
        // TODO: these interrupt flags do matter!
        //maskIORegister(IORegisterConstants.SREG, s, rs);
        //maskIORegister(IORegisterConstants.EIMSK, s, rs);
        //maskIORegister(IORegisterConstants.TIMSK, s, rs);
    }

    private void maskIORegister(int ior, MutableState s, ISEState rs) {
        if ( !rs.isIORegisterRead(ior) )
            s.setIORegisterAV(ior, AbstractArithmetic.UNKNOWN);
    }

    /**
     * The <code>ContextSensitive</code> class implements the context-sensitive analysis similar to 1-CFA. It
     * is an implementation of the <code>Analyzer.Policy</code> interface that determines what should be done
     * in the case of a call, return, push, pop, indirect call, etc. The context-sensitive analysis does not
     * model the contents of the stack, so pushes and pops essentially only modify the height of the stack.
     *
     * @author Ben L. Titzer
     */
    public class ContextSensitivePolicy implements AnalyzerPolicy {

        public StateCache.State frontierState;
        protected int edgeType;

        /**
         * The <code>call()</code> method is called by the abstract interpreter when it encounters a call
         * instruction within the program. Different policies may handle calls differently. This
         * context-sensitive analysis keeps track of the call site every time a new method is entered. Thus
         * the states coming into a method call are merged according to the call site, instead of all merged
         * together.
         *
         * @param s              the current abstract state
         * @param target_address the concrete target address of the call
         * @return null because the correct state transitions are inserted by the policy, and the abstract
         *         interpreter should not be concerned.
         */
        public MutableState call(MutableState s, int target_address) {
            if ( isea != null ) {
                ISEState rs = isea.getProcedureSummary(target_address);
                if ( rs != null )
                maskIrrelevantState(s, rs);
            }

            s.setPC(target_address);
            addEdge(frontierState, CALL_EDGE, s);

            // do not continue abstract interpretation after this state; edges will
            // be inserted later that represent possible return states
            return null;
        }

        /**
         * The <code>interrupt()</code> is called by the abstract interrupt when it encounters a place in the
         * program when an interrupt might occur.
         *
         * @param s   the abstract state just before interrupt
         * @param num the interrupt number that might occur
         * @return the state of the program after the interrupt, null if there is no next state
         */
        public MutableState interrupt(MutableState s, int num) {
            s.setFlag_I(AbstractArithmetic.FALSE);
            s.setPC((num - 1) * 4);
            addEdge(frontierState, INT_EDGE, s);

            // do not continue abstract interpretation after this state; edges will
            // be inserted later that represent possible return states
            return null;
        }

        /**
         * The <code>ret()</code> method is called by the abstract interpreter when it encounters a return
         * within the program. In the context-sensitive analysis, the return state must be connected with the
         * call site. This is done by accessing the call site list which is stored in this frontier state and
         * inserting edges for each call site.
         *
         * @param s the current abstract state
         * @return null because the correct state transitions are inserted by the policy, and the abstract
         *         interpreter should not be concerned.
         */
        public MutableState ret(MutableState s) {
            frontierState.setType(RET_STATE);
            postReturnState(frontierState);
            retCount++;

            // do not continue abstract interpretation after this state; this state
            // is a return state and therefore is a dead end
            return null;
        }

        /**
         * The <code>reti()</code> method is called by the abstract interpreter when it encounters a return
         * from an interrupt within the program.
         *
         * @param s the current abstract state
         * @return null because the correct state transitions are inserted by the policy, and the abstract
         *         interpreter should not be concerned.
         */
        public MutableState reti(MutableState s) {
            frontierState.setType(RETI_STATE);
            postReturnState(frontierState);
            retiCount++;

            // do not continue abstract interpretation after this state; this state
            // is a return state and therefore is a dead end
            return null;
        }

        /**
         * The <code>indirectCall()</code> method is called by the abstract interpreter when it encounters an
         * indirect call within the program. The abstract values of the address are given as parameters, so
         * that a policy can choose to compute possible targets or be conservative or whatever it so chooses.
         *
         * @param s        the current abstract state
         * @param addr_low the (abstract) low byte of the address
         * @param addr_hi  the (abstract) high byte of the address
         * @return null because the correct state transitions are inserted by the policy, and the abstract
         *         interpreter should not be concerned.
         */
        public MutableState indirectCall(MutableState s, char addr_low, char addr_hi) {
            int callsite = s.pc;
            List iedges = program.getIndirectEdges(callsite);
            if (iedges == null)
                throw Util.failure("No control flow information for indirect call at: " +
                        StringUtil.addrToString(callsite));
            Iterator i = iedges.iterator();
            while (i.hasNext()) {
                int target_address = ((Integer)i.next()).intValue();
                call(s, target_address);
            }

            return null;
        }

        /**
         * The <code>indirectJump()</code> method is called by the abstract interpreter when it encounters an
         * indirect jump within the program. The abstract values of the address are given as parameters, so
         * that a policy can choose to compute possible targets or be conservative or whatever it so chooses.
         *
         * @param s        the current abstract state
         * @param addr_low the (abstract) low byte of the address
         * @param addr_hi  the (abstract) high byte of the address
         * @return null because the correct state transitions are inserted by the policy, and the abstract
         *         interpreter should not be concerned.
         */
        public MutableState indirectJump(MutableState s, char addr_low, char addr_hi) {
            int callsite = s.pc;
            List iedges = program.getIndirectEdges(callsite);
            if (iedges == null)
                throw Util.failure("No control flow information for indirect jump at: " +
                        StringUtil.addrToString(callsite));
            Iterator i = iedges.iterator();
            while (i.hasNext()) {
                int target_address = ((Integer)i.next()).intValue();
                s.setPC(target_address);
                pushState(s);
            }

            return null;
        }

        /**
         * The <code>indirectCall()</code> method is called by the abstract interpreter when it encounters an
         * indirect call within the program. The abstract values of the address are given as parameters, so
         * that a policy can choose to compute possible targets or be conservative or whatever it so chooses.
         *
         * @param s        the current abstract state
         * @param addr_low the (abstract) low byte of the address
         * @param addr_hi  the (abstract) high byte of the address
         * @param ext      the (abstract) extended part of the address
         * @return null because the correct state transitions are inserted by the policy, and the abstract
         *         interpreter should not be concerned.
         */
        public MutableState indirectCall(MutableState s, char addr_low, char addr_hi, char ext) {
            throw new Error("extended indirect calls not supported");
        }

        /**
         * The <code>indirectJump()</code> method is called by the abstract interpreter when it encounters an
         * indirect jump within the program. The abstract values of the address are given as parameters, so
         * that a policy can choose to compute possible targets or be conservative or whatever it so chooses.
         *
         * @param s        the current abstract state
         * @param addr_low the (abstract) low byte of the address
         * @param addr_hi  the (abstract) high byte of the address
         * @param ext      the (abstract) extended part of the address
         * @return null because the correct state transitions are inserted by the policy, and the abstract
         *         interpreter should not be concerned.
         */
        public MutableState indirectJump(MutableState s, char addr_low, char addr_hi, char ext) {
            throw new Error("extended indirect jumps not supported");
        }

        /**
         * The <code>push()</code> method is called by the abstract interpreter when a push to the stack is
         * encountered in the program. The policy can then choose what outgoing and/or modelling of the stack
         * needs to be done.
         *
         * @param s   the current abstract state
         * @param val the abstract value to push onto the stack
         */
        public void push(MutableState s, char val) {
            edgeType = PUSH_EDGE;
        }

        /**
         * The <code>pop()</code> method is called by the abstract interpreter when a pop from the stack is
         * ecountered in the program. The policy can then choose to either return whatever information it has
         * about the stack contents, or return an UNKNOWN value.
         *
         * @param s the current abstract state
         * @return the abstract value popped from the stack
         */
        public char pop(MutableState s) {
            edgeType = POP_EDGE;
            // we do not model the stack, so popping values returns unknown
            return AbstractArithmetic.UNKNOWN;
        }

        /**
         * The <code>pushState</code> method is called by the abstract interpreter when a state is forked by
         * the abstract interpreter (for example when a branch condition is not known and both branches must
         * be taken.
         *
         * @param newState the new state created
         */
        public void pushState(MutableState newState) {
            addEdge(frontierState, edgeType, newState);
        }

        private void addEdge(StateCache.State from, int type, MutableState to) {
            StateCache.State t = graph.getCachedState(to);
            traceProducedState(t);
            addEdge(type, from, t, EDGE_DELTA[type]);
            pushFrontier(t);
        }

        private void pushFrontier(StateCache.State t) {
            // CASE 4: self loop
            if (t == frontierState) return;

            if (graph.isExplored(t)) {
                // CASE 3: state is already explored
                // do nothing; propagation phase will push callers to reachable returns
            } else if (graph.isFrontier(t)) {
                // CASE 2: state is already on frontier
                // do nothing; propagation phase will push callers to reachable returns
            } else {
                // CASE 1: new state, add to frontier
                graph.addFrontierState(t);
            }
        }

        private void addEdge(int type, StateCache.State s, StateCache.State t, int weight) {
            traceEdge(type, s, t, weight);
            StateTransitionGraph.Edge edge = graph.addEdge(s, type, weight, t);
            if (graph.isExplored(t))
                postNewEdge(edge);
        }

    }


    //-----------------------------------------------------------------------
    //             D E B U G G I N G   A N D   T R A C I N G
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------


    private void traceState(StateCache.State s) {
        if (TRACE) {
            printFullState("Exploring", s);
        }
    }

    private void printFullState(String head, StateCache.State s) {
        Terminal.print(head + ' ');
        StatePrinter.printStateName(s);
        Terminal.nextln();
        LegacyInstr instr = (LegacyInstr)program.readInstr(s.getPC());
        String str = StringUtil.leftJustify(instr.toString(), 14);
        StatePrinter.printState(str, s);
    }

    private void traceProducedState(StateCache.State s) {
        if (TRACE) {
            String str;
            if (graph.isExplored(s)) {
                str = "        E ==> ";
            } else if (graph.isFrontier(s)) {
                str = "        F ==> ";

            } else {
                str = "        N ==> ";
            }
            StatePrinter.printState(str, s);
        }
    }

    public static void traceEdge(int type, StateCache.State s, StateCache.State t, int weight) {
        if (!TRACE) return;
        Terminal.print("adding edge ");
        StatePrinter.printEdge(s, type, weight, t);
    }

}
