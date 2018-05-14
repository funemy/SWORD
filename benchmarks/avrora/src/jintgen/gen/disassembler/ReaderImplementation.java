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
 */

package jintgen.gen.disassembler;

import cck.text.StringUtil;
import cck.util.Util;
import cck.util.Arithmetic;
import jintgen.gen.GenBase;
import jintgen.isdl.*;
import jintgen.jigir.*;
import java.util.*;

/**
 * The <code>ReaderImplementation</code> class implements the functionality needed to generate
 * the code that reads various bit patterns from the instruction stream, as well as generate
 * classes in the disassembler that can read the operands and package them into an array.
 *
 * @author Ben L. Titzer
 */
class ReaderImplementation extends GenBase {

    HashMap<FormatDecl, EncodingReader> encodingInfo = new HashMap<FormatDecl, EncodingReader>();
    HashMap<String, EncodingReader> encodingRev = new HashMap<String, EncodingReader>();
    HashMap<String, ReadMethod> operandDecodeMethods = new HashMap<String, ReadMethod>();

    int maxoperands;
    int readMethods;
    DisassemblerGenerator dGen;

    public ReaderImplementation(DisassemblerGenerator dGen) {
        super(dGen.properties);
        this.dGen = dGen;
        setPrinter(dGen.p);
    }

    class ReadMethod {
        final int number;
        final int[] decode_bits;
        final List<Field> fields;

        ReadMethod(int[] decode) {
            number = readMethods++;
            decode_bits = decode;
            fields = computeFieldList(decode);
        }

        private List<Field> computeFieldList(int[] decode) {
            List<Field> list = new LinkedList<Field>();
            Field f = null;
            for ( int bit = 0; bit < decode.length; bit++ ) {
                if ( f == null || startNewField(f, decode, bit) ) {
                    f = new Field();
                    f.low_bit = decode[bit];
                    f.length = 1;
                    list.add(f);
                } else {
                    f.length++;
                }
            }
            return list;
        }

        private boolean startNewField(Field f, int[] decode, int bit) {
            boolean startNewWord = (decode[bit] % DisassemblerGenerator.WORD_SIZE == 0);
            boolean notContiguous = (decode[bit] != f.low_bit + f.length);
            return startNewWord || notContiguous;
        }

        public String toString() {
            return "readop_"+number+"(d)";
        }

        void generate() {
            startblock("static int readop_$1($disassembler d)", number);
            if (fields.isEmpty() ) {
                println("return 0;");
            } else {
                int offset = 0;
                for ( Field f : fields ) {
                    if ( offset == 0 ) print("int result = ");
                    else print("result |= ");
                    generateRead(f.low_bit, f.length);
                    if ( offset != 0 ) print(" << "+offset);
                    offset += f.length;
                    println(";");
                }
                println("return result;");
            }
            endblock();
        }

        private void generateRead(int logical_start, int length) {
            int wsize = DisassemblerGenerator.WORD_SIZE;
            int word = logical_start / wsize;

            if ( logical_start == 0 && length == wsize ) {
                print("d.word"+word);
            } else {
                int low_bit = DisassemblerGenerator.nativeBitOrder(logical_start, length);
                int off = low_bit % wsize;
                int mask = Arithmetic.getBitRangeMask(0, length - 1);
                String mstr = StringUtil.to0xHex(mask, wsize / 4);
                if ( off == 0 )
                    print("(d.word$1 & $2)", word, mstr);
                else
                    print("((d.word$1 >>> $2) & $3)", word, off, mstr);
            }
        }
    }

    class Field {
        int low_bit;
        int length;
    }

    void generateReads() {
        for ( ReadMethod m : operandDecodeMethods.values() ) m.generate();
    }

    void addEncoding(String eName, InstrDecl instr, FormatDecl ed, AddrModeDecl am) {
        int no = am.operands.size();
        if ( no > maxoperands ) maxoperands = no;
        if ( !encodingInfo.containsKey(ed) ) {
            EncodingReader er = new EncodingReader(eName, instr, ed, am);
            encodingInfo.put(ed, er);
            encodingRev.put(eName, er);
            dGen.numEncodings++;
        }
        int bitWidth = ed.getBitWidth();
        if ( bitWidth < dGen.minInstrLength )
            dGen.minInstrLength = bitWidth;
        if ( bitWidth > dGen.maxInstrLength )
            dGen.maxInstrLength = bitWidth;
    }

