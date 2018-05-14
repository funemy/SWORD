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

package jintgen.gen;

import cck.text.Printer;
import cck.text.StringUtil;
import cck.util.Option;
import jintgen.gen.disassembler.DGUtil;
import jintgen.isdl.*;
import jintgen.types.TypeRef;
import jintgen.jigir.JIGIRTypeEnv;
import java.io.IOException;
import java.util.*;

/**
 * The <code>ClassGenerator</code> class generates a set of classes that represent instructions in an
 * architecture. It will generate an outer class <code>Instr</code> that contains as inner classes, the
 * individual instructions contained in the architecture description.
 *
 * @author Ben L. Titzer
 */
public class InstrIRGenerator extends Generator {

    LinkedList<String> hashMapImport;

    protected final Option.Str CLASS_FILE = options.newOption("class-template", "Instr.java",
            "This option specifies the name of the file that contains a template for generating the " +
            "instruction classes.");

    public void generate() throws Exception {

        properties.setProperty("instr", className("Instr"));
        properties.setProperty("addr", className("AddrMode"));
        properties.setProperty("addrvisitor", className("AddrModeVisitor"));
        properties.setProperty("operand", className("Operand"));
        properties.setProperty("opvisitor", className("OperandVisitor"));
        properties.setProperty("visitor", className("InstrVisitor"));
        properties.setProperty("builder", className("InstrBuilder"));
        properties.setProperty("symbol", className("Symbol"));

        hashMapImport = new LinkedList<String>();
        hashMapImport.add("java.util.HashMap");

        generateOperandClasses();
        generateVisitor();
        generateInstrClasses();
        generateEnumerations();
        generateBuilder();
        generateAddrModeClasses();
    }

    //=========================================================================================
    // CODE TO EMIT VISITOR CLASS
    //=========================================================================================

    private void generateVisitor() throws IOException {
        setPrinter(newInterfacePrinter("visitor", null, null,
                tr("The <code>$visitor</code> interface allows user code that implements " +
                "the interface to easily dispatch on the type of an instruction without casting using " +
                "the visitor pattern.")));
        for (InstrDecl d : arch.instructions ) emitVisitMethod(d);
        p.endblock();
        close();
    }

    private void emitVisitMethod(InstrDecl d) {
        println("public void visit($instr.$1 i);", d.innerClassName);
    }

    //=========================================================================================
    // CODE TO EMIT INSTR CLASSES
    //=========================================================================================

    private void generateInstrClasses() throws IOException {
	LinkedList<String> imports = new LinkedList<String>();
	imports.add("avrora.arch.AbstractArchitecture");
	imports.add("avrora.arch.AbstractInstr");
	LinkedList<String> impl = new LinkedList<String>();
        impl.add("AbstractInstr");
        setPrinter(newAbstractClassPrinter("instr", imports, null, impl,
                tr("The <code>$instr</code> class is a container (almost a namespace) for " +
                "all of the instructions in this architecture. Each inner class represents an instruction " +
                "in the architecture and also extends the outer class.")));

        generateJavaDoc("The <code>accept()</code> method accepts an instruction visitor and " +
                "calls the appropriate <code>visit()</code> method for this instruction.\n" +
                "@param v the instruction visitor to accept");
        println("public abstract void accept($visitor v);");

        generateJavaDoc("The <code>accept()</code> method accepts an addressing mode visitor " +
                "and calls the appropriate <code>visit_*()</code> method for this instruction's addressing " +
                "mode.\n" +
                "@param v the addressing mode visitor to accept");
        startblock("public void accept($addrvisitor v)");
        println("// the default implementation of accept() is empty");
        endblock();

        generateJavaDoc("The <code>toString()</code> method converts this instruction to a string " +
                "representation. For instructions with operands, this method will render the operands " +
                "in the appropriate syntax as declared in the architecture description.\n" +
                "@return a string representation of this instruction");
        startblock("public String toString()");
        println("// the default implementation of toString() simply returns the name");
        println("return name;");
        endblock();

        generateJavaDoc("The <code>name</code> field stores a reference to the name of the instruction as a " +
                "string.");
        println("public final String name;");

        generateJavaDoc("The <code>size</code> field stores the size of the instruction in bytes.");
        println("public final int size;");

        generateJavaDoc("The <code>getSize()</code> method returns the size of this instruction in bytes.");
        startblock("public int getSize()");
        println("return size;");
        endblock();
        println("");

        generateJavaDoc("The <code>getName()</code> method returns the name of this instruction.");
        startblock("public String getName()");
        println("return name;");
        endblock();
        println("");

        generateJavaDoc("The <code>getArchitecture()</code> method returns the architecture of this instruction.");
        startblock("public AbstractArchitecture getArchitecture()");
        println("return null;");
        endblock();
        println("");

        generateJavaDoc(tr("The default constructor for the <code>$instr</code> class accepts a " +
                "string name and a size for each instruction.\n" +
                "@param name the string name of the instruction\n" +
                "@param size the size of the instruction in bytes"));
        startblock("protected $instr(String name, int size)");
        println("this.name = name;");
        println("this.size = size;");
        endblock();
        println("");

        generateSuperClasses();

        for (InstrDecl d : arch.instructions) emitClass(d);
        endblock();
        close();
    }

