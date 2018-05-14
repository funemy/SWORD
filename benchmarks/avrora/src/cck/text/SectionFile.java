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

package cck.text;

import java.io.*;

/**
 * The <code>SectionFile</code> class represents a file that can be used to for source code generation, etc,
 * where a template file has a section of text that needs to be generated, and the rest of the file is not
 * altered. This is accomplished with tags in the file that specify the beginning and end of the section to be
 * filled in.
 * <p/>
 * The <code>SectionFile</code> then behaves just like a FileOutputStream, except on the first write it will
 * skip to the beginning of the section where the output should be inserted. Then on close, the output stream
 * will write the section following the end, remove the old file, and rename the new file to the old file.
 *
 * @author Ben L. Titzer
 */
public class SectionFile extends FileOutputStream {

    private final BufferedReader template;

    private final String file_name;
    private final String start_delimiter;
    private final String end_delimiter;

    private boolean header_done;

    public SectionFile(String fname, String sect) throws FileNotFoundException {
        super(new File(fname + ".new"));
        file_name = fname;
        template = new BufferedReader(new FileReader(fname));
        start_delimiter = "//--BEGIN " + sect + "--";
        end_delimiter = "//--END " + sect + "--";
    }

    public void write(byte[] b) throws IOException {
        readHeader();
        super.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        readHeader();
        super.write(b, off, len);

    }

    public void write(int b) throws IOException {
        readHeader();
        super.write(b);
    }

    public void writeLine(String s) throws IOException {
        readHeader();
        super.write(s.getBytes());
        super.write('\n');
    }

    public void close() throws IOException {
        readHeader();
        discardSection();
        readFooter();

        template.close();
        super.close();

        File old = new File(file_name);
        old.delete();
        new File(file_name + ".new").renameTo(old);
    }

    private void readHeader() throws IOException {
        if (header_done) return;
        while (true) {
            String line = template.readLine();
            if (line == null) throw new IOException("no section delimiter found");
            super.write(line.getBytes());
            super.write('\n');
            if (line.equals(start_delimiter)) break;
        }
        header_done = true;
    }

    private void discardSection() throws IOException {
        while (true) {
            String line = template.readLine();
            if (line == null) break;
            if (line.equals(end_delimiter)) {
                super.write(line.getBytes());
                super.write('\n');
                break;
            }
        }

    }

    private void readFooter() throws IOException {
        while (true) {
            String line = template.readLine();
            if (line == null) break;
            super.write(line.getBytes());
            super.write('\n');
        }
    }
}
