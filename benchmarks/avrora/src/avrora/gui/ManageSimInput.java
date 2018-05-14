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

import avrora.Defaults;
import avrora.core.LoadableProgram;
import avrora.sim.Simulation;
import avrora.sim.platform.PlatformFactory;
import cck.util.Option;
import cck.util.Util;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;


/**
 * From a high level view, this class handles "input to the simulator," 
 * which entails entering files that can be loaded as program as well as 
 * specifiying options.  Node locations and monitor selection are NOT
 * handeled by this class.  This class is now only utilized by the
 * menu bar and there are no visual elements besides dialog boxes that this 
 * class owns.
 */
public class ManageSimInput {

    File currentFileAllInfo;
    JLabel currentFile;

    JButton openFile;

    JFileChooser fc;
    int stageForFileSelection; //we display different screens depending on where the user is
    JButton nextButton;
    JButton backButton;

    JDialog setOptionsDialog;
    LinkedList optionsDialogValues; //holds the current values we are setting in the options dialog
    JButton optionsDialogUpdate;

    JDialog fileSelectionDialog;
    JButton fileSelectionDialogUpdate;
    SpinnerNumberModel numOfNodesSpinner;


    /**
     * This is the "constructor" for this class.  It inits all the dialog boxes where 
     * appropiate
     * @return An object of ManageSimInput which can then be used to input to sim
     */
    public static ManageSimInput createManageSimInput() {
        ManageSimInput thesetup = new ManageSimInput();

        //Set up file chooser box...so if user clicks open file it opens a filechooser
        thesetup.fc = new JFileChooser();

        return thesetup;
    }

    /**
     * This creates the dialog box that asks for sim options
     * Called by SimMenuBar
     */
    public void createSetOptionsDialog() {
        //Make sure we have nice window decorations.
        //initLookAndFeel();
        JDialog.setDefaultLookAndFeelDecorated(true);

        setOptionsDialog = new JDialog(AvroraGui.instance.masterFrame, "Set Simulator Options");

        //Now we create a JPanel that will be linked to the dialog
        JPanel internalPanel = new JPanel();
        internalPanel.setOpaque(true);
        internalPanel.setLayout(new BorderLayout());

        //Let's add a title banner
        JLabel dialogBanner = new JLabel();
        dialogBanner.setText("Close window to cancel");
        internalPanel.add(dialogBanner, BorderLayout.NORTH);

        JPanel belowBannerPanel = new JPanel();
        belowBannerPanel.setLayout(new GridLayout(AvroraGui.instance.getOptionList().size(), 1));

        //Now we go to VisualAction and grab a list of the options
        //that we can set
        optionsDialogValues = new LinkedList();
        Iterator optionIter = AvroraGui.instance.getOptionList().iterator();
        Option currentOption;
        while (optionIter.hasNext()) {
            currentOption = (Option) optionIter.next();
            //NOTE: YOU COULD DETECT SPECIAL CASE HERE (e.g. if(option.getName().equals("MY SPECIAL OPTION"));
            belowBannerPanel.add(addOption(currentOption));
        }

        //Border of 30 on the outside
        belowBannerPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        internalPanel.add(belowBannerPanel, BorderLayout.CENTER);

        //Add the "Update" button
        optionsDialogUpdate = new JButton();
        optionsDialogUpdate.setText("Update");
        optionsDialogUpdate.setToolTipText("Click to update the simulator options");
        optionsDialogUpdate.addActionListener(AvroraGui.instance);
        internalPanel.add(optionsDialogUpdate, BorderLayout.SOUTH);
        internalPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        //Add the panel to the dialog
        setOptionsDialog.setContentPane(internalPanel);

        //Sizes things appropiatly
        setOptionsDialog.pack();

    }

    /**
     * This creates the dialog box that adds files ("nodes") to the simulator
     */
    public void createFileSelectionDialog() {

        stageForFileSelection = 0; //so we start on the first page.


        //We should init all the stuff here
        //The stuff that is init below is NOT necessarily displayed
        //at this time
        //button that holds "Open File"
        openFile = new JButton();
        openFile.setToolTipText("Get a simulation file");
        openFile.setMaximumSize(new Dimension(75, 40));
        openFile.addActionListener(AvroraGui.instance);
        openFile.setText("Select File");

        //The label that displays/stores the file name
        currentFile = new JLabel("<SELECT A FILE>");
        currentFile.setHorizontalAlignment(JLabel.CENTER);
        //Set border so its not too close to the button

        //Set up file chooser box...so if user clicks open file it opens a filechooser
        fc = new JFileChooser();

        //Set up the spinner
        numOfNodesSpinner = new SpinnerNumberModel();
        numOfNodesSpinner.setValue(new Integer(1));
        numOfNodesSpinner.setMinimum(new Integer(1));
        numOfNodesSpinner.setStepSize(new Integer(1));
        numOfNodesSpinner.addChangeListener(AvroraGui.instance);

        //Add the Update Button
        fileSelectionDialogUpdate = new JButton();
        fileSelectionDialogUpdate.setText("Update");
        fileSelectionDialogUpdate.setToolTipText("Click to add the node(s) to the sim");
        fileSelectionDialogUpdate.addActionListener(AvroraGui.instance);

        //Add the next button
        nextButton = new JButton();
        nextButton.setText("NEXT>>");
        nextButton.addActionListener(AvroraGui.instance);

        //Add the back button
        backButton = new JButton();
        backButton.setText("<<BACK");
        backButton.addActionListener(AvroraGui.instance);

        //We should reset the "currentFile" data
        currentFileAllInfo = null;

        //Make sure we have nice window decorations.
        //initLookAndFeel();
        JDialog.setDefaultLookAndFeelDecorated(true);
        fileSelectionDialog = new JDialog(AvroraGui.instance.masterFrame, "Add Nodes to Simulation");

        updateFileSelectionDialog();
    }

