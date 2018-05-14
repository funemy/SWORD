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

import java.io.PrintStream;

public class Printer {

    private final PrintStream o;
    private boolean begLine;
    private int listdepth;
    private boolean first;
    private boolean nlcomma;
    private int indent;

    public static final Printer STDOUT = new Printer(System.out);
    public static final Printer STDERR = new Printer(System.out);

    public Printer(PrintStream o) {
        this.o = o;
        this.begLine = true;
    }

    public void println(String s) {
        spaces();
        if (listdepth > 0) {
            if (!first) o.print(", ");
        }
        first = false;
        o.println(s);
        begLine = true;
        first = false;
    }

    public void print(String s) {
        spaces();
        if (listdepth > 0) {
            if (!first) o.print(", ");
        }
        first = false;
        o.print(s);
    }

    public void nextln() {
        if (!begLine) {
            o.print("\n");
            begLine = true;
        }
    }

    public void indent() {
        indent++;
    }

    public void spaces() {
        if (begLine) {
            for (int cntr = 0; cntr < indent; cntr++)
                o.print("    ");
            begLine = false;
        }
    }

    public void unindent() {
        indent--;
        if (indent < 0) indent = 0;
    }

    public void startblock() {
        println("{");
        indent();
    }

    public void startblock(String name) {
        println(name + " {");
        indent();
    }

    public void endblock() {
        unindent();
        println("}");
    }

    public void endblock(String s) {
        unindent();
        println('}' + s);
    }

    public void close() {
        o.close();
    }

    public void beginList(String beg) {
        print(beg);
        listdepth++;
        first = true;
    }

    public void beginList() {
        listdepth++;
        first = true;
    }

    public void endList(String end) {
        listdepth--;
        if (listdepth < 0) listdepth = 0;
        print(end);
    }

    public void endListln(String end) {
        listdepth--;
        if (listdepth < 0) listdepth = 0;
        println(end);
    }

    public void endList() {
        listdepth--;
        if (listdepth < 0) listdepth = 0;
    }

    public void endListln() {
        listdepth--;
        if (listdepth < 0) listdepth = 0;
        nextln();
    }
}
