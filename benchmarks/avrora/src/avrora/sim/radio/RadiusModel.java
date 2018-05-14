/**
 * Copyright (c) 2007, Ben L. Titzer
 * See the file "license.txt" for details.
 *
 * Created Nov 14, 2007
 */
package avrora.sim.radio;

import java.util.*;

/**
 * The <code>RadiusModel</code> definition.
 *
 * @author Ben L. Titzer
 */
public class RadiusModel implements Medium.Arbitrator {

    protected final double minimumDistance;
    protected final double minimumDistanceSq;
    protected final double maximumDistance;
    protected final double maximumDistanceSq;
    protected final Map positions;

    public static final class Position {
        public final double x;
        public final double y;
        public final double z;

        public Position(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public RadiusModel(double minDist, double maxDist) {
        maximumDistance = maxDist;
        maximumDistanceSq = maxDist * maxDist;
        minimumDistance = minDist;
        minimumDistanceSq = minDist * minDist;
        positions = new HashMap();
    }
    
    public int getNoise(int index){   
        return (-90);   
        
    }
        public double computeReceivedPower(Medium.Transmission t, Medium.Receiver receiver, int Milliseconds) {
        
        return (0);
    }

    public boolean lockTransmission(Medium.Receiver receiver, Medium.Transmission trans,int Milliseconds) {
        return distanceSq(trans.origin, receiver) <= maximumDistanceSq;
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

    public void setPosition(Radio radio, double x, double y, double z) {
        Position pos = new Position(x, y, z);
        positions.put(radio.getTransmitter(), pos);
        positions.put(radio.getReceiver(), pos);
    }

    public void setPosition(Radio radio, Position pos) {
        positions.put(radio.getTransmitter(), pos);
        positions.put(radio.getReceiver(), pos);
    }

    protected double distanceSq(Medium.Transmitter t, Medium.Receiver r) {
        double distSq = 0;
        Position a = (Position)positions.get(t);
        Position b = (Position)positions.get(r);
        if ( a != null && b != null) {
            double dx = a.x - b.x;
            double dy = a.y - b.y;
            double dz = a.z - b.z;
            // no need to take the square root if we are just checking a threshold.
            distSq = dx * dx + dy * dy + dz * dz;
        }
        if (distSq < minimumDistance) return minimumDistanceSq;
        else return distSq;
    }
}
