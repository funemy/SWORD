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

package avrora.monitors;

import avrora.sim.Simulator;
import avrora.sim.clock.MainClock;
import cck.util.TimeUtil;

/**
 * The <code>RealTimeMonitor</code> class slows down the simulation to real-time. This is
 * useful for simulations that run much faster than real time and for simulations that may
 * be connected to external device inputs.
 *
 * @author Ben L. Titzer
 */
public class RealTimeMonitor extends MonitorFactory {

    private class ThrottleEvent implements Simulator.Event {
        boolean initialized;
        long beginMs;
        final long period;
        final MainClock clock;

        public ThrottleEvent(Simulator s) {
            clock = s.getClock();
            period = clock.getHZ() / 100;
        }

        public void fire() {
            if ( !initialized ) {
                initialized = true;
                beginMs = System.currentTimeMillis();
                clock.insertEvent(this, period);
                return;
            }

            long cycles = clock.getCount();
            long msGoal = (long)TimeUtil.cyclesToMillis(cycles, clock.getHZ());
            while ( (System.currentTimeMillis() - beginMs) < msGoal ) ;

            clock.insertEvent(this, period);
        }
    }

    public RealTimeMonitor() {
        super("The \"real-time\" monitor slows down the simulation so that it runs as close as possible " +
                "to real-time.");
    }

    public Monitor newMonitor(Simulator s) {
        s.insertEvent(new ThrottleEvent(s), 1);
        return null;
    }
}
