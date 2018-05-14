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

import avrora.sim.Simulation;
import avrora.sim.Simulator;
import java.util.*;

/**
 * There are two types of visual monitors for the GUI.  First, there are single
 * node monitors.  Each time a monitor is added to a node, a new panel is created
 * and each "instance" of this monitor gets its chalkboard/panel to draw on.
 * Second, there are global monitors.  Every time a global monitor is attached 
 * to a new node, one one total chalkboard/diplay panel is created.  (e.g. a
 * global monitor might have five nodes attached to it, but each node only 
 * writes to one central chalkboard).
 * <p>
 * This class is for the first type: single node monitors.  It is a physical
 * implementation of a monitor factory -> thus once inited, it can create
 * nodes on command.
 * <p>
 * Since many of the single node monitors are very similar is data collection 
 * and display (just a few lines are different in the actual data collection),
 * most single node monitors can just use this class as a factory.
 *
 * @author Ben L. Titzer
 */
public abstract class SingleNodeMonitor implements Simulation.Monitor {

    final HashMap panelMap; // maps VisualSimulation.Node -> SingleNodePanel
    final HashMap monitorMap; // maps MonitorPanel -> PCMonitor
    final String monitorName;

    /**
     * Default constuctor, will init the hash maps that store information
     * about the monitors in this node
     */
    public SingleNodeMonitor(String n) {
        panelMap = new HashMap();
        monitorMap = new HashMap();
        monitorName = n;
    }

    /**
     * This actually informs our data structure that the list of
     * nodes passed to this function want this monitor.  A display 
     * panel is created.   Note that the monitors are NOT created
     * by attach - you need to call init
     *
     * @param nodes A list of the nodes that should be attached to the monitor
     */
    public void attach(Simulation sim, List nodes) {
        Iterator i = nodes.iterator();
        while ( i.hasNext()) {
            Simulation.Node n = (Simulation.Node)i.next();
            if ( panelMap.containsKey(n) ) continue;
            MonitorPanel p = AvroraGui.instance.createMonitorPanel(monitorName+" - "+n.id);
            SingleNodePanel snp = newPanel(n, p);
            panelMap.put(n, snp);
            n.addMonitor(this);
        }
    }

    /**
     * This is called at the beginning of the simulation to physically
     * create the nodes
     *
     * @param n The node the monitor is attached to
     * @param s The simulator that the monitor can be inserted into
     */
    public void construct(Simulation sim, Simulation.Node n, Simulator s) {
        SingleNodePanel snp = (SingleNodePanel)panelMap.get(n);
        snp.construct(s);
    }

    /**
     * This is called at the end of the simulation to remove any data structures
     * associated with the nodes.
     *
     * @param n The node the monitor is attached to
     * @param s The simulator that the monitor can be inserted into
     */
    public void destruct(Simulation sim, Simulation.Node n, Simulator s) {
        SingleNodePanel snp = (SingleNodePanel)panelMap.get(n);
        snp.destruct();
    }

    /**
     * You can multiple remove nodes from a monitor using this function
     *
     * @param nodes A <code> List </code> of nodes that should be removed.  If an 
     * element of the list is not already attached to the node, it will just
     * skip that element.
     */
    public void remove(Simulation sim, List nodes) {
        Iterator i = nodes.iterator();
        while ( i.hasNext()) {
            Simulation.Node n = (Simulation.Node)i.next();
            removeOne(n);
        }
    }

    private void removeOne(Simulation.Node n) {
        SingleNodePanel snp = (SingleNodePanel)panelMap.get(n);
        if ( snp == null ) return;

        snp.remove();
        AvroraGui.instance.removeMonitorPanel(snp.panel);
        panelMap.remove(n);
        n.removeMonitor(this);
    }

    protected abstract class SingleNodePanel {
        protected final Simulation.Node node;
        protected final MonitorPanel panel;

        SingleNodePanel(Simulation.Node n, MonitorPanel p) {
            node = n;
            panel = p;
        }

        protected abstract void construct(Simulator s);
        protected abstract void destruct();
        protected abstract void remove();
    }

    protected abstract SingleNodePanel newPanel(Simulation.Node n, MonitorPanel p);
}