    private void generateSuperClasses() {

        HashSet<AddrModeDecl> usedAddrs = new HashSet<AddrModeDecl>();
        HashSet<AddrModeSetDecl> usedSets = new HashSet<AddrModeSetDecl>();

        // only generate the classes that are used directly by instructions
        for(InstrDecl d: arch.instructions) {
            if ( d.addrMode.ref != null ) {
                AddrModeSetDecl as = arch.getAddressingModeSet(d.addrMode.ref.image);
                if ( as != null ) usedSets.add(as);
                else {
                    AddrModeDecl am = arch.getAddressingMode(d.addrMode.ref.image);
                    if ( am != null ) usedAddrs.add(am);
                }
            }
        }

        for (AddrModeDecl d : usedAddrs) emitAddrInstrClass(d);
        for (AddrModeSetDecl d : usedSets) emitAddrSetClass(d);

    }

    private void emitAddrInstrClass(AddrModeDecl d) {
        startblock("public abstract static class $1_Instr extends $instr", d.name);
        for (AddrModeDecl.Operand o : d.operands)
            println("public final $operand.$1 $2;", o.typeRef, o.name);
        startblock("protected $1_Instr(String name, int size, $addr.$1 am)", d.name);

        println("super(name, size);");
        initFields("this.$1 = am.$1;", d.operands);
        endblock();
        // emit the accept method for the addressing mode visitor
        startblock("public void accept($addrvisitor v)");
        print("v.visit_$1", d.name);
        printParams(nameList("this", d.operands));
        println(";");
        endblock();
        startblock("public String toString()");
        print("return name");
        emitRenderer(d.getProperty("syntax"), d.operands);
        println(";");
        endblock();
        endblock();
        println("");
    }

    private void emitAddrSetClass(AddrModeSetDecl d) {
        startblock("public abstract static class $1_Instr extends $instr", d.name);
        println("public final $addr.$1 am;", d.name);
        for (AddrModeDecl.Operand o : d.unionOperands)
            println("public final $operand $1;", o.name);
        startblock("protected $1_Instr(String name, int size, $addr.$1 am)", d.name);

        println("super(name, size);");
        println("this.am = am;", d.name);
        initFields("this.$1 = am.get_$1();", d.unionOperands);
        endblock();
        // emit the accept method for the addressing mode visitor
        startblock("public void accept($addrvisitor v)");
        println("am.accept(this, v);");
        endblock();
        // emit the toString() method
        startblock("public String toString()");
        println("return name+am.toString();");
        endblock();
        endblock();
        println("");
    }

    private void emitClass(InstrDecl d) {
        String cName = d.getInnerClassName();
        cName = StringUtil.trimquotes(cName.toUpperCase());
        boolean hasSuper = d.addrMode.localDecl == null;
        String sup = hasSuper ? addrModeName(d) + "_Instr" : tr("$instr");
        startblock("public static class $1 extends $2", cName, sup);

        emitFields(d, hasSuper);
        emitConstructor(cName, d, hasSuper);
        println("public void accept($visitor v) { v.visit(this); }");
        if ( !hasSuper ) {
            if ( DGUtil.addrModeClassExists(d) ) {
                startblock("public void accept($addrvisitor v)");
                print("v.visit_$1", addrModeName(d));
                printParams(nameList("this", d.getOperands()));
                println(";");
                endblock();
            }
            startblock("public String toString()");
            print("return name");
            emitRenderer(d.getProperty("syntax"), d.getOperands());
            println(";");
            endblock();
        }

        endblock();
        println("");
    }

