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

import jintgen.Main;
import jintgen.isdl.*;
import jintgen.isdl.parser.Token;
import jintgen.jigir.*;
import jintgen.types.Type;
import jintgen.types.TypeRef;
import java.io.IOException;
import java.util.*;
import cck.text.Printer;

/**
 * The <code>InterpreterGenerator</code> class is a visitor over the code of an instruction declaration or
 * subroutine that generates the appropriate Java code that implements an interpreter for the architecture.
 *
 * @author Ben L. Titzer
 */
public class InterpreterGenerator extends Generator {

    protected JavaCodePrinter javaCodePrinter;
    CodeSimplifier ncg;

    public void generate() throws IOException {
        initStatics();
        List<String> impl = new LinkedList<String>();
        impl.add(tr("$visitor"));
        Printer printer = newAbstractClassPrinter("interpreter", null, tr("$state"), impl,
                tr("The <code>$interpreter</code> class contains the code for executing each of the " +
                        "instructions for the \"$1\" architecture. It extends the $state class, which " +
                        "is code written by the user that defines the state associated with the interpreter. ", arch.name));
        setPrinter(printer);
        javaCodePrinter = new JavaCodePrinter();

	startblock(tr("public $interpreter(avrora.sim.Simulator sim) ", arch.name));
	println("super(sim);");
	endblock();
	println("");

        generateUtilities();
        generatePolyMethods();
        for (SubroutineDecl d : arch.subroutines) visit(d);
        for (InstrDecl d : arch.instructions) visit(d);
        endblock();
        close();
    }

    private void initStatics() {
        properties.setProperty("addr", className("AddrMode"));
        properties.setProperty("instr", className("Instr"));
        properties.setProperty("operand", className("Operand"));
        properties.setProperty("opvisitor", className("OperandVisitor"));
        properties.setProperty("visitor", className("InstrVisitor"));
        properties.setProperty("builder", className("InstrBuilder"));
        properties.setProperty("symbol", className("Symbol"));
        properties.setProperty("interpreter", className("InstrInterpreter"));
        properties.setProperty("state", className("State"));
        ncg = new CodeSimplifier(arch);
        ncg.genAccessMethods();
    }

    void generateUtilities() {
        startblock("boolean bit_get(int v, int bit)");
        println("return (v & (1 << bit)) != 0;");
        endblock();
        println("");

        startblock("int bit_set(int v, int bit, boolean value)");
        println("if ( value ) return v | (1 << bit);");
        println("else return v & ~(1 << bit);");
        endblock();
        println("");

        startblock("int bit_update(int v, int mask, int e)");
        println("return (v & ~mask) | (e & mask);");
        endblock();
        println("");

        startblock("int b2i(boolean v, int val)");
        println("if ( v ) return val;");
        println("else return 0;");
        endblock();
        println("");
    }

    void generatePolyMethods() {
        HashMap<Type, HashSet<OperandTypeDecl.AccessMethod>> readPolys =
                new HashMap<Type, HashSet<OperandTypeDecl.AccessMethod>>();
        HashMap<Type, HashSet<OperandTypeDecl.AccessMethod>> writePolys =
                new HashMap<Type, HashSet<OperandTypeDecl.AccessMethod>>();
        for ( OperandTypeDecl ot : arch.operandTypes ) {
            addPolyMethods(readPolys, ot.readDecls);
            addPolyMethods(writePolys, ot.writeDecls);
        }
        for ( Map.Entry<Type, HashSet<OperandTypeDecl.AccessMethod>> e : readPolys.entrySet() ) {
            generatePolyRead(e.getKey(), e.getValue());
        }
        for ( Map.Entry<Type, HashSet<OperandTypeDecl.AccessMethod>> e : writePolys.entrySet() ) {
            generatePolyWrite(e.getKey(), e.getValue());
        }
    }

