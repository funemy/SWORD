/**
 * Copyright (c) 2007, Regents of the University of California
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
 *
 * Created Oct 11, 2007
 */
package avrora.sim.radio;

import avrora.sim.clock.Clock;
import avrora.sim.clock.Synchronizer;
import avrora.sim.Simulator;
import avrora.sim.util.TransactionalList;

import java.util.*;

import cck.util.Arithmetic;

/**
 * The <code>Medium</code> definition drives the timming in the transmission and
 * reception of packets
 *
 * @author Ben L. Titzer
 * @author Rodolfo de Paz
 */
public class Medium {

    private static final int BYTE_SIZE = 8;

    private static int Pn = -95;//Noise Power in dBm
    private static double Pr = (double) Pn;//Received Power in dBm

    public interface Arbitrator {
        public boolean lockTransmission(Receiver receiver, Transmission tran, int Milliseconds);
        public char mergeTransmissions(Receiver receiver, List trans, long bit, int Milliseconds);
        public double computeReceivedPower(Medium.Transmission t, Medium.Receiver receiver, int Milliseconds);
        public int getNoise(int index);
    }

    /**
     * The <code>Probe</code> interface defined method to insert and removes
     * probes before and after transmit and receive.
     */
    public interface Probe {
        public void fireBeforeTransmit(Transmitter t, byte val);
        public void fireBeforeTransmitEnd(Transmitter t);
        public void fireAfterReceive(Receiver r, char val);
        public void fireAfterReceiveEnd(Receiver r);

        public class Empty implements Probe {
            public void fireBeforeTransmit(Transmitter t, byte val) { }
            public void fireBeforeTransmitEnd(Transmitter t) { }
            public void fireAfterReceive(Receiver r, char val) { }
            public void fireAfterReceiveEnd(Receiver r) { }
        }

        /**
         * The <code>List</code> class inherits from TransactionalList several
         * methods to implement all methods of the interface Probe
         */
        public class List extends TransactionalList implements Probe {
            public void fireBeforeTransmit(Transmitter t, byte val) {
                beginTransaction();
                for (Link pos = head; pos != null; pos = pos.next)
                    ((Probe) pos.object).fireBeforeTransmit(t, val);
                endTransaction();
            }

            public void fireBeforeTransmitEnd(Transmitter t) {
                beginTransaction();
                for (Link pos = head; pos != null; pos = pos.next)
                    ((Probe) pos.object).fireBeforeTransmitEnd(t);
                endTransaction();
            }

            public void fireAfterReceive(Receiver r, char val) {
                beginTransaction();
                for (Link pos = head; pos != null; pos = pos.next)
                    ((Probe) pos.object).fireAfterReceive(r, val);
                endTransaction();
            }

            public void fireAfterReceiveEnd(Receiver r) {
                beginTransaction();
                for (Link pos = head; pos != null; pos = pos.next)
                    ((Probe) pos.object).fireAfterReceiveEnd(r);
                endTransaction();
            }
        }
    }

    /**
     * The <code>Medium.TXRX</code> static class represents a Medium where
     * transmitter and receiver exchange bytes
     */
    protected static class TXRX {
        public final Medium medium;
        public final Clock clock;
        public final long cyclesPerByte;
        public final long leadCycles;
        public final long cyclesPerBit;
        protected Probe.List probeList;

        public boolean activated;

        /**
         * The <code>TXRX</code> constructor method
         *
         * @param m Medium
         * @param c Clock
         */
        protected TXRX(Medium m, Clock c) {
            medium = m;
            clock = c;
            long hz = c.getHZ();
            int bps = medium.bitsPerSecond;
            assert hz > bps;
            cyclesPerBit = (hz / bps);
            cyclesPerByte = BYTE_SIZE * cyclesPerBit;
            leadCycles = (medium.leadBits * hz / bps);
        }

        protected long getBitNum(long time) {
            return time / cyclesPerBit;
        }

        protected long getCycleTime(long bit) {
            return bit * cyclesPerBit;
        }

        public void insertProbe(Medium.Probe probe) {
            if (this.probeList == null) this.probeList = new Probe.List();
            this.probeList.add(probe);
        }

        public void removeProbe(Medium.Probe probe) {
            if (this.probeList != null) this.probeList.remove(probe);
        }
    }

    /**
     * The <code>Medium.Transmitter</code> class represents an object that is capable of
     * making transmissions into the medium. When activated, it begins transmitting bytes
     * into the medium after the lead time. Internally, this class implements its own
     * clock-level synchronization so that clients only have to implement the
     * <code>nextByte()</code> routine.
     */
    public static abstract class Transmitter extends TXRX {

