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

import cck.util.Util;
import jintgen.jigir.*;
import java.util.*;

/**
 * @author Ben L. Titzer
 */
public class DeadCodeEliminator extends StmtRebuilder<DeadCodeEliminator.DefUseEnvironment> {

    Set<String> globals;

    protected class DefUseEnvironment {
        DefUseEnvironment parent;
        HashSet<String> dead;
        HashSet<String> alive;

        DefUseEnvironment(DefUseEnvironment parent) {
            this.parent = parent;
            alive = new HashSet<String>();
            dead = new HashSet<String>();
        }

        void use(String var) {
            alive.add(var);
            dead.remove(var);
        }

        void def(String var) {
            dead.add(var);
            alive.remove(var);
        }

        boolean isDead(String name) {
            if (alive.contains(name))
                return false;
            else if (dead.contains(name))
                return true;
            else if (parent != null) return parent.isDead(name);
            return true;
        }

        void mergeIntoParent(DefUseEnvironment sibling) {
            addLiveIns(sibling);
            addLiveIns(this);
            addDead(sibling);
        }

        private void addLiveIns(DefUseEnvironment sibling) {
            for ( String s : sibling.alive ) {
                parent.alive.add(s);
            }
        }

        private void addDead(DefUseEnvironment sibling) {
            for ( String s : dead ) {
                // dead on both branches
                if (sibling.dead.contains(s)) {
                    parent.alive.remove(s);
                    parent.dead.add(s);
                }
            }
        }
    }

    public DeadCodeEliminator(Set<String> globals) {
        this.globals = globals;
    }

    public List<Stmt> process(List<Stmt> stmts) {
        DefUseEnvironment du = new DefUseEnvironment(null);
        du.alive.addAll(globals);
        return visitStmtList(stmts, du);
    }

    public List<Stmt> visitStmtList(List<Stmt> l, DefUseEnvironment denv) {
        Collections.reverse(l);
        List<Stmt> nl = new LinkedList<Stmt>();
        boolean changed = false;

        for ( Stmt sa : l ) {
            Stmt na = sa.accept(this, denv);
            if (na == null) {
                changed = true;
                continue;
            }
            if (na != sa) changed = true;
            nl.add(na);
        }

        if (changed) {
            Collections.reverse(nl);
            return nl;
        }
        Collections.reverse(l);
        return l;
    }

    public Stmt visit(IfStmt s, DefUseEnvironment denv) {
        DefUseEnvironment tenv = new DefUseEnvironment(denv);
        List<Stmt> nt = visitStmtList(s.trueBranch, tenv);
        DefUseEnvironment fenv = new DefUseEnvironment(denv);
        List<Stmt> nf = visitStmtList(s.falseBranch, fenv);

        tenv.mergeIntoParent(fenv);

        Expr nc = s.cond.accept(this, denv);

        if (nc != s.cond || nt != s.trueBranch || nf != s.falseBranch)
            return new IfStmt(nc, nt, nf);
        else
            return s;
    }

    public Stmt visit(DeclStmt s, DefUseEnvironment denv) {
        if (denv.isDead(s.name.toString())) return null;

        denv.def(s.name.toString());

        s.init.accept(this, denv);
        return s;
    }

    public Stmt visit(AssignStmt s, DefUseEnvironment denv) {
        throw Util.unimplemented();
    }

    public Expr visit(VarExpr e, DefUseEnvironment denv) {
        denv.use(e.variable.toString());
        return e;
    }

}
