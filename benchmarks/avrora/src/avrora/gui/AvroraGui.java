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

import avrora.Version;
import avrora.sim.Simulation;
import avrora.sim.types.SensorSimulation;
import cck.util.Options;
import cck.util.Util;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.net.URL;

/**
 * The <code> AvroraGUI </code> is the top level GUI component.  It should
 * be inited by VisualAction.  It stores the physical, visible components
 * of the GUI and also starts the various threads
 *
 * @author UCLA Compilers Group
 */
public class AvroraGui implements ActionListener, ChangeListener {

    /**
     * This is the actual instance of AvroraGui that is used.  Since it's
     * static, it can be called by anything that adds the current package
     */
    public static AvroraGui instance;

    /**
     * This function should be called by VisualAction to actually
     * init the static reference to a physical AvroraGUI object
     *
     * @param opt Options specified from the command line
     * @param args file names specified from the command line
     */
    public static void init(Options opt, String[] args) {
        new AvroraGui(opt, args);
    }

    /**
     * This is a list of arguments passed by the command line that
     * <code> avrora.Main </code> did not process.  Most likely
     * it's a list of filenames.
     */
    public String[] args;

    //High level elements of the GUI

    /**
     * This is GUI - everything fits inside the master frame 
     */
    public JFrame masterFrame;

    /**
     * This menu bar now contols most of the options specified by the GUI
     * (this is the file, edit, options menus)
     */
    public SimMenuBar topMenu;

    /**
     * This handles the speed up/slow down, pause, stop, and start of a sim
     */
    public ManageSimTime simTimeBox;

    /**
     * All information on node topology is displayed and set via this class 
     */
    public ManageTopology topologyBox;

    /**
     * This holds all our monitor "chalkboards" for displaying the data they collect
     */
    public JTabbedPane monitorResults; //Holds all the different monitor results, as tabbed windows


    private JLabel versioningInfo; //holds the text that displays at the bottom of the GUI
    private JPanel monitorOptions; //holds the options for the current monitor

    //This is the debug panel
    private JTextArea debugOutput; //This holds basic debug information
    private JPanel debugPanel; //this is init in createDebugOutput

    private HashMap monitorTabMap;

    //For convuluted reasons it's important to store the
    //current monitor being displayed (see paint thread)
    private MonitorPanel currentMonitorDisplayed;

    private static final String AVRORA_VERSION = "Avrora "+Version.TAG;
    private static final String AVRORA_GUI_VERSION = "Avrora Gui v.0.3.2";
    private static final int PAINT_THREAD_SLEEP_TIME = 200;

    //A thread that will repaint the monitor window
    PaintThread newPaintThread;

    private SensorSimulation simulation;

    private AvroraGui(Options opts, String[] a) {

        instance = this;

        //Set the look and feel.
        initLookAndFeel();

        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        monitorTabMap = new HashMap();

        //Create and set up the window.
        masterFrame = new JFrame("Avrora GUI");
        masterFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        simulation = new SensorSimulation();

        args = a;

        //First create the north quadrant, which contains
        //the banner and the sim toolbar

        this.simTimeBox = ManageSimTime.createManageSimTime();

        this.topMenu = SimMenuBar.createSimMenuBar();
        masterFrame.setJMenuBar(this.topMenu.menuBar);

        //Let's create a subpanel for displaying simtoolbar and file selection
        JPanel toolAndFile = new JPanel();
        toolAndFile.setLayout(new BorderLayout());
        toolAndFile.add(this.simTimeBox.simTimeEverything, BorderLayout.WEST);

        //The tool bar goes in the north quadrant
        masterFrame.getContentPane().add(toolAndFile, BorderLayout.NORTH);

        //In the center is the tabs with all the monitor results
        this.createDebugOutput();
        this.createMonitorResults(); //Created the tabbed panes for monitor results

        masterFrame.getContentPane().add(this.monitorResults, BorderLayout.CENTER);

        //The west quadrant contains the topology visual
        JPanel westPanel = new JPanel();
        westPanel.setLayout(new GridLayout(2, 1));

        //westPanel.add(this.topologyVisual);
        this.topologyBox = ManageTopology.createManageTopology();
        westPanel.add(this.topologyBox.topologyVisual);


        //init monitor options to blank panel, with the typical border
        this.monitorOptions = new JPanel();
        this.monitorOptions.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Monitor Options"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        westPanel.add(this.monitorOptions);
        masterFrame.getContentPane().add(westPanel, BorderLayout.WEST);

        //South holds versioning info
        this.createVersioningInfo();
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BorderLayout());
        //Add blank JPanel so our button is right size
        JPanel blankPanel = new JPanel();
        southPanel.add(blankPanel, BorderLayout.CENTER);

