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
 */

package avrora.syntax.elf;

import avrora.Main;
import avrora.actions.ELFDumpAction;
import avrora.arch.*;
import avrora.core.*;
import cck.elf.*;
import cck.text.StringUtil;
import cck.util.Option;
import cck.util.Util;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.List;

/**
 * The <code>ELFParser</code> class is capable of loading ELF (Executable and Linkable Format) files and
 * disassembling them into the simulator's internal format.
 *
 * @author Ben L. Titzer
 */
public class ELFParser extends ProgramReader {

    ELFHeader header;
    ELFProgramHeaderTable pht;
    ELFSectionHeaderTable sht;
    List symbolTables;
    ELFStringTable shstrtab;
    AbstractArchitecture arch;

    protected final Option.Bool SYMBOLS = newOption("load-symbols", true, "This option causes the ELF loader to load the symbol table (if it exists) from " + "the ELF file. The symbol table contains information about the names and sizes of " + "data items and functions within the executable. Enabling this option allows for " + "more source-level information during simulation, but disabling it speeds up loading " + "of ELF files.");

    public ELFParser() {
        super("The \"elf\" format loader reads a program from an ELF (Executable and Linkable " + "Format) as a binary and disassembles the sections corresponding to executable code.");
    }

    public Program read(String[] args) throws Exception {
        if (args.length == 0) Util.userError("no input files");
        if (args.length != 1) Util.userError("input type \"elf\" accepts only one file at a time.");

        String fname = args[0];
        Main.checkFileExists(fname);

        RandomAccessFile fis = new RandomAccessFile(fname, "r");

        // read the ELF header
        try {
            header = ELFLoader.readELFHeader(fis);
        } catch (ELFHeader.FormatError e) {
            Util.userError(fname, "invalid ELF header");
        }

        arch = getArchitecture();

        // read the program header table (if it exists)
        pht = ELFLoader.readPHT(fis, header);

        // read the section header table (if it exists)
        if (SYMBOLS.get()) {
            sht = ELFLoader.readSHT(fis, header);
            shstrtab = sht.getStringTable();
        }
        // load the sections from the ELF file
        Program p = loadSections(fis);

        // read the symbol tables (if they exist)
        loadSymbolTables(p, fis);

        return p;
    }

    private void loadSymbolTables(Program p, RandomAccessFile fis) throws IOException {
        SourceMapping map = new SourceMapping(p);
        p.setSourceMapping(map);
        if (SYMBOLS.get()) {
            symbolTables = ELFLoader.readSymbolTables(fis, header, sht);
            Iterator i = symbolTables.iterator();
            while (i.hasNext()) {
                ELFSymbolTable stab = (ELFSymbolTable)i.next();
                addSymbols(map, stab, stab.getStringTable());
            }
        }
    }

    public AbstractArchitecture getArchitecture() {
        String specarch = ARCH.get();
        String filearch = header.getArchitecture();
        AbstractArchitecture farch = ArchitectureRegistry.getArchitecture(filearch);
        if (!"".equals(specarch) && farch != ArchitectureRegistry.getArchitecture(specarch))
            Util.userError("ELF Error", "expected " + StringUtil.quote(specarch) + " architecture, but header reports " + StringUtil.quote(filearch));
        return farch;
    }

    private Program loadSections(RandomAccessFile fis) throws IOException {
        // load each section
        ELFDataInputStream is = new ELFDataInputStream(header, fis);
        Program p = createProgram();
        for (int cntr = 0; cntr < pht.entries.length; cntr++) {
            ELFProgramHeaderTable.Entry32 e = pht.entries[cntr];
            if (e.isLoadable() && e.p_filesz > 0) {
                fis.seek(e.p_offset);
                byte[] sect = is.read_section(e.p_offset, e.p_filesz);
                p.writeProgramBytes(sect, e.p_paddr);
                if (e.isExecutable()) disassembleSection(sect, e, p);
            }
        }
        return p;
    }

    private Program createProgram() {
        // find the dimensions of the program by searching loadable sections
        int minp = Integer.MAX_VALUE;
        int maxp = 0;
        for (int cntr = 0; cntr < pht.entries.length; cntr++) {
            ELFProgramHeaderTable.Entry32 e = pht.entries[cntr];
            if (e.isLoadable() && e.p_filesz > 0) {
                int start = e.p_paddr;
                int end = start + e.p_filesz;
                if (start < minp) minp = start;
                if (end > maxp) maxp = end;
            }
        }
        return new Program(arch, minp, maxp);
    }

    private void disassembleSection(byte[] sect, ELFProgramHeaderTable.Entry32 e, Program p) {
        AbstractDisassembler d = arch.getDisassembler();
        for (int off = 0; off < sect.length; off += 2) {
            AbstractInstr i = d.disassemble(e.p_paddr, off, sect);
            if (i != null) p.writeInstr(i, e.p_paddr + off);
        }
    }

    private void addSymbols(SourceMapping map, ELFSymbolTable stab, ELFStringTable str) {
        for (int cntr = 0; cntr < stab.entries.length; cntr++) {
            ELFSymbolTable.Entry e = stab.entries[cntr];
            if (e.isFunction() || e.isObject()) {
                String section = sht.getSectionName(e.st_shndx);
                String name = ELFDumpAction.getName(str, e.st_name);
                map.newLocation(section, name, e.st_value, findLMA(e));
            }
        }
    }

    private int findLMA(ELFSymbolTable.Entry e) {
        int vma_start = sht.entries[e.st_shndx].sh_addr;
        for ( int i = 0; i < pht.entries.length; i++ ) {
            if ( pht.entries[i].p_vaddr == vma_start )
                return e.st_value - vma_start + pht.entries[i].p_paddr;
        }
        return 0;
    }

}
