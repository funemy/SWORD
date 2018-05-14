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

import cck.text.Printer;
import cck.text.StringUtil;
import cck.util.Util;
import jintgen.gen.GenBase;
import jintgen.isdl.FormatDecl;
import jintgen.isdl.InstrDecl;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * @author Ben L. Titzer
 */
abstract class Decoder extends GenBase {
    boolean multiple;
    boolean chained;
    int treeNodes = 0;
    int numTrees = 0;
    DisassemblerGenerator dGen;
    final DTBuilder[] completeTree;

    final HashMap<String, DTNode> finalTrees;

    abstract void computeTree(int prio, DTNode n);
    abstract void generateDecoderMethod();
    abstract void generateSpecialActions();
    abstract void generateDecoderConstructors();
    abstract void generateSpecialFields();

    void compute() {
        for ( int cntr = 0; cntr < completeTree.length; cntr++ ) {
            DTBuilder dt = completeTree[cntr];
            if ( dt == null ) continue;
            DTNode root = dt.compute();
            computeTree(cntr, root);
        }
    }

    Decoder(DisassemblerGenerator dGen, int maxprio) {
        super(dGen.properties);
        this.dGen = dGen;
        setPrinter(dGen.p);
        multiple = dGen.MULTI_TREE.get();
        chained = dGen.CHAINED.get();
        completeTree = new DTBuilder[maxprio+1];
        finalTrees = new HashMap<String, DTNode>();
    }

    public static class Parallel extends Decoder {
        final DTNode[] instrTrees;
        final DTNode[] addrTrees;

        Parallel(DisassemblerGenerator dGen, int maxprio) {
            super(dGen, maxprio);
            instrTrees = new DTNode[maxprio+1];
            addrTrees = new DTNode[maxprio+1];
            if ( chained ) Util.userError("Chained decoder trees are only supported in non-parallel implementations");
        }

        void computeTree(int prio, DTNode root) {
            instrTrees[prio] = addFinalTree("instr"+prio, optimizeInstrs(root));
            addrTrees[prio] = addFinalTree("addr"+prio, optimizeAddrs(root));
        }

        private DTNode optimizeAddrs(DTNode root) {
            labelTreeWithEncodings(root);
            return new TreeFactorer(root).getNewTree();
        }

        private DTNode optimizeInstrs(DTNode root) {
            labelTreeWithInstrs(root);
            root = DGUtil.removeAll(root, "*");
            return new TreeFactorer(root).getNewTree();
        }

        void generateDecoderConstructors() {
            for ( int cntr = 0; cntr < addrTrees.length; cntr++ ) {
                DTNode addrTree = addrTrees[cntr];
                if ( addrTree != null )
                    generateDecodingTree("addr"+cntr, new AddrModeActionGetter(), addrTree, "ERROR");
                DTNode instrTree = instrTrees[cntr];
                if ( instrTree != null )
                    generateDecodingTree("instr"+cntr, new InstrActionGetter(), instrTree, "ERROR");
            }
        }

        void generateDecoderMethod() {
            generateJavaDoc("The <code>decoder_root()</code> method begins decoding the bit pattern " +
                    "into an instruction.");
            startblock("$instr decode_root()");
            println("size = 0;");
            println("builder = null;");
            println("addrMode = null;");
            if ( !multiple ) {
                println("return run_decoder(addr0, instr0);");
            } else {
                println("$instr i;");
                for ( int cntr = 0; cntr < addrTrees.length; cntr++ ) {
                    if ( addrTrees[cntr] == null ) continue;
                    println("i = run_decoder(addr$1, instr$1);", cntr);
                    if ( cntr < addrTrees.length - 1 ) print("if ( i != null ) ");
                    println("return i;");
                }
            }
            endblock();

            generateJavaDoc("The <code>run_decoder()</code> method begins decoding the bit pattern " +
                    "into an instruction. " +
                    "This implementation is <i>parallel</i>, meaning there are two trees: one for " +
                    "the instruction resolution and one of the addressing mode resolution. By beginning " +
                    "at the root node of the addressing mode and " +
                    "instruction resolution trees, the loop compares bits in the bit patterns and moves down " +
                    "the two trees in parallel. When both trees reach an endpoint, the comparison stops and " +
                    "an instruction will be built. This method accepts the value of the first word of the " +
                    "bits and begins decoding from there.\n");
            startblock("$instr run_decoder(DTNode addr, DTNode instr)");
            println("state = MOVE;");
            println("terminated = 0;");
            startblock("while ( state == MOVE )");
            println("int bits = (word0 >> addr.left_bit) & addr.mask;");
            println("addr = addr.move(this, bits);");
            println("instr = instr.move(this, bits);");
            endblock();
            println("if ( state == ERR ) return null;");
            println("else return builder.build(size, addrMode);");
            endblock();
        }

