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

import cck.text.StringUtil;

/**
 * The <code>ErrorReporter</code> class is used to generate errors related to source files
 * and contains a number of utility methods that allow error messages to be reported with
 * fewer lines of code.
 *
 * @author Ben L. Titzer
 */
public class ErrorReporter {
    public void error(String type, SourcePoint p, String report) {
        throw new SourceError(type, p, report, StringUtil.EMPTY_STRING_ARRAY);
    }

    public void error(String type, SourcePoint p, String report, String p1) {
        String[] ps = {p1};
        throw new SourceError(type, p, report, ps);
    }

    public void error(String type, SourcePoint p, String report, String p1, String p2) {
        String[] ps = {p1, p2};
        throw new SourceError(type, p, report, ps);
    }

    public void error(String type, SourcePoint p, String report, String p1, String p2, String p3) {
        String[] ps = {p1, p2, p3};
        throw new SourceError(type, p, report, ps);
    }

    public void redefined(String type, String thing, AbstractToken prevdecl, AbstractToken newdecl) {
        type = "Redefined" + type;
        String report = thing + ' ' + StringUtil.quote(prevdecl.image) + " previously defined at " + pos(prevdecl);
        error(type, newdecl.getSourcePoint(), report, prevdecl.image);
    }

    public void unresolved(String type, String thing, AbstractToken where) {
        type = "Unresolved" + type;
        String report = "Unresolved " + thing + ' ' + StringUtil.quote(where.image);
        error(type, where.getSourcePoint(), report, where.image);
    }

    protected String pos(AbstractToken t) {
        return t.beginLine + ":" + t.beginColumn;
    }
}