    private void emitFields(InstrDecl d, boolean hasSuper) {
        // emit the declaration of the fields
        if ( !hasSuper ) {
            for (AddrModeDecl.Operand o : d.getOperands())
                println("public final $operand.$1 $2;", o.typeRef, o.name);
        }
    }

    private void emitConstructor(String cName, InstrDecl d, boolean hasSuper) {
        // emit the declaration of the constructor
        if ( DGUtil.addrModeClassExists(d) ) {
            startblock("$1(int size, $addr.$2 am)", cName, addrModeName(d));
        } else {
            startblock("$1(int size)", cName, addrModeName(d));
        }
        if ( hasSuper ) {
            println("super($1, size, am);", d.name);
        } else {
            println("super($1, size);", d.name);
            initFields("this.$1 = am.$1;", d.getOperands());
        }
        endblock();
    }

    private void initFields(String format, List<AddrModeDecl.Operand> list) {
        for (AddrModeDecl.Operand o : list) {
            String n = o.name.image;
            println(format, n);
        }
    }

    private String addrModeName(InstrDecl d) {
        if ( d.addrMode.localDecl != null ) return javaName(d.name.image);
        else return d.addrMode.ref.image;
    }

    //=========================================================================================
    // CODE TO EMIT SYMBOL SETS
    //=========================================================================================

    private void generateEnumerations() throws IOException {
        setPrinter(newClassPrinter("symbol", hashMapImport, null, null,
                tr("The <code>$symbol</code> class represents a symbol (or an enumeration as " +
                "declared in the instruction set description) relevant to the instruction set architecture. " +
                "For example register names, status bit names, etc are given here. This class provides a " +
                "type-safe enumeration for such symbolic names.")));
        generateEnumHeader();

        for ( EnumDecl d : arch.enums ) {
            generateEnumClass(d);
        }

        endblock();
        close();
    }

    private void generateEnumHeader() {
        println("public final String symbol;");
        println("public final int value;");
        println("");
        println("$symbol(String sym, int v) { symbol = sym;  value = v; }");
        println("public int getValue() { return value; }");
        println("public int getEncodingValue() { return value; }");
        println("");
    }

    private void generateEnumClass(EnumDecl d) {
        SymbolMapping m = d.map;
        String n = m.name.image;
        properties.setProperty("enum", n);
        if ( d instanceof EnumDecl.Subset ) {
            // this enumeration is a subset of another enumeration, but with possibly different
            // encoding
            EnumDecl.Subset sd = (EnumDecl.Subset)d;
            startblock("public static class $enum extends $1", javaType(sd.parentRef));
            println("public final int encoding;");
            println("public int getEncodingValue() { return encoding; }");
            println("private static HashMap set = new HashMap();");
            startblock("private static $enum new$enum(String n, int v, int ev)");
            println("$enum obj = new $enum(n, v, ev);");
            println("set.put(n, obj);");
            println("return obj;");
            endblock();

            println("$enum(String sym, int v, int ev) { super(sym, v); encoding = ev; }");

            for ( SymbolMapping.Entry e : m.getEntries() ) {
                String en = e.name;
                String EN = en.toUpperCase();
                SymbolMapping.Entry se = sd.getParent().map.get(e.name);
                println("public static final $enum "+EN+" = new"+n+"(\""+en+"\", "+se.value+", "+e.value+");");
            }
        } else {
            // this enumeration is NOT a subset of another enumeration
            startblock("public static class $1 extends $symbol", n);
            println("private static HashMap set = new HashMap();");

            startblock("private static $enum new$enum(String n, int v)");
            println("$enum obj = new $enum(n, v);");
            println("set.put(n, obj);");
            println("return obj;");
            endblock();

            println("$enum(String sym, int v) { super(sym, v); }");

            for ( SymbolMapping.Entry e : m.getEntries() ) {
                String en = e.name;
                String EN = en.toUpperCase();
                println("public static final $enum "+EN+" = new"+n+"(\""+en+"\", "+e.value+");");
            }
        }

        endblock();
        println("");

        genGetMethod();

        println("");
    }

    private void genGetMethod() {
        startblock("public static $enum get_$enum(String name)");
        println("return ($enum)$enum.set.get(name);");
        endblock();
    }

