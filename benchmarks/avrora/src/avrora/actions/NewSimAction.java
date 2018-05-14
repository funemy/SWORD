/**
 * Copyright (c) 2005, Regents of the University of California
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
 * Creation date: Nov 14, 2005
 */

package avrora.actions;

import avrora.Main;
import avrora.arch.AbstractArchitecture;
import avrora.arch.AbstractDisassembler;
import avrora.syntax.elf.ELFParser;
import cck.util.Util;

/**
 * @author Ben L. Titzer
 */
public class NewSimAction extends Action {

    public static String HELP = "This action is an experimental testbed for developing and refining the " +
            "infrastructure surrounding the new simulation framework that allows multiple different " +
            "instruction architectures to be simulated.";

    public NewSimAction() {
        super(HELP);
    }

    public void run(String[] args) throws Exception {
        if ( args.length != 1 )
            Util.userError("no simulation file specified");
        String fn = args[0];
        Main.checkFileExists(fn);
        ELFParser loader = new ELFParser();
        loader.options.process(options);
        AbstractArchitecture arch = loader.getArchitecture();
        AbstractDisassembler d = arch.getDisassembler();
    }
}
