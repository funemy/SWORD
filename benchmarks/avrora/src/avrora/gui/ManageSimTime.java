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

//This module handles the Manage Sim Time toolbar in Avrora GUI.
//It can start, stop, pause (et cetera) the simulation.

package avrora.gui;

import avrora.sim.Simulation;
import cck.text.Terminal;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Vector;
import java.net.URL;

/**
 * From a high level view, the controls what the simulation is doing.
 * It can start, top, pause, speed up, and slow down the simulator.  It also
 * contains within it the visual component necessary to make this happen (the
 * start, stop buttons, the slider for controlling time.  AvroraGui accesses
 * these visual elements and displays them
 *
 * @author UCLA Compilers Group
 */
public class ManageSimTime {

    /**
     * This is a panel that contains all the visual elements of
     * this class.  It can be displayed by AvroraGui and the user
     * can then interact with it
     */
    public JPanel simTimeEverything;

    private JToolBar simTimeToolbar;
    private JSlider simTimeSlider;
    private SpinnerNumberModel simTimeDelaySpinner;
    private SpinnerNumberModel simTimeCycleSpinner;
    private JComboBox simTimeIorCSelect;

    private static int[] simTimeDelayDefaults;
    private static int[] simTimeCycleDefaults;
    private static int numofdefaults;

    private static final String REWIND = "rewind";
    private static final String STOP = "stop";
    private static final String PAUSE = "pause";
    private static final String RESUME = "resume";
    private static final String FASTFORWARD = "fastforward";

    /**
     * This is a "constructor" - it inits all internal fields
     * It is generally called by AvroraGui when the GUI is
     * being created
     *
     * @return An instance of ManageSimTime
     */
    public static ManageSimTime createManageSimTime() {
        ManageSimTime thesetup = new ManageSimTime();

        numofdefaults = 6;
        simTimeDelayDefaults = new int[numofdefaults];
        simTimeCycleDefaults = new int[numofdefaults];

        //These are "standard" delays we can insert into the sim
        //using the slider...users can also enter custom values using
        //the spinners

        //Fullspeed
        simTimeDelayDefaults[0] = 0;
        simTimeCycleDefaults[0] = 0;

        //Real time - spinenr values set to zero
        simTimeDelayDefaults[1] = 0;
        simTimeCycleDefaults[1] = 0;

        //slow
        simTimeDelayDefaults[2] = 100;
        simTimeCycleDefaults[2] = 1000;

        //slower
        simTimeDelayDefaults[3] = 1000;
        simTimeCycleDefaults[3] = 10;

        //slowest
        simTimeDelayDefaults[4] = 1000;
        simTimeCycleDefaults[4] = 1;

        //single step
        simTimeDelayDefaults[5] = 0;
        simTimeCycleDefaults[5] = 0;

        Hashtable temptable = new Hashtable();
        temptable.put(new Integer(-1), new JLabel("Custom"));
        temptable.put(new Integer(0), new JLabel("Full Speed"));
        temptable.put(new Integer(1), new JLabel("Real Time"));
        temptable.put(new Integer(2), new JLabel("Slow"));
        temptable.put(new Integer(3), new JLabel("Slower"));
        temptable.put(new Integer(4), new JLabel("Slowest"));
        temptable.put(new Integer(5), new JLabel("Single Step"));

        //Init the slider
        thesetup.simTimeSlider = new JSlider(-1, 5, 0);
        thesetup.simTimeSlider.setInverted(true);
        thesetup.simTimeSlider.setMajorTickSpacing(1);
        thesetup.simTimeSlider.setMinorTickSpacing(1);
        thesetup.simTimeSlider.setPaintLabels(true);
        thesetup.simTimeSlider.setPaintTicks(true);
        thesetup.simTimeSlider.setSnapToTicks(true);
        thesetup.simTimeSlider.setLabelTable(temptable);
        thesetup.simTimeSlider.addChangeListener(AvroraGui.instance);

        //Delay spinner
        thesetup.simTimeDelaySpinner = new SpinnerNumberModel();
        thesetup.simTimeDelaySpinner.setValue(new Integer(0));
        thesetup.simTimeDelaySpinner.setMinimum(new Integer(0));
        thesetup.simTimeDelaySpinner.setStepSize(new Integer(1));
        thesetup.simTimeDelaySpinner.addChangeListener(AvroraGui.instance);

        //Cycle/Instruction spinner
        thesetup.simTimeCycleSpinner = new SpinnerNumberModel();
        thesetup.simTimeCycleSpinner.setValue(new Integer(0));
        thesetup.simTimeCycleSpinner.setMinimum(new Integer(0));
        thesetup.simTimeCycleSpinner.setStepSize(new Integer(1));
        thesetup.simTimeCycleSpinner.addChangeListener(AvroraGui.instance);

        //Cycle/Instruction selector
        Vector iorcvector = new Vector();
        iorcvector.add("cycles");
        iorcvector.add("instructions");
        thesetup.simTimeIorCSelect = new JComboBox(iorcvector);
        thesetup.simTimeIorCSelect.addActionListener(AvroraGui.instance);

        //Toolbar
        thesetup.simTimeToolbar = new JToolBar("Avrora Simulation Toolbar");
        thesetup.simTimeToolbar.setFloatable(false);
        JButton newbutton;
        newbutton = makeSimButton("Rewind24", REWIND, "Slow down simulation", "Slower", AvroraGui.instance);
        thesetup.simTimeToolbar.add(newbutton);
        newbutton = makeSimButton("Stop24", STOP, "Stop the simulation", "Stop", AvroraGui.instance);
        thesetup.simTimeToolbar.add(newbutton);
        newbutton = makeSimButton("Pause24", PAUSE, "Pause the simulation", "Pause", AvroraGui.instance);
        thesetup.simTimeToolbar.add(newbutton);
        newbutton = makeSimButton("Play24", RESUME, "Resume a paused/single stepped simulation or start sim", "Play", AvroraGui.instance);
        thesetup.simTimeToolbar.add(newbutton);
        newbutton = makeSimButton("FastForward24", FASTFORWARD, "Increase simulation speed", "Faster", AvroraGui.instance);
        thesetup.simTimeToolbar.add(newbutton);
        thesetup.simTimeToolbar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        //Let's create the panel that holds everything we want
        thesetup.simTimeEverything = new JPanel();
        thesetup.simTimeEverything.setLayout(new BorderLayout());

        //Create a subpanel to hold delay/time options (The slider and spinners)
        JPanel spinnersubpanel = new JPanel();
        spinnersubpanel.setLayout(new BoxLayout(spinnersubpanel, BoxLayout.X_AXIS));
        JLabel delayfor = new JLabel(" Delay for ");
        delayfor.setHorizontalAlignment(JLabel.RIGHT);
        spinnersubpanel.add(delayfor);
        spinnersubpanel.add(new JSpinner(thesetup.simTimeDelaySpinner));
        JLabel msevery = new JLabel("  ms every  ");
        msevery.setHorizontalAlignment(JLabel.CENTER);
        spinnersubpanel.add(msevery);
        spinnersubpanel.add(new JSpinner(thesetup.simTimeCycleSpinner));
        thesetup.simTimeIorCSelect.setPreferredSize(new Dimension(50, 20));
        thesetup.simTimeIorCSelect.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        spinnersubpanel.add(thesetup.simTimeIorCSelect);
        spinnersubpanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel tempsubpanel = new JPanel();
        tempsubpanel.setLayout(new BorderLayout());
        tempsubpanel.add(spinnersubpanel, BorderLayout.NORTH);
        tempsubpanel.add(thesetup.simTimeSlider, BorderLayout.SOUTH);
        tempsubpanel.setPreferredSize(new Dimension(430, 80));

        thesetup.simTimeEverything.add(thesetup.simTimeToolbar, BorderLayout.WEST);
        thesetup.simTimeEverything.add(tempsubpanel, BorderLayout.EAST);

        thesetup.simTimeEverything.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Manage Simulation Time"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        return thesetup;
    }

