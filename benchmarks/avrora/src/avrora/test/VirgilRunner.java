/**
 * Copyright (c) 2006, Regents of the University of California
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
 * Creation date: Mar 28, 2007
 */

package avrora.test;

import avrora.core.Program;
import avrora.syntax.elf.*;
import avrora.sim.platform.Platform;
import avrora.sim.platform.DefaultPlatform;
import avrora.sim.mcu.MicrocontrollerFactory;
import avrora.sim.Simulator;
import avrora.sim.AtmelInterpreter;
import avrora.sim.Simulation;
import avrora.sim.types.SingleSimulation;
import avrora.Defaults;
import cck.util.Arithmetic;
import cck.text.Terminal;

/**
 * The <code>VirgilRunner</code> class implements a simple facade that can
 * be used by java programs to load a machine code program generated from
 * a Virgil program, feed input into it, run it, and get the output. This
 * is used to automatically test the correctness of the Virgil compiler,
 * for example. This class only supports a simplified set of functionality.
 *
 * @author Ben L. Titzer
 */
public class VirgilRunner {

    protected final String binaryFile;
    protected final Program program;
    protected final DefaultPlatform.Factory factory;

    public static void main(String[] args) throws Exception {
        if ( args.length < 2 ) {
            Terminal.printRed("Usage");
            Terminal.println(": runner <program> <input>");
            System.exit(-1);
        }
        VirgilRunner vr = new VirgilRunner(args[0]);
        int input = Integer.parseInt(args[1]);
        int output = vr.run(0x91, 0x92, input);
        Terminal.println(Integer.toString(output));
        System.exit(output);
    }

    public VirgilRunner(String bf) throws Exception {
        binaryFile = bf;
        ELFParser p = new ELFParser();
        program = p.read(new String[] { binaryFile });
        MicrocontrollerFactory mcf = Defaults.getMicrocontroller("atmega128");
        factory = new DefaultPlatform.Factory(8000000, 32768, mcf);
    }

    public int run(int staddr, int ioaddr, int input) {
        // create the node
        Platform p = factory.newPlatform(0, new SingleSimulation(), program);
        Simulator sim = p.getMicrocontroller().getSimulator();
        AtmelInterpreter inter = (AtmelInterpreter) sim.getInterpreter();

        try {
            // feed the input
            inter.writeDataByte(ioaddr, Arithmetic.low(input));
            inter.writeDataByte(ioaddr + 1, Arithmetic.high(input));
            // run the program
            sim.start();
        } catch ( Throwable t) {
            return -1;
        }
        // get the status code
        int status = inter.getDataByte(staddr);
        // return status code or output value
        return status != 0 ? status : inter.getDataByte(ioaddr) & 0xff;
    }
}