        protected Transmission transmission;
        protected final Transmitter.Ticker ticker;
        protected boolean shutdown;

        /**
         * The constructor <code>Transmitter</code> creates an extension of TXRX
         * constructor adding an instance of <code>Ticker</code>
         */
        protected Transmitter(Medium m, Clock c) {
            super(m, c);
            ticker = new Ticker();
        }

        /**
         * The <code>beginTransmit</code> method creates a new transmission instatiating
         * <code>medium.newtransmission</code> and inserts a new ticker Event in
         * the simulator in a leadCycles time
         *
         * @param pow  power for the new transmission (dBm)
         * @param freq frequency for the new transmission (Mhz)
         */
        public final void beginTransmit(double pow, double freq) {
            if (!activated) {
                transmission = medium.newTransmission(this, pow, freq);
                activated = true;
                clock.insertEvent(ticker, leadCycles);
            }
        }

        /**
         * The <code>endTransmit</code> method shutdowns the transmitter and
         * ends the transmission calling the <code>transmission.end</code> method
         */
        public final void endTransmit() {
            if (activated) {
                shutdown = true;
                transmission.end();
            }
        }

        /**
         * The <code>nextByte</code> abstract method which has to be implemented
         * by the Radio implementation
         */
        public abstract byte nextByte();

        /**
         * The <code>Ticker</code> class implements a Simulator Event call Ticker
         * that is fired  when a timed event occurs within the simulator in order
         * to model a mote transmission
         */

        protected class Ticker implements Simulator.Event {
            public void fire() {
                if (shutdown) {
                    // shut down the transmitter
                    if (probeList != null) probeList.fireBeforeTransmitEnd(Transmitter.this);
                    transmission = null;
                    shutdown = false;
                    activated = false;
                } else if (activated) {
                    // otherwise, transmit a single byte and add it to the buffer
                    int indx = transmission.counter++;
                    byte val = nextByte();
                    if (indx >= transmission.data.length) {
                        // grow the transmission length when necessary
                        byte[] ndata = new byte[transmission.data.length + 16];
                        System.arraycopy(transmission.data, 0, ndata, 0, transmission.data.length);
                        transmission.data = ndata;
                    }
                    transmission.data[indx] = val;
                    if (probeList != null) probeList.fireBeforeTransmit(Transmitter.this, val);
                    clock.insertEvent(this, cyclesPerByte);
                }
            }
        }
    }

    /**
     * The <code>Medium.Receiver</code> class represents an object that can receive transmissions
     * from the medium. When activated, it listens for transmissions synchronously using
     * its own clock-level synchronization. It receives transmissions that may be the
     * result of multiple interfering transmissions.
     */
    public static abstract class Receiver extends TXRX {
        private static final int BIT_DELAY = 1;
        protected boolean locked;
        protected double frequency;
        public Receiver.Ticker ticker;

        //Receiver class constructor
        protected Receiver(Medium m, Clock c) {
            super(m, c);
            ticker = new Ticker();
        }

        //Begin receiving. Insert a event.
        public final void beginReceive(double freq) {
            frequency = freq;
            if (!activated) {
                activated = true;
                clock.insertEvent(ticker, leadCycles + cyclesPerByte);
            }
        }

        //Ending reception. Remove event.
        public final void endReceive() {
            // Reception has been terminated, but check if receiver was still locked onto some transmission
            if (locked) {
                nextByte(false, (byte) 0);
                if (probeList != null) probeList.fireAfterReceiveEnd(Receiver.this);
            }
            activated = false;
            locked = false;
            clock.removeEvent(ticker);
        }

        public abstract byte nextByte(boolean lock, byte b);

        public abstract void setRssiValid(boolean v);

        public abstract boolean getRssiValid();

        public abstract void setRSSI(double rssi);

        public abstract void setBER(double BER);

        /**
         * The <code>Ticker</code> class implements a Simulator Event call Ticker
         * that is fired  when a timed event occurs within the simulator in order
         * to model a mote reception
         */
        protected class Ticker implements Simulator.Event {

            public void fire() {
                if (activated) {
                    if (locked) {
                        // if receiver is locked onto some transmission, wait for neighbors' byte(s)
                        fireLocked(clock.getCount());
                    } else {
                        // if receiver is not locked, determine whether a lock will occur this interval
                        fireUnlocked(clock.getCount());
                    }
                }
            }

