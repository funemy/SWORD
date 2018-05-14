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

package avrora.sim.clock;

import cck.text.StringUtil;
import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * The <code>ClockDomain</code> class represents a collection of clocks for a device or platform,
 * including the main clock used for the microcontroller.
 *
 * @author Ben L. Titzer
 */
public class ClockDomain {

    protected final HashMap clockMap;
    protected final MainClock mainClock;

    /**
     * The constructor for the <code>ClockDomain</code> class constructs the main clock (from which
     * all other clocks are derived). It accepts as a parameter the speed of the main clock.
     * @param mainHz the speed of the main clock in cycles per second
     */
    public ClockDomain(long mainHz) {
        clockMap = new HashMap();
        mainClock = new MainClock("main", mainHz);
        clockMap.put("main", mainClock);
    }

    /**
     * The <code>getMainClock()</code> method returns the main clock for this clock domain.
     * @return an instance of the <code>MainClock</code> class that contains the main clock for
     * this clock domain.
     */
    public MainClock getMainClock() {
        return mainClock;
    }

    /**
     * The <code>getClock()</code> method looks for a clock with the specified name in this clock
     * domain.
     * @param name the name of the clock as a string
     * @return an instance of the <code>Clock</code> interface for the specified clock name
     * @throws NoSuchElementException if no clock with the specified name exists in this domain
     */
    public Clock getClock(String name) {
        Clock clock = (Clock)clockMap.get(name);
        if ( clock == null )
            throw new NoSuchElementException(StringUtil.quote(name)+" clock not found");
        return clock;
    }

    /**
     * The <code>addClock()</code> method adds a clock to this clock domain. It will be indexed
     * by the name returned by the clock's <code>getName()</code> method. If there is a clock
     * already in this domain with the name specified, the new clock will be returned for subsequent
     * calls to <code>getClock()</code>, and the old clock will no longer be accessible.
     * @param c the clock to add to this domain
     */
    public void addClock(Clock c) {
        clockMap.put(c.getName(), c);
    }

    /**
     * The <code>newClock()</code> method creates a new clock derived from the main clock of
     * this clock domain with the given name and clockspeed. The clock will automatically be
     * added to this clock domain with the specified name.
     * @param name the name of the new clock
     * @param hz the clockspeed of the new clock in cycles per second
     * @return a new <code>Clock</code> instance with the specified properties
     */
    public Clock newClock(String name, long hz) {
        if ( hz == mainClock.getHZ() ) {
            clockMap.put(name, mainClock);
            return mainClock;
        }
        DerivedClock c = new DerivedClock(name, mainClock, hz);
        addClock(c);
        return c;
    }

    /**
     * The <code>hasClock()</code> method queries the clock domain whether it contains a particular
     * named clock.
     * @param name the name of the clock to check for
     * @return true if a clock in this domain exists with the specified name; false otherwise
     */
    public boolean hasClock(String name) {
        return clockMap.get(name) != null;
    }
}
