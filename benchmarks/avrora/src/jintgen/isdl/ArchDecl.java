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

import cck.text.Verbose;
import jintgen.isdl.parser.Token;
import jintgen.isdl.verifier.JIGIRErrorReporter;
import jintgen.jigir.JIGIRTypeEnv;
import jintgen.types.TypeRef;
import java.util.LinkedList;
import java.util.List;

/**
 * The <code>Architecture</code> class represents a collection of instructions, encodings, operands, and
 * subroutines that describe an instruction set architecture.
 *
 * @author Ben L. Titzer
 */
public class ArchDecl {

    public static boolean INLINE = true;

    Verbose.Printer printer = Verbose.getVerbosePrinter("isdl");

    public final Token name;

    public final HashList<String, SubroutineDecl> subroutines;
    public final HashList<String, InstrDecl> instructions;
    public final HashList<String, OperandTypeDecl> operandTypes;
    public final HashList<String, FormatDecl> formats;
    public final HashList<String, AddrModeDecl> addrModes;
    public final HashList<String, AddrModeSetDecl> addrSets;
    public final HashList<String, EnumDecl> enums;
    public final HashList<String, GlobalDecl> globals;
    public final List<Item> userTypes;

    public final JIGIRTypeEnv typeEnv;
    public final Environment globalEnv;
    public final JIGIRErrorReporter ERROR;

    /**
     * The constructor for the <code>Architecture</code> class creates an instance with the specified
     * name that is empty and ready to receive new instruction declarations, encodings, etc.
     * @param n
     */
    public ArchDecl(Token n) {
        name = n;

        subroutines = new HashList<String, SubroutineDecl>();
        instructions = new HashList<String, InstrDecl>();
        operandTypes = new HashList<String, OperandTypeDecl>();
        formats = new HashList<String, FormatDecl>();
        addrModes = new HashList<String, AddrModeDecl>();
        addrSets = new HashList<String, AddrModeSetDecl>();
        enums = new HashList<String, EnumDecl>();
        globals = new HashList<String, GlobalDecl>();
        userTypes = new LinkedList<Item>();
        ERROR = new JIGIRErrorReporter();

        typeEnv = new JIGIRTypeEnv(ERROR);
        globalEnv = new Environment(null);
    }

    public String getName() {
        return name.image;
    }

    public void addSubroutine(SubroutineDecl d) {
        printer.println("loading subroutine " + d.name.image + "...");
        subroutines.put(d.name.image, d);
    }

    public void addInstruction(InstrDecl i) {
        printer.println("loading instruction " + i.name.image + "...");
        instructions.put(i.name.image, i);
    }

    public void addOperand(OperandTypeDecl d) {
        printer.println("loading operand declaration " + d.name.image + "...");
        userTypes.add(d);
        operandTypes.put(d.name.image, d);
    }

    public void addEncoding(FormatDecl d) {
        printer.println("loading encoding format " + d.name.image + "...");
        formats.put(d.name.image, d);
    }

    public void addAddressingMode(AddrModeDecl d) {
        printer.println("loading addressing mode " + d + "...");
        addrModes.put(d.name.image, d);
    }

    public void addAddressingModeSet(AddrModeSetDecl d) {
        printer.println("loading addressing mode set " + d + "...");
        addrSets.put(d.name.image, d);
    }

    public void addEnum(EnumDecl m) {
        printer.println("loading enum " + m + "...");
        userTypes.add(m);
        enums.put(m.name.image, m);
    }

    public void addGlobal(Token n, TypeRef t) {
        GlobalDecl decl = new GlobalDecl(n, t);
        globals.put(n.image, decl);
    }

    public InstrDecl getInstruction(String name) {
        return instructions.get(name);
    }

    public SubroutineDecl getSubroutine(String name) {
        return subroutines.get(name);
    }

    public OperandTypeDecl getOperandDecl(String name) {
        return operandTypes.get(name);
    }

    public FormatDecl getEncoding(String name) {
        return formats.get(name);
    }

    public AddrModeDecl getAddressingMode(String name) {
        return addrModes.get(name);
    }

    public AddrModeSetDecl getAddressingModeSet(String name) {
        return addrSets.get(name);
    }

    public EnumDecl getEnum(String name) {
        return enums.get(name);
    }
}
