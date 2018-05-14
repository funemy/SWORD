/**
 * Created on 15.11.2004
 *
 * Copyright (c) 2004-2005, Olaf Landsiedel, Protocol Engineering and
 * Distributed Systems, University of Tuebingen
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

import avrora.sim.FiniteStateMachine;
import avrora.sim.Simulator;
import avrora.sim.energy.Energy;

/**
 * This class is a placeholder for tracking the energ consumption of the sensor board. Currently
 * there are no sensors implemented for simulation.
 *
 * @author Olaf Landsiedel
 */
public class SensorBoard {

    protected Simulator sim;
    // names of the states of this device
    private final String[] modeName = {"on:  "};
    // power consumption of the device states
    private final double[] modeAmpere = {0.0007};
    // default mode of the device is on
    private static final int startMode = 0;

    public SensorBoard(Simulator s) {
        sim = s;
        //setup energy recording
        //note: the name sensorBoard was choosen on purpose as it is used in the log files
        //if you use sensor board, you may have trouble with importing the data as it is separated by white spaces
        FiniteStateMachine fsm = new FiniteStateMachine(s.getClock(), startMode, modeName, 0);
        new Energy("SensorBoard", modeAmpere, fsm, sim.getSimulation().getEnergyControl());
    }
}
