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

//This manages the high level menu bar for the GUI

package avrora.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This simple class handles the top level menu bar.  The top level
 * bar contols inputs to the simulator and monitor additions, so the
 * ManageMonitor and ManageSimInput instances are contianed within this 
 * class
 * <p>
 * If you are looking to add to the top level menu of the GUI, you edit
 * this file.
 *
 * @author UCLA Compilers Group
 */
public class SimMenuBar {
    /**
     * This is the high level menu "object" that should be displayed
     */
    public JMenuBar menuBar;

    private ManageSimInput theSimInput; //this is all the code for getting sim files
                                //and changing options
    
    private ManageMonitors theManageMonitors; //All code for monitor changes
                                            //happen here

    private static final String FILE = "File";
    private static final String SIMOPTIONS = "Options...";
    private static final String LOADPROGRAM = "Load Program...";
    private static final String ADDFILE = "Add Nodes...";
    private static final String REMOVENODES = "Remove Nodes...";

    private static final String MONITORS = "Monitors";
    private static final String ADDMONITORS = "Add Monitors...";

    /**
     * This functions as the constructor for this class.  It inits all internal
     * values and "creates" the visual objects necessary for displaying the 
     * menu bar
     *
     * @return An instance of this class
     */
    public static SimMenuBar createSimMenuBar() {
        SimMenuBar thesetup = new SimMenuBar();
        thesetup.theSimInput = ManageSimInput.createManageSimInput();
        thesetup.theManageMonitors = ManageMonitors.createManageMonitors();

        thesetup.menuBar = new JMenuBar();
        thesetup.updateMenuBar();

        return thesetup;
    }

    /**
     * This physically creates the look of the menu bar.
     * It should be called upon start up and when any change to a sim setting
     * would cause a change in the menu bar.  (e.g. when the sim starts)
     */
    public void updateMenuBar() {

        if (!AvroraGui.instance.getSimulation().isRunning()) {
            menuBar.removeAll();
            JMenu newMenu;
            newMenu = new JMenu(FILE);
            menuBar.add(newMenu);

            JMenuItem newItem;

            newItem = new JMenuItem(SIMOPTIONS);
            newItem.addActionListener(AvroraGui.instance);
            newMenu.add(newItem);

            newItem = new JMenuItem(LOADPROGRAM);
            newItem.addActionListener(AvroraGui.instance);
            newMenu.add(newItem);

            newItem = new JMenuItem(ADDFILE);
            newItem.addActionListener(AvroraGui.instance);
            newMenu.add(newItem);

            newItem = new JMenuItem(REMOVENODES);
            newItem.addActionListener(AvroraGui.instance);
            newMenu.add(newItem);

            newMenu = new JMenu(MONITORS);
            menuBar.add(newMenu);

            newItem = new JMenuItem(ADDMONITORS);
            newItem.addActionListener(AvroraGui.instance);
            newMenu.add(newItem);
        } else {
            menuBar.removeAll();
            menuBar.add(new JMenu("Sim is running"));
            //Currently no options can be changed during runtime
        }
    }

    /**
     * This checks to see if the event was caused by
     * a widget in this class.
     *
     * @return true, if a widget inside this class was the cause of the event
     * otherwise false
     */
    public boolean checkAndDispatch(ActionEvent e) {
        Object source = e.getSource();
        if (source instanceof JMenuItem) {
            if (((JMenuItem) source).getText().equals(SIMOPTIONS)) {
                theSimInput.createSetOptionsDialog();
                theSimInput.setOptionsDialog.setLocationRelativeTo(null); //center on screen
                theSimInput.setOptionsDialog.setVisible(true);
                return true;
            }
            if (((JMenuItem) source).getText().equals(ADDFILE)) {
                theSimInput.createFileSelectionDialog();
                theSimInput.fileSelectionDialog.setLocationRelativeTo(null); //center on screen
                theSimInput.fileSelectionDialog.setVisible(true);
                return true;
            }
            if (((JMenuItem) source).getText().equals(REMOVENODES)) {
                AvroraGui.instance.topologyBox.removeSelectedNodes();
                return true;
            }
            if (((JMenuItem) source).getText().equals(ADDMONITORS)) {
                theManageMonitors.createMonitorsDialog();
                theManageMonitors.chooseMonitorsDialog.setLocationRelativeTo(null); //center on screen
                theManageMonitors.chooseMonitorsDialog.setVisible(true);
                return true;
            }
        }

        //We should check to see if something in theSimInput or ManageMonitors caused the action
        if (theManageMonitors.checkAndDispatch(e))
        {
            return true;
        }
        else 
        {
            return theSimInput.checkAndDispatch(e);
        }
    }


}
