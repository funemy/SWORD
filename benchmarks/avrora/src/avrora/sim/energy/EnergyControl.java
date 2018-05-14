/**
 * Created on 19. September 2004, 00:19
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

import java.util.Iterator;
import java.util.LinkedList;

/**
 * implementation of energy control handles subscription of monitors and consumers
 *
 * @author Olaf Landsiedel
 */
public class EnergyControl {

    //consumer list
    // e.g. list of devices which consume energy
    public final LinkedList consumer;

    //list of monitors which want to be informed about
    //energy consumption
    private final LinkedList subscriber;
    private boolean active;

    public EnergyControl() {
        consumer = new LinkedList();
        subscriber = new LinkedList();
    }

    /**
     * add energy monitor
     *
     * @param energyMonitor monitor
     */
    public void subscribe(EnergyObserver energyMonitor) {
        subscriber.add(energyMonitor);
    }

    /**
     * get list of consumers
     *
     * @return consumer list
     */
    public LinkedList getConsumers() {
        return consumer;
    }

    /**
     * update the state of a device
     *
     * @param energy the energy model of the device
     */
    public void stateChange(Energy energy) {
        Iterator it = subscriber.iterator();
        while (it.hasNext()) {
            ((EnergyObserver) it.next()).stateChange(energy);
        }
    }

    public void activate() {
        if (!active) {
            active = true;
            Iterator it = consumer.iterator();
            while (it.hasNext()) {
                ((Energy) it.next()).activate();
            }
        }
    }

    public void addConsumer(Energy energy) {
        consumer.add(energy);
        if (active) energy.activate();
    }
}