    //=========================================================================================
    // CODE TO EMIT OPERAND TYPES
    //=========================================================================================
    private void generateOperandClasses() throws IOException {
        setPrinter(newAbstractClassPrinter("operand", hashMapImport, null, null,
                tr("The <code>$operand</code> interface represents operands that are allowed to " +
                "instructions in this architecture. Inner classes of this interface enumerate the possible " +
                "operand types to instructions and their constructors allow for dynamic checking of " +
                "correctness constraints as expressed in the instruction set description.")));

        int cntr = 1;
        for ( OperandTypeDecl d : arch.operandTypes ) {
            println("public static final byte $1_val = $2;", d.name, cntr++);
        }

        generateJavaDoc("The <code>op_type</code> field stores a code that determines the type of " +
                "the operand. This code can be used to dispatch on the type of the operand by switching " +
                "on the code.");
        println("public final byte op_type;");

        generateJavaDoc("The <code>accept()</code> method implements the visitor pattern for operand " +
                "types, allowing a user to double-dispatch on the type of an operand.");
        p.println(tr("public abstract void accept($opvisitor v);"));

        generateJavaDoc("The default constructor for the <code>$operand</code> class simply stores the " +
                "type of the operand in a final field.");
        startblock("protected $operand(byte t)");
        println("op_type = t;");
        endblock();

        generateJavaDoc(tr("The <code>$operand.Int</code> class is the super class of operands that can " +
                "take on integer values. It implements rendering the operand as an integer literal."));
        startblock("abstract static class Int extends $operand");
        println("public final int value;");
        startblock("Int(byte t, int val)");
        println("super(t);");
        println("this.value = val;");
        endblock();
        startblock("public String toString()");
        println("return Integer.toString(value);");
        endblock();
        endblock();
        println("");

        generateJavaDoc(tr("The <code>$operand.Sym</code> class is the super class of operands that can " +
                "take on symbolic (enumerated) values. It implements rendering the operand as " +
                "the name of the corresponding enumeration type."));
        startblock("abstract static class Sym extends $operand");
        println("public final $symbol value;");
        startblock("Sym(byte t, $symbol sym)");
        println("super(t);");
        println("if ( sym == null ) throw new Error();");
        println("this.value = sym;");
        endblock();
        startblock("public String toString()");
        println("return value.symbol;");
        endblock();
        endblock();
        println("");

        generateJavaDoc(tr("The <code>$operand.Addr</code> class is the super class of operands that represent " +
                "an address. It implements rendering the operand as a hexadecimal number."));
        startblock("abstract static class Addr extends $operand");
        println("public final int value;");
        startblock("Addr(byte t, int addr)");
        println("super(t);");
        println("this.value = addr;");
        endblock();
        startblock("public String toString()");
        println("String hs = Integer.toHexString(value);");
        println("StringBuffer buf = new StringBuffer(\"0x\");");
        println("for ( int cntr = hs.length(); cntr < 4; cntr++ ) buf.append('0');");
        println("buf.append(hs);");
        println("return buf.toString();");
        endblock();
        endblock();
        println("");

        generateJavaDoc(tr("The <code>$operand.Rel</code> class is the super class of operands that represent " +
                "an address that is computed relative to the program counter. It implements rendering " +
                "the operand as the PC plus an offset."));
        startblock("abstract static class Rel extends $operand");
        println("public final int value;");
        println("public final int relative;");
        startblock("Rel(byte t, int addr, int rel)");
        println("super(t);");
        println("this.value = addr;");
        println("this.relative = rel;");
        endblock();
        startblock("public String toString()");
        println("if ( relative >= 0 ) return \".+\"+relative;");
        println("else return \".\"+relative;");
        endblock();
        endblock();
        println("");

        for ( OperandTypeDecl d : arch.operandTypes )
            generateOperandType(d);

        endblock();
        close();

        // generate visitor class
        Printer vprinter = newInterfacePrinter("opvisitor", hashMapImport, null,
                tr("The <code>$opvisitor</code> interface allows clients to use the Visitor pattern to " +
                        "resolve the types of operands to instructions."));

        // generate the visit methods explicitly declared operand types
        for ( OperandTypeDecl d : arch.operandTypes )
            vprinter.println(tr("public void visit($operand.$1 o);", d.name));

        vprinter.endblock();
        vprinter.close();
    }

