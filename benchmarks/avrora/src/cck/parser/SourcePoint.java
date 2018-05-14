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

package cck.parser;

import cck.text.Terminal;

/**
 * The <code>SourcePoint</code> class represents a location within a program for the purposes of tracking
 * error messages and debug information. It encapsulates the module (file) contents, the line, the beginning
 * column and ending column.
 *
 * @author Ben L. Titzer
 */
public class SourcePoint {

    public final String file;
    public final int beginLine;
    public final int endLine;
    public final int beginColumn;
    public final int endColumn;

    public SourcePoint(String m, int l, int bc, int ec) {
        file = (m == null) ? "(unknown)" : m;
        beginLine = l;
        endLine = l;
        beginColumn = bc;
        endColumn = ec;
    }

    public SourcePoint(String m, int l, int el, int bc, int ec) {
        file = (m == null) ? "(unknown)" : m;
        beginLine = l;
        endLine = el;
        beginColumn = bc;
        endColumn = ec;
    }

    public SourcePoint(SourcePoint l, SourcePoint r) {
        file = l.file;
        beginLine = l.beginLine;
        beginColumn = l.beginColumn;
        endColumn = r.endColumn;
        endLine = r.endLine;
    }

    public SourcePoint(AbstractToken l, AbstractToken r) {
        file = l.file;
        beginLine = l.beginLine;
        beginColumn = l.beginColumn;
        endLine = r.endLine;
        endColumn = r.endColumn;
    }

    public String toString() {
        return file + ' ' + beginLine + ':' + beginColumn;
    }

    public String toShortString() {
        return beginLine + ":" + beginColumn;
    }

    public void report() {
        Terminal.print("[");
        Terminal.printBrightBlue(file);
        Terminal.print(" @ ");
        Terminal.printBrightCyan(beginLine + ":" + beginColumn);
        Terminal.print("]");
    }
}
