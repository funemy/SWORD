/**
 * Copyright (c) 2007, Ben L. Titzer
 * See the file "license.txt" for details.
 *
 * Created Jan 27, 2008
 */
package avrora.monitors;

import avrora.core.SourceMapping;
import avrora.sim.Simulator;
import avrora.sim.mcu.MCUProperties;
import avrora.sim.clock.MainClock;
import cck.text.StringUtil;
import cck.text.Terminal;
import cck.util.Option;

/**
 * The <code>CallTreeProfiler</code> definition.
 *
 * @author Ben L. Titzer
 */
public class CallTreeProfiler extends MonitorFactory {

    private static final int MAX_CALL_DEPTH = 100;

    final Option.Long DEPTH = options.newOption("profile-depth", 5,
            "This option controls how deep the display of the whole-program profiler goes.");
    final Option.Double THRESHOLD = options.newOption("profile-threshold", 0.0d,
            "This option controls the threshold at which call subtrees are show in the profiler.");

    static class CallTreeNode {
        int address;
        CallTreeNode sibling;
        CallTreeNode children;
        int count;
        long accumulated;
        long startcycles;
    }

    public class ProfileMonitor implements CallTrace.Monitor, Monitor {

        private final CallTreeNode[] stack = new CallTreeNode[MAX_CALL_DEPTH];
        private final CallTreeNode[] interrupts;
        private final double threshold = THRESHOLD.get();
        private int stackDepth;
        private final Simulator simulator;
        private final SourceMapping sourceMapping;
        private final MainClock clock;

        ProfileMonitor(Simulator sim) {
            stack[0] = new CallTreeNode();
            this.simulator = sim;
            sourceMapping = simulator.getProgram().getSourceMapping();
            clock = simulator.getClock();
            new CallTrace(sim).attachMonitor(this);
            interrupts = new CallTreeNode[simulator.getInterpreter().getInterruptTable().getNumberOfInterrupts()];
            for (int i = 0; i < interrupts.length; i++) {
                // each interrupt root node has a negative address to signal that the parent should not be charged
                interrupts[i] = new CallTreeNode();
                interrupts[i].address = -1 - i;
            }
        }

        public void fireBeforeCall(long time, int pc, int target) {
            CallTreeNode parent = stack[stackDepth];
            CallTreeNode thisNode = parent.children;
            while (thisNode != null) {
                if (thisNode.address == target) break;
                thisNode = thisNode.sibling;
            }
            if (thisNode == null) {
                // didnt find a call tree node for this one, create and insert
                thisNode = new CallTreeNode();
                thisNode.address = target;
                thisNode.sibling = parent.children;
                parent.children = thisNode;
            }
            thisNode.startcycles = time;
            stack[++stackDepth] = thisNode;
        }

        public void fireAfterReturn(long time, int pc, int retaddr) {
            // record the complete invocation time for this node
            popStack(time);
        }

        private void popStack(long time) {
            CallTreeNode thisNode = stack[stackDepth--];
            thisNode.count++;
            long elapsed = time - thisNode.startcycles;
            thisNode.accumulated += elapsed;
            if (thisNode.address < 0) {
                // discount the parent's accumulated time if this is an interrupt
                stack[stackDepth].accumulated -= elapsed;
            }
        }

        public void fireBeforeInterrupt(long time, int pc, int inum) {
            CallTreeNode thisNode = interrupts[inum];
            thisNode.startcycles = time;
            stack[++stackDepth] = thisNode;
        }

        public void fireAfterInterruptReturn(long time, int pc, int retaddr) {
            popStack(time);
        }

        public void report() {
            while (stackDepth > 0) {
                fireAfterReturn(clock.getCount(), 0, 0);
            }
            long total = 0;
            CallTreeNode root = stack[0];
            CallTreeNode child = root.children;
            while (child != null) {
                total += child.accumulated;
                child = child.sibling;
            }
            reportSubTree(root, 0, root.accumulated = total);
            for (int i = 0; i < interrupts.length; i++) {
                if (interrupts[i].count > 0) {
                    reportSubTree(interrupts[i], 0, interrupts[i].accumulated);
                }
            }
        }

        private void reportSubTree(CallTreeNode node, int indent, double pval) {
            long nested = node.accumulated;
            long inside = nested;
            CallTreeNode child = node.children;
            while (child != null) {
                inside -= child.accumulated;
                child = child.sibling;
            }
            float insidePercent = asPercent(inside, pval);
            float nestedPercent = asPercent(nested, pval);
            Terminal.print(StringUtil.space(indent * 4));
            Terminal.printGreen(StringUtil.leftJustify(getNodeName(node), 40));
            Terminal.printCyan("" + inside);
            Terminal.print(" cycles ");
            Terminal.printCyan(StringUtil.toFixedFloat(insidePercent, 5));
            Terminal.print(" % / ");
            Terminal.printCyan(""+node.count);
            Terminal.print(" = ");
            if (node.count > 0)
                Terminal.printCyan(StringUtil.toFixedFloat(inside/node.count, 2));
            Terminal.print(" (");
            Terminal.print("" + nested);
            Terminal.print(" total ");
            Terminal.print(StringUtil.toFixedFloat(nestedPercent, 5));
            Terminal.print(" %)");
            Terminal.nextln();
            if (indent < DEPTH.get()) {
                child = node.children;
                while (child != null) {
                    reportSubTree(child, indent + 1, pval);
                    child = child.sibling;
                }
            }
        }

        String getNodeName(CallTreeNode node) {
            if (node.address > 0) {
                return sourceMapping.getName(node.address);
            } else if (node.address < 0 ) {
                MCUProperties mcuProperties = simulator.getMicrocontroller().getProperties();
                int inum = 0 - node.address;
                return "#" + inum + " " + mcuProperties.getInterruptName(inum);
            } else {
                return "root";
            }
        }

        private float asPercent(double nval, double pval) {
            return (float) (100 * nval / pval);
        }
    }

    static class Entry implements Comparable {
        final String name;
        final int count;
        Entry(String name, int count) {
            this.name = name;
            this.count = count;
        }
        public int compareTo(Object o) {
            Entry e = (Entry)o;
            if (count < e.count) return -1;
            if (count == e.count) return name.compareTo(e.name);
            return 1;
        }
    }

    public CallTreeProfiler() {
        super("The call tree monitor builds a complete call tree and records the time spent " +
                "executing each function, both internally and in nested calls.");
    }

    public avrora.monitors.Monitor newMonitor(Simulator s) {
        return new ProfileMonitor(s);
    }

}