    private void generateOperandType(OperandTypeDecl d) {
        String otname = d.name.image;
        // generate visit method inside visitor
        if ( d.isValue() ) {
            OperandTypeDecl.Value vd = (OperandTypeDecl.Value)d;
            startblock("public static class $1 extends $2", otname, operandSuperClass(vd));
            generateSimpleType(vd);
        } else if ( d.isCompound() ) {
            startblock("public static class $1 extends $operand", otname);
            generateCompoundType((OperandTypeDecl.Compound)d);
        }
        // generate accept method in operand class
        startblock("public void accept($opvisitor v)");
        println("v.visit(this);");
        endblock();
        endblock();
        println("");
    }

    private String operandSuperClass(OperandTypeDecl.Value d) {
        EnumDecl ed = arch.getEnum(d.typeRef.getTypeConName());
        if ( ed != null ) return "Sym";
        else {
            if ( d.isRelative() ) return "Rel";
            if ( d.isAddress() ) return "Addr";
            return "Int";
        }
    }

    private void generateSimpleType(OperandTypeDecl.Value d) {
        EnumDecl ed = arch.getEnum(d.typeRef.getTypeConName());
        properties.setProperty("oname", d.name.image);
        properties.setProperty("kind", d.typeRef.getTypeConName());
        if ( ed != null ) {
            startblock("$oname(String s)");
            println("super($oname_val, $symbol.get_$kind(s));");
            endblock();
            startblock("$oname($symbol.$kind sym)");
            println("super($oname_val, sym);");
            endblock();
        } else {
            println("public static final int low = "+d.low+ ';');
            println("public static final int high = "+d.high+ ';');
            if ( d.isRelative() ) {
                JIGIRTypeEnv.TYPE_addr a = ((JIGIRTypeEnv.TYPE_addr)d.typeRef.getType());
                startblock("$oname(int pc, int rel)");
                int align = a.getAlign();
                if ( align > 1 )
                    println("super($oname_val, pc + $1 + $1 * rel, $builder.checkValue(rel, low, high));", align);
                else
                    println("super($oname_val, pc + 1 + rel, $builder.checkValue(rel, low, high));");
            } else if ( d.isAddress() ) {
                JIGIRTypeEnv.TYPE_addr a = ((JIGIRTypeEnv.TYPE_addr)d.typeRef.getType());
                startblock("$oname(int addr)");
                int align = a.getAlign();
                if ( align > 1 )
                    println("super($oname_val, $1 * $builder.checkValue(addr, low, high));", align);
                else
                    println("super($oname_val, $builder.checkValue(addr, low, high));");
            } else {
                startblock("$oname(int val)");
                println("super($oname_val, $builder.checkValue(val, low, high));");
            }
            endblock();
        }
    }

    private void generateCompoundType(OperandTypeDecl.Compound d) {
        // generate fields of compound operand
        emitOperandFields(d.subOperands);
        // emit the constructor
        print("public $1", d.name.image);
        printParams(nameTypeList(d.subOperands));
        startblock(" ");
        properties.setProperty("oname", d.name.image);
        println("super($oname_val);");
        initFields("this.$1 = $1;", d.subOperands);
        endblock();
    }

    //=========================================================================================
    // CODE TO EMIT OTHER STUFF
    //=========================================================================================

    private void generateBuilder() throws IOException {
        setPrinter(newAbstractClassPrinter("builder", hashMapImport, null, null, null));

        println("public abstract $instr build(int size, $addr am);");

        println("static final HashMap builders = new HashMap();");

        startblock("static $builder add(String name, $builder b)");
        println("builders.put(name, b);");
        println("return b;");
        endblock();

        for ( InstrDecl d : arch.instructions ) {
            startblock("public static class $1_builder extends $builder", d.innerClassName);
            startblock("public $instr build(int size, $addr am)");
            if ( DGUtil.addrModeClassExists(d))
                println("return new $instr.$1(size, ($addr.$2)am);", d.innerClassName, addrModeName(d));
            else
                println("return new $instr.$1(size);", d.innerClassName);
            endblock();
            endblock();
        }

        for ( InstrDecl d : arch.instructions ) {
            println("public static final $builder $1 = add($2, new $1_builder());", d.innerClassName, d.name);
        }

        startblock("public static int checkValue(int val, int low, int high)");
        startblock("if ( val < low || val > high )");
        println("throw new Error();");
        endblock();
        println("return val;");
        endblock();

        endblock();
        close();
    }

