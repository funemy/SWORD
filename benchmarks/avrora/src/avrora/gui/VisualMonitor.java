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

import avrora.monitors.Monitor;
import javax.swing.*;

/**
 * The <code>VisualMonitor</code> class represents a monitor attached to a <code>Simulator</code> instance. Created
 * by the <code>MonitorFactory</code> class, a monitor collects statistics about a program as it runs, and
 * then when the simulation is complete, generates a report.
 * <p>
 * All visual monitors are expected to "draw" some graphical output (e.g. a graph, a table) to a displayable
 * Swing panel.
 *
 * @author Ben L. Titzer
 */
public interface VisualMonitor extends Monitor {

    /**
     * This function should be called by the thread that periodically repaints the
     * monitors chalkaboard.  It is assumed that this function will also handle any 
     * internal data maniuplations that are necessary
     */
    public void updateDataAndPaint(); 

    /**
     * This is a temporary hack...once Global Monitors is rewritten to be more robust, this
     * will be deleted
     */
    public GraphEvents getGraph();

    /**
     * This is called right after a monitor is actually init (when the sim is just beginning
     * It physically let's the new monitor "know" about it's painting surfaces
     * <p>
     * Note that it's possible with the new implementation that this function will be unnecessary
     *
     * @param thePanel The main display panel for the monitor
     * @param theOptionsPanel The panel that the monitor can display options to
     */
    public void setVisualPanel(JPanel thePanel, JPanel theOptionsPanel);

    /**
     * The <code>report()</code> method is called after the simulation is complete. The monitor generates a
     * textual or other format representation of the information collected during the execution of the
     * program.
     * <p>
     * For visual monitors, typically is would do a "last check" to make sure it got all it's
     * data from temporary storage onto the display.
     */
    public void report();
}
