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

import cck.util.Util;
import javax.swing.*;

/**
 * The <code> MonitorPanel </code> represents a pair of panels for a monitor, where one panel
 * is the display panel (selectable through the main tab) and one is the options
 * panel which is displayed when the user accesses the options for this monitor.
 *
 * @author Ben L. Titzer
 */
public class MonitorPanel {

    final String name;

    final JPanel displayPanel;
    final JPanel optionsPanel;

    Updater updater;

    interface Updater {
        public void update();
    }


    MonitorPanel(String n, JPanel dp, JPanel op) {
        name = n;
        displayPanel = dp;
        optionsPanel = op;
    }

    /**
     * This function should be called between different simulations in order
     * to clear all the old data 
     */
    public void clear() {
        throw Util.unimplemented();
    }

    /**
     * This function will be called by PaintThread if it detects that this
     * monitor is the currently displayed monitor
     */
    public void paint() {
        if ( updater != null ) updater.update();
    }

    public void setUpdater(Updater u) {
        this.updater = u;
    }
}
