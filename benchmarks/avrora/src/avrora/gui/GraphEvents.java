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

import cck.text.Terminal;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

/**
 * This is a generic helper class for visual monitors.  It's purpose is to help
 * graph events that occur in the simulator (constant this with <code> 
 * GraphNumbers </code>, which graph data values as time goes by.
 * For each event that occurs, a colored dot will appear.  The x-axis maps
 * time gone by.
 * <p>
 * Events must be inserted into this class in temporal order
 * <p>
 * This class is not yet done.  It needs to be more generic - right now it's
 * hacked to specifically allow LED output to be graphed.
 *
 * @author UCLA Compilers Group
 */
public class GraphEvents extends JPanel implements ChangeListener, AdjustmentListener {
    private MyVector[] publicNumbers; //access by monitors to add stuff
    private MyVector[] privateNumbers; //only accessed by paint
    private static final int VECSIZE = 5;
    private JPanel parentPanel;

    private Object vSync; //just a private sync variable

    /**
    * This is the bar that determines what part of
    * the graph is displayed
    */
    public JScrollBar horzBar;

    //All these fields can be set by options

    /**
     * number of pixels per x-axis value
     */
    public double stepsize;

    /**
     * The visual component for setting <code> stepsize </code>
     */
    public SpinnerNumberModel stepsizeVisual;

    private Color backColor; //color of background
    private Color tickColor; //color of tick marks/graph lines


    //The min and max values of the data in question: for VERTICAL sizing
    //the step size is the HORZ step size (there is no notion of vertical step size)
    /* Called by a monitor who wants to use this class
     * @param pminvalue For vertical sizing
     * @param pmaxvalue For vertical sizing
     * @param pstepsize For horz sizing - number of pixels per unit of x-axis
     */
    public GraphEvents(int pminvalue, int pmaxvalue, double pstepsize) {

        vSync = new Object();

        publicNumbers = new MyVector[VECSIZE];
        privateNumbers = new MyVector[VECSIZE];
        for (int i = 0; i < VECSIZE; i++) {
            publicNumbers[i] = new MyVector();
            privateNumbers[i] = new MyVector();
        }

        //Set option defaults
        Color lineColor = Color.GREEN;
        backColor = Color.BLACK; //default background color is black
        tickColor = Color.LIGHT_GRAY; //default tick mark color is gray
        int xAxisMajorTickMark = 20;
        int minvalue = pminvalue;
        int maxvalue = pmaxvalue;
        stepsize = pstepsize; //x-axis step size
    }

    /**
     * Returns a panel which can be displayed that contains the graph numbers
     * panel and a horz scrollbar at the bottom that makes changes viewing area easy
     *
     * @return What you should directly display to the screen
     */
    public JPanel chalkboardAndBar() {
        JPanel temppanel = new JPanel();
        temppanel.setLayout(new BorderLayout());
        horzBar = new JScrollBar(JScrollBar.HORIZONTAL);
        horzBar.addAdjustmentListener(this);

        //init the scroll bar
        updateHorzBar();

        JPanel innertemppanel = new JPanel();
        innertemppanel.setLayout(new OverlayLayout(innertemppanel));
        innertemppanel.add(this);
        temppanel.add(innertemppanel, BorderLayout.NORTH);

        //we need to adjust the this panel's preferred size
        //by subtracting the horz scrollbar's size from it
        Dimension newDimen = parentPanel.getSize();
        newDimen.height = newDimen.height - horzBar.getPreferredSize().height;
        this.setPreferredSize(newDimen);

        temppanel.add(this, BorderLayout.NORTH);
        temppanel.add(horzBar, BorderLayout.SOUTH);

        return temppanel;
    }

    /**
    * This function updates the scroll bar as new
    * numbers are added to the vector or if we decided to
    * jump to a certian value
    * if the paramter is true, it will set the scroll bar to be the new
    * max value...otherwise it just keeps value to whatver it used to be
    * Synchronized because GUI thread and paintthread will access the horz bar
    */
    public synchronized void updateHorzBar() {
        int newExtent = (int)((double)this.getSize().width / stepsize);

        int maxvalue;
        maxvalue = 0;

        //Here we have to find the "biggest" set of data and set maxvalue
        //to the largest datapoint
        for (int i = 0; i < VECSIZE; i++) {
            if (privateNumbers[i].size() != 0) {
                if (maxvalue < privateNumbers[i].getLast()) {
                    maxvalue = privateNumbers[i].getLast();
                }
            }
        }


        //to handle the case where we really don't need the bar
        if (maxvalue < newExtent) {
            //then we just have a scroll bar that does nothing
            horzBar.setValues(0, 0, 0, 0);
            return;
        }

        int newValue = horzBar.getValue();
        //we check to see if the bar is current at it's maximum...if so
        //then keep it that way despite adding new values
        if (horzBar.getValue() + horzBar.getModel().getExtent() == horzBar.getMaximum()) {
            newValue = maxvalue - newExtent;
        }
        horzBar.setValues(newValue, newExtent, 0, maxvalue);

    }