    void addPolyMethods(HashMap<Type, HashSet<OperandTypeDecl.AccessMethod>> polys, Iterable<OperandTypeDecl.AccessMethod> meths) {
        for ( OperandTypeDecl.AccessMethod m : meths ) {
            if ( !m.usedPolymorphically ) continue;
            HashSet<OperandTypeDecl.AccessMethod> set = polys.get(m.type);
            if ( set == null ) {
                set = new HashSet<OperandTypeDecl.AccessMethod>();
                polys.put(m.type, set);
            }
            set.add(m);
        }
    }
    void generatePolyRead(Type t, HashSet<OperandTypeDecl.AccessMethod> meths) {
        String typeString = CodeSimplifier.getTypeString(t);
        startblock("int $1_$2($operand o)", "$read_poly", typeString);
        startblock("switch ( o.op_type )");
        for ( OperandTypeDecl.AccessMethod m : meths ) {
            OperandTypeDecl ot = m.getOperandType();
            println("case $operand.$1_val: return $2_$3(($operand.$1)o);", ot.name, "$read", typeString);
        }
        endblock();
        println("throw cck.util.Util.failure(\"invalid operand type in read\");");
        endblock();
        println("");
    }

    void generatePolyWrite(Type t, HashSet<OperandTypeDecl.AccessMethod> meths) {
        String typeString = CodeSimplifier.getTypeString(t);
        startblock("void $1_$2($operand o, int value)", "$write_poly", typeString);
        startblock("switch ( o.op_type )");
        for ( OperandTypeDecl.AccessMethod m : meths ) {
            OperandTypeDecl ot = m.getOperandType();
            println("case $operand.$1_val: $2_$3(($operand.$1)o, value); return;", ot.name, "$write", typeString);
        }
        endblock();
        println("throw cck.util.Util.failure(\"invalid operand type in write\");");
        endblock();
        println("");
    }

    public void visit(InstrDecl d) {
        startblock("public void visit($instr.$1 i) ", d.innerClassName);
        // initialize the map of local variables to operands
        javaCodePrinter.variableMap = new HashMap<String, String>();
        for (AddrModeDecl.Operand o : d.getOperands()) {
            javaCodePrinter.variableMap.put(o.name.image, "i." + o.name.image);
        }
        // emit the code of the body
        generateCode(d.code.getStmts());
        endblock();
        println("");
    }

    public void visit(SubroutineDecl d) {
        if ( !d.code.hasBody()) {
            print("protected abstract " + renderType(d.ret) + ' ' + d.name.image);
            beginList("(");
            for (SubroutineDecl.Parameter p : d.getParams()) {
                print(renderType(p.type) + ' ' + p.name.image);
            }
            endListln(");");
            return;
        }
        if (d.inline && Main.INLINE.get()) return;
        print("public $1 $2", renderType(d.ret), d.name.image);
        beginList("(");
        for (SubroutineDecl.Parameter p : d.getParams()) {
            print(renderType(p.type) + ' ' + p.name.image);
        }
        endList(") ");
        startblock();
        // initialize the map of local variables to operands
        javaCodePrinter.variableMap = new HashMap<String, String>();
        for (SubroutineDecl.Parameter p : d.getParams()) {
            String image = p.name.image;
            javaCodePrinter.variableMap.put(image, image);
        }
        generateCode(d.code.getStmts());
        endblock();
        println("");
    }

    void generateCode(List<Stmt> stmts) {
        CodeSimplifier ncg = new CodeSimplifier(arch);
        stmts = ncg.visitStmtList(stmts, new CGEnv(null, 0));
        javaCodePrinter.visitStmtList(stmts);
    }

    protected class JavaCodePrinter extends PrettyPrinter {

        protected HashMap<String, String> variableMap;

        JavaCodePrinter() {
            super(p);
        }

        protected String getVariable(Token variable) {
            String var = variableMap.get(variable.image);
            if (var == null) var = variable.image;
            return var;
        }

        public void visit(ConversionExpr e) {
            print('(' +renderType(e.typeRef)+ ')');
            inner(e.expr, Expr.PREC_TERM);
        }

        public void visit(BinOpExpr e) {
            String operation = e.operation.image;
            if ( e.getBinOp() instanceof Logical.AND ) operation = "&&";
            if ( e.getBinOp() instanceof Logical.OR ) operation = "||";
            if ( e.getBinOp() instanceof Logical.XOR ) operation = "!=";
            binop(operation, e.left, e.right, e.getPrecedence());
        }

        public void visit(Literal.EnumVal e) {
            print("$symbol.$1.$2", e.token, e.entry.name);
        }

        protected String renderType(TypeRef tr) {
            return InterpreterGenerator.this.renderType(tr);
        }

    }

}