        void generateSpecialActions() {
            // in the parallel implementation, we need separate actions to set the reader and the builder
            generateJavaDoc("The <code>SetBuilder</code> class is an action that is fired when the decoding tree " +
                    "reaches a node where the instruction is known. This action fires and sets the <code>builder</code> " +
                    "field to point the appropriate builder for the instruction.");
            startblock("static class SetBuilder extends Action");
            println("$builder builder;");
            println("SetBuilder($builder b) { builder = b; }");
            println("void execute($disassembler d) { d.builder = builder; }");
            endblock();

            generateJavaDoc("The <code>SetReader</code> class is an action that is fired when the decoding tree " +
                    "reaches a node where the addressing mode is known. This action fires and sets the " +
                    "<code>addrMode</code> field to point the operands read from the instruction stream.");
            startblock("static class SetReader extends Action");
            println("OperandReader reader;");
            println("SetReader(OperandReader r) { reader = r; }");
            println("void execute($disassembler d) { d.addrMode = reader.read(d); }");
            endblock();

            generateJavaDoc("The <code>DTLoop</code> class is a node that terminates the exploration " +
                    "of the decoder when both the instruction and addressing mode decoders have reached " +
                    "the this state.");
            startblock("static class DTLoop extends DTNode");
            println("DTLoop() { super(null, 0, 0); }");
            startblock("DTNode move($disassembler d, int bits)");
            println("if ( d.terminated >= 2 ) d.state = OK;");
            println("return this;");
            endblock();
            endblock();

            generateJavaDoc("The <code>DTTerminal</code> class is a node that terminates the exploration " +
                    "of the instruction decoder.");
            startblock("static class DTTerminal extends DTNode");
            println("DTTerminal(Action a) { super(a, 0, 0); }");
            startblock("DTNode move($disassembler d, int bits)");
            println("d.terminated++;");
            println("if ( action != null ) action.execute(d);");
            println("return LOOP;");
            endblock();
            endblock();

            generateJavaDoc("The <code>LOOP</code> node is reached when either of the decoder trees reaches " +
                    "a terminal node. This node essentially waits for both trees to reach either an OK state " +
                    " or an ERR state.");
            println("public static final DTLoop LOOP = new DTLoop();");

        }

        void generateSpecialFields() {
            println("int terminated;");
        }

    }

    public static class Serial extends Decoder {
        final DTNode[] finalTrees;

        Serial(DisassemblerGenerator dGen, int maxprio) {
            super(dGen, maxprio);
            finalTrees = new DTNode[maxprio+1];
        }

        void computeTree(int prio, DTNode root) {
            finalTrees[prio] = addFinalTree("root"+prio, optimizeTree(root));
        }

        private DTNode optimizeTree(DTNode root) {
            labelTreeWithInsts(root);
            return new TreeFactorer(root).getNewTree();
        }

        void generateDecoderConstructors() {
            String last = "ERROR";
            for ( int cntr = finalTrees.length - 1; cntr >= 0; cntr-- ) {
                DTNode root = finalTrees[cntr];
                if ( root != null ) {
                    String treeName = "root"+cntr;
                    generateDecodingTree(treeName, new InstActionGetter(), root, last);
                    if ( chained ) last = treeName;
                }
            }
        }