    void generateAddrModeClasses() throws IOException {

        setPrinter(newInterfacePrinter("addr", hashMapImport, null,
                tr("The <code>$addr</code> class represents an addressing mode for this architecture. An " +
                "addressing mode fixes the number and type of operands, the syntax, and the encoding format " +
                "of the instruction.")));

        println("public void accept($instr i, $addrvisitor v);");

        for ( AddrModeSetDecl as : arch.addrSets ) {
            startblock("public interface $1 extends $addr", as.name);
            for ( AddrModeDecl.Operand o : as.unionOperands )
                println("public $operand get_$1();", o.name);
            endblock();
        }

        List<AddrModeDecl> list = new LinkedList<AddrModeDecl>();
        for ( AddrModeDecl am : arch.addrModes ) list.add(am);
        for ( InstrDecl id : arch.instructions ) {
            // for each addressing mode declared locally
            if ( id.addrMode.localDecl != null && DGUtil.addrModeClassExists(id) )
                list.add(id.addrMode.localDecl);
        }
        for ( AddrModeDecl am : list ) emitAddrMode(am);
        endblock();
        close();

        emitAddrModeVisitor(list);
    }

    private void emitAddrModeVisitor(List<AddrModeDecl> list) throws IOException {
        setPrinter(newInterfacePrinter("addrvisitor", hashMapImport, null,
                tr("The <code>$addrvisitor</code> interface implements the visitor pattern for addressing modes.")));
        for ( AddrModeDecl am : list ) {
            print("public void visit_$1", javaName(am.name.image));
            printParams(nameTypeList("$instr", "i", am.operands));
            println(";");
        }
        endblock();
        close();
    }

    private void emitAddrMode(AddrModeDecl am) {
        String amName = javaName(am.name.image);
        beginList("public static class $1 implements ", amName);
        print("$addr");
        for ( AddrModeSetDecl as : am.sets ) print(as.name.image);
        endList(" ");
        // generate fields
        startblock();
        emitOperandFields(am.operands);
        // generate constructor
        print("public $1", amName);
        printParams(nameTypeList(am.operands));
        // generate field writes
        startblock(" ");
        initFields("this.$1 = $1;", am.operands);
        endblock();
        // generate accept method
        startblock("public void accept($instr i, $addrvisitor v)");
        print("v.visit_$1", amName);
        printParams(nameList("i", am.operands));
        println(";");
        endblock();
        startblock("public String toString()");
        print("return \"\"");
        emitRenderer(am.getProperty("syntax"), am.operands);
        println(";");
        endblock();
        for ( AddrModeDecl.Operand o : am.operands ) {
            OperandTypeDecl d = o.getOperandType();
            println("public $operand get_$2() { return $2; }", d.name, o.name);
        }
        endblock();
    }

    private void emitOperandFields(List<AddrModeDecl.Operand> list) {
        List<String> ntl = nameTypeList(list);
        for ( String str : ntl ) println("public final "+str+ ';');
    }

    private void emitRenderer(Property syntax, List<AddrModeDecl.Operand> list) {
        if ( syntax != null && syntax.type.isBasedOn("String")) {
            String format = StringUtil.trimquotes(syntax.value.image);
            int max = format.length();
            StringBuffer buf = new StringBuffer();
            buf.append(' ');
            for ( int pos = 0; pos < max; pos++ ) {
                char ch = format.charAt(pos);
                StringBuffer var = new StringBuffer();
                if ( ch == '%' ) {
                    for ( pos++; pos < max; pos++) {
                        char vch = format.charAt(pos);
                        if ( !Character.isLetterOrDigit(vch) && vch != '.' ) { pos--; break; }
                        var.append(vch);
                    }
                    print("+");
                    print(StringUtil.quote(buf));
                    print("+");
                    print(var.toString());
                    buf = new StringBuffer();
                } else {
                    buf.append(ch);
                }
            }
            if ( buf.length() > 0 ) {
                print("+");
                print(StringUtil.quote(buf));
            }

        } else {
            String sep = "' '";
            for ( AddrModeDecl.Operand o : list ) {
                print(" + $1 + $2", sep, o.name);
                sep = "\", \"";
            }
        }
    }

    protected String javaType(TypeRef tr) {
        return renderType(tr);
    }
}