            /**
             * The <code>fireUnlocked</code> method is done when the receiver
             * is not locked onto some transmission.
             *
             * @param time
             */
            private void fireUnlocked(long time) {
                long oneBitBeforeNow = getBitNum(time) - BIT_DELAY;
                //wait until all neighbors are in time before a possible tx to this thread
                waitForNeighbors(time - cyclesPerByte);
                //find the earliest new transmission and store it in tx
                Transmission tx = earliestNewTransmission(oneBitBeforeNow - BYTE_SIZE);
                if (tx != null) {
                    // there is a new transmission; calculate delivery of first byte.
                    long dcycle = getCycleTime(tx.firstBit + BYTE_SIZE + BIT_DELAY);
                    long delta = dcycle - time;
                    //assert dcycle >= time;
                    if (delta <= 0) {
                        // lock on and deliver the first byte right now.
                        locked = true;
                        deliverByte(oneBitBeforeNow);
                        return;
                    } else if (delta < leadCycles) {
                        // lock on and insert event at delivery time of first bit.
                        locked = true;
                        clock.insertEvent(this, delta);
                        return;
                    } else if (delta < leadCycles + cyclesPerByte) {
                        // don't lock on yet, but wait for delivery time
                        clock.insertEvent(this, delta);
                        return;
                    }
                }
                // there is no transmission. Remain unlocked.
                clock.insertEvent(this, leadCycles);

            }

            /**
             * The <code>fireLocked</code> method is done when the receiver
             * is locked onto some transmission
             *
             * @param time
             */
            private void fireLocked(long time) {
                long oneBitBeforeNow = getBitNum(time) - BIT_DELAY; // there is a one bit delay
                waitForNeighbors(time - cyclesPerByte);
                deliverByte(oneBitBeforeNow);
            }

            /**
             * The <code>deliverByte</code> method delivers bytes to receiver
             *
             * @param oneBitBeforeNow
             */
            private void deliverByte(long oneBitBeforeNow) {
                List it = getIntersection(oneBitBeforeNow - BYTE_SIZE);
                if (it != null) {//there is a transmission
                    boolean one = false;
                    double rssi = 0.0;
                    double SNR = 0;
                    double BER = 0;
                    assert it.size() > 0;
                    Iterator i = it.iterator();
                    while (i.hasNext()) {
                        Transmission t = (Transmission) i.next();
                        if (one) {//more than one transmission
                            double I = medium.arbitrator.computeReceivedPower(t, Receiver.this, (int) clock.cyclesToMillis(clock.getCount()));
                            //add interference to received power in linear scale
                            rssi = 10 * Math.log10(Math.pow(10, rssi / 10) + Math.pow(10, I / 10));
                            SNR = SNR - I;
                        } else {//only one transmission - no interference -
                            one = true;
                            Pr = medium.arbitrator.computeReceivedPower(t, Receiver.this, (int) clock.cyclesToMillis(clock.getCount()));
                            Pn = medium.arbitrator.getNoise((int) clock.cyclesToMillis(clock.getCount()));
                            rssi = Pr;
                            SNR = Pr - Pn;
                        }
                        double snr = Math.pow(10D, (SNR / 10D));
                        //ebno = snr / spectral efficiency = snr / log(1 + snr)
                        double ebno = snr / Math.log(1 + snr);
                        //BER vs Ebno in AWGN channel
                        double x = Math.sqrt(2 * ebno);
                        double x2 = Math.pow(x, 2);
                        BER = Math.exp(-x2 / 2) / (1.64D * x + Math.sqrt(0.76D * (x2) + 4D));
                        setBER(BER);
                        setRSSI(rssi);
                    }
                    // merge transmissions into a single byte and send it to receiver
                    // we return val in order to get rssi and corr value
                    char val = medium.arbitrator.mergeTransmissions(Receiver.this, it, oneBitBeforeNow - BYTE_SIZE, (int) clock.cyclesToMillis(clock.getCount()));
                    //store high byte for corrupted bytes
                    int hval = (int) (val >>> 8);
                    int value = (int) (0xff & nextByte(true, (byte) val));
                    value |= (hval) & (value << 8);
                    value |= hval;
                    val = (char) value;
                    if (probeList != null) probeList.fireAfterReceive(Receiver.this, val);
                    clock.insertEvent(this, cyclesPerByte);

                } else {//no transmissions intersect
                    // all transmissions are over.
                    locked = false;
                    nextByte(false, (byte) 0);
                    if (probeList != null) probeList.fireAfterReceiveEnd(Receiver.this);
                    clock.insertEvent(this, leadCycles + cyclesPerByte);
                }
            }

        }

