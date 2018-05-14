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
 * Created Sep 30, 2006
 */
package avrora.actions;

import cck.util.Util;
import cck.text.Terminal;
import cck.text.StringUtil;
import cck.text.TermUtil;
import cck.elf.*;
import avrora.Main;

import java.util.Iterator;
import java.util.List;
import java.io.RandomAccessFile;

/**
 * The <code>CFGAction</code> is an Avrora action that loads ELF files and dumps various
 * information.
 *
 * @author Ben L. Titzer
 */
public class ELFDumpAction extends Action {

    public static final String HELP = "The \"elf-dump\" action loads an ELF executable and " +
            "produces a listing of many important pieces of information, including the architecture, " +
            "sections, symbol tables and sizes of various structures.";

    /**
     * The default constructor of the <code>CFGAction</code> class simply creates an empty instance with the
     * appropriate name and help string.
     */
    public ELFDumpAction() {
        super(HELP);
    }

    /**
     * The <code>run()</code> method starts the control flow graph utility. The arguments passed
     * are the name of the file(s) that contain the program. The program is loaded, its control
     * flow graph is built, and it is output to the console in one of two textual formats; the
     * output supported by the dot tool, and a simple colored textual format.
     *
     * @param args the command line arguments to the control flow graph utility
     * @throws Exception
     */
    public void run(String[] args) throws Exception {

        String fname = args[0];
        Main.checkFileExists(fname);

        RandomAccessFile fis = new RandomAccessFile(fname, "r");

        try {
            // read the ELF header
            ELFHeader header = ELFLoader.readELFHeader(fis);
            printHeader(header);

            // read the program header table (if it exists)
            ELFProgramHeaderTable pht = ELFLoader.readPHT(fis, header);
            printPHT(pht);

            // read the section header table
            ELFSectionHeaderTable sht = ELFLoader.readSHT(fis, header);
            printSHT(sht);

            // read the symbol tables
            List symbolTables = ELFLoader.readSymbolTables(fis, header, sht);
            Iterator i = symbolTables.iterator();
            while ( i.hasNext() ) {
                ELFSymbolTable stab = (ELFSymbolTable) i.next();
                printSymbolTable(stab, sht);
            }

        } catch ( ELFHeader.FormatError e) {
            Util.userError(fname, "invalid ELF header");
        }
    }

    public static void printHeader(ELFHeader header) {
        Terminal.nextln();
        TermUtil.printSeparator();
        Terminal.printGreen("Ver Machine     Arch     Size  Endian");
        Terminal.nextln();
        TermUtil.printThinSeparator();
        Terminal.print(StringUtil.rightJustify(header.e_version, 3));
        Terminal.print(StringUtil.rightJustify(header.e_machine, 8));
        Terminal.print(StringUtil.rightJustify(header.getArchitecture(), 9));
        Terminal.print(StringUtil.rightJustify(header.is64Bit() ? "64 bits" : "32 bits", 9));
        Terminal.print(header.isLittleEndian() ? "  little" : "  big");
        Terminal.nextln();
    }

    public static void printSHT(ELFSectionHeaderTable sht) {
        TermUtil.printSeparator(Terminal.MAXLINE, "Section Header Table");
        Terminal.printGreen("Ent  Name                        Type   Address  Offset    Size  Flags");
        Terminal.nextln();
        TermUtil.printThinSeparator();
        for ( int cntr = 0; cntr < sht.entries.length; cntr++ ) {
            ELFSectionHeaderTable.Entry32 e = sht.entries[cntr];
            Terminal.print(StringUtil.rightJustify(cntr, 3));
            Terminal.print("  "+StringUtil.leftJustify(e.getName(), 24));
            Terminal.print(StringUtil.rightJustify(e.getType(), 8));
            Terminal.print("  "+StringUtil.toHex(e.sh_addr, 8));
            Terminal.print(StringUtil.rightJustify(e.sh_offset, 8));
            Terminal.print(StringUtil.rightJustify(e.sh_size, 8));
            Terminal.print("  "+e.getFlags());
            Terminal.nextln();
        }
    }

    public static String getName(ELFStringTable st, int ind) {
        if ( st == null ) return "";
        return st.getString(ind);
    }

    public static void printPHT(ELFProgramHeaderTable pht) {
        TermUtil.printSeparator(Terminal.MAXLINE, "Program Header Table");
        Terminal.printGreen("Ent     Type  Virtual   Physical  Offset  Filesize  Memsize  Flags");
        Terminal.nextln();
        TermUtil.printThinSeparator();
        for ( int cntr = 0; cntr < pht.entries.length; cntr++ ) {
            ELFProgramHeaderTable.Entry32 e = pht.entries[cntr];
            Terminal.print(StringUtil.rightJustify(cntr, 3));
            Terminal.print(StringUtil.rightJustify(ELFProgramHeaderTable.getType(e), 9));
            Terminal.print("  "+StringUtil.toHex(e.p_vaddr, 8));
            Terminal.print("  "+StringUtil.toHex(e.p_paddr, 8));
            Terminal.print(StringUtil.rightJustify(e.p_offset, 8));
            Terminal.print(StringUtil.rightJustify(e.p_filesz, 10));
            Terminal.print(StringUtil.rightJustify(e.p_memsz, 9));
            Terminal.print("  "+e.getFlags());
            Terminal.nextln();
        }
    }

    public static void printSymbolTable(ELFSymbolTable stab, ELFSectionHeaderTable sht) {
        TermUtil.printSeparator(Terminal.MAXLINE, "Symbol Table");
        Terminal.printGreen("Ent  Type     Section     Bind    Name                     Address      Size");
        Terminal.nextln();
        TermUtil.printThinSeparator();
        ELFStringTable str = stab.getStringTable();
        for ( int cntr = 0; cntr < stab.entries.length; cntr++ ) {
            ELFSymbolTable.Entry e = stab.entries[cntr];
            Terminal.print(StringUtil.rightJustify(cntr, 3));
            Terminal.print("  "+StringUtil.leftJustify(e.getType(), 7));
            Terminal.print("  "+StringUtil.leftJustify(sht.getSectionName(e.st_shndx), 12));
            Terminal.print(StringUtil.leftJustify(e.getBinding(), 8));
            Terminal.print(StringUtil.leftJustify(getName(str, e.st_name), 22));
            Terminal.print("  "+StringUtil.toHex(e.st_value, 8));
            Terminal.print("  "+StringUtil.rightJustify(e.st_size, 8));
            Terminal.nextln();
        }
    }
}
