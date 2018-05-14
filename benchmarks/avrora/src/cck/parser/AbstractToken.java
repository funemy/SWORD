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

/**
 * This class is used to unify the Token classes from all JavaCC-generated parsers.
 *
 * @author Ben L. Titzer
 */
public abstract class AbstractToken {
    /**
     * beginLine and beginColumn describe the position of the first character of this token; endLine and
     * endColumn describe the position of the last character of this token.
     */
    public int beginLine, beginColumn, endLine, endColumn;

    /**
     * The string image of the token.
     */
    public String image;

    /**
     * The file in which the token originated.
     */
    public String file;

    /**
     * Returns the image.
     */
    public String toString() {
        return image;
    }

    public abstract AbstractToken getNextToken();

    public SourcePoint getSourcePoint() {
        return new SourcePoint(file, beginLine, endLine, beginColumn, endColumn);
    }

    public SourcePoint asSourcePoint(String f) {
        return new SourcePoint(f, beginLine, endLine, beginColumn, endColumn);
    }

    public static AbstractToken newToken(String img, SourcePoint pt) {
        AbstractToken t = new AbstractToken() {
            public AbstractToken getNextToken() { return null; }
        };
        t.image = img;
        if ( pt != null ) {
            t.file = pt.file;
            t.beginLine = pt.beginLine;
            t.beginColumn = pt.beginColumn;
            t.endLine = pt.endLine;
            t.endColumn = pt.endColumn;
        }
        return t;
    }
}
