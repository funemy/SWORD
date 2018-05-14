/*
 * "Copyright (c) 2009 Cork Institute of Technology, Ireland
 * All rights reserved."
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE CORK INSTITUTE OF TECHNOLOGY BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE CORK INSTITUTE
 * OF TECHNOLOGY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE CORK INSTITUTE OF TECHNOLOGY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 */
package avrora.sim.radio;

import java.util.*;
import avrora.sim.state.Complex;
/**
 * The <code>LossyModel</code> definition.
 *
 * @author Rodolfo de Paz
 */
public class LossyModel implements Medium.Arbitrator {

    protected static final double Sensitivity = -95;
    protected int TimeBefore = 0;
    protected final Map positions;
    protected final double lambda = Math.exp(-5D/6D);
    protected final double u = Math.sqrt((1-Math.pow(lambda,2D)));
    protected double Csf,Sf;
    protected boolean first = true;

    public static final class Position {
        public final double x,y,z,rho;

        public Position(double x, double y, double z, double rho) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rho = rho;
        }
    }

    public static final class Noise{
        public final ArrayList Noise;

        public Noise(ArrayList Noise){
            this.Noise = Noise;
        }
    }

    public LossyModel() {
        positions = new HashMap();
    }

    public boolean lockTransmission(Medium.Receiver receiver, Medium.Transmission trans,int Milliseconds) {
        double PowerRec = computeReceivedPower(trans,receiver,Milliseconds);
        int Pn = getNoise(Milliseconds);
        if (trans.f == receiver.frequency){
            if (PowerRec > Sensitivity && PowerRec > Pn) return true;
            else return false;
        }else return false;
    }

    public char mergeTransmissions(Medium.Receiver receiver, List it, long bit,int Milliseconds) {
        assert it.size() > 0;
        boolean one = false;
        int value = 0;
        Iterator i = it.iterator();
        while ( i.hasNext() ) {
            Medium.Transmission next = (Medium.Transmission)i.next();
            if (lockTransmission(receiver, next, Milliseconds)) {
                if (one) {
                    int nval = 0xff & next.getByteAtTime(bit);
                    value |= (nval << 8) ^ (value << 8); // compute corrupted bits
                    value |= nval;
                } else {
                    one = true;
                    value = 0xff & next.getByteAtTime(bit);
                }
            }
        }
        assert one;
        return (char)value;
    }

    private static Random rn = new Random();

    public static double getGaussian(double mean, double std){
        return mean + std*rn.nextGaussian();
    }

    private double Rayleigh() {
        Complex c = new Complex(getGaussian(0,1),getGaussian(0,1));
        return c.abs(c);
    }

    private double Shadowing(double mean,double std,int Milliseconds){
       //Correlated shadowing computation updating values every 1second
        if (first) {
            first=false;
            return Sf = getGaussian(mean,std);
        } else {
            if (Milliseconds < 1000) {
                return Csf = Sf;
            } else {
                if ((Milliseconds - TimeBefore) > 1000) {
                    TimeBefore = Milliseconds;
                    return Csf = lambda * Csf + u * getGaussian(mean,std);
                } else {
                    return Csf;
                }
            }
        }
    }

    public void setPosition(Radio radio, double x, double y, double z, double rho) {
        Position pos = new Position(x, y, z, rho);
        positions.put(radio.getTransmitter(), pos);
        positions.put(radio.getReceiver(), pos);
    }

    public void setPosition(Radio radio, Position pos) {
        positions.put(radio.getTransmitter(), pos);
        positions.put(radio.getReceiver(), pos);
    }

    public int getNoise(int index){
        if (noise.sizeNoise() == 1) return noise.getNoise(0);
        else {
                index = index % noise.sizeNoise();
        }
        return noise.getNoise(index);

    }

    protected double distance(Medium.Transmitter t, Medium.Receiver r) {
        //Distance computed from mote 3D coordinates of topology file
        double dist = 0;
        Position a = (Position)positions.get(t);
        Position b = (Position)positions.get(r);
        if ( a != null && b != null) {
            double dx = a.x - b.x;
            double dy = a.y - b.y;
            double dz = a.z - b.z;
            dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        return dist;
    }

    protected double densityObstacles(Medium.Transmitter t, Medium.Receiver r) {
        /*Density of obstacles computed as the maximum rho between transmitter and
        receiver motes inserted in topology file*/
        double rho = 0D;
        Position a = (Position)positions.get(t);
        Position b = (Position)positions.get(r);
        if ( a != null && b != null) {
            rho = Math.max(a.rho,b.rho);
        }
        return rho;
    }

    /**
     * The <code>computeReceivedPower</code> method
     * @param t transmission
     * @param receiver
     * @param Millisecons
     * @return received power (dBm)
     */
    public double computeReceivedPower(Medium.Transmission t, Medium.Receiver receiver, int Milliseconds) {
        //Indoor channel model with consideration of movement of people/obstacles
        //people density -number of people or obstacles over an occupied area- (m-2)
        double p = densityObstacles(t.origin, receiver);
        //length of ray over area with moving people(m)
        double d = distance(t.origin, receiver);
        //Time sharing between bad and good state
        double A = Math.pow((1-p),0.2*d);
        //Log-distance Pathloss model (d0,n)
        double n = 3;
        /*Reference pathloss Lo calculated with Free space formula
        double PathLoss = 20*Math.log10((4*Math.PI*d0*t.f*1E6)/(299792458));*/
        //Reference pathloss from measurements for d0=1m
        double PathLoss = 55;
        if (d > 1) PathLoss = PathLoss + 10*n*Math.log10(d);
        //Rayleigh fading
        double L_Rayleigh = Rayleigh();
        //Ricean fading
        int k = 6;//Rician k factor
        double L_Rician =(L_Rayleigh/(Math.sqrt(k))+1);
        //Shadowing
        double std = (Math.log10(55*d*p+1)/Math.log10(7)) + 0.5;
        double mean = Math.pow(3*d*p,0.7);
        double s = Shadowing(mean,std,Milliseconds);
        //double s = getGaussian(mean,std);
        double Lsf = Math.pow(10,s/20);
        //Fading computation following Lutz's model
        double Fading = 10*Math.log10(A * L_Rician + (1-A) * (Lsf*L_Rayleigh));
        return (t.Pt - Fading - PathLoss);
    }
}
