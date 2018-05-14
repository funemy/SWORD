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

package avrora.syntax;

import avrora.arch.legacy.LegacyRegister;
import cck.parser.*;
import cck.text.StringUtil;

/**
 * The <code>AVRErrorReporter</code> contains one method per compilation error. The method constructs a
 * <code>SourceError</code> object that represents the error and throws it. One method per type of error
 * provides a convenient interface and allows pinpointing the generation of each type of error within the
 * verifier.
 *
 * @author Ben L. Titzer
 */
public class AVRErrorReporter extends ErrorReporter {

    private SourcePoint point(AbstractToken t) {
        return new SourcePoint(t.file, t.beginLine, t.beginColumn, t.endColumn);
    }

    private SourcePoint point(ASTNode n) {
        AbstractToken l = n.getLeftMostToken();
        AbstractToken r = n.getRightMostToken();
        return new SourcePoint(l.file, l.beginLine, l.beginColumn, r.endColumn);
    }

    public void UnknownRegister(AbstractToken reg) {
        String report = "unknown register " + StringUtil.quote(reg);
        error("UnknownRegister", point(reg), report);
    }

    public void InstructionCannotBeInSegment(String seg, AbstractToken instr) {
        String report = "instructions cannot be declared in " + seg + " cseg";
        error("InstructionCannotBeInSegment", point(instr), report);
    }

    public void RegisterExpected(SyntacticOperand o) {
        String report = "register expected";
        error("RegisterExpected", point(o), report);
    }

    public void IncorrectRegister(SyntacticOperand o, LegacyRegister reg, String expect) {
        String report = "incorrected register " + StringUtil.quote(reg) + ", expected one of " + expect;
        error("IncorrectRegister", point(o), report);
    }

    public void ConstantExpected(SyntacticOperand o) {
        String report = "constant expected";
        error("ConstantExpected", point(o), report);
    }

    public void ConstantOutOfRange(SyntacticOperand o, int value, String range) {
        String report = "constant " + StringUtil.quote("" + value) + " out of expected range " + range;
        error("ConstantOutOfRange", point(o), report, "" + value);
    }

    public void WrongNumberOfOperands(AbstractToken instr, int seen, int expected) {
        String report = "wrong number of operands to instruction " + StringUtil.quote(instr) + ", expected " + expected + " and found " + seen;
        error("WrongNumberOfOperands", point(instr), report);
    }

    public void UnknownVariable(AbstractToken name) {
        String report = "unknown variable or label " + StringUtil.quote(name.image);
        error("UnknownVariable", point(name), report, name.image);
    }

    public void DataCannotBeInSegment(String seg, ASTNode loc) {
        String report = "initialized data cannot be in " + seg + " segment";
        error("DataCannotBeInSegment", point(loc), report, seg);
    }

    public void IncludeFileNotFound(AbstractToken tok) {
        String report = "include file not found " + tok;
        error("IncludeFileNotFound", point(tok), report);
    }
}
