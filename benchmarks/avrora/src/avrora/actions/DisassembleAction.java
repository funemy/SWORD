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
import avrora.arch.*;
import cck.text.StringUtil;
import cck.text.Terminal;
import cck.util.Option;
import cck.util.Util;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * The <code>DisassembleAction</code> class represents an action that allows the user to disassemble
 * a binary file and display the instructions. This is useful for debugging the disassembler and also
 * for inspecting binaries.
 *
 * @author Ben L. Titzer
 */
public class DisassembleAction extends Action {

    Option.Str ARCH = newOption("arch", "avr",
            "This option selects the architecture for the disassembler.");
    Option.Long MAX_LENGTH = newOption("max-length", 16,
            "This option specifies the maximum length of an instruction in bytes.");
    Option.Bool EXHAUSTIVE = newOption("exhaustive", false,
            "When this option is specified, this action will test the disassembler exhaustively by " +
            "trying bit patterns systematically.");
    Option.Str FILE = newOption("file", "",
            "When this option is specified, this action will test the disassembler by loading the " +
            "specified file and disassembling the data contained inside.");

    public DisassembleAction() {
        super("The \"disassemble\" action disassembles a binary file into source level instructions.");
    }

    /**
     * The <code>run()</code> method executes the action. The arguments on the command line are passed.
     * The <code>Disassemble</code> action expects the first argument to be the name of the file to
     * disassemble.
     * @param args the command line arguments
     * @throws Exception if there is a problem reading the file or disassembling the instructions in the
     * file
     */
    public void run(String[] args) throws Exception {
        byte[] buf = new byte[128];

        AbstractArchitecture arch = ArchitectureRegistry.getArchitecture(ARCH.get());
        AbstractDisassembler da = arch.getDisassembler();
        if ( EXHAUSTIVE.get() ) {
            // run the exhaustive disassembler
            exhaustive(da);
        } else if ( !FILE.isBlank() ) {
            // load and disassemble a complete file
            disassembleFile(da);
        } else {
            // disassemble the bytes specified on the command line
            disassembleArguments(args, buf, da);
        }
    }

    private void disassembleArguments(String[] args, byte[] buf, AbstractDisassembler da) {
        if ( args.length < 1 )
            Util.userError("no input data");
        for ( int cntr = 0; cntr < args.length; cntr++ ) {
            buf[cntr] = (byte)StringUtil.evaluateIntegerLiteral(args[cntr]);
        }

        disassembleAndPrint(buf, 0, da);
    }

    private void disassembleFile(AbstractDisassembler da) throws IOException {
        String fname = FILE.get();
        Main.checkFileExists(fname);
        FileInputStream fis = new FileInputStream(fname);
        byte[] buf = new byte[fis.available()];
        fis.read(buf);
        for ( int cntr = 0; cntr < buf.length; ) {
            cntr += disassembleAndPrint(buf, cntr, da);
        }
    }

    void exhaustive(AbstractDisassembler da) {
        byte[] buf = new byte[(int)MAX_LENGTH.get()];
        for ( int cntr = 0; cntr < 0x10000; cntr++ ) {
            buf[0] = (byte)cntr;
            buf[1] = (byte)(cntr >> 8);
            try {
                disassembleAndPrint(buf, 0, da);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int disassembleAndPrint(byte[] buf, int off, AbstractDisassembler da) {
        String result;
        int len = 2;
        AbstractInstr instr = da.disassemble(0, off, buf);
        if ( instr == null ) result = "null";
        else {
            result = instr.toString();
            len = instr.getSize();
        }
        print(buf, off, len, result);
        return len;
    }

    private void print(byte[] buf, int off, int len, String str) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(StringUtil.addrToString(off));
        sbuf.append(": ");
        for ( int cntr = 0; cntr < len; cntr++ ) {
            StringUtil.toHex(sbuf, buf[off+cntr], 2);
            sbuf.append(' ');
        }
        for ( int cntr = sbuf.length(); cntr < 30; cntr++ ) sbuf.append(' ');
        sbuf.append(str);
        Terminal.println(sbuf.toString());
    }
}
