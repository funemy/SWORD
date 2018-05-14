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
 * @author Ben L. Titzer
 */
public class ELFDataInputStream {

    final boolean bigEndian;
    final ELFHeader header;
    final RandomAccessFile file;

    public ELFDataInputStream(ELFHeader header, RandomAccessFile f) {
        this.header = header;
        bigEndian = header.isBigEndian();
        file = f;
    }

    public byte[] read_section(int off, int length) throws IOException {
        byte[] buffer = new byte[length];
        file.seek(off);
        for ( int cntr = 0; cntr < length; ) {
            cntr += file.read(buffer, cntr, length - cntr);
        }
        return buffer;
    }

    public byte read_Elf32_byte() throws IOException {
        return (byte)read_1();
    }

    public int read_Elf32_uchar() throws IOException {
        return read_1();
    }

    public int read_Elf32_Addr() throws IOException {
        return read_4();
    }

    public short read_Elf32_Half() throws IOException {
        return (short)read_2();
    }

    public int read_Elf32_Off() throws IOException {
        return read_4();
    }

    public int read_Elf32_SWord() throws IOException {
        return read_4();
    }

    public int read_Elf32_Word() throws IOException {
        return read_4();
    }

    private int read_1() throws IOException {
        return file.read() & 0xff;
    }

    private int read_2() throws IOException {
        int b1 = read_1();
        int b2 = read_1();
        if ( bigEndian ) return asShort(b2, b1);
        return asShort(b1, b2);
    }

    private int read_4() throws IOException {
        int b1 = read_1();
        int b2 = read_1();
        int b3 = read_1();
        int b4 = read_1();
        if ( bigEndian ) return asInt(b4, b3, b2, b1);
        return asInt(b1, b2, b3, b4);
    }

    private short asShort(int bl, int bh) {
        return (short)((bh << 8) | bl);
    }

    private int asInt(int b1, int b2, int b3, int b4) {
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

}
