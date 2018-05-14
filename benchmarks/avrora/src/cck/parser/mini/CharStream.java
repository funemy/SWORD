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
 * Creation date: Jan 12, 2006
 */

package cck.parser.mini;

import cck.util.Util;

/**
 * The <code>CharStream</code> class represents a character stream that
 * supplies character to a parser. The stream records the position (in terms
 * of line / column) and allows peeking ahead in the stream.
 *
 * @author Ben L. Titzer
 */
public class CharStream {

    public static class Position {
        public int line;
        public int column;
    }


    public char consume() {
        throw Util.unimplemented();
    }

    public Position getPosition() {
        throw Util.unimplemented();
    }

    public Position getPosition(int relative) {
        throw Util.unimplemented();
    }

    public Position getPosition(Position dest) {
        throw Util.unimplemented();
    }

    public Position getPosition(Position dest, int relative) {
        throw Util.unimplemented();
    }

    public char peek(int relative) {
        throw Util.unimplemented();
    }

    public void peek(char[] ch, int relative, int len) {
        throw Util.unimplemented();
    }
}
