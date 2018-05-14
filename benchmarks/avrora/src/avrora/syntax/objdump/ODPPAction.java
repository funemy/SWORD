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

import avrora.actions.Action;
import cck.text.Verbose;
import cck.util.Option;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;

/**
 * The <code>ObjDumpPreprocessor</code> class is a utility class that takes the output from the
 * <code>avr-objdump</code> utility and produces a cleaned up version that is more suitable for parsing into
 * the internal format of Avrora.
 *
 * @author Ben L. Titzer
 * @author Vids Samanta
 */
public class ODPPAction extends Action {

    protected final Option.Str FILE = newOption("file", "", "The \"file\" option, when set, indicates the file to which to output the " + "preprocessed objdump output.");
    protected final Option.List SECTIONS = newOptionList("sections", ".text,.data", "This option specifies a list of sections that the loader should load from " + "the output.");

    private static final String HELP = "The \"odpp\" action tests the functionality of the objdump preprocessor that " + "cleans up the output of objdump into something more suitable for automated parsing.";

    public ODPPAction() {
        super(HELP);
    }

    public ODPPAction(Option.List s) {
        super(HELP);
        SECTIONS.set(s.stringValue());
    }

    public void run(String[] args) throws Exception {
        ObjDumpReformatter rf = new ObjDumpReformatter(SECTIONS.get());
        if (FILE.isBlank()) {
            System.out.println(rf.cleanCode(args[0]));
        } else {
            FileOutputStream outf = new FileOutputStream(FILE.get());
            PrintWriter p = new PrintWriter(outf);
            p.write(rf.cleanCode(args[0]).toString());
            p.close();
        }
    }
}