    /**
     * This function checks to see if an event was caused by
     * this panel.  If so, it reacts to it.
     *
     * @return true if this panel caused the event, otherwise false
     */
    public boolean checkAndDispatch(ActionEvent e) {
        String cmd = e.getActionCommand();

        Simulation sim = AvroraGui.instance.getSimulation();
        if (STOP.equals(cmd)) {
            sim.stop();
            AvroraGui.instance.stopPaintThread();
            return true;

        } else if (RESUME.equals(cmd)) {
            //If a sim is paused, resume it...if a sim is single stepped paused, go to next instruction
            //otherwise, start the sim
            if (sim.isPaused()) {
                sim.resume();
            } else if (simTimeSlider.getValue() == 5 && sim.isRunning())  {
                // TODO: implement stepping of simulation
                //sim.step();
            } else if (!sim.isRunning()) {

                //Set the correct terminal (that is, the debug terminal)
                PrintStream tempstream = new PrintStream(new DebugStream(AvroraGui.instance));
                Terminal.setOutput(tempstream);

                // TODO: reset monitor panels
                //clearMonitorPanels();
                sim.start();
                AvroraGui.instance.startPaintThread();
            } else {
                //this would most probably be run if we
                //hit play and a sim is already running unpaused...in which
                //case we do nothing (We DONT want to start two simulations
            }
            return true;
        } else if (PAUSE.equals(cmd)) {
            sim.pause();
            return true;
        } else if (REWIND.equals(cmd)) {
            //move slider to the left
            int slideValue = simTimeSlider.getValue();
            if (slideValue <= -1) {
                //do nothing because we are in custom mode
            } else if (slideValue == numofdefaults) {
                //do nothing because we are already running on the slowest setting
            } else {
                simTimeSlider.setValue(slideValue + 1);
            }
            return true;
        } else if (FASTFORWARD.equals(cmd)) {
            //move slider to the right
            int slideValue = simTimeSlider.getValue();
            if (slideValue <= 0) {
                //do nothing because we are in custom mode or at fastest
            } else {
                simTimeSlider.setValue(slideValue - 1);
            }
            return true;
        } else if (e.getSource() == simTimeIorCSelect) {
            //so we are making a change from cycles to instructions or vica versa
            updateSimChangeSpeedValues();
            return true;
        } else {
            return false; //this module did not cause the action;
        }
    }

