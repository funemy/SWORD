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

import cck.text.StringUtil;
import cck.util.Util;
import java.awt.*;
import java.util.LinkedList;

/**
 * The <code>TimeScale</code> class handles the conversion of time scales in displaying timing windows
 * within the GUI simulation. It has an internal notion of the scale and the start time. It has methods
 * to render a scale bar and to convert a time scale value (in cycles) to an X coordinate in the drawing
 * rectangle.
 *
 * @author Ben L. Titzer
 */
public class TimeScale {
    final int height;
    long startTime;
    final Color backgroundColor;
    final Color borderColor;
    final Color tickColor;
    final Color fontColor;

    static final long hz;
    static final double nsPerCycle;
    static final int SCROLL_SIZE = 35;
    static final int MIN_TICK_WIDTH = 40;
    static ZoomLevel[] zooms;
    static int startZoom;
    static final double ONE_BILLION = 1000000000;
    static final String[] units = { "ns", "us", "ms", "s" };

    int zoom;

    TimeScale() {
        height = 20;
        backgroundColor = Color.GRAY;
        borderColor = Color.WHITE;
        tickColor = Color.DARK_GRAY;
        fontColor = Color.RED;
        zoom = startZoom;
    }

    static {
        hz = 7372800;
        nsPerCycle = ONE_BILLION / hz;

        double scaleup = 1.25895;
        double s100 = (double)hz / 100;
        zooms = new ZoomLevel[0];

        LinkedList lout = new LinkedList();
        for ( double scale = s100; scale > 1; scale /= scaleup ) {
            lout.add(newZoomLevel(scale));
        }
        ZoomLevel[] zout = (ZoomLevel[])lout.toArray(zooms);

        LinkedList lin = new LinkedList();
        for ( double scale = 1; scale > 0.02; scale /= scaleup ) {
            lin.add(newZoomLevel(scale));
        }
        ZoomLevel[] zin = (ZoomLevel[])lin.toArray(zooms);

        zooms = new ZoomLevel[lout.size()+lin.size()];
        System.arraycopy(zout, 0, zooms, 0, zout.length);
        System.arraycopy(zin, 0, zooms, zout.length, zin.length);

        startZoom = zout.length;
    }

    static ZoomLevel newZoomLevel(double scale) {
        long nsecs = 1;
        double cycles = getCycles(1);
        double max = 2*hz;
        for ( int cntr = 0; cycles < max; cntr++ ) {
            double tickWidth = cycles / scale;
            if ( tickWidth > MIN_TICK_WIDTH ) {
                int dec = (300 - cntr) % 3;
                return new ZoomLevel(scale, dec, nsecs, units[(cntr+2)/3]);
            }
            cycles *= 10;
            nsecs *= 10;
        }
        throw Util.failure("Zoom level not supported: "+scale);
    }

    public int getMaxZoom() {
        return zooms.length - 1;
    }

    static class ZoomLevel {
        final double scale; // scales in cycles per pixel
        final int dec;      // decimal positions within the unit
        final long nsecs;    // 10^pos
        final String units; // string unit name (e.g. "ms")

        final double majorTickWidth;
        final double minorTickWidth;

        ZoomLevel(double s, int d, long ns, String un) {
            scale = s;
            dec = d;
            nsecs = ns;
            units = un;

            majorTickWidth = getCycles(nsecs) / scale;
            minorTickWidth = majorTickWidth / 10;
        }
    }

    public void drawScale(Dimension dim, Graphics g) {
        g.setColor(backgroundColor);
        int y = dim.height - height;
        int my = (int)(y + height * 0.6);
        int medy = (int)(y + height * 0.4);
        g.fillRect(0, y, dim.width, dim.height);

        g.setFont(g.getFont().deriveFont(9.0f));

        ZoomLevel zl = getZoomLevel();
        long startNsecs = (long)getNS(startTime);
        long ns = startNsecs - startNsecs % zl.nsecs;
        double startPos = (getCycles(ns) - startTime) / zl.scale;
        int count = (int)((ns / zl.nsecs) % 1000);

        // show the actual clock ticks
        double cycWidth = 1 / zl.scale;
        if ( cycWidth >= 2 ) {
            g.setColor(this.borderColor);
            for ( double pos = 0; pos < dim.width; pos += cycWidth ) {
                int xpos = (int)pos;
                g.drawLine(xpos, y, xpos, y+4);
            }
        }

        // show the timing scale
        g.setColor(tickColor);
        double max = dim.width + zl.majorTickWidth;
        for ( double pos = startPos; pos < max; pos += zl.majorTickWidth ) {
            // draw the sub-ticks for this label
            for ( int mt = 1; mt < 10; mt++ ) {
                int mx = (int)(pos + zl.minorTickWidth*mt);
                if ( mt == 5 )
                    g.drawLine(mx, medy, mx, dim.height);
                else
                    g.drawLine(mx, my, mx, dim.height);
            }
            // draw the string label for this tick
            int xpos = (int)pos;
            // draw the line from top to bottom
            g.drawLine(xpos, 0, xpos, dim.height);
            // draw the font label
            drawTickLabel(zl, count, g, xpos, y);
            count++;
            if ( count == 1000 ) count = 0;
        }

        // draw the border
        g.setColor(borderColor);
        g.drawLine(0, y, dim.width, y);

    }

    private double getNS(long cycles) {
        return cycles * nsPerCycle;
    }

    private static double getCycles(long nsecs) {
        return nsecs / nsPerCycle;
    }

    private void drawTickLabel(ZoomLevel zl, int tick, Graphics g, int cntr, int y) {
        String str = StringUtil.toDecimal(tick, zl.dec);
        FontMetrics m = g.getFontMetrics();
        String stru = str+" "+getZoomLevel().units;
        int width = m.stringWidth(stru);
        g.setColor(fontColor);
        g.drawString(stru, cntr - width + 5, y - 2);
        g.setColor(tickColor);
    }

    public int getZoom() {
        return zoom;
    }

    private ZoomLevel getZoomLevel() {
        return zooms[zoom];
    }

    public int getX(long time) {
        if ( time < startTime ) return -1;
        return (int)((time - startTime) / getZoomLevel().scale);
    }

    public void setZoom(int nzoom) {
        if ( nzoom >= zooms.length ) nzoom = zooms.length-1;
        if ( nzoom < 0 ) nzoom = 0;
        zoom = nzoom;
    }

    public void zoomin() {
        if ( zoom < zooms.length-1 ) zoom++;
    }

    public void zoomout() {
        if ( zoom > 0 ) zoom--;
    }

    public int getExtent(int width, long maxtime) {
        return width / SCROLL_SIZE;
    }

    public int getScrollBarSize(long maxtime) {
        return size(maxtime);
    }

    public void setPosition(int np) {
        startTime = (long)(np * SCROLL_SIZE * getZoomLevel().scale);
    }

    public int getPosition() {
        return size(startTime);
    }

    private int size(long cycles) {
        return (int)(cycles / getZoomLevel().scale / SCROLL_SIZE);
    }

    public long getStartTime() {
        return startTime;
    }

    public double getScale() {
        return getZoomLevel().scale;
    }
}