    private void updateFileSelectionDialog() {

        //Now we create a JPanel that will be linked to the dialog
        JPanel internalPanel = new JPanel();
        internalPanel.setOpaque(true);
        internalPanel.setLayout(new BorderLayout());

        JPanel belowBannerPanel = new JPanel();
        JPanel navigationPanel = new JPanel();

        //so the top is the same no matter what...
        //depending on what page is displayed, that determines the next step
        if (stageForFileSelection == 0) {
            //Let's add a title banner
            JLabel dialogBanner = new JLabel();
            dialogBanner.setText("Step 1: Specify File");
            internalPanel.add(dialogBanner, BorderLayout.NORTH);

            //Create the actual layout
            belowBannerPanel.setLayout(new BorderLayout());
            JPanel tempPanel = new JPanel();
            tempPanel.add(openFile);
            belowBannerPanel.add(tempPanel, BorderLayout.NORTH);
            belowBannerPanel.add(currentFile, BorderLayout.SOUTH);

            //We want the navigation panel to read "NEXT>>"
            navigationPanel.setLayout(new BorderLayout());
            navigationPanel.add(nextButton, BorderLayout.EAST);

        } else if (stageForFileSelection == 1) {
            //Let's add a title banner
            JLabel dialogBanner = new JLabel();
            dialogBanner.setText("Step 2: Specify Number of Nodes");
            internalPanel.add(dialogBanner, BorderLayout.NORTH);

            belowBannerPanel.setLayout(new BorderLayout());
            belowBannerPanel.add(currentFile, BorderLayout.NORTH);
            JLabel userInstr = new JLabel("Select the number of duplicate nodes based upon selected file");
            currentFile.setHorizontalAlignment(JLabel.CENTER);
            belowBannerPanel.add(userInstr, BorderLayout.CENTER);
            JSpinner tempSpin = new JSpinner(numOfNodesSpinner);
            JPanel tempPanel2 = new JPanel();
            tempPanel2.add(tempSpin);
            belowBannerPanel.add(tempPanel2, BorderLayout.SOUTH);

            navigationPanel.setLayout(new BorderLayout());
            JPanel tempPanel = new JPanel();
            tempPanel.setLayout(new FlowLayout());
            tempPanel.add(backButton);
            tempPanel.add(fileSelectionDialogUpdate);
            navigationPanel.add(tempPanel, BorderLayout.EAST);
        }

        internalPanel.add(belowBannerPanel, BorderLayout.CENTER);
        internalPanel.add(navigationPanel, BorderLayout.SOUTH);

        internalPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        //Set reasonable size
        internalPanel.setPreferredSize(new Dimension(400, 150));

        //Add the panel to the dialog
        fileSelectionDialog.setContentPane(internalPanel);

        //Sizes things appropiatly
        fileSelectionDialog.pack();
        fileSelectionDialog.validate();

    }


    /**
     * Called when first init the dialog box..it actually adds the look
     * of the option to the box
     * Each "type" of option has a special case here: that is, if it's a boolean option
     * we provide it with a checkbox; if it's a numeric option we provide it with a numeric spinner
     *
     * @param theOption The Option that we need to create a visual widget for
     *
     * @return A visual widget that can be displayed (it will vary depending on option type)
     */
    private Component addOption(Option theOption) {
        if (theOption instanceof Option.Bool) {
            JCheckBox theCheckBox = new JCheckBox(theOption.getName());
            theCheckBox.setSelected(((Option.Bool) theOption).get());
            theCheckBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            optionsDialogValues.add(theCheckBox);

            //Tool tip setup
            //Couldn't get a method that was worth anything to work here
            //The problem is that you can only print help to the terminal...you
            //can't access an option's help string....it's protected.  Any way around it
            //would just be a hack...maybe look into changing the Option class if this feature is important
            //would just need to add a "getHelpString()" function
            //theCheckBox.setToolTipText(OptionExtend.getDescription(theOption));

            return theCheckBox;
        } else if (theOption instanceof Option.Double) {
            JPanel outerShell = new JPanel();
            outerShell.setLayout(new BorderLayout());
            //This spinner module allows the user to use up and down arrows to change seconds values
            SpinnerNumberModel theSpinner = new SpinnerNumberModel();
            theSpinner.setValue(new Double(((Option.Double) theOption).get()));

            JLabel spinnerTitle = new JLabel();
            spinnerTitle.setText(theOption.getName() + " ");

            outerShell.add(spinnerTitle, BorderLayout.WEST);
            outerShell.add(new JSpinner(theSpinner), BorderLayout.CENTER);
            outerShell.setPreferredSize(new Dimension(150, 30));
            outerShell.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            optionsDialogValues.add(theSpinner);
            return outerShell;
        } else if (theOption instanceof Option.Str) {
            JPanel outerShell = new JPanel();
            outerShell.setLayout(new BorderLayout());
            JTextField theText = new JTextField();
            theText.setText(((Option.Str) theOption).get());

            JLabel textBoxTitle = new JLabel();
            textBoxTitle.setText(theOption.getName() + " ");

            outerShell.add(textBoxTitle, BorderLayout.WEST);
            outerShell.add(theText, BorderLayout.CENTER);
            outerShell.setPreferredSize(new Dimension(150, 30));
            outerShell.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            optionsDialogValues.add(theText);
            return outerShell;
        }
        return null;
    }

