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
import java.util.HashMap;

/**
 * The <code>ELFStringTable</code> class represents a string table that is
 * present in the ELF file. A string table section contains a sequence of
 * null-terminated strings and can be indexed by integers to obtain the
 * string corresponding to the sequence of characters starting at the
 * specified index up to (and not including) the next null character.
 *
 * @author Ben L. Titzer
 */
public class ELFStringTable {

    protected final HashMap map;
    protected final byte[] data;

    protected final ELFSectionHeaderTable.Entry32 entry;

    /**
     * The constructor for the <code>ELFStringTable</code> class creates a new
     * string table with the specified number of bytes reserved for its internal
     * string storage
     * @param header the ELF header of the file
     * @param entry the section header table entry corresponding to this string
     * table
     */
    public ELFStringTable(ELFHeader header, ELFSectionHeaderTable.Entry32 entry) {
        data = new byte[entry.sh_size];
        map = new HashMap();
        this.entry = entry;
    }

    /**
     * The <code>read()</code> method reads this string table from the specified
     * file, beginning at the specified offset and continuining for the length
     * of the section.
     * @param f the random access file to read the data from
     * @throws IOException if there is a problem reading the file
     */
    public void read(RandomAccessFile f) throws IOException {
        if ( data.length == 0 ) return;
        f.seek(entry.sh_offset);
        for ( int read = 0; read < data.length; ) {
            read += f.read(data, read, data.length - read);
        }
    }

    /**
     * The <code>getString()</code> method gets a string in this section corresponding
     * to the specified index. Since Java strings are not null-terminated as the
     * ones in this section are, this method employs a hash table to remember frequently
     * accessed strings.
     * @param ind the index into the section for which to retrieve the string
     * @return a string corresponding to reading bytes at the specified index up to
     * and not including the next null character
     */
    public String getString(int ind) {
        if ( ind >= data.length ) return "";
        String str = (String)map.get(new Integer(ind));
        if ( str == null ) {
            StringBuffer buf = new StringBuffer();
            for ( int pos = ind; pos < data.length; pos++ ) {
                byte b = data[pos];
                if ( b == 0 ) break;
                buf.append((char)b);
            }
            str = buf.toString();
            map.put(new Integer(ind), str);
        }
        return str;
    }
}
