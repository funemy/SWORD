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
 * Creation date: Nov 3, 2005
 */

package jintgen.isdl.verifier;

import cck.util.Util;
import cck.text.StringUtil;
import jintgen.isdl.*;
import jintgen.jigir.*;
import java.util.HashMap;
import java.util.List;

/**
 * @author Ben L. Titzer
 */
public class EncodingVerifier extends VerifierPass {

    public EncodingVerifier(ArchDecl arch) {
        super(arch);
    }

    public void verify() {
        uniqueCheck("Format", "Encoding format", arch.formats);
        verifyAddrModes();
        verifyInstructions();
    }

    private void verifyAddrModes() {
        for ( AddrModeDecl am : arch.addrModes ) {
            verifyEncodings(am.encodings, am);
        }
    }

    private void verifyInstructions() {
        for ( InstrDecl id : arch.instructions ) {
            // verify the encodings declared in this local addressing mode
            if ( id.addrMode.localDecl != null ) {
                verifyEncodings(id.addrMode.localDecl.encodings, id.addrMode.localDecl);
            }
        }
    }

    private void verifyEncodings(Iterable<FormatDecl> el, AddrModeDecl am) {
        // for each of the declared encodings, find the parent and verify the size
        for ( FormatDecl encoding : el ) {
            verifyEncoding(encoding, am);
        }
    }

    private void verifyEncoding(FormatDecl ed, AddrModeDecl am) {
        if (ed instanceof FormatDecl.Derived) {
            FormatDecl.Derived dd = (FormatDecl.Derived)ed;
            FormatDecl parent = arch.getEncoding(dd.pname.image);
            if ( parent == null )
                ERROR.UnresolvedFormat(dd.pname);
            dd.setParent(parent);
        }

        int encodingSize = computeEncodingSize(ed, am);
        if (encodingSize <= 0 || encodingSize % 16 != 0)
            throw Util.failure("encoding not word aligned: " + ed.name + " is " + encodingSize + " bits, at:  "
                    +ed.name.beginLine+ ':' +ed.name.beginColumn);
    }

    private int computeEncodingSize(FormatDecl encoding, AddrModeDecl am) {
        BitWidthComputer bwc = new BitWidthComputer(am);
        int size = 0;
        List<FormatDecl.BitField> fields;
        if ( encoding instanceof FormatDecl.Derived ) {
            FormatDecl.Derived ed = (FormatDecl.Derived)encoding;
            for ( FormatDecl.Substitution s : ed.subst ) {
                bwc.addSubstitution(s.name.image, s.expr);
            }
            fields = ed.parent.fields;
        } else {
            fields = encoding.fields;
        }
        for ( FormatDecl.BitField e : fields ) {
            e.field.accept(bwc);
            e.width = bwc.width;
            size += bwc.width;
        }
        encoding.bitWidth = size;
        return size;
    }

    class BitWidthComputer extends CodeVisitor.Default {

        int width = -1;
        HashMap<String, Integer> operandWidthMap;
        HashMap<String, Expr> substMap;

        BitWidthComputer(AddrModeDecl d) {
            substMap = new HashMap<String, Expr>();
            operandWidthMap = new HashMap<String, Integer>();
            for ( AddrModeDecl.Operand o : d.operands ) {
                addSubOperands(o, o.name.image);
            }
        }

        void addSubstitution(String str, Expr e) {
            substMap.put(str, e);
        }

        void addSubOperands(AddrModeDecl.Operand op, String prefix) {
            OperandTypeDecl ot = op.getOperandType();
            if ( ot == null ) ERROR.UnresolvedOperandType(op.typeRef.getToken());
            if ( ot.isCompound() ) {
                OperandTypeDecl.Compound cd = (OperandTypeDecl.Compound)ot;
                for ( AddrModeDecl.Operand o : cd.subOperands )
                    addSubOperands(o, prefix+ '.' +o.name.image);
            } else if ( ot.isValue() ) {
                OperandTypeDecl.Value sd = (OperandTypeDecl.Value)ot;
                operandWidthMap.put(prefix, sd.size);
            } else if ( ot.isUnion() ) {
                // do nothing
            }
        }

        public void visit(FixedRangeExpr e) {
            int diff = (e.high_bit - e.low_bit);
            if (diff < 0) diff = -diff;
            width = diff + 1;
        }

        public void visit(IndexExpr e) {
            // TODO: this is not necessarily true for a map index expr
            width = 1;
        }

        public void visit(Literal.IntExpr e) {
            if (isBinary(e)) width = e.token.image.length() - 2;
            else if (isHex(e)) width = (e.token.image.length() - 2) * 4;
            else width = TypeChecker.getTypeOfLiteral(typeEnv, e.value).getSize();
        }

        private boolean isHex(Literal.IntExpr e) {
            return StringUtil.isHex(e.token.image);
        }

        private boolean isBinary(Literal.IntExpr e) {
            char ch = e.token.image.charAt(1);
            return ch == 'b' || ch == 'B';
        }

        public void visit(Literal.BoolExpr e) {
            width = 1;
        }

        public void visit(VarExpr e) {
            Expr se = substMap.get(e.variable.image);
            if ( se != null ) {
                se.accept(this);
                return;
            }

            Integer i = operandWidthMap.get(e.variable.image);

            if ( i != null ) width = (i);
            else ERROR.CannotComputeSizeOfVariable(e.variable);
        }

        public void visit(DotExpr e) {
            String str = e.expr +"."+e.field;
            Integer i = operandWidthMap.get(str);

            if ( i != null ) width = (i);
            else ERROR.CannotComputeSizeOfVariable(e.field);
        }

        public void error(Expr e) {
            ERROR.CannotComputeSizeOfExpression(e);
        }
    }

}
