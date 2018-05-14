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
 * The <code>ELFSymbolTable</code> class represents a symbol table within
 * an ELF file. The symbol table is used to locate functions and variables
 * for relocation and debugging purposes.
 *
 * @author Ben L. Titzer
 */
public class ELFSymbolTable {

    public static final int STT_NOTYPE  = 0;
    public static final int STT_OBJECT  = 1;
    public static final int STT_FUNC    = 2;
    public static final int STT_SECTION = 3;
    public static final int STT_FILE    = 4;
    public static final int STT_LOPROC  = 13;
    public static final int STT_HIPROC  = 15;

    public static final int STB_LOCAL = 0;
    public static final int STB_GLOBAL = 1;
    public static final int STB_WEAK = 2;
    public static final int STB_LOPROC = 13;
    public static final int STB_HIPROC = 15;

    public class Entry {
        public int st_name;
        public int st_value;
        public int st_size;
        public int st_info;
        public int st_other;
        public short st_shndx;

        public String getBinding() {
            switch ( (st_info >> 4) & 0xf ) {
                case STB_LOCAL: return "LOCAL";
                case STB_GLOBAL: return "GLOBAL";
                case STB_WEAK: return "WEAK";
                default: return "unknown";
            }
        }

        public String getType() {
            switch ( st_info & 0xf ) {
                case STT_NOTYPE: return "n";
                case STT_OBJECT: return "object";
                case STT_FUNC: return "func";
                case STT_SECTION: return "section";
                case STT_FILE: return "file";
                default: return "unknown";
            }
        }

        public boolean isFunction() {
            return (st_info & 0xf) == STT_FUNC;
        }

        public boolean isObject() {
            return (st_info & 0xf) == STT_OBJECT;
        }

        public String getName() {
            if ( strtab != null ) return strtab.getString(st_name);
            return "";
        }

    }

    public final ELFHeader header;
    public final ELFSectionHeaderTable.Entry32 entry;
    public final Entry[] entries;
    protected ELFStringTable strtab;

    /**
     * The constructor for the <code>ELFSymbolTable</code> class creates a new
     * symbol table with the specified ELF header from the specified ELF section
     * header table entry.
     * @param header the header of the ELF file
     * @param entry the entry in the section header table corresponding to this
     * symbol table
     */
    public ELFSymbolTable(ELFHeader header, ELFSectionHeaderTable.Entry32 entry) {
        this.header = header;
        this.entry = entry;
        entries = new Entry[entry.sh_size / entry.sh_entsize];
    }

    /**
     * The <code>read()</code> method reads this symbol table from the specified random
     * access file. The file is first advanced to the appropriate position with the
     * <code>seek()</code> method and then the entries are loaded.
     * @param f the random access file from which to read the symbol table
     * @throws IOException if there is a problem reading from the file
     */
    public void read(RandomAccessFile f) throws IOException {
        // seek to the beginning of the section
        f.seek(entry.sh_offset);
        // create the elf data input stream
        ELFDataInputStream is = new ELFDataInputStream(header, f);
        // read each of the entries
        for ( int cntr = 0; cntr < entries.length; cntr++ ) {
            Entry e = new Entry();
            e.st_name = is.read_Elf32_Word();
            e.st_value = is.read_Elf32_Addr();
            e.st_size = is.read_Elf32_Word();
            e.st_info = is.read_Elf32_uchar();
            e.st_other = is.read_Elf32_uchar();
            e.st_shndx = is.read_Elf32_Half();
            entries[cntr] = e;
            for ( int pad = 16; pad < entry.sh_entsize; pad++ ) f.read();
        }
    }

    public void setStringTable(ELFStringTable str) {
        strtab = str;
    }

    public ELFStringTable getStringTable() {
        return strtab;
    }
}
