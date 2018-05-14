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

import cck.text.*;
import cck.util.Arithmetic;
import cck.util.Util;
import jintgen.gen.ConstantPropagator;
import jintgen.isdl.*;
import jintgen.isdl.parser.Token;
import jintgen.jigir.Expr;
import jintgen.jigir.Literal;
import java.util.*;

/**
 * The <code>DGUtil</code> class contains a set of utility methods that are useful in
 * implementing, debugging, and understanding the disassembler generator.
 *
 * @author Ben L. Titzer
 */
public class DGUtil {

    /**
     * The <code>toString()</code> method converts an instance of the <code>EncodingInst</code>
     * class into a string.
     * @param ei the encoding info instance to convert to a string
     * @return a string representation of the encoding info
     */
    public static String toString(EncodingInst ei) {
        StringBuffer buf = new StringBuffer(25+ei.bitStates.length);
        buf.append(ei.instr.name.toString());
        buf.append(" x ");
        buf.append(ei.addrMode.name.toString());
        int space = 20 - buf.length();
        while ( space > 0 ) {
            buf.append(' ');
            space--;
        }
        buf.append(": ");
        for ( int cntr = 0; cntr < ei.bitStates.length; cntr++ ) {
            switch ( ei.bitStates[cntr] ) {
                case EncodingInst.ENC_ZERO:
                    buf.append('0');
                    break;
                case EncodingInst.ENC_ONE:
                    buf.append('1');
                    break;
                case EncodingInst.ENC_MATCHED_ONE:
                    buf.append('U');
                    break;
                case EncodingInst.ENC_MATCHED_ZERO:
                    buf.append('u');
                    break;
                case EncodingInst.ENC_VAR:
                    buf.append('.');
                    break;
            }
        }
        return buf.toString();
    }

    /**
     * The <code>printTree()</code> method is a utility method to dump out a tree
     * to a specified printer in a textual format.
     * @param p the printer to dump the tree to
     * @param dt the decoding tree to print
     */
    public static void printTree(Printer p, DTNode dt) {
        HashSet<DTNode> nodes = new HashSet<DTNode>();
        printTree(nodes, p, dt, 0);
    }

    /**
     * The <code>printTree()</code> method is a utility method to dump out a tree
     * to a specified printer in a textual format.
     * @param p the printer to dump the tree to
     * @param dt the decoding tree to print
     * @param depth the indenting depth
     */
    public static void printTree(HashSet<DTNode> nodes, Printer p, DTNode dt, int depth) {
        p.print("#"+dt.number+ ' ');
        if ( nodes.contains(dt) ) {
            p.nextln();
            return;
        }
        nodes.add(dt);
        String label = dt.getLabel();
        if ( label != null ) p.print(label+ ' ');
        if ( dt.isLeaf() ) {
            printLeaf(dt, p);
            return;
        }
        int length = dt.right_bit - dt.left_bit + 1;
        p.println("decode["+dt.left_bit+ ':' +dt.right_bit+"]: ");
        DTNode def = null;
        for ( Map.Entry<Integer, DTNode> e : dt.getSortedEdges() ) {
            DTNode cdt = e.getValue();
            int val = e.getKey();
            if ( val < 0 ) {
                def = cdt;
                continue;
            }
            printNode(nodes, p, depth, val, length, cdt);
        }
        if ( def != null )
            printNode(nodes, p, depth, -1, length, def);
    }

    private static void printLeaf(DTNode dt, Printer p) {
        String label = dt.getLabel();
        if ( label == null )
            for ( EncodingInst ei : dt.encodings ) ei.print(0, p);
    }

    private static void printNode(HashSet<DTNode> nodes, Printer p, int depth, int val, int length, DTNode cdt) {
        indent(p, depth+1);
        p.print(getEdgeLabel(val, length)+" -> ");
        printTree(nodes, p, cdt, depth+1);
        p.nextln();
    }

    /**
     * The <code>indent()</code> method simply prints a number of leading spaces that
     * help indentation for printing out trees.
     * @param p the printer to indent
     * @param depth the depth to indent
     */
    public static void indent(Printer p, int depth) {
        for ( int cntr = 0; cntr < depth; cntr++ ) p.print("    ");
    }

    public static void ambiguous(Set<EncodingInst> set) {
        Terminal.nextln();
        Terminal.printRed("ERROR");
        Terminal.println(": the following encodings are ambiguous:");
        for ( EncodingInst el : set )
            el.print(0, Printer.STDOUT);
        throw Util.failure("Disassembler generator cannot continue");
    }

    public static void printDotTree(String title, DTNode dt, Printer p) {
        p.startblock("digraph "+title);
        p.println("rankdir=LR;");
        p.println("ranksep=2.0;");
        p.println("node[fontsize=20,fontname=Monaco];");
        HashSet<Integer> nodes = new HashSet<Integer>();
        printNode(p, 0, dt, nodes);
        p.endblock();
    }

    private static void printNode(Printer p, long state, DTNode dt, HashSet<Integer> nodes) {
        long nstate = getBitStates(state, dt);
        int length = dt.right_bit - dt.left_bit + 1;
        String name = getName(dt, state);
        p.println(name+ ';');
        for ( Map.Entry<Integer, DTNode> e : dt.getEdges() ) {
            int value = e.getKey();
            DTNode cdt = e.getValue();
            if ( !nodes.contains(cdt.number) ) {
                printNode(p, nstate, cdt, nodes);
                nodes.add(cdt.number);
            }
            String edgeLabel = StringUtil.quote(getEdgeLabel(value, length));
            p.println(name+" -> "+getName(cdt, nstate)+" [label="+edgeLabel+"];");
        }
    }