        void generateDecoderMethod() {
            generateJavaDoc("The <code>decoder_root()</code> method begins decoding the bit pattern " +
                    "into an instruction.");
            startblock("$instr decode_root()");
            println("size = 0;");
            println("builder = null;");
            println("addrMode = null;");
            if ( chained || !multiple ) {
                println("return run_decoder(root0);");
            } else {
                println("$instr i;");
                for ( int cntr = 0; cntr < finalTrees.length; cntr++ ) {
                    if ( finalTrees[cntr] == null ) continue;
                    println("i = run_decoder(root$1);", cntr);
                    if ( cntr < finalTrees.length - 1 ) print("if ( i != null ) ");
                    println("return i;");
                }
            }
            endblock();

            generateJavaDoc("The <code>run_decoder()</code> method begins decoding the bit pattern " +
                    "into an instruction starting at the specified <code>DTNode</code> representing " +
                    "the root of a decoder. This implementation resolves both instruction and addressing " +
                    "mode with one decoder. It begins at the root node and continues comparing bits and " +
                    "following the appropriate paths until a terminal node is reached.\n" +
                    "@param node a reference to the root of the decoder where to begin decoding");
            startblock("private $instr run_decoder(DTNode node)");
            println("state = MOVE;");
            startblock("while ( state == MOVE )");
            println("int bits = (word0 >> node.left_bit) & node.mask;");
            println("node = node.move(this, bits);");
            endblock();
            println("if ( state == ERR ) return null;");
            println("else return builder.build(size, addrMode);");
            endblock();
        }

        void generateSpecialActions() {
            generateJavaDoc("The <code>DTTerm</code> class represents a terminal node in the decoding " +
                    "tree. Terminal nodes are reached when decoding is finished, and represent either " +
                    "successful decoding (meaning instruction and addressing mode were discovered) or " +
                    "unsucessful decoding (meaning the bit pattern does not encode a valid instruction.");
            startblock("static class DTTerm extends DTNode");
            startblock("DTTerm(Action a)");
            println("super(a, 0, 0);");
            endblock();
            startblock("DTNode move($disassembler d, int val)");
            println("d.state = OK;");
            println("if ( action != null ) action.execute(d);");
            println("return this;");
            endblock();
            endblock();

            generateJavaDoc("The <code>SetBuilderAndRead</code> class is an action that is fired when the " +
                    "decoding tree reaches a node where both the instruction and encoding are known. This action " +
                    "fires and sets the <code>builder</code> field to point the appropriate builder for the " +
                    "instruction, as well as setting the <code>addrMode</code> field to point to the operands " +
                    "extracted from the instruction stream.");
            startblock("static class SetBuilderAndRead extends Action");
            println("$builder builder;");
            println("OperandReader reader;");
            println("SetBuilderAndRead($builder b, OperandReader r) { builder = b; reader = r; }");
            println("void execute($disassembler d) { d.builder = builder; d.addrMode = reader.read(d); }");
            endblock();

            generateJavaDoc("The <code>DTTerminal</code> class is a node that terminates the exploration " +
                    "of the decoder.");
            startblock("static class DTTerminal extends DTNode");
            println("DTTerminal(Action a) { super(a, 0, 0); }");
            startblock("DTNode move($disassembler d, int bits)");
            println("d.state = OK;");
            println("if ( action != null ) action.execute(d);");
            println("return this;");
            endblock();
            endblock();

        }

        void generateSpecialFields() {

        }

    }

    DTNode addFinalTree(String n, DTNode t) {
        finalTrees.put(n, t);
        treeNodes += DGUtil.numberNodes(t);
        numTrees++;
        return t;
    }

    void print(Printer p) {
        for ( DTNode dt : finalTrees.values() )
            DGUtil.printTree(p, dt);
    }