        /**
         * The <code>isChannelClear</code> method determines wether the channel is clear
         * or not
         *
         * @return true if channel is clear and false otherwise
         */
        public final boolean isChannelClear(int RSSI_reg, int MDMCTRL0_reg) {
            //There are 3 modes (ED, 802.15.4 compliant detection, both)
            int cca_mode = (MDMCTRL0_reg & 0x00c0) >>> 6;
            if (!activated) {//not receiving, CCA true depending on cca_mode
                long time = clock.getCount();
                long bit = getBitNum(time) - BIT_DELAY; // there is a one bit delay
                waitForNeighbors(time - cyclesPerByte);
                List it = getIntersection(bit - BYTE_SIZE);
                if (it != null) {//if there is a transmission compute rssi
                    boolean one = false;
                    double rssi = 0.0;
                    assert it.size() > 0;
                    Iterator i = it.iterator();
                    while (i.hasNext()) {
                        Transmission t = (Transmission) i.next();
                        if (one) {//more than one transmission
                            double I = medium.arbitrator.computeReceivedPower(t, Receiver.this, (int) clock.cyclesToMillis(clock.getCount()));
                            //add interference to received power in linear scale
                            rssi = 10 * Math.log10(Math.pow(10, rssi / 10) + Math.pow(10, I / 10));
                        } else {//only one transmission - no interference -
                            one = true;
                            Pr = medium.arbitrator.computeReceivedPower(t, Receiver.this, (int) clock.cyclesToMillis(clock.getCount()));
                            Pn = medium.arbitrator.getNoise((int) clock.cyclesToMillis(clock.getCount()));
                            rssi = Pr;
                        }
                    }
                    //cca modes 1 and 3 compare threshold with rssi to determine CCA
                    if (cca_mode == 1 | cca_mode == 3) {
                        int cca_hyst = (MDMCTRL0_reg & 0x0700) >>> 8;
                        int cca_thr = ((RSSI_reg & 0xff00) >>> 8) - 256;
                        int rssi_val = (int) rssi + 45;
                        return rssi_val < cca_thr - cca_hyst;
                    }//other modes true if no transmissions
                    else return it != null;
                } else return it != null;//no transmissions CCA true
                //receiving
            } else return !locked;//true if it is not locked onto a tx
        }

        /**
         * The <code>earliestNewTransmission</code> method determines if there is a
         * new transmission from the other threads
         *
         * @param bit equal to oneBitBeforeNow - BYTE_SIZE
         * @return tx new transmission
         */
        private Transmission earliestNewTransmission(long bit) {
            Transmission tx = null;
            synchronized (medium) {
                Iterator i = medium.transmissions.iterator();
                while (i.hasNext()) {
                    Transmission t = (Transmission) i.next();
                    if (bit <= t.firstBit && medium.arbitrator.lockTransmission(Receiver.this, t, (int) clock.cyclesToMillis(clock.getCount()))) {
                        if (tx == null) tx = t;
                        else if (t.firstBit < tx.firstBit) tx = t;
                    } else if (bit - 8 - 2 * medium.leadBits > t.lastBit) {
                        // remove older transmissions
                        i.remove();
                    }
                }
            }
            return tx;
        }

        /**
         * The <code>getIntersection</code> method calculate if transmissions intersect
         *
         * @param bit time in which calculate if tx intersect (oneBitBeforeNow - BYTE_SIZE)
         * @return it representing the list of transmissions that intersect
         */
        private List getIntersection(long bit) {
            List it = null;
            synchronized (medium) {
                Iterator i = medium.transmissions.iterator();
                while (i.hasNext()) {
                    Transmission t = (Transmission) i.next();
                    if (intersect(bit, t)) {
                        if (it == null) it = new LinkedList();
                        it.add(t);
                    }
                }
            }
            return it;
        }

        /**
         * The method <code>intersect</code> calculates if byte to transmit intersect
         * with another transmission
         *
         * @param bit time in which calculate if tx intersect (oneBitBeforeNow - BYTE_SIZE)
         * @param t   Transmission to find out if intersects
         * @return true if they intersect, false otherwise
         */
        private boolean intersect(long bit, Transmission t) {
            return bit >= t.firstBit && bit < t.lastBit;
        }

        private void waitForNeighbors(long gtime) {
            if (medium.synch != null) medium.synch.waitForNeighbors(gtime);
        }
    }

    public static class BasicArbitrator implements Arbitrator {
        public boolean lockTransmission(Receiver receiver, Transmission trans, int Milliseconds) {
            return true;
        }

        public char mergeTransmissions(Receiver receiver, List it, long bit, int Milliseconds) {
            assert it.size() > 0;
            Iterator i = it.iterator();
            Transmission first = (Transmission) i.next();
            int value = 0xff & first.getByteAtTime(bit);
            while (i.hasNext()) {
                Transmission next = (Transmission) i.next();
                int nval = 0xff & next.getByteAtTime(bit);
                value |= (nval << 8) ^ (value << 8); // compute corrupted bits
                value |= nval;
            }
            return (char) value;
        }