    private static String getEdgeLabel(int value, int length) {
        return value == -1 ? "*" : StringUtil.toBin(value, length);
    }

    private static String getName(DTNode dt, long state) {
        StringBuffer buf = new StringBuffer(100);
        buf.append('"');
        buf.append(dt.number);
        buf.append(':');
        buf.append(dt.getLabel());
        buf.append("\\n");
        for ( int cntr = 0; cntr < 16; cntr++ )
            buf.append(Arithmetic.getBit(state, cntr) ? "X" : ".");
        buf.append("\\n[");
        buf.append(dt.left_bit);
        buf.append(':');
        buf.append(dt.right_bit);
        buf.append(']');
        buf.append('"');
        return buf.toString();
    }

    public static Collection<DTNode> topologicalOrder(DTNode n) {
        HashSet<Integer> set = new HashSet<Integer>();
        List<DTNode> list = new LinkedList<DTNode>();
        n.addTopologicalOrder(list, set);
        return list;
    }

    public static Collection<DTNode> preOrder(DTNode n) {
        HashSet<Integer> set = new HashSet<Integer>();
        List<DTNode> list = new LinkedList<DTNode>();
        n.addPreOrder(list, set);
        return list;
    }

    public static int numberNodes(DTNode n) {
        int number = 0;
        for ( DTNode c : preOrder(n) ) c.number = number++;
        return number;
    }

    public static long getBitStates(long prev, DTNode n) {
        if ( n.isLeaf() ) return prev;
        for ( int bit = n.left_bit; bit <= n.right_bit; bit++ )
            prev = Arithmetic.setBit(prev, bit, true);
        return prev;
    }

    public static DTNode removeAll(DTNode n, String label) {
        if ( label.equals(n.getLabel()) ) return null;
        // rebuild each of the children
        HashMap<Integer, DTNode> nc = new HashMap<Integer, DTNode>();
        for ( Map.Entry<Integer, DTNode> e : n.getEdges() ) {
            int value = e.getKey();
            DTNode child = e.getValue();
            DTNode nchild = removeAll(child, label);
            if ( nchild != null )
                nc.put(value, nchild);
        }

        // reset left and right bits for a terminal node
        if (nc.isEmpty() )
            return n.shallowCopy(0, 0, nc);
        else
            return n.shallowCopy(nc);
    }

    public static List<FormatDecl.BitField> reduceEncoding(FormatDecl ed, InstrDecl id, AddrModeDecl am) {
        LinkedList<FormatDecl.BitField> nl = new LinkedList<FormatDecl.BitField>();
        ConstantPropagator cp = new ConstantPropagator();
        ConstantPropagator.Environ ce = cp.createEnvironment();
        List<FormatDecl.BitField> fs = initConstantEnviron(ce, id, am, ed);
        for ( FormatDecl.BitField bf : fs ) {
            Expr ne = bf.field.accept(cp, ce);
            FormatDecl.BitField nbf = new FormatDecl.BitField(ne, bf.getWidth());
            nl.add(nbf);
        }
        return nl;
    }

    public static List<FormatDecl.BitField> initConstantEnviron(ConstantPropagator.Environ ce, InstrDecl id, AddrModeDecl am, FormatDecl ed) {
        List<FormatDecl.BitField> list = initConstantEnviron(ed, ce);
        if ( am != null ) addProperties(am.properties.values(), ce);
        if ( id != null ) addProperties(id.properties.values(), ce);
        return list;
    }

    public static void addProperties(Iterable<Property> properties, ConstantPropagator.Environ ce) {
        for ( Property p : properties ) addProperty(p, ce);
    }

    public static List<FormatDecl.BitField> initConstantEnviron(FormatDecl ed, ConstantPropagator.Environ ce) {
        List<FormatDecl.BitField> fields;
        if ( ed instanceof FormatDecl.Derived ) {
            FormatDecl.Derived dd = (FormatDecl.Derived)ed;
            fields = dd.parent.fields;

            // put all the substitutions into the map
            for ( FormatDecl.Substitution s : dd.subst ) {
                ce.put(s.name.image, s.expr);
            }
        } else {
            fields = ed.fields;
        }
        return fields;
    }

    public static void addProperty(Property p, ConstantPropagator.Environ ce) {
        if ( p.type.isBasedOn("int") ) {
            ce.put(p.name.image, new Literal.IntExpr(p.value));
        } else if ( p.type.isBasedOn("boolean") ) {
            ce.put(p.name.image, new Literal.BoolExpr(p.value));
        }
    }

    public static String pos(Token t) {
        return t.beginColumn+":"+t.beginLine;
    }

    public static boolean isPolyMorphic(InstrDecl d) {
        return d.addrMode.addrModes.size() > 1;
    }

    public static boolean addrModeClassExists(InstrDecl d) {
        AddrModeDecl localDecl = d.addrMode.localDecl;
        return localDecl == null || !localDecl.operands.isEmpty();
    }
}
