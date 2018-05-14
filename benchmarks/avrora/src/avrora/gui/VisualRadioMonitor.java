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

import avrora.core.Program;
import avrora.monitors.MonitorFactory;
import avrora.monitors.Monitor;
import avrora.sim.Simulator;
import avrora.sim.util.ProgramProfiler;
import javax.swing.*;
import java.awt.*;
import java.util.Vector;

/**
 * This is a prototype for a "global monitor."  Several
 * different nodes are all displayed on the same one panel.
 * Basically, this monitor will display radio packet transmissions
 * as dots on the screen.  You can see transmission delays across
 * nodes.
 * <p>
 * The internal code here is not well documented and will soon be entirely 
 * rewritten to a) accomodate a new way of handeling global monitors and 
 * b) a new way of accessing data inside the simulator.
 *
 * @author UCLA Compilers Group
 */
public class VisualRadioMonitor extends MonitorFactory {

    //Helps it be global
    public static JPanel masterPanel;
    public static JPanel optionsPanel;
    public static boolean isDisplayed;
    public static Vector allCurrentMonitors; //a vector so we can access all instances of this class
    public static Vector allCurrentGraphEvents;
    public static int numofnodes; //for stupid reasons, this is always one greater than num of nodes
    //added to simulator

                
    static {
        masterPanel = new JPanel();
        masterPanel.setLayout(new GridLayout(1, 1));
        optionsPanel = new JPanel(false);
        JLabel optionsFiller = new JLabel("Options for the monitor can be set here. ");
        optionsPanel.setLayout(new GridLayout(1, 1));
        optionsPanel.add(optionsFiller);
        isDisplayed = false;
        allCurrentMonitors = new Vector();
        allCurrentGraphEvents = new Vector();
        numofnodes = 1;
    }

    public class VisualMonitor implements avrora.gui.VisualMonitor {


        public final Simulator simulator;
        public final Program program;
        public final ProgramProfiler profile;
        public JPanel visualPanel;
        public JPanel visualOptionsPanel;
        public GraphEvents theGraph;
        public Object vSync;

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

        //This is a quick hack to get things working...we'll have to
        //go back and rewrite to make things more generalized
        public GraphEvents getGraph() {
            return theGraph;
        }


        //allows vAction to link the GUI and our monitor via the passed panels..
        //it is also where we init our graph and start the paint thread
        //Think of it as the constructor for the visual elements of this monitor
        public void setVisualPanel(JPanel thePanel, JPanel theOptionsPanel) {

            allCurrentMonitors.add(this);  //for global access

            visualPanel = thePanel;

            //This is where we should set up the graph panel itself (aka the chalkbord)
            visualPanel.removeAll();
            visualPanel.setLayout(new BorderLayout());

            theGraph = new GraphEvents(0, 500, 3);

            allCurrentGraphEvents.add(theGraph);

            theGraph.setParentPanel(visualPanel);
            visualPanel.add(theGraph.chalkboardAndBar(), BorderLayout.CENTER);

            visualPanel.validate();

            //And we should set up the options panel

        }

        VisualMonitor(Simulator s) {
            simulator = s;
            program = s.getProgram();
            profile = new ProgramProfiler(program);
        }

        /**
         * The <code>report()</code> method generates a textual report after the simulation is complete.
         * The report does nothing in this case, because this is a visual monitor
         */
        public void report() {
            updateDataAndPaint();  //in case there is still stuff in the queue...
            //we better take it out
            //destroy our static vector of monitors
            allCurrentMonitors = new Vector();
            allCurrentGraphEvents = new Vector();
        }

    }

    /**
     * The constructor for the <code>VisualPCMonitor</code> class builds a new <code>MonitorFactory</code>
     * capable of creating monitors for each <code>Simulator</code> instance passed to the
     * <code>newMonitor()</code> method.
     */
    public VisualRadioMonitor() {
        super("The \"radio\" monitor tracks radio usage.");
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
