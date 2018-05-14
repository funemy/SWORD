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
 * Creation date: Oct 11, 2005
 */

package jintgen.isdl.verifier;

import cck.test.TestCase;
import cck.test.TestEngine;
import jintgen.isdl.ArchDecl;
import jintgen.isdl.parser.ISDLParser;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * @author Ben L. Titzer
 */
public class VerifierTestHarness implements TestEngine.Harness {

    class VerifierTest extends TestCase.ExpectSourceError {

        VerifierTest(String fname, Properties props) {
            super(fname, props);
        }

        public void run() throws Exception {
            File archfile = new File(filename);
            FileInputStream fis = new FileInputStream(archfile);
            ISDLParser parser = new ISDLParser(fis);
            ArchDecl a = parser.ArchDecl();
            new Verifier(a).verify();
        }
    }

    public TestCase newTestCase(String fname, Properties props) throws Exception {
        return new VerifierTest(fname, props);
    }
}
