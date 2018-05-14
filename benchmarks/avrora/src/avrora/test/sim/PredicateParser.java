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
 * Creation date: Nov 29, 2005
 */

package avrora.test.sim;

import cck.text.StringUtil;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The <code>PredicateParser</code> class implements a simple recursive-descent parser for the
 * expression language used to specify the initial state and the final state of a simulation.
 *
 * @author Ben L. Titzer
 */
public class PredicateParser {

    public List parseInitializers(String init) throws Exception {
        List inits = new LinkedList();
        CharacterIterator i = new StringCharacterIterator(init);
        while (true) {
            StringUtil.skipWhiteSpace(i);
            Predicate s = readInit(i);
            // verboseln("parsed: "+s.left+" = "+s.right);
            StringUtil.skipWhiteSpace(i);
            inits.add(s);
            if (i.current() == CharacterIterator.DONE)
                break;
            else
                StringUtil.expectChar(i, ',');
        }
        return inits;
    }

    public List parseResult(String result) throws Exception {
        List predicates = new LinkedList();
        CharacterIterator i = new StringCharacterIterator(result);
        while (true) {
            Predicate s = readPredicate(i);
            // verboseln("parsed: "+s.left+" = "+s.right);
            StringUtil.skipWhiteSpace(i);
            predicates.add(s);
            if (i.current() == CharacterIterator.DONE)
                break;
            else
                StringUtil.expectChar(i, ',');
        }
        return predicates;
    }

    // expr = expr
    private Predicate readPredicate(CharacterIterator i) throws Exception {
        // verboseln("Predicate @ "+i.getIndex()+" > '"+i.current()+"'");
        StringUtil.skipWhiteSpace(i);
        TestExpr left = readExpr(i);
        StringUtil.skipWhiteSpace(i);
        StringUtil.expectChar(i, '=');
        TestExpr right = readExpr(i);

        return new Predicate(left, right);
    }

    // term = expr
    private Predicate readInit(CharacterIterator i) throws Exception {
        // verboseln("Predicate @ "+i.getIndex()+" > '"+i.current()+"'");
        StringUtil.skipWhiteSpace(i);
        TestExpr left = readTerm(i);
        StringUtil.skipWhiteSpace(i);
        StringUtil.expectChar(i, '=');
        TestExpr right = readExpr(i);

        return new Predicate(left, right);
    }

    // term (+/- term)*
    private TestExpr readExpr(CharacterIterator i) throws Exception {
        // verboseln("Expr @ "+i.getIndex()+" > '"+i.current()+"'");
        StringUtil.skipWhiteSpace(i);
        TestExpr t = readTerm(i);

        StringUtil.skipWhiteSpace(i);
        if (StringUtil.peekAndEat(i, '+'))
            t = new TestExpr.Add(t, readExpr(i));
        else if (StringUtil.peekAndEat(i, '-'))
            t = new TestExpr.Subtract(t, readExpr(i));

        return t;
    }

    // @label
    // ident
    // ident[expr]
    // const
    private TestExpr readTerm(CharacterIterator i) throws Exception {
        // verboseln("Term @ "+i.getIndex()+" > '"+i.current()+"'");
        TestExpr e;
        char c = i.current();

        if ( c == '@' ) { // label
            i.next();
            TestExpr.Var v = readIdent(i);
            return new TestExpr.Label(v.name);
        } else if (Character.isLetter(c)) {
            TestExpr.Var v = readIdent(i);
            if ( StringUtil.peekAndEat(i, '[') ) {
                TestExpr ind = readExpr(i);
                e = new TestExpr.Index(v.name, ind);
                StringUtil.expectChar(i, ']');
            } else e = v;
        } else if (Character.isDigit(c))
            e = readConst(i);
        else if (c == '-')
            e = readConst(i);
        else
            throw new Exception("invalid start of term @ " + i.getIndex());

        return e;
    }

    // ident
    private TestExpr.Var readIdent(CharacterIterator i) {
        StringBuffer buf = new StringBuffer(32);

        while (true) {
            char c = i.current();
            // verboseln("Ident @ "+i.getIndex()+" > "+squote(c));

            if (!Character.isLetterOrDigit(c) && c != '.') break;

            buf.append(c);
            i.next();
        }

        String name = buf.toString();
        return new TestExpr.Var(name);
    }

    // number
    private TestExpr readConst(CharacterIterator i) {
        return new TestExpr.Const(StringUtil.readIntegerValue(i));
    }

}
