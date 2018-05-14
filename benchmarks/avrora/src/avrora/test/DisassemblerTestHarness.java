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

package avrora.test;

import avrora.arch.legacy.LegacyDisassembler;
import avrora.arch.legacy.LegacyInstr;
import avrora.core.Program;
import avrora.core.ProgramReader;
import avrora.syntax.Module;
import avrora.syntax.objdump.ObjDump2ProgramReader;
import cck.test.*;
import cck.text.StringUtil;
import java.util.Properties;

/**
 * The <code>SimulatorTestHarness</code> implements a test harness that interfaces the
 * <code>avrora.test.AutomatedTester</code> in order to automate testing of the AVR parser and simulator.
 *
 * @author Ben L. Titzer
 */
public class DisassemblerTestHarness implements TestEngine.Harness {

    class DisassemblerTest extends TestCase.ExpectSourceError {

        Module module;
        Program program;
        LegacyDisassembler disassembler;
        LegacyInstr[] instrs;

        DisassemblerTest(String fname, Properties props) {
            super(fname, props);
            disassembler = new LegacyDisassembler();
        }

        public void run() throws Exception {
            ProgramReader r = new ObjDump2ProgramReader();
            String[] args = { filename };
            program = r.read(args);

            byte[] data = new byte[program.program_end];
            instrs = new LegacyInstr[program.program_end];

            for ( int cntr = 0; cntr < program.program_end; cntr++ ) {
                data[cntr] = program.readProgramByte(cntr);
            }

            for ( int cntr = 0; cntr < program.program_end; cntr = program.getNextPC(cntr) ) {
                LegacyInstr i = (LegacyInstr)program.readInstr(cntr);
                if ( i == null ) continue;

                instrs[cntr] = disassembler.disassembleLegacy(data, 0, cntr);
            }
        }

        public TestResult match(Throwable t) {
            
            for ( int cntr = 0; cntr < program.program_end; cntr = program.getNextPC(cntr) ) {
                LegacyInstr i = (LegacyInstr)program.readInstr(cntr);
                if ( i == null ) continue;

                LegacyInstr id = instrs[cntr];
                if ( !i.equals(id) ) {
                    return new TestResult.TestFailure("disassembler error at "+StringUtil.addrToString(cntr)
                            +", expected: "+i+" received: "+id);
                }
            }

            return new TestResult.TestSuccess();
        }

    }

    public TestCase newTestCase(String fname, Properties props) throws Exception {
        return new DisassemblerTest(fname, props);
    }

}