    /**
     * used by paint so it knows what value to start painting with
     */
    public int getHorzBarValue() {
        //Note: does this need to be synched?
        //Right now, no.
        return horzBar.getValue();
    }

    //Every option you can set has:
    //a) a getmethod which returns it's value
    //b) a setmethod which sets it's value
    //c) a visualSet method which returns a component that can be used to adjust/view its value
    //   (listeners are already set up and handeled by this class)

    /**
     * @return stepsize value
     */
    public double getStepSize() {
        return stepsize;
    }

    /**
     * @param pstepsize The value that step size should be set to
     */
    public void setStepSize(double pstepsize) {
        stepsize = pstepsize;
    }

    /**
     * This creates a visual object that the user
     * can interact with to set step size
     *
     * @return A panel that contains a spinner for setting step size
     */
    public JPanel visualSetStepSize() {
        if (stepsizeVisual == null) {
            createstepsizeVisual();
        }
        JPanel returnthis = new JPanel();
        returnthis.setLayout(new BorderLayout());
        JLabel stepSizeLabel = new JLabel("X-Axis Step Size: ");
        returnthis.add(stepSizeLabel, BorderLayout.WEST);
        JSpinner spinit = new JSpinner(stepsizeVisual);
        spinit.setPreferredSize(new Dimension(80, 20));
        returnthis.add(spinit, BorderLayout.EAST);
        returnthis.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return returnthis;
    }

    private void createstepsizeVisual() {
        stepsizeVisual = new SpinnerNumberModel();
        stepsizeVisual.setValue(new Double(stepsize));
        stepsizeVisual.setMinimum(new Double(0));
        stepsizeVisual.addChangeListener(this);
    }

    /**
     * This function creates the actual options panel
     * that can be displayed.  This allows the user to
     * set monitor options
     *
     * @return A panel (can be directly displayed) of all monitor options
     */
    public JPanel getOptionsPanel() {
        JPanel allOptions = new JPanel();
        allOptions.setLayout(new GridLayout(10, 1));
        //allOptions.setLayout(new BorderLayout());
        allOptions.add(visualSetStepSize());
        allOptions.add(new JPanel()); //filler so there is blank space
        return allOptions;
    }


    /**
     * Used in order to size thing correctly.  Should be called
     * right after the constructor is called
     */
    public void setParentPanel(JPanel pparentPanel) {
        parentPanel = pparentPanel;
    }

    /**
     * A monitor can add data using this function
     * On next repaint, it will be added to the graph
     *
     * @param vecnum an index that specified "which" event occured
     * @param anAddress the time that the event occured
     */
    public void addToVector(int vecnum, int anAddress) {
        //This is currently a hack - it should be changed to make it
        //more generic
        //0 -> transmit
        //1 -> receive
        //2 -> red
        //3 -> yellow
        //4 -> green

        synchronized (vSync) {
            publicNumbers[vecnum].add(anAddress);
        }
    }

    /**
    * This function is called by paint and it does what is necessary
    * to update the privateNumbers vector
    * @return true if it actually got some numbers, otherwise returns false
    */
    public boolean internalUpdate() {
        boolean somethingchanged;
        somethingchanged = false;
        synchronized (vSync) {
            for (int i = 0; i < VECSIZE; i++) {
                if (publicNumbers[i].size() == 0) {

                } else {
                    somethingchanged = true;
                    //so we need to take anything in private and move it to public

                    //do the move
                    try {
                        privateNumbers[i].addAll(publicNumbers[i]);
                    } catch (OutOfMemoryError e) {
                        //Note that it's possible to get an out of memory exception
                        //elsewhere, but "most probably" it will be here
                        Terminal.println("RAN OUT OF HEAP SPACE FOR MONITOR");
                        Terminal.println("SIZE OF MONITORS VECTOR AT THE TIME: " + Integer.toString(privateNumbers[i].size()));
                    }

                    publicNumbers[i].removeAllElements();
                }
            }
        }
        //and update the horz scroll bar to reflect the new values
        updateHorzBar();
        return somethingchanged;
    }