    String getName(FormatDecl ed) {
        return encodingInfo.get(ed).name;
    }

    ReadMethod getReadMethod(int[] d) {
        String dc = getDecoderString(d);
        ReadMethod rm = operandDecodeMethods.get(dc);
        if ( rm == null ) {
            rm = new ReadMethod(d);
            operandDecodeMethods.put(dc, rm);
        }
        return rm;
    }

    String getDecoderString(int[] decoder) {
        StringBuffer buf = new StringBuffer();
        for ( int i : decoder ) {
            buf.append(i);
            buf.append('.');
        }
        return buf.toString();
    }

    class EncodingReader {
        final String name;
        final InstrDecl instr;
        final FormatDecl decl;
        final AddrModeDecl addrMode;
        final HashMap<String, String> operandDecodeString;

        EncodingReader(String n, InstrDecl i, FormatDecl ed, AddrModeDecl addr) {
            name = n;
            instr = i;
            decl = ed;
            addrMode = addr;
            operandDecodeString = new HashMap<String, String>();
        }

        void computeDecoders() {
            computeDecoders("", addrMode.operands);
        }

        void computeDecoders(String prefix, Iterable<AddrModeDecl.Operand> operands) {
            List<FormatDecl.BitField> nl = DGUtil.reduceEncoding(decl, null, addrMode);
            for ( AddrModeDecl.Operand o : operands ) {
                OperandTypeDecl ot = dGen.arch.getOperandDecl(o.typeRef.getTypeConName());
                FormatDecl.Cond cond = decl.getCond();
                String opname = prefix+o.name.image;
                String et = getEnumType(ot);
                if ( cond != null && cond.name.image.equals(opname) )
                    computeConditional(opname, et, cond);
                else if ( ot.isValue() )
                    computeValue(opname, ot, nl, et);
                else if ( ot.isCompound() ) {
                    computeDecoders(prefix+opname+ '.', ot.subOperands);
                }
            }
        }

        private void computeValue(String opname, OperandTypeDecl ot, List<FormatDecl.BitField> nl, String et) {
            OperandTypeDecl.Value vt = (OperandTypeDecl.Value)ot;
            int[] decoder = computeScatter(opname, vt, nl);
            ReadMethod rm = getReadMethod(decoder);
            if ( et != null )
                operandDecodeString.put(opname, tr("$1_table[$2]", et, rm));
            else if (ot.isRelative() ) {
                operandDecodeString.put(opname, "d.pc, "+getReadExpr(rm, vt));
            } else {
                operandDecodeString.put(opname, getReadExpr(rm, vt));
            }
        }

        private void computeConditional(String opname, String et, FormatDecl.Cond cond) {
            if ( et != null )
                operandDecodeString.put(opname, tr("$symbol.$1.$2", et, cond.expr));
            else
                operandDecodeString.put(opname, cond.expr.toString());
        }

        String getReadExpr(ReadMethod rm, OperandTypeDecl.Value ot) {
            if ( ot.isSigned() ) return "signExtend("+rm+", "+ot.size+ ')';
            else return rm.toString();
        }

        String getEnumType(OperandTypeDecl ot) {
            if ( ot.isValue() ) {
                OperandTypeDecl.Value vt = (OperandTypeDecl.Value)ot;
                EnumDecl ed = dGen.arch.getEnum(vt.typeRef.getTypeConName());
                if ( ed != null ) return ed.name.image;
                return null;
            }
            return null;
        }

        int[] computeScatter(String name, OperandTypeDecl.Value vd, List<FormatDecl.BitField> fs) {
            int bit = 0;
            int[] result = new int[vd.size];
            Arrays.fill(result, -1);
            for ( FormatDecl.BitField f : fs) {
                visitExpr(f, name, bit, result);
                bit += f.getWidth();
            }
            for ( int cntr = 0; cntr < result.length; cntr++ ) {
                if ( result[cntr] < 0 )
                    throw Util.failure("bit "+cntr+" of operand "+StringUtil.quote(name)+" in encoding "+
                            decl.name+" at "+DGUtil.pos(decl.name)+" is not present in the encoding");
            }
            return result;
        }

