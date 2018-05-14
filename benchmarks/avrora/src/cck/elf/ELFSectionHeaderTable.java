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

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * The <code>ELFSectionHeaderTable</code> class represents a cleaned-up view
 * of the ELF format's section header table that contains a summary of each
 * section in the ELF file. Each section might contain code, data, symbol
 * table information, etc. This class reads this header table from a
 * random access file.
 *
 * @author Ben L. Titzer
 */
public class ELFSectionHeaderTable {

    public static final int SHT_NULL = 0;
    public static final int SHT_PROGBITS = 1;
    public static final int SHT_SYMTAB = 2;
    public static final int SHT_STRTAB = 3;
    public static final int SHT_RELA = 4;
    public static final int SHT_HASH = 5;
    public static final int SHT_DYNAMIC = 6;
    public static final int SHT_NOTE = 7;
    public static final int SHT_NOBITS = 8;
    public static final int SHT_REL = 9;
    public static final int SHT_SHLIB = 10;
    public static final int SHT_DYNSYM = 11;
    public static final int SHT_LOPROC = 0x70000000;
    public static final int SHT_HIPROC = 0x7fffffff;
    public static final int SHT_LOUSER = 0x80000000;
    public static final int SHT_HIUSER = 0x8fffffff;

    public static final int SHF_WRITE = 0x1;
    public static final int SHF_ALLOC = 0x2;
    public static final int SHF_EXECINSTR = 0x4;
    public static final int SHF_MASKPROC = 0xf0000000;

    public class Entry32 {
        public int sh_name;
        public int sh_type;
        public int sh_flags;
        public int sh_addr;
        public int sh_offset;
        public int sh_size;
        public int sh_link;
        public int sh_info;
        public int sh_addralign;
        public int sh_entsize;

        public String getType() {
            switch ( sh_type ) {
                case SHT_NULL: return "null";
                case SHT_PROGBITS: return "program";
                case SHT_SYMTAB: return "symtab";
                case SHT_STRTAB: return "strtab";
                case SHT_RELA: return "rela";
                case SHT_HASH: return "hash";
                case SHT_DYNAMIC: return "dynamic";
                case SHT_NOTE: return "note";
                case SHT_NOBITS: return "nobits";
                case SHT_REL: return "rel";
                case SHT_SHLIB: return "shlib";
                case SHT_DYNSYM: return "dynsym";
                default: return "unknown";
            }
        }

        public String getFlags() {
            StringBuffer flags = new StringBuffer();
            if ( (sh_flags & SHF_WRITE) != 0 ) flags.append("WRITE ");
            if ( (sh_flags & SHF_ALLOC) != 0 ) flags.append("ALLOC ");
            if ( (sh_flags & SHF_EXECINSTR) != 0 ) flags.append("EXEC ");
            return flags.toString();
        }

        public boolean isStringTable() {
            return sh_type == SHT_STRTAB;
        }

        public boolean isSymbolTable() {
            return sh_type == SHT_SYMTAB;
        }

        public String getName() {
            if ( strtab != null ) return strtab.getString(sh_name);
            return "";
        }
    }

    public final ELFHeader header;
    public final Entry32[] entries;
    protected ELFStringTable strtab;

    /**
     * The constructor for the <code>ELFSectionHeaderTable</code> class creates a new instance
     * corresponding to the specified ELF header. The ELF header contains an entry that
     * stores the offset of the beginning of the section header table relative to the start
     * of the file.
     * @param header the ELF header containing information about this ELF file
     */
    public ELFSectionHeaderTable(ELFHeader header) {
        this.header = header;
        entries = new Entry32[header.e_shnum];
    }

    /**
     * The <code>read()</code> method reads the section header table from the specified
     * file. The file must support random access, since the beginning offset of the table
     * is specified in the header table informationi.
     * @param fis the random access file that contains the section header table
     * @throws IOException if there is a problem reading the data from the file
     */
    public void read(RandomAccessFile fis) throws IOException {
        if ( entries.length == 0 ) return;
        // seek to the beginning of the section header table
        fis.seek(header.e_shoff);
        ELFDataInputStream is = new ELFDataInputStream(header, fis);
        // load each of the section header entries
        for ( int cntr = 0; cntr < entries.length; cntr++ ) {
            Entry32 e = new Entry32();
            e.sh_name      = is.read_Elf32_Word();
            e.sh_type      = is.read_Elf32_Word();
            e.sh_flags     = is.read_Elf32_Word();
            e.sh_addr      = is.read_Elf32_Addr();
            e.sh_offset    = is.read_Elf32_Off();
            e.sh_size      = is.read_Elf32_Word();
            e.sh_link      = is.read_Elf32_Word();
            e.sh_info      = is.read_Elf32_Word();
            e.sh_addralign = is.read_Elf32_Word();
            e.sh_entsize   = is.read_Elf32_Word();

            entries[cntr] = e;
            for ( int pad = 40; pad < header.e_shentsize; pad++ ) fis.read();
        }
    }

    public void setStringTable(ELFStringTable str) {
        strtab = str;
    }

    public ELFStringTable getStringTable() {
        return strtab;
    }

    public String getSectionName(int ind) {
        if ( ind < 0 || ind >= entries.length ) return "";
        Entry32 e = entries[ind];
        return e.getName();
    }

}
