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
 * Created Sep 5, 2005
 */
package cck.elf;

import cck.text.StringUtil;
import cck.util.Util;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * The <code>ELFProgramHeaderTable</code> class represents a program header table
 * contained in an ELF file. This table contains information about each section
 * in the program. This class represents a cleaned up view of the table. Since
 * the size and number of entries in this table are determined from the information
 * contained in the ELF header, this class requires an instance of the
 * <code>ELFHeader</code> class to be passed to the constructor.
 *
 * </p>
 * The ELF format states that a program header table is required for executables; this
 * table contains information for the operating system (or bootloader or programmer
 * in the case of embedded systems) to create a process image from the binary. This table
 * is optional for relocatable object files.
 *
 * @author Ben L. Titzer
 */
public class ELFProgramHeaderTable {

    public static final int PT_NULL = 0;
    public static final int PT_LOAD = 1;
    public static final int PT_DYNAMIC = 2;
    public static final int PT_INTERP = 3;
    public static final int PT_NOTE = 4;
    public static final int PT_SHLIB = 5;
    public static final int PT_PHDR = 6;
    public static final int PT_LOPROC = 0x70000000;
    public static final int PT_HIPROC = 0x7fffffff;

    public static final int PF_EXEC = 0x1;
    public static final int PF_WRITE = 0x2;
    public static final int PF_READ = 0x4;

    public class Entry32 {
        public int p_type;
        public int p_offset;
        public int p_vaddr;
        public int p_paddr;
        public int p_filesz;
        public int p_memsz;
        public int p_flags;
        public int p_align;

        public String getFlags() {
            StringBuffer flags = new StringBuffer();
            if ((p_flags & PF_EXEC) != 0) flags.append("EXEC ");
            if ((p_flags & PF_WRITE) != 0) flags.append("WRITE ");
            if ((p_flags & PF_READ) != 0) flags.append("READ ");
            return flags.toString();
        }

        public boolean isLoadable() {
            return p_type == PT_LOAD;
        }

        public boolean isExecutable() {
            return (p_flags & PF_EXEC) != 0;
        }
    }

    public final ELFHeader header;
    public final Entry32[] entries;

    /**
     * The constructor for the <code>ELFProgramHeaderTable</code> class creates a new instance
     * for the file containing the specified ELF header. The <code>ELFHeader</code> instance
     * contains information about the ELF file including the machine endianness that is
     * important for the program header table.
     * @param header the initialized ELF header from the file specified.
     */
    public ELFProgramHeaderTable(ELFHeader header) {
        if ( !header.is32Bit() )
            throw Util.failure("Only 32 bit ELF files are supported.");
        this.header = header;
        entries = new Entry32[header.e_phnum];
    }

    /**
     * The <code>read()</code> method reqds the program header table from the specified
     * input stream. This method assumes that the input stream has been positioned at
     * the beginning of the program header table.
     * @param fis the input stream from which to read the program header table
     * @throws IOException if there is a problem reading the header table from the input
     */
    public void read(RandomAccessFile fis) throws IOException {
        if ( entries.length == 0 ) return;
        // seek to the beginning of the table
        fis.seek(header.e_phoff);
        ELFDataInputStream is = new ELFDataInputStream(header, fis);
        // read each entry
        for ( int cntr = 0; cntr < entries.length; cntr++ ) {
            Entry32 e = new Entry32();
            e.p_type   = is.read_Elf32_Word();
            e.p_offset = is.read_Elf32_Off();
            e.p_vaddr  = is.read_Elf32_Addr();
            e.p_paddr  = is.read_Elf32_Addr();
            e.p_filesz = is.read_Elf32_Word();
            e.p_memsz  = is.read_Elf32_Word();
            e.p_flags  = is.read_Elf32_Word();
            e.p_align  = is.read_Elf32_Word();
            entries[cntr] = e;
            // read the rest of the entry (padding)
            for ( int pad = 32; pad < header.e_phentsize; pad++) fis.read();
        }
    }

    public Entry32 getEntry(int ind) {
        return entries[ind];
    }

    public static String getType(Entry32 e) {
        switch ( e.p_type ) {
            case PT_NULL: return "null";
            case PT_LOAD: return "load";
            case PT_DYNAMIC: return "dynamic";
            case PT_INTERP: return "interp";
            case PT_NOTE: return "note";
            case PT_SHLIB: return "shlib";
            case PT_PHDR: return "phdr";
            default: return StringUtil.toHex(e.p_type, 8);
        }
    }

}