        public double computeReceivedPower(Medium.Transmission t, Medium.Receiver receiver, int Milliseconds) {
            return Pr;
        }

        public int getNoise(int index) {
            return Pn;
        }
    }

    /**
     * The {@code Transmission} class represents a transmission originating from
     * a particular {@code Transmitter} to this medium. A transmission consists
     * of a sequences of bytes sent one after another into the medium. Each transmission
     * has a start time and a power level.
     */
    public class Transmission {
        public final Transmitter origin;
        public final long start;
        public final long firstBit;
        public final double power;
        public final double Pt;
        public final double f;
        public long lastBit;
        public long end;

        protected int counter;
        protected byte[] data;

        /**
         * The constructor for the <code> Transmission </code> class creates a new
         * transmission with several properties like start and end times, first
         * and last bit to be transmitted and the data itself.
         *
         * @param o   Transmitter object
         * @param pow Power for the transmission
         * @param freq the frequency for the transmission
         */
        protected Transmission(Transmitter o, double pow, double freq) {
            origin = o;
            power = pow;
            Pt = pow;
            f = freq;
            start = o.clock.getCount();
            end = Long.MAX_VALUE;
            long l = start + o.leadCycles;
            firstBit = origin.getBitNum(l);
            lastBit = Long.MAX_VALUE;
            data = new byte[Arithmetic.roundup(o.medium.maxLength, BYTE_SIZE)];
        }

        /**
         * The method <code>end()</code> finishes the transmission and it updates
         * end time and last bit transmitted
         */
        public void end() {
            end = origin.clock.getCount();
            lastBit = firstBit + counter * BYTE_SIZE;
        }

        /**
         * The method <code>getByteAtTime()</code> gets the transmission data byte
         * at the time of bit
         *
         * @param bit time in bits for getting the byte
         * @return hi byte gotten
         */
        public byte getByteAtTime(long bit) {
            assert bit >= firstBit;
            int offset = (int) (bit - firstBit);
            int shift = offset & 0x7;
            int indx = offset / BYTE_SIZE;
            int hi = 0xff & data[indx] << shift;
            if (shift > 0) {
                int low = 0xff & data[1 + indx];
                return (byte) (hi | low >> (BYTE_SIZE - shift));
            }
            return (byte) hi;
        }
    }

    public final Synchronizer synch;
    public final Arbitrator arbitrator;

    public final int bitsPerSecond;
    public final int leadBits;
    public final int minLength;
    public final int maxLength;

    protected List transmissions = new LinkedList();

    /**
     * The constructor for the <code>Medium</code> class creates a new shared transmission
     * medium with the specified properties, including the bits per second, the lead time
     * before beginning transmission, and the minimum transmission size in bits. These
     * parameters are used to configure the medium and to ensure maximum possible simulation
     * performance.
     *
     * @param synch the synchronizer used to synchronize concurrent senders and receivers
     * @param arb   the arbitrator that determines how to merge received transmissions
     * @param bps   the bits per second throughput of this medium
     * @param ltb   the lead time in bits before beginning a transmission and the first bit
     * @param mintl the minimum transmission length
     * @param maxtl the maximum transmission length
     */
    public Medium(Synchronizer synch, Arbitrator arb, int bps, int ltb, int mintl, int maxtl) {
        this.synch = synch;
        bitsPerSecond = bps;
        leadBits = ltb;
        minLength = mintl;
        maxLength = maxtl;
        if (arb == null)
            arbitrator = new BasicArbitrator();
        else
            arbitrator = arb;
    }

    /**
     * The synchronized class <code>newTransmission</code> creates a new Transmission
     * object and adds it to the list of transmissions
     *
     * @param o Transmitter that creates the new transmission
     * @param p power for the new transmission
     * @return tx new transmission created
     */
    protected synchronized Transmission newTransmission(Transmitter o, double p, double f) {
        Transmission tx = new Transmission(o, p, f);
        transmissions.add(tx);
        return tx;
    }

    /**
     * The method <code>isCorruptedByte</code> computes if the byte is corrupted or not
     *
     * @param c byte to be computed
     * @return true if it is corrupted, false otherwise
     */
    public static boolean isCorruptedByte(char c) {
        return (c & 0xff00) != 0;
    }

    public static byte getCorruptedBits(char c) {
        return (byte) (c >> 8);
    }

    public static byte getTransmittedBits(char c) {
        return (byte) c;
    }
}
