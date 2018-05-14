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

import cck.stat.Sequence;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

/**
 * The class assists visual monitors with graphing time-series data
 * values.  It visually displays them using a line graph
 */
public class GraphNumbers extends JPanel implements ChangeListener, AdjustmentListener {

    private Sequence publicNumbers; //access by monitors to add stuff
    private Sequence privateNumbers; //only accessed by paint
    private final JPanel parentPanel;

    private static final int valueMargin = 5;

    /**
    * This is the bar that determines what part of
    * the graph is displayed
    */
    public JScrollBar horzBar;

    TimeScale timeScale;

    /**
     * The visual widget that sets the step size
     */
    public SpinnerNumberModel stepsizeVisual;

    /**
     * The max value of the y-axis
     */
    public int maxvalue;

    /**
     * The visual wdiget that sets the max value for the y-axis
     */
    public SpinnerNumberModel maxvalueVisual;

    //options not done yet
    private Color lineColor; //color of line that is drawn
    private Color backColor; //color of background
    private Color cursorColor;
    private int minvalue;
    protected static final int VALUE_SCALE_WIDTH = 15;
    protected static final int MIN__VALUE__TICK = 3;

    //Other features to add:
    //ability to user this class "on top of" another GraphNumbers class => multiple lines on one graph
    //ability to see/get the value of the line based upon a mouse over/mouse click event
    //double check to see if scroll bar is sizing correctly

    /**
     * Called by a visual action that wants this class to help with displaying time series data
     */
    public GraphNumbers(JPanel parent) {

        parentPanel = parent;

        publicNumbers = new Sequence();
        privateNumbers = new Sequence();

        //Set option defaults
        lineColor = Color.GREEN; //default line color is green
        backColor = Color.BLACK; //default background color is black
        cursorColor = Color.CYAN;
        minvalue = 0; //min and max values for the y-axis
        maxvalue = 0;
        timeScale = new TimeScale();
    }

    /**
     * Returns a panel which can be displayed that contains the graph numbers
     * panel and a horz scrollbar at the bottom that makes changes viewing area easy
     * @return Basically, what you want to display to the screen
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
     * Synchronized because GUI thread and paintthread will access the horz bar
     */
    public synchronized void updateHorzBar() {

        int width = this.getSize().width;
        long maxtime = privateNumbers.size();
        int newExtent = timeScale.getExtent(width, maxtime);
        int size = timeScale.getScrollBarSize(maxtime);
        //to handle the case where we really don't need the bar
        if (size < newExtent) {
            //then we just have a scroll bar that does nothing
            horzBar.setValues(0, 0, 0, 0);
            return;
        }

        int newValue = horzBar.getValue();
        //we check to see if the bar is current at it's maximum...if so
        //then keep it that way despite adding new values
        if (newValue + horzBar.getModel().getExtent() == horzBar.getMaximum()) {
            newValue = size - newExtent;
        }

        horzBar.setValues(newValue, newExtent, 0, size);
    }

    /**
     * This is called to get the visual widget that the user can set step 
     * size with.
     * @return A panel containing a spinner that controls stepsize value
     */
    public JPanel getZoomLevelOption() {
        if (stepsizeVisual == null) {
            makeZoomLevelOption();
        }
        JPanel returnthis = new JPanel();
        returnthis.setLayout(new BorderLayout());
        JLabel stepSizeLabel = new JLabel("Zoom Level: ");
        returnthis.add(stepSizeLabel, BorderLayout.WEST);
        JSpinner spinit = new JSpinner(stepsizeVisual);
        spinit.setPreferredSize(new Dimension(80, 20));
        returnthis.add(spinit, BorderLayout.EAST);
        returnthis.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return returnthis;
    }

    private void makeZoomLevelOption() {
        stepsizeVisual = new SpinnerNumberModel();
        stepsizeVisual.setValue(new Integer(timeScale.getZoom()+1));
        stepsizeVisual.setMinimum(new Integer(1));
        stepsizeVisual.setMaximum(new Integer(timeScale.getMaxZoom()+1));
        stepsizeVisual.addChangeListener(this);
    }

    /**
    * This is called to get the visual widget that the user can set y-axis
    * max value with.
    * @return A panel containing a spinner that controls maxvalue value
   */
    public JPanel visualSetMaxValue() {
        if (maxvalueVisual == null) {
            createmaxvalueVisual();
        }
        JPanel returnthis = new JPanel();
        returnthis.setLayout(new BorderLayout());
        JLabel maxvalueLabel = new JLabel("Y-Axis max value");
        returnthis.add(maxvalueLabel, BorderLayout.WEST);
        JSpinner spinit = new JSpinner(maxvalueVisual);
        spinit.setPreferredSize(new Dimension(80, 20));
        returnthis.add(spinit, BorderLayout.EAST);
        returnthis.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return returnthis;
    }

    private void createmaxvalueVisual() {
        maxvalueVisual = new SpinnerNumberModel();
        maxvalueVisual.setValue(new Integer(maxvalue));
        stepsizeVisual.setMinimum(new Integer(1));
        maxvalueVisual.addChangeListener(this);
    }

    /**
     * This function returns a panel that has all
     * the visual options aligned in a column
     * @return a panel that can be directly displayed to the screen
     */
    public JPanel getOptionsPanel() {
        JPanel allOptions = new JPanel();
        allOptions.setLayout(new GridLayout(10, 1));
        //allOptions.setLayout(new BorderLayout());
        allOptions.add(getZoomLevelOption());
        allOptions.add(visualSetMaxValue());
        allOptions.add(new JPanel()); //filler so there is blank space
        return allOptions;
    }

