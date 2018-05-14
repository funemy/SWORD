	/**
 * Created on 18. September 2004, 20:41
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
 * Neither the name of the Protocol Engineering and Distributed Systems
 * Group, the name of the University of Tuebingen nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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


package avrora.sim.energy;

import avrora.sim.FiniteStateMachine;
import avrora.sim.clock.Clock;

/**
 * Class for energy modeling. All consumers create an instance of this class and keep it updated with all
 * state changes concerning power consumption. This class keeps track of all these state changes and cycles
 * spend in each state. The state changes are propagated to monitors based on a subscription system. This
 * enables logging of power consumption.
 *
 * @author Olaf Landsiedel
 */
public class Energy implements FiniteStateMachine.Probe {

    //name of the device, which energy consumption is traced by
    //this class instance
    private String deviceName;
    //current draw for each state
    private double[] ampere;
    //cycles spent in each state
    private long[] cycles;
    // current state, e.g. mode
    private int currentMode;
    // the mode (e.g. state) the system was in before
    private int oldMode;
    // cycle the state was changed last
    private long lastChange;
    // voltage, needed for computation of energy consumption
    private static final double voltage = 3.0d;
    // time one mcu cycle takes
    private double cycleTime;
    //the state machine handles the sate of the device
    private FiniteStateMachine stateMachine;
    //the clock -> it knwos the time ;-)
    private Clock clock;

    private EnergyControl energyControl;

    /**
     * create new energy class, to enable energy modelling
     *
     * @param deviceName  name of the device to model
     * @param modeAmpere  array of current draw for each device state (in Ampere)
     * @param fsm         finite state machine of this device
     * @param energyControl
     */
    public Energy(String deviceName, double[] modeAmpere, FiniteStateMachine fsm, EnergyControl energyControl) {
        // remember all params
        this.deviceName = deviceName;
        this.clock = fsm.getClock();
        this.ampere = modeAmpere;
        this.stateMachine = fsm;
        this.currentMode = fsm.getStartState();
        int freq = (int)clock.getHZ();
        this.cycleTime = 1.0d / freq;
        this.energyControl = energyControl;
        energyControl.addConsumer(this);
    }

    /**
     * The <code>fireBeforeTransition()</code> method allows the probe to gain control
     * before the state machine transitions between two states. The before state and the
     * after state are passed as parameters.
     * @param beforeState the before state represented as an integer
     * @param afterState the after state represented as an integer
     */
    public void fireBeforeTransition(int beforeState, int afterState){
        //we use fireAfterTransition
    }

    /**
     * The <code>fireAfterTransition()</code> method allows the probe to gain control
     * after the state machine transitions between two states. The before state and the
     * after state are passed as parameters.
     * @param beforeState the before state represented as an integer
     * @param afterState the after state represented as an integer
     */
    public void fireAfterTransition(int beforeState, int afterState){
        if (afterState != currentMode) {
            cycles[currentMode] += clock.getCount() - lastChange;
            oldMode = currentMode;
            currentMode = afterState;
            lastChange = clock.getCount();
            //notify the energy control that I am now in a new state
            energyControl.stateChange(this);
        }
    }

    /**
     * get the power consumption of this device
     *
     * @return power consumption in Joule
     */
    public double getTotalConsumedEnergy() {
        double total = 0.0d;
        for (int i = 0; i < ampere.length; i++)
            total += getConsumedEnergy(i);
        return total;
    }

    /**
     * get the power consumption of a state
     *
     * @param mode the mode or state
     * @return power consumption in Joule
     */
    public double getConsumedEnergy(int mode) {
        return voltage * getCycles(mode) * ampere[mode] * cycleTime;
    }

    /**
     * get the number of modes of this device
     *
     * @return mode number
     */
    public int getModeNumber() {
        return ampere.length;
    }

    /**
     * get the current state or mode of the device
     *
     * @return current mode
     */
    public int getCurrentMode() {
        return currentMode;
    }

    /**
     * get the name of a mode
     *
     * @param mode mode number
     * @return mode name
     */
    public String getModeName(int mode) {
        return stateMachine.getStateName(mode);
    }

    /**
     * get the current draw of a mode
     *
     * @param mode mode number
     * @return current draw in Ampere
     */
    public double getModeAmpere(int mode) {
        return ampere[mode];
    }

    /**
     * get the cycles spend in a device state
     *
     * @param mode mode number
     * @return cycles
     */
    public long getCycles(int mode) {
        if ( cycles == null ) return 0;
        long ret = cycles[mode];
        if (mode == currentMode)
            ret += clock.getCount() - lastChange;
        return ret;
    }

    /**
     * get the device name
     *
     * @return device name
     */
    public String getName() {
        return deviceName;
    }

    /**
     * get old mode
     *
     * @return old mode
     */
    public int getOldMode() {
        return oldMode;
    }

    /**
     * get the current draw
     *
     * @return current draw in Ampere
     */
    public double getCurrentAmpere() {
        return ampere[currentMode];
    }

    /**
     * get the current draw of the old mode
     *
     * @return current draw
     */
    public double getOldAmpere() {
        return ampere[oldMode];
    }

    /**
     * active energy modeling, e.g. insert the probe into to
     * component state machine
     */
    public void activate() {
        //insert probe into finite state machine
        stateMachine.insertProbe(this);
        // setup cycle array to store the cycles of each state
        cycles = new long[ampere.length];
        for (int i = 0; i < cycles.length; i++)
            cycles[i] = 0;
    }

}
