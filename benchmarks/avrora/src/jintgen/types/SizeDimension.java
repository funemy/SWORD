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
 * Creation date: Oct 3, 2005
 */

package jintgen.types;

import cck.parser.AbstractToken;
import cck.text.StringUtil;
import cck.util.Util;
import java.util.List;

/**
 * The <code>SizeDimension</code> class represents a size dimension for an integer
 * type in the ISDL code.
 *
 * @author Ben L. Titzer
 */
public class SizeDimension extends TypeCon.Dimension {

    public SizeDimension() {
        super("size");
    }

    public Object build(TypeEnv te, List params) {
        int size;
        int len = params.size();
        if ( len != 1) throw Util.failure("size type dimension expects 1 parameter");
        Object obj = params.get(0);
        if ( obj instanceof AbstractToken )
            size = sizeOf(((AbstractToken)obj).image);
        else if ( obj instanceof String )
            size = sizeOf((String)obj);
        else if ( obj instanceof Integer )
            size = ((Integer)obj);
        else throw Util.failure("size type dimension expects AbstractToken, String or Integer");
        return size;
    }

    private int sizeOf(String str) {
        return StringUtil.evaluateIntegerLiteral(str);
    }
}
