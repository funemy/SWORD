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

package avrora.sim.platform.sensors;

import avrora.Main;
import avrora.sim.Simulator;
import avrora.sim.clock.Clock;
import avrora.sim.mcu.Microcontroller;
import cck.util.Util;
import java.io.*;

/**
 * The <code>ReplaySensorData</code> class implements a sensor data source that replays
 * the sensor data from a file.
 *
 * @author Ben L. Titzer
 */
public class ReplaySensorData implements SensorData {

    final Clock clock;
    final File file;
    final FileReader fr;
    final StreamTokenizer st;
    final ChangeReading change;
    int currentReading;

    public ReplaySensorData(Microcontroller m, String fn) throws IOException {
        clock = m.getClockDomain().getMainClock();
        Main.checkFileExists(fn);
        file = new File (fn);
        fr = new FileReader(file);
        st = new StreamTokenizer(fr);
        change = new ChangeReading();
        getNextReading();
        scheduleNextChange();
    }

    private void getNextReading() throws IOException {
        int tt = st.nextToken();
        if ( tt == StreamTokenizer.TT_EOF ) return;
        if ( tt != StreamTokenizer.TT_NUMBER )
            throw Util.failure("sensor data format error: expected number as sensor reading");
        currentReading = (int)st.nval & 0x3ff;
    }

    class ChangeReading implements Simulator.Event {
        public void fire() {
            try {
                getNextReading();
                scheduleNextChange();
            } catch (IOException e) {
                throw Util.unexpected(e);
            }
        }
    }

    private void scheduleNextChange() throws IOException {
        int tt = st.nextToken();
        if ( tt == StreamTokenizer.TT_EOF ) return;
        if ( tt != StreamTokenizer.TT_NUMBER )
            throw Util.failure("sensor data format error: expected number as time value");
        clock.insertEvent(change, (long)(st.nval * clock.getHZ()));
    }

    public int reading() {
        return currentReading;
    }
}
