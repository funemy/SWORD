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

package avrora.actions;

import avrora.Main;
import avrora.core.Program;
import avrora.core.SourceMapping;
import avrora.stack.isea.ISEAnalyzer;
import cck.util.Option;
import cck.util.Util;

/**
 * The <code>ISEAAction</code> class implements interprocedural side-effect analysis. This class
 * implements an action that allows the user to invoke the analysis on a program from the command
 * line.
 *
 * @author Ben L. Titzer
 */
public class ISEAAction extends Action {

    protected final Option.Str START = newOption("procedure", "0x0000",
            "When this option is specified, the ISE analyzer will analyze only the specified procedure, rather " +
            "than the entire program.");

    public ISEAAction() {
        super("This action invokes the inter-procedural side-effect analysis tool.");
    }

    public void run(String[] args) throws Exception {
        Program p = Main.loadProgram(args);
        ISEAnalyzer a = new ISEAnalyzer(p);
        if ( !START.isBlank() ) {
            SourceMapping.Location location = p.getSourceMapping().getLocation(START.get());
            if ( location == null )
                Util.userError("Cannot find program location "+START.get());
            a.analyze(location.lma_addr);
        } else {
            a.analyze();
        }
    }
}
