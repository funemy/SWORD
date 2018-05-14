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

package avrora.gui;

import avrora.arch.legacy.LegacyRegister;
import avrora.arch.legacy.LegacyState;
import avrora.core.Program;
import avrora.monitors.MonitorFactory;
import avrora.monitors.Monitor;
import avrora.sim.Simulator;
import avrora.sim.State;
import avrora.sim.util.ProgramProfiler;
import javax.swing.*;
import java.awt.*;

/**
 * The <code>VisualRegMonitor</code> class is a monitor that tracks the current value of a register
 */
public class VisualRegMonitor extends MonitorFactory {

    /**
     * The <code>Monitor</code> class implements a monitor for the stack height that inserts a probe after
     * every instruction in the program and checks the stack height after each instruction is executed.
     */
    public class VisualMonitor implements avrora.gui.VisualMonitor, Simulator.Probe {
        public final Simulator simulator;
        public final Program program;
        public final ProgramProfiler profile;
        public JPanel visualPanel;
        public JPanel visualOptionsPanel;
        public GraphNumbers theGraph;

        //ugly temp hack
        GraphEvents tempEvent;

        public GraphEvents getGraph() {
            return tempEvent;
        }

        public void updateDataAndPaint() {
            //So if there are new numbers that we added,
            //we repaint the thing.
            if (theGraph.internalUpdate()) {
                //So I know, I know - I'm suppose to call repaint()
                //But it doesn't work in this case...Java batches
                //the repaint request and gets to it when it feels like
                //it....destroying the illusion of seeing the graph
                //update in real time.
                //I guess my point is, you can change this this to repaint,
                //but we REALLY want paint ot be called and not have the AWT
                //mess anything up or decide to do something else.
                theGraph.paint(theGraph.getGraphics());
            }
        }

        //allows vAction to link the GUI and our monitor via the passed panels..
        //it is also where we init our graph and start the paint thread
        public void setVisualPanel(JPanel thePanel, JPanel theOptionsPanel) {
            visualPanel = thePanel;
            //This is where we should set up the graph panel itself
            visualPanel.removeAll();
            visualPanel.setLayout(new BorderLayout());
            theGraph = new GraphNumbers(visualPanel);
            visualPanel.add(theGraph, BorderLayout.CENTER);
            visualPanel.validate();

            //And we should set up the options panel
            visualOptionsPanel = theOptionsPanel;
            visualOptionsPanel.removeAll();
            visualOptionsPanel.setLayout(new BorderLayout());
            visualOptionsPanel.add(theGraph.getOptionsPanel(), BorderLayout.CENTER);
            visualOptionsPanel.validate();
        }

        public void fireBefore(State s, int address) {
            // do nothing
        }

        public void fireAfter(State s, int address) {
            int regvalue = ((LegacyState)s).getRegisterUnsigned(LegacyRegister.R26);
            //add regvalue to our vector
            theGraph.recordNumber(regvalue);
        }

        VisualMonitor(Simulator s) {
            simulator = s;
            program = s.getProgram();
            profile = new ProgramProfiler(program);
            // insert the global probe
            s.insertProbe(this);
        }

        /**
         * The <code>report()</code> method generates a textual report after the simulation is complete.
         * The report does nothing in this case, because this is a visual monitor
         */
        public void report() {
            theGraph.repaint();
        }

    }

    /**
     * The constructor for the <code>StackMonitor</code> class builds a new <code>MonitorFactory</code>
     * capable of creating monitors for each <code>Simulator</code> instance passed to the
     * <code>newMonitor()</code> method.
     */
    public VisualRegMonitor() {
        super("The \"Reg26\" monitor tracks the current value of the reg 26.");
    }

    /**
     * The <code>newMonitor()</code> method creates a new monitor that is capable of monitoring the stack
     * height of the program over its execution.
     *
     * @param s the simulator to create a monitor for
     * @return an instance of the <code>Monitor</code> interface for the specified simulator
     */
    public Monitor newMonitor(Simulator s) {
        return new VisualMonitor(s);
    }
}
