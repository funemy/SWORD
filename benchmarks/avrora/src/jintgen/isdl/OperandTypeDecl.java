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
import cck.util.Util;
import jintgen.isdl.parser.Token;
import jintgen.jigir.CodeRegion;
import jintgen.jigir.JIGIRTypeEnv;
import jintgen.types.*;
import java.util.*;

/**
 * The <code>OperandDecl</code> class represents the declaration of a set of values (or registers) that can
 * serve as an operand to a particular instruction. For example, an operand declaration might be the set of
 * all general purpose registers, or it might be the set of high general purpose registers, or the set of
 * address registers, etc.
 *
 * @author Ben L. Titzer
 */
public abstract class OperandTypeDecl extends Item {

    public final List<AddrModeDecl.Operand> subOperands;
    public final List<AccessMethod> readDecls;
    public final List<AccessMethod> writeDecls;
    public final HashMap<Type, Accessor> readAccessors;
    public final HashMap<Type, Accessor> writeAccessors;

    public static class Accessor {
        public final boolean polymorphic;
        protected SubroutineDecl subroutine;
        public Type type;
        Accessor(boolean p) {
            polymorphic = p;
        }
        public SubroutineDecl getSubroutine() {
            return subroutine;
        }
        public void setSubroutine(SubroutineDecl d) {
            subroutine = d;
        }
    }

    public class AccessMethod extends Accessor {
        public final Token which;
        public final TypeRef typeRef;
        public final CodeRegion code;
        public boolean usedPolymorphically;
        AccessMethod(Token w, TypeRef t, CodeRegion c) {
            super(false);
            typeRef = t;
            which = w;
            code = c;
        }
        public OperandTypeDecl getOperandType() {
            return OperandTypeDecl.this;
        }
    }

    public static class PolymorphicAccessor extends Accessor {
        public final List<AccessMethod> targets;
        public PolymorphicAccessor(Type t) {
            super(true);
            type = t;
            targets = new LinkedList<AccessMethod>();
        }
        public void addTarget(AccessMethod m) {
            targets.add(m);
        }
    }

    protected OperandTypeDecl(Token n) {
        super(n);
        subOperands = new LinkedList<AddrModeDecl.Operand>();
        readDecls = new LinkedList<AccessMethod>();
        writeDecls = new LinkedList<AccessMethod>();
        readAccessors = new HashMap<Type, Accessor>();
        writeAccessors = new HashMap<Type, Accessor>();
    }

    /**
     * The <code>Value</code> class represents an operand to an instruction that is
     * a value such as an immediate or an absolute address.
     */
    public static class Value extends OperandTypeDecl {

        public final int low;
        public final int high;
        public final TypeRef typeRef;
        public final int size;
        final boolean signed;

        public Value(Token n, Token b, TypeRef k, Token l, Token h) {
            super(n);
            typeRef = k;
            size = StringUtil.evaluateIntegerLiteral(b.image);
            if (l == null) {
                low = -1;
                signed = false;
            } else {
                low = StringUtil.evaluateIntegerLiteral(l.image);
                signed = low < 0;
            }
            high = h == null ? -1 : StringUtil.evaluateIntegerLiteral(h.image);
        }

        public boolean isValue() {
            return true;
        }

        public void addSubOperand(AddrModeDecl.Operand o) {
            throw Util.failure("Suboperands are not allowed to Simple operands");
        }

        public boolean isRelative() {
            Type t = typeRef.getType();
            if ( t instanceof JIGIRTypeEnv.TYPE_addr ) {
                return ((JIGIRTypeEnv.TYPE_addr)t).isRelative();                
            }
            return false;
        }

        public boolean isEnum() {
            TypeCon typeCon = typeRef.getType().getTypeCon();
            return typeCon instanceof JIGIRTypeEnv.TYPE_enum;
        }

        public boolean isSigned() {
            return signed;
        }

        public boolean isAddress() {
            return typeRef.isBasedOn("address");
        }
    }

    /**
     * The <code>Compound</code> class represents an operand declaration that consists of
     * multiple sub-operands.
     */
    public static class Compound extends OperandTypeDecl {

        public Compound(Token n) {
            super(n);
        }

        public boolean isCompound() {
            return true;
        }
    }

    /**
     * The <code>Union</code> class represents an operand type that is the union of multiple
     * operand types.
     */
    public static class Union extends OperandTypeDecl {
        public final HashSet<OperandTypeDecl> types;

        public Union(Token n) {
            super(n);
            types = new HashSet<OperandTypeDecl>();
        }

        public boolean isUnion() {
            return true;
        }

        public void addType(OperandTypeDecl d) {
            types.add(d);
        }
    }

    public void addReadDecl(Token r, TypeRef tr, CodeRegion cr) {
        readDecls.add(new AccessMethod(r, tr, cr));
    }

    public void addWriteDecl(Token r, TypeRef tr, CodeRegion cr) {
        writeDecls.add(new AccessMethod(r, tr, cr));
    }

    public boolean isCompound() {
        return false;
    }

    public boolean isValue() {
        return false;
    }

    public boolean isUnion() {
        return false;
    }

    public boolean isEnum() {
        return false;
    }

    public void addSubOperand(AddrModeDecl.Operand o) {
        subOperands.add(o);
    }

    public boolean isRelative() {
        return false;
    }

    public boolean isSigned() {
        return false;
    }

    public boolean isAddress() {
        return false;
    }
}
