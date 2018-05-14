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
 * Created Sep 23, 2005
 */
package cck.parser;

import cck.text.Terminal;

/**
 * The <code>SourceError</code> class represents an error that occurs in a source file,
 * such as a verification error (in the case of jintgen), a compilation error (in the
 * case of a compiler), or an assembler error. A <code>SourceError</code> occurs
 * in a named source file at a particular location (line number and column number).
 *
 * @author Ben L. Titzer
 */
public class SourceException extends SourceError {

    /**
     * The <code>trace</code> field stores a reference to the stack trace corresponding
     * to where the source exception ocurred.
     */
    public final StackTrace trace;

    /**
     * The default constructor for a source error accepts an error type, a program
     * point which indicates the location in a file where the error occured, a message,
     * and a list of parameters to the error (such as the name of a class or method
     * where the error occurred).
     *
     * @param type a string that indicates the type of error that occured such as
     *             "Undefined Variable"
     * @param p    the point in the file where the error occurred
     * @param msg  a short message reported to the user that explains the error
     * @param ps   a list of parameters to the error such as the name of the variable
     *             that is undeclared, etc.
     */
    public SourceException(String type, StackTrace p, String msg, String[] ps) {
        super(type, p == null ? null : p.getSourcePoint(), msg, null);
        trace = p;
    }

    /**
     * The <code>report()</code> method generates a textual report of this error
     * for the user. For source errors, this method will report the file, line number,
     * and column number where this error occurred.
     */
    public void report() {
        Terminal.print("");
        Terminal.printRed(errorType);
        Terminal.println(": " + message + ' ');
        for (StackTrace tr = trace; tr != null; tr = tr.prev) {
            Terminal.print("\t");
            Terminal.print("in ");
            Terminal.printGreen(tr.getMethod() + ' ');
            SourcePoint p = tr.getSourcePoint();
            if (p != null) p.report();
            Terminal.nextln();
        }
    }

}
