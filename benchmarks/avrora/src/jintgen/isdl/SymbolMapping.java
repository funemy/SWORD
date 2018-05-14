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

package jintgen.isdl;

import cck.text.StringUtil;
import jintgen.isdl.parser.Token;
import java.util.*;

/**
 * The <code>SymbolMapping</code> class represents a class that maps string symbols
 * (names) to an integer. This mapping is used in encoding such things as register
 * names into instructions, for example.
 *
 * @author Ben L. Titzer
 */
public class SymbolMapping {

    public final Token name;
    final HashMap<String, Entry> mapping;
    final List<Entry> list;

    public SymbolMapping(Token n) {
        name = n;
        mapping = new HashMap<String, Entry>();
        list = new LinkedList<Entry>();
    }

    /**
     * The <code>add()</code> method adds a new symbol mapped to a new value to this list of
     * mappings.
     * @param sym the token representing the symbol name
     * @param val the integer value for this symbol
     */
    public void add(Token sym, Token val) {
        String name = sym.image;
        Entry entry = new Entry(sym, val);
        mapping.put(name, entry);
        list.add(entry);
    }

    /**
     * The <code>getEntries()</code> method returns an iterable over the entries in this map.
     * @return an iterable capable of iterating over all of the entries in the map
     */
    public Iterable<Entry> getEntries() {
        return list;
    }

    public Entry get(String name) {
        return mapping.get(name);
    }

    public int size() {
        return list.size();
    }

    public static class Entry {
        public final Token ntoken;
        public final Token vtoken;

        public final String name;
        public final int value;

        Entry(Token n, Token v) {
            ntoken = n;
            vtoken = v;
            name = ntoken.image;
            value = StringUtil.evaluateIntegerLiteral(v.image);
        }
    }
}
