/**
 * Copyright (c) 2007, Ben L. Titzer
 * See the file "license.txt" for details.
 *
 * Created Nov 10, 2007
 */
package avrora.sim.radio;

import avrora.sim.Simulator;

/**
 * The <code>Radio2</code> interface represents a radio device, which
 * typically has a simulator, a transmitter, receiver, and can be connected
 * to a {@code Medium} which manages transmissions.
 *
 * @author Ben L. Titzer
 */
public interface Radio {

    public Simulator getSimulator();
    public void setMedium(Medium m);
    public Medium getMedium();
    public Medium.Transmitter getTransmitter();
    public Medium.Receiver getReceiver();
}