    void dotDump() throws Exception {
        for ( Map.Entry<String, DTNode> e : finalTrees.entrySet() ) {
            String name = e.getKey();
            FileOutputStream fos = new FileOutputStream(name+".dot");
            Printer p = new Printer(new PrintStream(fos));
            DGUtil.printDotTree(name, e.getValue(), p);
        }
    }

    void add(EncodingInst ei) {
        int priority = ei.encoding.getPriority();
        if ( !multiple ) priority = 0;
        DTBuilder dt = completeTree[priority];
        if ( dt == null ) dt = completeTree[priority] = new DTBuilder();
        dt.addEncoding(ei);
    }

    void generate() {
        generateNodeClasses();
        generateFields();
        generateSpecialFields();
        generateDecoderConstructors();
        generateEntryPoints();
        generateDecoderMethod();
    }

    public void generateNodeClasses() {
        generateJavaDoc("The <code>DTNode</code> class represents a node in a decoding graph. Each node " +
                "compares a range of bits and branches to other nodes based on the value. Each node may " +
                "also have an action (such as fixing the addressing mode or instruction) that is " +
                "executed when the node is reached. Actions on the root node are not executed.");
        startblock("static abstract class DTNode");
        println("final int left_bit;");
        println("final int mask;");
        println("final Action action;");
        println("DTNode(Action a, int lb, int msk) { action = a; left_bit = lb; mask = msk; }");
        println("abstract DTNode move($disassembler d, int val);");
        endblock();

        generateJavaDoc("The <code>DTArrayNode</code> implementation is used for small (less than 32) " +
                "and dense (more than 50% full) edge lists. It uses an array of indices that is " +
                "directly indexed by the bits extracted from the stream.");
        startblock("static class DTArrayNode extends DTNode");
        println("final DTNode[] nodes;");
        startblock("DTArrayNode(Action a, int lb, int msk, DTNode[] n)");
        println("super(a, lb, msk);");
        println("nodes = n;");
        endblock();
        startblock("DTNode move($disassembler d, int val)");
        println("if ( action != null ) action.execute(d);");
        println("return nodes[val];");
        endblock();
        endblock();

        generateJavaDoc("The DTSortedNode implementation is used for sparse edge lists. It uses a " +
                "sorted array of indices and uses binary search on the value of the bits.");
        startblock("static class DTSortedNode extends DTNode");
        println("final DTNode def;");
        println("final DTNode[] nodes;");
        println("final int[] values;");
        startblock("DTSortedNode(Action a, int lb, int msk, int[] v, DTNode[] n, DTNode d)");
        println("super(a, lb, msk);");
        println("values = v;");
        println("nodes = n;");
        println("def = d;");
        endblock();
        startblock("DTNode move($disassembler d, int val)");
        println("if ( action != null ) action.execute(d);");
        println("int ind = Arrays.binarySearch(values, val);");
        println("if ( ind >= 0 && ind < values.length && values[ind] == val )");
        println("    return nodes[ind];");
        println("else");
        println("    return def;");
        endblock();
        endblock();

        generateJavaDoc("The <code>DTErrorTerm</code> class is a node that terminates the exploration " +
                "of the instruction decoder with failure.");
        startblock("static class DTErrorTerm extends DTNode");
        println("DTErrorTerm() { super(null, 0, 0); }");
        startblock("DTNode move($disassembler d, int bits)");
        println("d.state = ERR;");
        println("return this;");
        endblock();
        endblock();

        generateJavaDoc("The <code>ERROR</code> node is reached for incorrectly encoded instructions and indicates " +
                "that the bit pattern was an incorrectly encoded instruction.");
        println("public static final DTErrorTerm ERROR = new DTErrorTerm();");

        generateActionClasses();

        generateJavaDoc("The <code>OperandReader</code> class is an object that is capable of reading the " +
                "operands from the bit pattern of an instruction, once the addressing mode is known. One " +
                "of these classes is generated for each addressing mode. When the addressing mode is " +
                "finally known, an action will fire that sets the operand reader which is used to read " +
                "the operands from the bit pattern.");
        startblock("static abstract class OperandReader");
        println("abstract $addr read($disassembler d);");
        endblock();
    }

