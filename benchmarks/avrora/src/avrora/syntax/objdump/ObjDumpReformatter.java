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

package avrora.syntax.objdump;

import cck.text.StringUtil;
import cck.util.Util;
import java.io.*;
import java.util.*;

/**
 * @author Ben L. Titzer
 */
public class ObjDumpReformatter {

    HashSet sections;
    List sectlist;

    public ObjDumpReformatter(List slist) {
        sections = new HashSet();
        Iterator i = slist.iterator();
        while (i.hasNext()) {
            sections.add(i.next());
        }
        sectlist = slist;
    }

    public StringBuffer cleanCode(String inFile) throws IOException {
        try {
            //Status.begin("Preprocessing");
            StringBuffer out = new StringBuffer(200000);
            BufferedReader in = new BufferedReader(new FileReader(inFile));
            cleanFile(in, out);
            //Status.success();
            return out;
        } catch (IOException e) {
            // rethrow IO exceptions (e.g. file not found)
            //Status.error(e);
            throw e;
        } catch (Throwable e) {
            //Status.error(e);
            throw Util.unexpected(e);
        }
    }

    private void cleanFile(BufferedReader in, StringBuffer out) throws IOException {

        line_count = 0;
        String line = nextLine(in);

        //clean up first section
        line = readHeader(in, out, line);

        while (line != null) {
            String section = getSectionName(line);
            if (section != null) {
                // read the whole section
                line = readSection(in, out, section);
            } else {
                // ignore this line if it is between sections
                line = nextLine(in);
            }
        }
    }

    private String getSectionName(String line) {
        int offset = line.indexOf("Disassembly of section");
        if (offset != -1) {
            return line.substring(line.indexOf('.'), line.indexOf(':'));
        }
        return null;
    }

    private String readHeader(BufferedReader in, StringBuffer out, String line) throws IOException {
        while (line != null) {
            if (line.indexOf("Disassembly of section") != -1) {
                break;
            }
            if (line.indexOf("main.exe") != -1) out.append("program \"main.exe\":\n\n");

            Iterator i = sectlist.iterator();
            while (i.hasNext()) {
                String s = (String)i.next();
                if (line.indexOf(s) != -1) printSectionHeader(s, out, line);
            }

            line = nextLine(in);
        }
        return line;
    }

    private void printSectionHeader(String section, StringBuffer out, String line) {
        append(out, "  section ", section, " ");
        StringTokenizer st = new StringTokenizer(line);
        st.nextToken(); // 0
        st.nextToken(); //.text
        append(out, " size=0x", st.nextToken());
        append(out, " vma=0x", st.nextToken());
        append(out, " lma=0x", st.nextToken());
        append(out, " offset=0x", st.nextToken());
        append(out, " ;", st.nextToken());
        out.append(" \n");
    }

    private static void append(StringBuffer out, Object a, Object b) {
        out.append(a);
        out.append(b);
    }

    private static void append(StringBuffer out, Object a, Object b, Object c) {
        out.append(a);
        out.append(b);
        out.append(c);
    }

    private String readSection(BufferedReader in, StringBuffer out, String section) throws IOException {

        if (sections.contains(section)) return convertSection(in, out, section);
        else return ignoreSection(in, out, section);
    }

    private String ignoreSection(BufferedReader in, StringBuffer out, String section) throws IOException {
        append(out, "; section ", section, " removed");
        String line = nextLine(in);
        while (line != null) {
            append(out, "; ", line, "\n");
            if (getSectionName(line) != null) return line;
            line = nextLine(in);
        }
        return line;
    }

    private String convertSection(BufferedReader in, StringBuffer out, String section) throws IOException {
        // add the start of the section name
        append(out, "\nstart ", section, ":\n");

        // read the next line
        String line = nextLine(in);

        while (line != null) {

            // beginning of new section
            if (getSectionName(line) != null) return line;

            // ignore "..." in output
            if (line.indexOf("...") != -1) {
                line = nextLine(in);
                out.append("; ...");
            }

            if (line.indexOf("Address ") != -1) {
                line = line.substring(0, line.indexOf("Address "));
                line += nextLine(in);
            }

            if (isLabel(line)) {
                out.append("label 0x");
                StringTokenizer st = new StringTokenizer(line);
                out.append(st.nextToken());
                String name = st.nextToken().replaceAll("[<,>]", "\"");
                append(out, "  ", name, "\n");
            } else {

                String tok;
                StringTokenizer st = new StringTokenizer(line);
                if (st.hasMoreTokens()) {
                    tok = st.nextToken();
                    out.append(StringUtil.rightJustify("0x" + tok, 10));
                    while (st.hasMoreTokens()) {
                        tok = st.nextToken();

                        if (tok.matches("\\p{XDigit}\\p{XDigit}")) append(out, " 0x", tok);
                        else if (tok.charAt(0) == '<') append(out, "; ", tok);
                            // workaround for objdump 2.16.1 bug
                        else if (tok.startsWith("0x0x")) append(out, " ", tok.substring(2, tok.length()));
                        else append(out, " ", tok);

                    }
                    out.append('\n');
                }
            }
            line = nextLine(in);
        }
        return line;
    }

    int line_count;

    private String nextLine(BufferedReader in) throws IOException {
        line_count++;
        return in.readLine();
    }

    /**
     * @param s
     * @return true if statement is of the form: <hexdig> <\<LABEL\>:>
     */
    private boolean isLabel(String s) {
        return s.indexOf('<') != -1 && s.indexOf(">:") != -1;
    }
}
