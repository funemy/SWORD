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
 * Created Sep 20, 2005
 */
package jintgen.types;

import java.util.HashMap;
import java.util.Map;

/**
 * The <code>Type</code> class represents a type within the IR representation
 * of interpreter and operand code. A type is either an enum (which has an
 * underlying integral type) or an integral type with a size.
 *
 * @author Ben L. Titzer
 */
public class Type {

    protected final TypeCon typeCon;
    protected final HashMap<String, Object> dimensions;

    public Type(TypeCon tc, HashMap<String, Object> dims) {
        typeCon = tc;
        dimensions = dims;
    }

    /**
     * The <code>isAssignableFrom()</code> method is the main mechanism in type checking. For
     * each variable, parameter, or field that has a declared type, when an assignment occurs,
     * this method is called with the computed type of the expression and the declared type
     * to check that the assignment is allowed.
     * @param other the type of the expression being assigned
     * @return true if this type can be assigned from the specified type; false otherwise
     */
    public boolean isAssignableFrom(Type other) {
        return this.typeCon.isAssignableFrom(other.typeCon);
    }

    /**
     * The <code>isComparableTo()</code> method checks whether this type can be compared to
     * values of the specified type.
     * @param other the other type that this type is being compared to
     * @return true if this type can be compared to the specified type; false otherwise
     */
    public boolean isComparableTo(Type other) {
        return this.typeCon.isComparableTo(other.typeCon);
    }

    /**
     * The <code>toString()</code> method returns a string representation of this type, including
     * whether it is signed (or unsigned) and its width in bits.
     * @return a string representation of this type
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(typeCon.getName());
        boolean first = true;
        if (!dimensions.isEmpty() ) {
            buf.append('(');
            for ( Map.Entry<String, Object> e : dimensions.entrySet() ) {
                if ( !first ) buf.append(", ");
                buf.append(e.getKey());
                buf.append(": ");
                buf.append(e.getValue());
                first = false;
            }
            buf.append(')');
        }
        return buf.toString();
    }

    public TypeCon getTypeCon() {
        return typeCon;
    }

    /**
     * The <code>isBasedOn()</code> method checks whether this type matches the specified base type name.
     * @param s the name of the base type to check against
     * @return true if this type is based on the specified base type; false otherwise
     */
    public boolean isBasedOn(String s) {
        return typeCon.name.equals(s);
    }

    public Object getDimension(String n) {
        return dimensions.get(n);
    }
}