        southPanel.add(this.versioningInfo, BorderLayout.SOUTH);
        masterFrame.getContentPane().add(southPanel, BorderLayout.SOUTH);
    }

    /**
     * A {@link Simulation} object holds data about the current sim running
     *
     * @return Information about the current simulation running
     */
    public Simulation getSimulation() {
        return simulation;
    }

    /**
     * Gets all monitors attached to the simulator.  Uses {@link GUIDefaults}
     *
     * @return List of all monitors
     */
    public List getMonitorList() {
        return GUIDefaults.getMonitorList();
    }

    /**
     * Gets all options for the GUI and simulator.  Uses {@link GUIDefaults}
     *
     * @return List of all monitors
     */
    public List getOptionList() {
        return GUIDefaults.getOptionList();
    }

    /**
     * This allows the GUI to respond to mouse clicks or other events.
     * It calls the various methods in subclasses that are responsible 
     * for modules on the screen
     *
     * @param e Holds information about what event happened
     */
    public void actionPerformed(ActionEvent e) {
        if (simTimeBox.checkAndDispatch(e)) {
        } else if (topMenu.checkAndDispatch(e)) {
        }
    }

     /**
     * Some modules (like a spinner) detect a state change and not an action.
     * Thus, visual action can handle both.  A specific hack here is that this
     * function changes the visual monitor options panel if the stat change
     * that occured as a change to visual monitor display panel.
     *
     * @param e Holds information about what event happened
     */
    public void stateChanged(ChangeEvent e) {
        if (simTimeBox.sliderAndSpinnerDispatch(e)) {
        } else if (e.getSource() == monitorResults) {
            //whenever we change the tab pane we have to change
            //the monitor options panel to correspond with the
            //right monitor

            JPanel monitorPanel = (JPanel) monitorResults.getComponentAt(monitorResults.getSelectedIndex());

            String titleOfPanel;

            //So now we have the actualy panel...we can feed that to our VisualAction
            //and it will return the corresponding options panel
            JPanel thePanel = getOptionsFromMonitor(monitorPanel);
            titleOfPanel = getMonitorName(monitorPanel);
            currentMonitorDisplayed = getMonitorPanel(monitorPanel);

            if (thePanel == null) {
                //then it must be the debug panel...let's set
                //the options panel to just a blank panel
                thePanel = new JPanel(false);
                titleOfPanel = "Monitor";
            }
            //Now thePanel is what we want to display
            monitorOptions.removeAll();
            monitorOptions.setLayout(new GridLayout(1, 1));
            monitorOptions.add(thePanel);
            //redo the title border, now with the title of the monitor
            monitorOptions.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(titleOfPanel + " Options"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            monitorOptions.validate();

        }
     }

    private MonitorPanel getMonitorPanel(JPanel monitorPanel) {
        MonitorPanel p = (MonitorPanel)monitorTabMap.get(monitorPanel);
        if ( p == null ) return null;
        return p;
    }

    private String getMonitorName(JPanel monitorPanel) {
        MonitorPanel p = (MonitorPanel)monitorTabMap.get(monitorPanel);
        if ( p == null ) return null;
        return p.name;
    }

    private JPanel getOptionsFromMonitor(JPanel monitorPanel) {
        MonitorPanel p = (MonitorPanel)monitorTabMap.get(monitorPanel);
        if ( p == null ) return null;
        return p.optionsPanel;
    }

    //This creates the "console window" inside the GUI
    private void createDebugOutput() {
        debugOutput = new JTextArea("Console initialized. " + "Textual output from Avrora will be displayed here.\n");
        debugOutput.setFont(new Font("Courier", Font.PLAIN, 14));
        debugOutput.setBackground(Color.BLACK);
        debugOutput.setForeground(Color.WHITE);
        debugOutput.setLineWrap(true);
        debugOutput.setWrapStyleWord(true);
        debugOutput.setEditable(false);
        JScrollPane debugScrollPane = new JScrollPane(debugOutput);
        debugScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        //Let's not set a preferred size...we'll just accept whatever we can get
        //debugScrollPane.setPreferredSize(new Dimension(800, 600));
        debugPanel = new JPanel();
        //this sets is to that the debug panel takes up the full scenen
        debugPanel.setLayout(new OverlayLayout(debugPanel));
        debugPanel.add(debugScrollPane);
    }

    //This function creates the monitor results tab window, with the
    //debug tab as the default tab displayed
    //Should be called after createDebugOutput()
    private void createMonitorResults() {
        monitorResults = new JTabbedPane(JTabbedPane.BOTTOM);

        //By default, only the debug tab is displayed
        //We should set it so that it takes up the full panel
        debugPanel.setPreferredSize(new Dimension(monitorResults.getSize()));
        monitorResults.addTab("Debug Information", debugPanel);

        monitorResults.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("View Monitors"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        monitorResults.addChangeListener(this);
    }


    private void createVersioningInfo() {
        versioningInfo = new JLabel(AVRORA_VERSION + "; " + AVRORA_GUI_VERSION, SwingConstants.RIGHT);
    }

    /**
    * Used by DebugStream to allow writing to the debug window.  This might
    * be changed soon - Ben didn't like the way this was done (not efficient).
    * @param b generally just one letter that needs to be output
    */
    public void debugAppend(String b) {
        debugOutput.append(b);
    }

    /**
     * Once the GUI has been "created" we call this function
     * to physically display it to the screen
     */
    public void showGui() {
        //Display the window.

        masterFrame.pack();  //set to min size the components allow if its not maxed
        masterFrame.setExtendedState(JFrame.MAXIMIZED_BOTH); //will max window
        masterFrame.setVisible(true);
    }

    //The swing tutorial had this code, to allow it to set up
    //the default look and feel
    //Specify the look and feel to use.  Valid values:
    //null (use the default), "Metal", "System", "Motif", "GTK+"
    static final String LOOKANDFEEL = null;

    //Provided by swing tutorial to set up default look and feel
    private static void initLookAndFeel() {
        String lookAndFeel;

        if (LOOKANDFEEL != null) {
            if ("Metal".equals(LOOKANDFEEL)) {
                lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
            } else if ("System".equals(LOOKANDFEEL)) {
                lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            } else if ("Motif".equals(LOOKANDFEEL)) {
                lookAndFeel = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
            } else if ("GTK+".equals(LOOKANDFEEL)) { //new in 1.4.2
                lookAndFeel = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
            } else {
                System.err.println("Unexpected value of LOOKANDFEEL specified: " + LOOKANDFEEL);
                lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
            }

            try {
                UIManager.setLookAndFeel(lookAndFeel);
            } catch (ClassNotFoundException e) {
                System.err.println("Couldn't find class for specified look and feel:" + lookAndFeel);
                System.err.println("Did you include the L&F library in the class path?");
                System.err.println("Using the default look and feel.");
            } catch (UnsupportedLookAndFeelException e) {
                System.err.println("Can't use the specified look and feel (" + lookAndFeel + ") on this platform.");
                System.err.println("Using the default look and feel.");
            } catch (Exception e) {
                System.err.println("Couldn't get specified look and feel (" + lookAndFeel + "), for some reason.");
                System.err.println("Using the default look and feel.");
                e.printStackTrace();
            }
        }
    }

    //Provided by swing tutorial - just used to load images
    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    protected static ImageIcon createImageIcon(String path) {
        URL imageURL = AvroraGui.class.getResource(path);

        if (imageURL == null) {
            throw Util.failure("Resource not found: " + path);
        } else {
            return new ImageIcon(imageURL);
        }
    }

    /**
     * This function will dispatch a new thread
     * that will repaint our dynamic monitor window if new
     * data has been collected
     */
    public void startPaintThread() {
        newPaintThread = new PaintThread();
        newPaintThread.start();
    }

    /**
     * This function will dispatch a new thread
     * that will repaint our dynamic monitor window if new
     * data has been collected
     */
    public void stopPaintThread() {
    }

    /**
     * This function creates an options panel and a display panel
     * for a non-global visual monitor.  It the displays the default
     * simple look for the panels until the simulator is actually run.
     * It also registers the monitor panel pair in the <code> monitorTabMap
     * </code>
     *
     * @param name Name of the monitor
     * @return The pair of panels wrapped together in one class
     */
    public MonitorPanel createMonitorPanel(String name) {
        JPanel panel = new JPanel(false);  //This is the panel passed to the monitor
        JLabel filler = new JLabel("This panel will update once the simulator is run.");
        filler.setHorizontalAlignment(JLabel.CENTER);
        panel.setLayout(new GridLayout(1, 1));
        panel.add(filler);

        //Actually display the panel...add it to the tab
        monitorResults.addTab(name, panel);

        //Now let's create the options panel
        JPanel optionsPanel = new JPanel(false);
        JLabel optionsFiller = new JLabel("Options for the monitor can be set here. ");
        optionsPanel.setLayout(new GridLayout(1, 1));
        optionsPanel.add(optionsFiller);

        MonitorPanel p = new MonitorPanel(name, panel, optionsPanel);
        monitorTabMap.put(panel, p);
        return p;
    }

    /**
     * This will remove a visual monitor panel
     * from the tabbed pane.  It will also remove the <code>
     * MonitorPanel </code> instance from our global list of monitor
     * panels.  It will NOT destory the monitor options panel, but so
     * long as the panel is never displayed again this isn't an issue.
     *
     * @param p The MonitorPanel that needs to be removed
     */
    public void removeMonitorPanel(MonitorPanel p) {
        int i = monitorResults.indexOfTab(p.name);
        monitorResults.removeTabAt(i);
        monitorTabMap.remove(p.displayPanel);
    }

    //TODO: Change to make it actually work with global monitors
    //TODO: Add a method that kills this thread
    /**
     * This thread will call the various monitor
     * update and repaint methods for whatever monitor
     * is currently being display in real time.
     */
    public class PaintThread extends Thread {
        public void run() {
            try {

                //So we need to see if
                //a monitor result is currently
                //being display...if so, repaint it
                //and sleep for the specified amount of time
                while (getSimulation().isRunning()) {
                    if (monitorResults.getSelectedIndex() != 0) {
                        if (currentMonitorDisplayed == null) {
                            //we need to get all the nodes that are of type visualradio monitor
                            Vector allMons = VisualRadioMonitor.allCurrentMonitors;
                            for (Enumeration e = allMons.elements(); e.hasMoreElements();) {
                                VisualMonitor tempMon = (VisualMonitor) e.nextElement();
                                tempMon.updateDataAndPaint();
                            }
                        } else {
                            currentMonitorDisplayed.paint();
                        }
                    }
                    Thread.currentThread().sleep(PAINT_THREAD_SLEEP_TIME);
                }
            } catch (InterruptedException except) {
                //If interrupted, do nothing
            }
        }

    }
}