    void generateActionClasses() {
        generateJavaDoc("The <code>Action</code> class represents an action that can happen when the " +
                "decoder reaches a particular node in the tree. The action may be to fix the instruction " +
                "or addressing mode, or to signal an error.");
        startblock("static abstract class Action");
        println("abstract void execute($disassembler d);");
        endblock();

        generateJavaDoc("The <code>ErrorAction</code> class is an action that is fired when the decoding tree " +
                "reaches a state which indicates the bit pattern is not a valid instruction.");
        startblock("static class ErrorAction extends Action");
        println("void execute($disassembler d) { d.state = ERR; }");
        endblock();

        generateSpecialActions();
    }

    void generateFields() {
        generateJavaDoc("The <code>size</code> field is set to the length of the instruction when the " +
                "decoder reaches a terminal state with a valid instruction.");
        println("private int size;");
        generateJavaDoc("The <code>builder</code> field stores a reference to the builder that was " +
                "discovered as a result of traversing the decoder tree. The builder corresponds to one " +
                "and only one instruction and has a method that can build a new instance of the instruction " +
                "from the operands.");
        println("private $builder builder;");
        generateJavaDoc("The <code>addrMode</code> field stores a reference to the operands that were " +
                "extracted from the bit pattern as a result of traversing the decoding tree. When a node is " +
                "reached where the addressing mode is known, then the action on that node executes and " +
                "reads the operands from the bit pattern, storing them in this field.");
        println("private $addr addrMode;");
        generateJavaDoc("The <code>state</code> field controls the execution of the main decoder loop. When " +
                "the decoder begins execution, the state field is set to <code>MOVE</code>. The decoder " +
                "continues until an action fires or a terminal node is reached that sets this field to " +
                "either <code>OK</code> or <code>ERR</code>.");
        println("private int state;");

        generateJavaDoc("The <code>pc</code> field stores the current PC, which is needed for PC-relative " +
                "calculations in loading some operand types.");
        println("private int pc;");

        generateJavaDoc("The <code>state</code> field is set to <code>MOVE</code> at the beginning of the " +
                "decoding process and remains this value until a terminal state is reached. This value " +
                "indicates the main loop should continue.");
        println("private static final int MOVE = 0;");
        generateJavaDoc("The <code>state</code> field is set to <code>OK</code> when the decoder has reached " +
                "a terminal state corresponding to a valid instruction.");
        println("private static final int OK = 1;");
        generateJavaDoc("The <code>state</code> field is set to <code>ERR</code> when the decoder reaches a " +
                "state corresponding to an incorrectly encoded instruction.");
        println("private static final int ERR = -1;");

        for ( int cntr = 0; cntr < dGen.maxInstrLength - 1; cntr += DisassemblerGenerator.WORD_SIZE ) {
            int word = cntr / DisassemblerGenerator.WORD_SIZE;
            generateJavaDoc("The <code>word"+word+"</code> field stores a word-sized chunk of the instruction " +
                    "stream. It is used by the decoders instead of repeatedly accessing the array. This implementation " +
                    "has been configured with "+DisassemblerGenerator.WORD_SIZE+"-bit words.");
            println("private int word"+word+ ';');
        }
    }

    void generateEntryPoints() {
        generateAbstractMethod();
        generateArrayEntryPoint("byte", 8, true);
        generateArrayEntryPoint("char", 16, false);
        generateArrayEntryPoint("short", 16, true);
        generateArrayEntryPoint("int", 32, true);
        generateArrayEntryPoint("long", 64, true);
    }

