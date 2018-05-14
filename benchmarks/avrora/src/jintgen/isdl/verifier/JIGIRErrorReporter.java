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

package jintgen.isdl.verifier;

import cck.parser.AbstractToken;
import cck.text.StringUtil;
import jintgen.isdl.OperandTypeDecl;
import jintgen.isdl.parser.Token;
import jintgen.jigir.*;
import jintgen.types.*;

/**
 * @author Ben L. Titzer
 */
public class JIGIRErrorReporter extends TypeErrorReporter {

    public void ExtraOperandInAddrModeUnification(Token addrSet, Token addrMode, Token operand) {
        String report = "Cannot unify addressing mode " + StringUtil.quote(addrMode) + " into set " + StringUtil.quote(addrSet) + " at " + pos(addrMode) + " because it defines an operand " + StringUtil.quote(operand) + " not in the other addressing modes";
        error("ExtraOperandInAddrModeUnification", addrMode.getSourcePoint(), report);
    }

    public void MissingOperandInAddrModeUnification(Token addrSet, Token addrMode, String operand) {
        String report = "Cannot unify addressing mode " + StringUtil.quote(addrMode) + " into set " + StringUtil.quote(addrSet) + " at " + pos(addrMode) + " because it does not define an operand " + StringUtil.quote(operand) + " present in the other addressing modes";
        error("MissingOperandInAddrModeUnification", addrMode.getSourcePoint(), report);
    }

    public void UnresolvedOperandType(AbstractToken t) {
        unresolved("OperandType", "operand type", t);
    }

    public void UnresolvedEnum(AbstractToken t) {
        unresolved("Enum", "enumeration", t);
    }

    public void UnresolvedFormat(AbstractToken t) {
        unresolved("Format", "encoding format", t);
    }

    public void UnresolvedAddressingMode(AbstractToken t) {
        unresolved("AddrMode", "addressing mode", t);
    }

    public void UnresolvedVariable(AbstractToken t) {
        unresolved("Variable", "variable", t);
    }

    public void UnresolvedSubroutine(AbstractToken t) {
        unresolved("Subroutine", "subroutine", t);
    }

    public void UnresolvedSubOperand(AbstractToken t) {
	unresolved("Suboperand", "suboperand", t);
    }

    public void ArityMismatch(AbstractToken t) {
        String report = "Argument count mismatch";
        error("ArityMismatch", t.getSourcePoint(), report);
    }

    public void RedefinedInstruction(Token prevdecl, Token newdecl) {
        redefined("Instruction", "Instruction", prevdecl, newdecl);
    }

    public void RedefinedFormat(Token prevdecl, Token newdecl) {
        redefined("Format", "Encoding format", prevdecl, newdecl);
    }

    public void RedefinedEnum(Token prevdecl, Token newdecl) {
        redefined("Enum", "Enumeration", prevdecl, newdecl);
    }

    public void RedefinedAddrMode(Token prevdecl, Token newdecl) {
        redefined("AddrMode", "Addressing mode", prevdecl, newdecl);
    }

    public void RedefinedAddrModeSet(Token prevdecl, Token newdecl) {
        redefined("AddrModeSet", "Addressing mode set", prevdecl, newdecl);
    }

    public void RedefinedLocal(Token var) {
        String report = "Local variable" + ' ' + StringUtil.quote(var.image) + " already defined in this scope";
        error("RedefinedLocal", var.getSourcePoint(), report, var.image);
    }

    public void RedefinedOperand(Token prevdecl, Token newdecl) {
        redefined("Operand", "Operand", prevdecl, newdecl);
    }

    public void RedefinedOperandType(Token prevdecl, Token newdecl) {
        redefined("OperandType", "Operand Type", prevdecl, newdecl);
    }

    public void RedefinedSubroutine(Token prevdecl, Token newdecl) {
        redefined("Subroutine", "Subroutine", prevdecl, newdecl);
    }

    public void RedefinedSymbol(Token prevdecl, Token newdecl) {
        redefined("Symbol", "Symbol", prevdecl, newdecl);
    }

    public void CannotComputeSizeOfVariable(Token t) {
        String report = "Cannot compute size of variable "+StringUtil.quote(t.image);
        error("CannotComputeSizeOfVariable", t.getSourcePoint(), report);
    }

    public void CannotComputeSizeOfExpression(Expr e) {
        String report = "Cannot compute size of expression";
        error("CannotComputeSizeOfExpression", e.getSourcePoint(), report);
    }

