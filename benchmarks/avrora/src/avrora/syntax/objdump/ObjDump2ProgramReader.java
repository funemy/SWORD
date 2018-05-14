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

package avrora.syntax.objdump;

import avrora.arch.legacy.LegacyArchitecture;
import avrora.core.Program;
import avrora.core.ProgramReader;
import avrora.syntax.RawModule;
import cck.util.Util;
import java.io.*;

/**
 * The <code>ObjdumpProgramReader</code> is an implementation of the <code>ProgramReader</code> that reads
 * source assembly files in the Atmel style syntax. It can handle only one file at a time.
 *
 * @author Ben L. Titzer
 */
public class ObjDump2ProgramReader extends ProgramReader {

    /**
     * The <code>read()</code> method takes the command line arguments passed to main and interprets it as a
     * list of filenames to load. It expects only one filename to be present. It will load, parse, and
     * simplify the program and return it.
     *
     * @param args the string arguments representing the names of the files to read
     * @return a program obtained by parsing and building the file
     * @throws ParseException if the file does not parse correctly
     * @throws IOException    if there is a problem reading from the files
     */
    public Program read(String[] args) throws Exception {
        if (args.length == 0) Util.userError("no input files");
        if (args.length != 1) Util.userError("input type \"odpp\" accepts only one file at a time.");
        if (getArchitecture() != LegacyArchitecture.INSTANCE)
            Util.userError("input type  \"odpp\" parses only the \"legacy\" architecture.");

        File f = new File(args[0]);
        RawModule module = new RawModule(true, true);

        //Status.begin("Parsing");
        ObjDumpParser parser = new ObjDumpParser(new FileReader(f), module, f.getName());
        parser.Module();
        //Status.success();
        //Status.begin("Building");
        Program p = module.build();
        //Status.success();
        addIndirectEdges(p);
        return p;
    }

    public ObjDump2ProgramReader() {
        super("The \"odpp\" input format reads programs that are the " + "output of the \"avr-objdump\" utility provided with avr-binutils " + "and that have been preprocessed with Avrora's preprocessor utility. ");
    }

}