    void generateAbstractMethod() {
        generateJavaDoc("The <code>disassemble()</code> method disassembles a single instruction from " +
                "a stream of bytes. If the binary data at that location contains a valid " +
                "instruction, then it is created and returned. If the binary data at the " +
                "specified location is not a valid instruction, this method returns null.\n" +
                "@param base the base address corresponding to index 0 in the array\n" +
                "@param index the index into the specified array where to begin disassembling\n" +
                "@param code the binary data to disassemble into an instruction\n" +
                "@return a reference to a new instruction object representing the instruction " +
                "at that location; null if the binary data at the specified location does not " +
                "represent a valid instruction");
        startblock("public AbstractInstr disassemble(int base, int index, byte[] code)");
        println("return decode(base, index, code);");
        endblock();
    }

    void generateArrayEntryPoint(String type, int elemSize, boolean signed) {
        if ( DisassemblerGenerator.WORD_SIZE < elemSize ) return;
        generateJavaDoc(tr("The <code>decode()</code> method is the main entrypoint to the disassembler. " +
                "Given an array of type <code>$1[]</code>, a base address, and an index, the disassembler will attempt to " +
                "decode one instruction at that location. If successful, the method will return a reference " +
                "to a new <code>$instr</code> object. \n" +
                "@param base the base address of the array\n" +
                "@param index the index into the array where to begin decoding\n" +
                "@param code the actual code\n" +
                "@return an instance of the <code>$instr</code> class corresponding to the " +
                "instruction at this address if a valid instruction exists here; null otherwise", type));
        startblock("public $instr decode(int base, int index, $1[] code)", type);
        int num = DisassemblerGenerator.WORD_SIZE / elemSize;
        String off = "";
        for ( int cntr = 0; cntr < dGen.maxInstrLength - 1; cntr += DisassemblerGenerator.WORD_SIZE ) {
            int word = cntr / DisassemblerGenerator.WORD_SIZE;
            println("word$1 = word(code, index$2);", word, off);
            off = " + "+(word*num + num);
        }
        int i = (elemSize / 8);
        String scale = i > 1 ? " * "+i : "";
        println("pc = base + index"+scale+ ';');
        println("return decode_root();");
        endblock();
        println("");
        generateWordMethod(type, num, elemSize, signed);
    }

    void generateWordMethod(String type, int num, int bits, boolean signed) {
        startblock("int word($1[] code, int index)", type);
        String mask = StringUtil.to0xHex((1 << bits) - 1, bits / 4);
        println("if ( index > code.length - $1 ) return 0;", num);
        print("else return ");
        for ( int cntr = 0; cntr < num; cntr++ ) {
            int ind;
            if ( DisassemblerGenerator.LITTLE_ENDIAN ) ind = cntr;
            else ind = num - cntr - 1;
            String indx = add("index", ind);
            if ( signed && cntr < num - 1 )
                print(shift("(code[$1] & $2)", cntr * bits), indx, mask);
            else
                print(shift("code[$1]", cntr * bits), indx);
            if ( cntr < num - 1 ) print(" | ");
        }
        println(";");
        endblock();
    }

    void generateDecodingTree(String treeName, ActionGetter ag, DTNode dt, String def) {
        String methname = "make_"+treeName;
        generateJavaDoc("The <code>"+methname+"()</code> method creates a new instance of a " +
                "decoding tree by allocating the DTNode instances and connecting the references " +
                "together correctly. It is called only once in the static initialization of the " +
                "disassembler to build a single shared instance of the decoder tree implementation " +
                "and the reference to the root node is stored in a single private static field of " +
                "the same name.");
        startblock("static DTNode "+methname+"()");
        String last = def;
        for ( DTNode n : DGUtil.topologicalOrder(dt) )
            last = generateDecodingNode(n, ag, def);
        println("return "+last+ ';');
        endblock();
        generateJavaDoc(tr("The <code>$1</code> field stores a reference to the root of " +
                "a decoding tree. It is the starting point for decoding a bit pattern.", treeName));
        println("private static final DTNode $1 = make_$1();", treeName);

    }

    String add(String str, int a) {
        if ( a == 0 ) return str;
        else return str+" + "+a;
    }

    String shift(String str, int s) {
        if ( s == 0 ) return str;
        else return '(' +str+" << "+s+ ')';
    }