    public void CannotComputeSizeOfLiteral(Literal l) {
        String report = "Cannot compute size of literal "+StringUtil.quote(l.token);
        error("CannotComputeSizeOfLiteral", l.getSourcePoint(), report);
    }

    public void IntTypeExpected(String what, Expr e) {
        String report = "Integer type expected in " + what + ", found " + e.getType();
        error("IntTypeExpected", e.getSourcePoint(), report);
    }

    public void IntTypeTooLarge(Expr e, int expected, int recieved) {
	String report = "Integer type expecting at most " + expected 
	    + " bits, recieved " + recieved + " bits";
	error("IntTypeTooLarge", e.getSourcePoint(), report);
    }

    public void SignMismatch(Expr e, boolean expected, boolean recieved) {
	String s1 = expected ? "signed" : "unsigned";
	String s2 = recieved ? "signed" : "unsigned";
	String report = "Interger signs mismatch: expected " + s1 + 
	    ", recieved " + s2;
	error("SignMismatch", e.getSourcePoint(), report);
    }

    public void RangeOutOfBounds(Expr e, int low, int high, int limit) {
	String report = "Fixed range indices are out of bounds: [" + low + ':' + high + "]; should be between within [0:" + limit + ')';
	error("RangeOutOfBounds", e.getSourcePoint(), report);
    }

    public void InvalidRange(Expr e, int low, int high) {
	String report = "Low index (" + low + ") should be less than high "
	    + "index (" + high + ')';
	error("InvalidRange", e.getSourcePoint(), report);
    }

    public void UnresolvedOperator(Token op, Type lt, Type rt) {
        String report = "Unresolved operator "+StringUtil.quote(op)+ " on types ("+lt.getTypeCon()+ ',' +rt.getTypeCon()+ ')';
        error("UnresolvedOperator", op.getSourcePoint(), report);
    }

    public void UnresolvedOperator(Token op, Type lt) {
        String report = "Unresolved operator "+StringUtil.quote(op)+ " on type ("+lt.getTypeCon()+ ')';
        error("UnresolvedOperator", op.getSourcePoint(), report);
    }

    public void NotAnLvalue(Expr e) {
        String report = "not an lvalue";
        error("NotAnLvalue", e.getSourcePoint(), report);
    }

    public void TypeDoesNotSupportIndex(Expr e) {
        String report = "type "+e.getType()+" does not support index operation";
        error("TypeDoesNotSupportIndex", e.getSourcePoint(), report);
    }

    public void ValueTypeExpected(TypeRef tr) {
        AbstractToken token = tr.getToken();
        String report = "Value type expected, found "+token;
        error("ValueTypeExpected", token.getSourcePoint(), report);
    }
    
    public void CompoundTypeExpected(Expr e) {
	String report = "Compound Operand type expected, found " + e.getType();
	error("CompoundTypeExpected", e.getSourcePoint(), report);
    }

    public void ReturnStmtNotInSubroutine(ReturnStmt s) {
        String report = "Return statment not in subroutine";
        error("ReturnStmtNotInSubroutine", s.expr.getSourcePoint(), report);
    }

    public void OperandTypeExpected(Token o, Type t) {
        String report = "Operand type expected, found "+t.toString();
        error("OperandTypeExpected", o.getSourcePoint(), report);
    }

    public void UnresolvedAccess(String access, OperandTypeDecl ot, Token w, Type t) {
        String report = operandName(ot) +" has no "+w+" method";
        if ( t != null ) report += " with type "+StringUtil.quote(t);
        error("Unresolved"+access+"Method", w.getSourcePoint(), report);
    }

    public void AmbiguousAccess(String access, OperandTypeDecl ot, Token w) {
        String report = operandName(ot) +" has multiple "+w+" methods";
        error("Ambiguous"+access, w.getSourcePoint(), report);
    }

    public void TypeCannotBeConverted(Expr e, Type t) {
        String report = "expression of type "+e.getType()+" cannot be converted to type "+t;
        error("TypeCannotBeConverted", e.getSourcePoint(), report);
    }

    private String operandName(OperandTypeDecl ot) {
        if ( ot.isUnion() )
            return "Polymorphic operand type "+ StringUtil.quote(ot.name);
        else
            return "Operand type "+ StringUtil.quote(ot.name);
    }
}
