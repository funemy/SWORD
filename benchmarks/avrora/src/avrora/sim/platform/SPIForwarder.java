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

package avrora.sim.platform;

import avrora.sim.Simulator;
import avrora.sim.clock.Clock;
import avrora.sim.mcu.SPI;
import avrora.sim.mcu.SPIDevice;
import cck.text.Terminal;
import cck.util.Util;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The <code>SerialForwarder</code> class implements a serial forwarder that takes traffic to and from a socket and
 * directs it into the UART chip of a simulated device.
 *
 * @author Olaf Landsiedel
 * @author Ben L. Titzer
 */
public class SPIForwarder implements SPIDevice {

    public static final int BPS = 2400;

    private ServerSocket serverSocket;
    protected int portNumber;
    private Socket socket;

    private OutputStream out;
    private InputStream in;

    private SPI spi;
    private SFTicker ticker;
    private byte[] buffer;
    protected int bitsPerSecond;

    public SPIForwarder(SPI spdv, int pn, int bps, boolean master) {
        spdv.connect(this);

        spi = spdv;
        portNumber = pn;
        bitsPerSecond = bps;
        buffer = new byte[1];

        if ( master ) {
            ticker = new SFTicker(spdv.getClock());
            ticker.start();
        }

        try {
            serverSocket = new ServerSocket(portNumber);
            Terminal.print("Waiting for spi connection on port " + portNumber + "...");
            Terminal.flush();
            socket = serverSocket.accept();
            Terminal.println("connected to " + socket.getRemoteSocketAddress());
            out = socket.getOutputStream();
            in = socket.getInputStream();
        } catch (IOException e) {
            throw Util.unexpected(e);
        }
    }


    public SPI.Frame nextFrame() {
        try {
            // called when we are expected to supply data to the SPI.
            byte data = 0;
            if ( in.available() > 0 ) {
                // if there is something available in the socket, transfer it
                if ( in.read(buffer, 0, 1) > 0 ) data = buffer[0];
            }
            return SPI.newFrame(data);
        } catch (IOException e) {
            throw Util.unexpected(e);
        }
    }


    public SPI.Frame exchange(SPI.Frame frame) {
        try {
            receive(frame);
            return nextFrame();
        } catch (IOException e) {
            throw Util.unexpected(e);
        }
    }

    private void receive(SPI.Frame frame) throws IOException {
        out.write(frame.data);
    }

    public void connect(SPIDevice d) {
        // do nothing.
    }

    private class SFTicker implements Simulator.Event {

        private final long delta;
        private final Clock clock;

        SFTicker(Clock c) {
            delta = c.getHZ() / bitsPerSecond;
            clock = c;
        }

        public void fire() {
            try {
                if (in.available() >= 1) {
                    // send our frame to the SPI
                    receive(spi.exchange(nextFrame()));
                }
            } catch (IOException e) {
                throw Util.unexpected(e);
            }
            clock.insertEvent(this, delta);
        }

        void start() {
            clock.insertEvent(this, delta);
        }
    }
}