    String generateDecodingNode(DTNode n, ActionGetter ag, String def) {
        String action = ag.getAction(n);
        if ( n.isLeaf() ) {
            String nname = dGen.nodeName(n);
            println("DTNode $1 = new DTTerminal($2);", nname, action);
            return nname;
        }
        DisassemblerGenerator.DTNodeImpl nodeImpl = dGen.newDTNode(n, action, def);
        for ( Map.Entry<Integer, DTNode> e : n.getSortedEdges() ) {
            int value = e.getKey();
            DTNode cdt = e.getValue();
            nodeImpl.add(value, dGen.nodeName(cdt));
        }
        nodeImpl.generate();
        return nodeImpl.nname;
    }

    abstract class ActionGetter {
        abstract String getAction(DTNode n);
    }

    class InstrActionGetter extends ActionGetter {
        String getAction(DTNode n) {
            String label = n.getLabel();
            if ( "*".equals(label) ) return "null";
            if ( "-".equals(label) ) return "null";
            else return tr("new SetBuilder($builder.$1)", label);
        }
    }

    class AddrModeActionGetter extends ActionGetter {
        String getAction(DTNode n) {
            String label = n.getLabel();
            if ( "*".equals(label) ) return "null";
            if ( "-".equals(label) ) return "null";
            else return tr("new SetReader($1)", dGen.reader.getReaderCreation(label));
        }
    }

    class InstActionGetter extends ActionGetter {
        String getAction(DTNode n) {
            String label = n.getLabel();
            if ( "*".equals(label) ) return "null";
            if ( "-".equals(label) ) return "null";
            else {
                EncodingInst ei = n.encodings.iterator().next();
                String reader = dGen.reader.getName(ei.encoding);
                return tr("new SetBuilderAndRead($builder.$1, $2)", ei.instr.innerClassName, dGen.reader.getReaderCreation(reader));
            }
        }
    }

    void labelTreeWithInstrs(DTNode dt) {
        HashSet<InstrDecl> instrs = new HashSet<InstrDecl>();
        for ( EncodingInst ei : dt.encodings )
            instrs.add(ei.instr);

        // label all the children
        if ( instrs.size() == 1 ) {
            dt.setLabel(instrs.iterator().next().innerClassName);
            // label all the children
            for ( DTNode cdt : dt.getChildren() )
            labelTree("*", cdt);
        } else {
            dt.setLabel("-");
            for ( DTNode cdt : dt.getChildren() )
                labelTreeWithInstrs(cdt);
        }
    }

    void labelTreeWithEncodings(DTNode dt) {
        HashSet<FormatDecl> addrs = new HashSet<FormatDecl>();
        for ( EncodingInst ei : dt.encodings )
            addrs.add(ei.encoding);

        if ( addrs.size() == 1 ) {
            // label all the children
            FormatDecl ed = addrs.iterator().next();
            dt.setLabel(dGen.reader.getName(ed));
            for ( DTNode cdt : dt.getChildren() )
            labelTree("*", cdt);
        } else {
            // label all the children
            dt.setLabel("-");
            for ( DTNode cdt : dt.getChildren() )
                labelTreeWithEncodings(cdt);
        }
    }

    void labelTreeWithInsts(DTNode dt) {
        HashSet<EncodingInst> insts = new HashSet<EncodingInst>();
        for ( EncodingInst ei : dt.encodings )
            insts.add(ei);

        if ( insts.size() == 1 ) {
            // label all the children
            EncodingInst ei = insts.iterator().next();
            dt.setLabel(ei.instr.innerClassName+ 'x' +dGen.reader.getName(ei.encoding));
            for ( DTNode cdt : dt.getChildren() )
            labelTree("*", cdt);
        } else {
            // label all the children
            dt.setLabel("-");
            for ( DTNode cdt : dt.getChildren() )
                labelTreeWithInsts(cdt);
        }
    }

    void labelTree(String l, DTNode dt) {
        dt.setLabel(l);
        for ( DTNode cdt : dt.getChildren() )
            labelTree(l, cdt);
    }

}