        private void visitExpr(FormatDecl.BitField f, String name, int bit, int[] result) {
            Expr e = f.field;
            if ( matches(e, name) ) {
                for ( int cntr = 0; cntr < f.getWidth(); cntr++ ) result[cntr] = cntr + bit;
            } else if ( e.isBitRangeExpr() ) {
                FixedRangeExpr bre = (FixedRangeExpr)e;
                if ( matches(bre.expr, name) ) {
                    for ( int cntr = 0; cntr < f.getWidth(); cntr++ ) {
                        int indx = cntr+bre.low_bit;
                         // we don't care about bits beyond the end of our declared operand
                        if ( indx < result.length )
                            result[indx] = cntr + bit;
                    }
                }
            } else if ( e instanceof IndexExpr ) {
                IndexExpr be = (IndexExpr)e;
                if ( matches(be.expr, name) && be.index.isLiteral() ) {
                    int value = ((Literal.IntExpr)be.index).value;
                    result[value] = bit;
                }
            }
        }

        private boolean matches(Expr e, String name) {
            if ( e instanceof VarExpr ) {
                VarExpr ve = (VarExpr)e;
                return name.equals(ve.variable.image);
            } else if ( e instanceof DotExpr ) {
                DotExpr de = (DotExpr)e;
                return name.equals(de.expr +"."+de.field);
            }
            return false;
        }

        void generateReader() {
            // if we do not need to generate a real reader for this one
            if ( instr != null && !DGUtil.addrModeClassExists(instr))  return;

            dGen.properties.setProperty("reader", name);
            startblock("static class $reader_reader extends OperandReader");
            startblock("$addr read($disassembler d)");
            println("d.size = $1;", decl.getBitWidth() / 8);
            for ( AddrModeDecl.Operand o : addrMode.operands )
                generateOperandRead("", o);
            print("return new $addr.$1", javaName(addrMode.name.image));
            printParams(nameList(addrMode.operands));
            println(";");
            nextln();
            endblock();
            endblock();
        }

        void generateOperandRead(String prefix, AddrModeDecl.Operand o) {
            String oname = o.name.image;
            String vname = prefix+oname;
            String vn = javaName(vname);
            OperandTypeDecl td = dGen.arch.getOperandDecl(o.typeRef.getTypeConName());
            if ( td.isCompound() ) {
                generateCompound(td, vname);
            } else if ( td.isValue() ) {
                OperandTypeDecl.Value vtd = (OperandTypeDecl.Value)td;
                // not a conditional encoding; load bits and generate tables
                print("$operand.$1 $2 = new $operand.$1(", vtd.name, vn);
                generateRawRead(vname);
                println(");");
            }
        }

        private void generateCompound(OperandTypeDecl td, String vname) {
            for ( AddrModeDecl.Operand so : td.subOperands )
                generateOperandRead(vname+ '.', so);
            String vn = javaName(vname);
            beginList("$operand.$1 $2 = new $operand.$1(", td.name, vn);
            for ( AddrModeDecl.Operand so : td.subOperands ) {
                print(vname+ '_' +so.name);
            }
            endList(");");
            nextln();
        }

        private void generateRawRead(String oname) {
            String str = operandDecodeString.get(oname);
            assert str != null;
            print(str);
        }
    }

    public void generateOperandReaders() {

        for ( EncodingReader r : encodingInfo.values() ) {
            r.computeDecoders();
        }

        generateReads();

        generateJavaDoc("The <code>NULL_reader</code> class is used for instructions that define their " +
                "own addressing mode and have no operands. This reader sets the size of the instruction " +
                "to the appropriate size for the encoding and the addressing mode to <code>null</code>.");
        startblock("public static class NULL_reader extends OperandReader");
        println("final int size;");
        startblock("NULL_reader(int sz)");
        println("this.size = sz;");
        endblock();
        startblock("$addr read($disassembler d)");
        println("d.size = size;");
        println("return null;");
        endblock();
        endblock();

        startblock("private static int signExtend(int val, int size)");
        println("// shift all the way to the left and then back (arithmetically)");
        println("int shift = 32 - size;");
        println("return (val << shift) >> shift;");
        endblock();

        for ( EncodingReader er : encodingInfo.values() )
            er.generateReader();
    }

    String getReaderCreation(String r) {
        EncodingReader reader = this.encodingRev.get(r);
        if ( reader.instr != null && !DGUtil.addrModeClassExists(reader.instr) )
            return tr("new NULL_reader($1)", reader.decl.getBitWidth()/8);
        else return tr("new $1_reader()", r);
    }
}
