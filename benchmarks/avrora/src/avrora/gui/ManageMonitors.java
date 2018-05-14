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

//This manages a file chooser box for choosing the simulator file
//also manages a dialog box for setting simulator options

package avrora.gui;

import avrora.sim.Simulation;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * This class manages the dialog boxes for adding a monitor to the simulator.
 * It uses <code> GUIDefaults </code> to get a list of the monitors and several
 * functions inside <code> AvroraGUI </code> for actually adding visual panels
 * when necessary.  
 * <p>
 * There is depracated code here for a "Manage Monitor" button.  Currently,
 * we instead manage monitors using the top menu bar instead of a special button.
 *
 * @author UCLA Compilers Group
 */
public class ManageMonitors {

    JDialog chooseMonitorsDialog;
    JButton monitorsDialogUpdate;
    LinkedList checkBoxContainer;

    /**
     * This should be called during the GUI init in order
     * to have "slots" for the various dialog boxes.  This function
     * use to be more useful back before monitor management was handled
     * by the menu bar (we use to have to declare visual components here
     *
     * Really, this is only here to be consistent with the other
     * Manage* class files - all are inited in the same way
     *
     * @return An instance of this class (used to create dialogs)
     */
    public static ManageMonitors createManageMonitors() {
        ManageMonitors thesetup = new ManageMonitors();
        //Note: the dialog box is created upon the button push
        return thesetup;
    }

    /**
     * This creates a dialog box that displays a choice of monitors
     * to add.  It is assumed that the user has already used the topology
     * window to select specific nodes that the monitors will be added to
     */
    public void createMonitorsDialog() {
        //Make sure we have nice window decorations.
        JDialog.setDefaultLookAndFeelDecorated(true);

        checkBoxContainer = new LinkedList();

        chooseMonitorsDialog = new JDialog(AvroraGui.instance.masterFrame, "Add Monitors to Selected Nodes");

        //Now we create a JPanel that will be linked to the dialog
        JPanel internalPanel = new JPanel();
        internalPanel.setOpaque(true);
        internalPanel.setLayout(new BorderLayout());

        //Let's add a title banner
        JLabel dialogBanner = new JLabel();
        dialogBanner.setText("Check the monitors you want to add");
        internalPanel.add(dialogBanner, BorderLayout.NORTH);

        JPanel belowBannerPanel = new JPanel();

        //This gets monitors from GUIDefaults
        addMonitorsFromClassMap(belowBannerPanel);

        //Border of 30 on the outside
        belowBannerPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        internalPanel.add(belowBannerPanel, BorderLayout.CENTER);

        //Add the "Update" button
        monitorsDialogUpdate = new JButton();
        monitorsDialogUpdate.setText("Update");
        monitorsDialogUpdate.setToolTipText("Click to update the monitors list");
        monitorsDialogUpdate.addActionListener(AvroraGui.instance);
        internalPanel.add(monitorsDialogUpdate, BorderLayout.SOUTH);
        internalPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        //Add the panel to the dialog
        chooseMonitorsDialog.setContentPane(internalPanel);

        //Sizes things appropiatly
        chooseMonitorsDialog.pack();
    }

    /**
     * This function gets the monitor list from AvroraGui, which in turn
     * gets it from GUIDefaults.  
     */
    private void addMonitorsFromClassMap(JPanel belowBannerPanel) {
        //Let's get a storted list of monitor names registered with the VisualAction
        List monitorList = AvroraGui.instance.getMonitorList();
        Iterator monitorIter = monitorList.iterator();

        belowBannerPanel.setLayout(new GridLayout(monitorList.size(), 1));

        //Scroll through, adding all monitors...
        while (monitorIter.hasNext()) {
            String currentMonitor = (String) monitorIter.next();
            //Add a checkbox representing this list
            JCheckBox theCheckBox = new JCheckBox(currentMonitor);

            theCheckBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            belowBannerPanel.add(theCheckBox);
            //add the check box to a container so we can examine the values at a later date
            checkBoxContainer.add(theCheckBox);
        }
    }

    /**
     * This function checks if an event was caused by
     * this panel.  If so, it reacts to it.
     * This will be called by AvroraGui, the global listener for all events
     *
     * @return true if this panel was the cause of the event, false otherwise
     */
    public boolean checkAndDispatch(ActionEvent e) {
        //if the manage monitors button was pushed...we have to load our dialog

        /* Monitor button no longer exists
       if (e.getSource() == monitorsButton) {
           createMonitorsDialog();
           chooseMonitorsDialog.setLocationRelativeTo(null); //center on screen
           chooseMonitorsDialog.setVisible(true);

           return true;
        */
        if (e.getSource() == monitorsDialogUpdate) {
            //Our goal is to find all the selected
            //nodes.  For each node, if the monitor
            //has not already been added, then we
            //create a panel for it, register that panel,
            //and add the monitor to that node's monitor list

            //We first begin by getting a vector of strings
            //that represent the names of the monitors we wish to add
            Vector toMONITORS = new Vector();
            Iterator checkBoxIter = checkBoxContainer.iterator();
            while (checkBoxIter.hasNext()) {
                JCheckBox currentBox = (JCheckBox)checkBoxIter.next();
                if (currentBox.isSelected()) {
                    //it's selected, so add it to our list
                    toMONITORS.add(currentBox.getText());
                }
            }

            // for each selected monitor, give it a chance to attach to the nodes
            LinkedList nodes = getNodeList();
            for (int j = 0; j < toMONITORS.size(); j++) {
                String currentMonitor = (String)toMONITORS.elementAt(j);
                Simulation.Monitor mf = getMonitorFactory(currentMonitor);
                mf.attach(AvroraGui.instance.getSimulation(), nodes);
            }

            //We are done with the dialog...get rid of it
            chooseMonitorsDialog.setVisible(false);

            //we should also redraw the node table
            AvroraGui.instance.topologyBox.createSimpleAirTable();
            return true;
        } else {
            return false; //this module did not cause the action;
        }

    }

    private Simulation.Monitor getMonitorFactory(String n) {
        return GUIDefaults.getMonitor(n);
    }

    /**
     * This gets a list of currently selected nodes from the topology window
     */
    private LinkedList getNodeList() {
        Simulation sim = AvroraGui.instance.getSimulation();
        LinkedList nodes = new LinkedList();
        int[] selectedRows = AvroraGui.instance.topologyBox.table.getSelectedRows();
        for (int i = 0; i < selectedRows.length; i++) {
            //let's get the NID of that row
            Object v = AvroraGui.instance.topologyBox.theModel.getValueAt(selectedRows[i], 0);
            int nid = ((Integer) v).intValue();
            Simulation.Node node = sim.getNode(nid);
            nodes.add(node);
        }
        return nodes;
    }
}