    /**
     * This function checks to see if an event was caused by
     * this panel.  If so, it reacts to it.  Instead of looking
     * for action events, it looks for ChangeEvents.
     *
     * @return true if this panel caused the event, otherwise false
     */
    public boolean sliderAndSpinnerDispatch(ChangeEvent e) {
        if (e.getSource() == simTimeSlider) {
            changeSpinnerBasedOnSlider();  //will call updateSimChangeSpeedValues()
            return true;
        } else if (e.getSource() == simTimeDelaySpinner || e.getSource() == simTimeCycleSpinner) {
            //We need to set slider to custom if the values in spinner's
            //don't match a default
            boolean isadefault = false;
            int valueOfDelay = ((Integer) simTimeDelaySpinner.getValue()).intValue();
            int valueOfCycles = ((Integer) simTimeCycleSpinner.getValue()).intValue();
            if (valueOfDelay == 0 && valueOfCycles == 0) {
                //Both are zero, we shouldn't do anything.
                updateSimChangeSpeedValues();
                return true;
            }
            //So this runs when both are not zero...we scroll through looking to see if it's a default
            for (int i = 0; i < numofdefaults; i++) {
                if (simTimeDelayDefaults[i] == valueOfDelay && simTimeCycleDefaults[i] == valueOfCycles) {
                    isadefault = true;
                    simTimeSlider.setValue(i);
                }
            }
            //So if it's not at 0,0 and it's not a default, then
            //it must be custom
            if (!isadefault) {
                simTimeSlider.setValue(-1);
            }
            updateSimChangeSpeedValues();
            return true;
        } else {
            return false;  //this module did not cause ChangeEvent
        }
    }

    /**
     * this is called upon a change in the slider...it updates the spinner if necessary
     */
    private void changeSpinnerBasedOnSlider() {
        if (simTimeSlider.getValue() >= 0) //so it's not custom
        {
            int sliderValue = simTimeSlider.getValue();
            simTimeDelaySpinner.setValue(new Integer(simTimeDelayDefaults[sliderValue]));
            simTimeCycleSpinner.setValue(new Integer(simTimeCycleDefaults[sliderValue]));
            simTimeSlider.setValue(sliderValue); //to a looping change
            updateSimChangeSpeedValues();
        } else {
            updateSimChangeSpeedValues(); //custom - then we just leave as is
        }
    }

    //This function will grab spinner values and
    //feed them to visualAction
    //NOTE: this function is called several times (sometimes a couple times too many)
    //per change in timing values....so it was written to allow for that
    private int olddelay;
    private long oldinbetweenperiod;
    private boolean oldiorc;
    private int oldeventtype;

    private void updateSimChangeSpeedValues() {
        //Since this is called many times (the GUI responds to many events...some of which
        //aren't changes the simulator needs to know about), we compare the current values with
        //the values the last time this is called...if no change we don't notify the simulator
        int newdelay = ((Integer) simTimeDelaySpinner.getValue()).intValue();
        long newinbetweenperiod = ((Integer) simTimeCycleSpinner.getValue()).longValue();
        boolean newiorc;
        newiorc = !"cycles".equals(simTimeIorCSelect.getSelectedItem());

        int neweventtype;
        int slidervalue = simTimeSlider.getValue();
        if (slidervalue == 0) {
            neweventtype = 1; //run at full speed
        } else if (slidervalue == 1) {
            neweventtype = 0; //realtime
        } else if (slidervalue == 5) {
            neweventtype = 3; //singlestep
        } else {
            neweventtype = 2; //slow it down
        }

        if (newdelay == olddelay && newinbetweenperiod == oldinbetweenperiod && oldiorc == newiorc && oldeventtype == neweventtype) {
            //do nothing, because there has been no change
        } else {
            // TODO: change the actual simulation speed
            olddelay = newdelay;
            oldinbetweenperiod = newinbetweenperiod;
            oldiorc = newiorc;
            oldeventtype = neweventtype;
        }
    }

    //This is taken from the swing tutorial...to steamline adding buttons
    //to the simtoolbar
    private static JButton makeSimButton(String imageName, String actionCommand, String toolTipText, String altText, AvroraGui papp) {
        //Look for the image.
        String imgLocation = "images/" + imageName + ".gif";
        URL imageURL = AvroraGui.class.getResource(imgLocation);

        //Create and initialize the button.
        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(papp);

        if (imageURL != null) {                      //image found
            button.setIcon(new ImageIcon(imageURL, altText));
        } else {                                     //no image found
            button.setText(altText);
            System.err.println("Resource not found: " + imgLocation);
        }

        return button;
    }
}
