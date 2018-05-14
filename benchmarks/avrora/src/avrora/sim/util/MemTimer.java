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

package avrora.sim.util;

import avrora.sim.*;
import cck.text.Terminal;


/**
 * <code>MemTimer</code> lets simulated applications start and stop
 * timers
 *
 * @author John Regehr
 */
public class MemTimer extends Simulator.Watch.Empty {

    int base;
    long start_time = 0;
    int timer_state = 0;

    public MemTimer(int b) {
        base = b;
    }

    public void fireBeforeWrite(State state, int data_addr, byte value) {
        if (data_addr != base) {
            Terminal.printRed("Unexpected interception by printer!");
            System.exit(-1);
        }
        AtmelInterpreter a = (AtmelInterpreter) state.getSimulator().getInterpreter();
        switch (value) {
            case 100:
                if (timer_state != 0) {
                    Terminal.printRed("timer: multiple starts in a row??");
                    Terminal.nextln();
                } else {
                    start_time = state.getCycles();
                }
                timer_state = 1;
                break;
            case 101:
                if (timer_state != 1) {
                    Terminal.printRed("timer: multiple stops in a row??");
                    Terminal.nextln();
                } else {
                    long stop_time = state.getCycles();
                    long duration = stop_time - start_time;
                    Terminal.printRed("timer: " + String.valueOf(duration) + " cycles");
                    Terminal.nextln();
                }
                timer_state = 0;
                break;
            default:
                Terminal.printRed("Unexpected command to timer!");
                Terminal.nextln();
        }
    }
}