    /**
     * This function is called by fire methods inside a monitor.  It
     * physically adds data values that will be displayed upon
     * next update/repaint
     * @param number the value for the time series data in question
     */
    public void recordNumber(int number) {
        synchronized (this) {
            publicNumbers.add(number);
        }
    }

    /**
     * This function is called by paint and it does what is necessary
     * to update the privateNumbers vector
     * returns true if it actually got some numbers, otherwise returns false
     * It might also be called by paint thread
     */
    public boolean internalUpdate() {
        Sequence newNumbers = publicNumbers;
        synchronized ( this ) {
            if ( newNumbers.size() == 0 ) {
                return false;
            }
            publicNumbers = new Sequence();
        }

        // add all the new numbers
        privateNumbers.addAll(newNumbers);
        int max = privateNumbers.max();
        if ( max > maxvalue ) maxvalue = max;

        //and update the horz scroll bar to reflect the new values
        updateHorzBar();
        return true;
    }

    /**
     * This actually paints the graph...note that it repaints the whole graph
     * everytime its called (to improve performance, we could make use of an update function)
     * The code here is actually faily ugly
     * but eh..
     * @param g The graphic that represents the panel to be painted
     */
    public void paint(Graphics g) {

        Dimension panelDimen = this.getSize();

        //Set up background color, will erase all previous lines
        g.setColor(backColor);
        g.fillRect(0, 0, panelDimen.width, panelDimen.height);

        // draw the time scale (horizontal axis)
        timeScale.setPosition(horzBar.getValue());
        timeScale.drawScale(panelDimen, g);
        long startTime = timeScale.getStartTime();

        // draw the value scale (vertical axis)
        drawValueScale(panelDimen, g);

        int height = panelDimen.height - timeScale.height;
        int maxY = height - valueMargin;
        double scalingfactor = (maxY - valueMargin) / (double)(maxvalue - minvalue);
        int eofpx = 0; //holds coorinates of last line we drew
        int eofpy = 0;
        boolean firstone = true;  //the first line is a special case

        //we have to look up the starting value for the x-axis
        Sequence.Iterator mi = privateNumbers.iterator((int)startTime);
        g.setColor(lineColor);
        for (long i = startTime; mi.hasNext() ; i++) {
            double currentValue = mi.next();
            if (firstone) {
                //don't draw the vertical line, just the horzintal
                eofpy = maxY - (int)(currentValue * scalingfactor);
                eofpx = timeScale.getX(i);
                g.drawLine(0, eofpy, eofpx, eofpy);
                firstone = false;
            } else {
                //two lines (one horizontal and one vertical), using the previous point as a starting location
                int npy = maxY - (int)(currentValue * scalingfactor);
                //vertical
                int npx = timeScale.getX(i);
                g.drawLine(eofpx, eofpy, eofpx, npy);
                //horz
                g.drawLine(eofpx, npy, npx, npy);
                eofpy = npy;
                eofpx = npx;
            }

            // are there any measurements left?
            if (!mi.hasNext() ) {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(eofpx, 0, panelDimen.width, height);
                g.setColor(cursorColor);
                g.drawLine(eofpx, 0, eofpx, height);
            }

            // did we go off the end to the right?
            if ( eofpx > panelDimen.width ) break;
        }
    }

    private void drawValueScale(Dimension dim, Graphics g) {
        int height = dim.height - timeScale.height;
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, VALUE_SCALE_WIDTH, height);
        int startY = height - valueMargin;
        double scale = (startY - valueMargin) / (double)(maxvalue - minvalue);
        int vtick = 1;
        for ( ; vtick < 1000000000; vtick *= 10 ) {
            if ( scale * vtick > MIN__VALUE__TICK ) break;
        }
        int majorTick = vtick * 10;
        int value = (minvalue / majorTick) * majorTick;
        if ( minvalue < 0 ) value -= majorTick;

        g.setColor(Color.DARK_GRAY);
        g.setFont(g.getFont().deriveFont(9.0f));

        for ( ; ; value += vtick) {
            int y = (int)(startY - (value - minvalue) * scale);
            if ( y <= 0 ) break;
            int modulus = value % majorTick;
            if ( modulus == 0 ) {
                // draw line across whole screen
                g.drawLine(0, y, dim.width, y);
                g.setColor(Color.RED);
                g.drawString(Integer.toString(value), VALUE_SCALE_WIDTH+3, y+3);
                g.setColor(Color.DARK_GRAY);
            } else {
                if ( modulus == 5 )
                    g.drawLine(5, y, VALUE_SCALE_WIDTH-1, y);
                else
                    g.drawLine(8, y, VALUE_SCALE_WIDTH-1, y);
            }
        }
        // draw the border line
        g.setColor(Color.WHITE);
        g.drawLine(VALUE_SCALE_WIDTH, 0, VALUE_SCALE_WIDTH, height);
    }

    /**
     * this function processes the monitor options and resets the internal variables appropriately
     ** @param e Info about the event that happened
     */
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == stepsizeVisual) {
            adjustZoom();
            repaint();
        } else if (e.getSource() == maxvalueVisual) {
            maxvalue = ((Integer) maxvalueVisual.getValue()).intValue();
            repaint();
        }
    }

    private void adjustZoom() {
        int zoomlevel = ((Integer) stepsizeVisual.getValue()).intValue() - 1;
        timeScale.setZoom(zoomlevel);
        this.horzBar.setValue(timeScale.getPosition());
        this.updateHorzBar();
    }

    /**
     * This function handles a user change to the scroll bar
     * @param e Info about the event that happened
     */
    public void adjustmentValueChanged(AdjustmentEvent e) {
        repaint();
    }

}