    /**
     * This actually is called once Update is pressed - it updates the
     * option's value
     *
     * @param theOption The option that will physically be changed
     * @param theComponent The visual widget, passed so we know the new option value
     */
    private void updateOption(Option theOption, Object theComponent) {
        if (theOption instanceof Option.Bool) {
            //If bool, then it's a checkbox
            theOption.set(Boolean.toString(((JCheckBox) theComponent).isSelected()));
        } else if (theOption instanceof Option.Double) {
            //first get the spinnner model
            SpinnerNumberModel tempSpinnerModel = (SpinnerNumberModel)theComponent;
            //frikin types
            theOption.set(Double.toString(tempSpinnerModel.getNumber().doubleValue()));
        } else if (theOption instanceof Option.Str) {
            //it's a JTextField
            JTextField theText = (JTextField)theComponent;
            theOption.set(theText.getText());
        }

    }

    /**
     * This function sees if an event was caused by
     * this panel.  If so, it reacts to it by calling other methods.
     *
     * @return true if indeed this class caused the event
     */
    public boolean checkAndDispatch(ActionEvent e) {
        //if open file button was pushed...load file chooser

        if (e.getSource() == optionsDialogUpdate) {
            return optionsUpdate();
        } else if (e.getSource() == openFile) {
            return openFileUpdate();
        } else if (e.getSource() == fileSelectionDialogUpdate) {
            return fileSelectionUpdate();
        } else if (e.getSource() == nextButton) {
            return nextButtonUpdate();
        } else if (e.getSource() == backButton) {
            return backButtonUpdate();
        } else {
            return false; //this module did not cause the action;
        }
    }

    private boolean openFileUpdate() {
        int returnVal = fc.showOpenDialog(null); //will center with screen, not window

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            currentFileAllInfo = fc.getSelectedFile();
        } else {
            //User cancelled the open file request, really we just do nothing
        }
        return true;
    }

    private boolean setOptionsUpdate() {
        createSetOptionsDialog();
        setOptionsDialog.setLocationRelativeTo(null); //center on screen
        setOptionsDialog.setVisible(true);
        return true;
    }

    private boolean optionsUpdate() {
        Iterator optionIter = AvroraGui.instance.getOptionList().iterator();
        Option currentOption;
        ListIterator componentIter = optionsDialogValues.listIterator(0);

        while (optionIter.hasNext()) {
            currentOption = (Option) optionIter.next();
            //NOTE: YOU COULD DETECT SPECIAL CASE HERE (e.g. if(option.getName().equals("MY SPECIAL OPTION"));

            updateOption(currentOption, componentIter.next());
        }
        //Destroy window
        setOptionsDialog.setVisible(false); //so really, this doesn't destroy it, but when the button is pushed
        //again, it will just re-create the dialog using the VisualAction values
        //This is slightly messy...the dialog stays around and then
        //get regenerated upon another button push


        return true;
    }

    private boolean backButtonUpdate() {
        stageForFileSelection = 0;
        updateFileSelectionDialog();
        return true;
    }

    private boolean nextButtonUpdate() {
        if (currentFileAllInfo == null) {
            return true;
        }

        stageForFileSelection = 1;
        updateFileSelectionDialog();
        return true;
    }

    private boolean fileSelectionUpdate() {
        //We don't want to add nodes if the user never
        //actually selected a file
        if (currentFileAllInfo == null) {
            return true;
        }

        LoadableProgram pp = new LoadableProgram(currentFileAllInfo);
        try {
            pp.load();
        } catch ( Exception e ) {
            // TODO: display exception loading file
            throw Util.failure(e.toString());
        }
        PlatformFactory pf = Defaults.getPlatform("mica2");
        Simulation s = AvroraGui.instance.getSimulation();

        int max = ((Integer) numOfNodesSpinner.getValue()).intValue();
        for (int i = 0; i < max; i++) {
            //add the stuff to the sim
            Simulation.Node n = s.createNode(pf, pp);
        }

        //We should redraw the table
        AvroraGui.instance.topologyBox.createSimpleAirTable();

        //And get rid of dialog box
        fileSelectionDialog.setVisible(false);
        return true;
    }
}