    /**
     * This actually paints the graph...note that it repaints the whole graph
     * everytime its called (to improve performance, we could make use of an update function)
     * The code here is actually faily ugly
     * but eh..
     */
    public void paint(Graphics g) {

        Dimension panelDimen = this.getSize();

        //Set up background color, will erase all previous lines
        g.setColor(backColor);
        g.fillRect(0, 0, panelDimen.width, panelDimen.height);

        //So basically, we have to find all the events that occur in between starting
        //value and ending
        int startingvalue = getHorzBarValue();

        //Let's output the horz tick lines here
        for (int i = 0; i < panelDimen.width; i = i + 100) {
            //vertical line and label
            g.setColor(tickColor);
            g.drawLine(i, 0, i, panelDimen.height);
            g.drawString(Integer.toString((int) ((double) i * stepsize + startingvalue)) + "ms", i, panelDimen.height - 10);

        }

        //Output line labels
        g.setColor(Color.BLUE);
        g.drawString("Transmit", 0, panelDimen.height / 4 - 10);

        g.setColor(Color.MAGENTA);
        g.drawString("Receive", 0, panelDimen.height / 2 - 10);

        g.setColor(tickColor);
        g.drawString("LEDs", 0, 3 * panelDimen.height / 4 - 10);


        for (int j = 0; j < VECSIZE; j++) {


            //ending value is size of panel or end of list, whichever is less
            int endingvalue;
            if (privateNumbers[j].size() == 0) {
                endingvalue = 0;
            } else if ((int)(double)panelDimen.width / stepsize < privateNumbers[j].get(privateNumbers[j].size() - 1)) {
                endingvalue = startingvalue + (int)((double)panelDimen.width / stepsize);
            } else {
                endingvalue = privateNumbers[j].get(privateNumbers[j].size() - 1);
            }

            int currentYPoint;
            if (j == 0)  //transmit
            {
                currentYPoint = panelDimen.height / 4;
                g.setColor(Color.BLUE);
            } else if (j == 1) //receive
            {
                currentYPoint = panelDimen.height / 2;
                g.setColor(Color.MAGENTA);
            } else if (j == 2) //RED
            {
                currentYPoint = 3 * panelDimen.height / 4 - 20;
                g.setColor(Color.RED);
            } else if (j == 3) //YELLOW
            {
                currentYPoint = 3 * panelDimen.height / 4;
                g.setColor(Color.YELLOW);
            } else //GREEN
            {
                currentYPoint = 3 * panelDimen.height / 4 + 20;
                g.setColor(Color.GREEN);
            }

            //We have to do a check to see if, for this data series
            //we are already past what we need to draw
            if (privateNumbers[j].size() == 0 || privateNumbers[j].get(privateNumbers[j].size() - 1) <= startingvalue) {
                //we don't draw anything
            } else {


                //Let's fastfoward to starting value
                int i = 0;
                while (privateNumbers[j].get(i) < startingvalue) {
                    i++;
                }

                //Here we do this check to see if we start our area
                //with a light enabled
                boolean startColorOn = false;
                if (i % 2 == 1) {
                    //if odd, then we start with a color being on from the beginning
                    startColorOn = true;
                }

                //Start here, instead of drawing output list of values;
                //Make sure to check to see if they are indeed in sorted order

                //so now we draw!
                while (privateNumbers[j].get(i) <= endingvalue || startColorOn) {
                    //If it's a color, we draw a bar between two adjacent points
                    //anything else we just draw dots
                    if (j == 2 || j == 3 || j == 4) {
                        int currentXPoint1;
                        if (startColorOn) {
                            currentXPoint1 = 0;
                            startColorOn = false;
                        } else {
                            currentXPoint1 = (int)((double)(privateNumbers[j].get(i) - startingvalue) * stepsize);
                            i++;
                        }
                        int currentXPoint2;
                        if (i < privateNumbers[j].size()) {
                            currentXPoint2 = (int)((double)(privateNumbers[j].get(i) - startingvalue) * stepsize);
                            i++;
                        } else {
                            currentXPoint2 = panelDimen.width;
                        }
                        g.fillOval(currentXPoint1 - 2, currentYPoint - 2, 4, 4);
                        g.fillOval(currentXPoint2 - 2, currentYPoint - 2, 4, 4);
                        g.drawLine(currentXPoint1, currentYPoint, currentXPoint2, currentYPoint);
                    } else {
                        int currentXPoint = (int)((double)(privateNumbers[j].get(i) - startingvalue) * stepsize);
                        i++;
                        g.fillOval(currentXPoint - 2, currentYPoint - 2, 4, 4);
                    }
                    if (i >= privateNumbers[j].size())
                        break;
                }
            }
        }


    }

    /**
     * this function processes the monitor options and re-sets the internal variables appropiatly
     *
     *@param e Information about what event occured
     */
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == stepsizeVisual) {
            stepsize = ((Double) stepsizeVisual.getValue()).doubleValue();
            repaint();
        }
    }

    /**
     * If the scroll bar was adjusted, we should repaint.  This method
     * takes care of that
     */
    public void adjustmentValueChanged(AdjustmentEvent e) {
        repaint();
    }

    /**
     * We don't want to store millions of Integer, but we still want
     * an array that grows...so we define a MyVector class just for that
     */
    public class MyVector {
        int[] vec;
        int current;

        public MyVector() {
            vec = new int[100];
            current = 0;
        }

        public void add(int a) {
            if (current == vec.length) {
                //create a new array of double the size
                //Isn't there some proof that says that's good?  Go CS112!
                int[] vec2 = new int[vec.length * 2];
                System.arraycopy(vec, 0, vec2, 0, vec.length);
                vec = vec2;
            }
            vec[current] = a;
            current++;
        }

        public int get(int i) {
            if (i < current && i >= 0)
                return vec[i];
            else
                return 0; //this is sorta stupid, but we'll be careful
        }

        public void addAll(MyVector a) {
            for (int i = 0; i < a.size(); i++) {
                add(a.get(i));
            }
        }

        public int size() {
            return current;
        }

        public int getLast() {
            if (current > 0)
                return this.get(current - 1);
            else
                return 0;

        }

        public void removeAllElements() {
            vec = new int[100];
            current = 0;
        }
    }
}
