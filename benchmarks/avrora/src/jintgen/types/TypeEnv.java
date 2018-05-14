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
 * Creation date: Sep 28, 2005
 */

package jintgen.types;

import java.util.HashMap;
import java.util.HashSet;

/**
 * The <code>TypeEnv</code> class represents a type environment that contains a
 * list of type constructors (<code>TypeCon</code> instances) and types
 * (<code>Type</code> instances). A new type environment can be constructed
 * and initialized with a language's initial type constructors (e.g. arrays
 * and function types) and populated later with types declared by the program.
 *
 * @author Ben L. Titzer
 */
public abstract class TypeEnv {

    protected final TypeEnv parent;
    public final TypeErrorReporter ERROR;
    protected final HashMap<String, TypeCon> typeCons;
    protected final HashMap<String, Relation> relations;
    protected final HashMap<Tuple3<String, TypeCon, TypeCon>, TypeCon.BinOp> binops;
    protected final HashMap<Tuple2<String, TypeCon>, TypeCon.UnOp> unops;

    public void addTypeCon(TypeCon tc, Relation... rels) {
        addGlobalTypeCon(tc);
        for ( Relation r : rels ) r.add(tc, tc);
    }

    /**
     * The <code>Relation</code> class represents a relation between type constructors. A relation
     * may represent assignability, castability, or implicit promotion rules. The relation
     * can be thought of as a map from <code>(TypeCon x TypeCon) -> Bool</code> or as a directed
     * graph connected <code>TypeCon</code> instances together.
     */
    public static class Relation {
        protected final String name;
        protected final HashMap<TypeCon, Node> nodes;

        protected static class Node {
            protected final TypeCon typeCon;
            protected final HashSet<Node> neighbors;
            Node(TypeCon tc) {
                typeCon = tc;
                neighbors = new HashSet<Node>();
            }
        }

        public Relation(String nm) {
            name = nm;
            nodes = new HashMap<TypeCon, Node>();
        }

        public void add(TypeCon a, TypeCon b) {
            nodeOf(a).neighbors.add(nodeOf(b));
        }

        public boolean contains(TypeCon a, TypeCon b) {
            Node n = nodes.get(a);
            if ( n == null ) return false;
            Node m = nodes.get(b);
            if ( m == null ) return false;
            return n.neighbors.contains(m);
        }

        protected Node nodeOf(TypeCon a) {
            Node node = nodes.get(a);
            if ( node == null ) {
                node = new Node(a);
                nodes.put(a, node);
            }
            return node;
        }
    }

    /**
     * The <code>TransitiveRelation</code> class represents a relation between type constructors
     * that is transitive (i.e. A X B and B X C implies A X C). A relation
     * may represent assignability, castability, or implicit promotion rules. The relation
     * can be thought of as a map from <code>(TypeCon x TypeCon) -> Bool</code> or as a directed
     * graph connected <code>TypeCon</code> instances together.
     */
    public static class TransitiveRelation extends Relation {
        public TransitiveRelation(String nm) {
            super(nm);
        }

        public boolean contains(TypeCon a, TypeCon b) {
            TypeEnv.Relation.Node n = nodes.get(a);
            if ( n == null ) return false;
            TypeEnv.Relation.Node m = nodes.get(b);
            if ( m == null ) return false;
            return search(n, n.neighbors, m);
        }

        protected boolean search(Node start, HashSet<Node> n, Node target) {
            if ( n.contains(target) ) return true;
            for ( Node nn : n ) {
                if ( nn != start ) {
                    if ( search(start, nn.neighbors, target) ) return true;
                }
            }
            return false;
        }
    }

    /**
     * The constructor for the <code>TypeEnv</code> class creates an internal map
     * used to store type constructors and types and allows them to be resolved
     * by name.
     */
    protected TypeEnv(TypeErrorReporter ter) {
        parent = null;
        ERROR = ter;
        typeCons = new HashMap<String, TypeCon>();
        relations = new HashMap<String, Relation>();
        binops = new HashMap<Tuple3<String, TypeCon, TypeCon>, TypeCon.BinOp>();
        unops = new HashMap<Tuple2<String, TypeCon>, TypeCon.UnOp>();
    }

    /**
     * The <code>resolveTypeCon</code> method looks up a type constructor by its
     * unique string name.
     * @param name the name of the type constructor as a string
     * @return a reference to a <code>TypeCon</code> instance representing the
     * type constructor if it exists; null otherwise
     */
    public TypeCon resolveTypeCon(String name) {
        TypeCon typeCon = typeCons.get(name);
        if ( typeCon == null && parent != null ) return parent.resolveTypeCon(name);
        return typeCon;
    }

    /**
     * The <code>addLocalTypeCon()</code> method adds a new, named, type constructor
     * to this type environment. This method is used in the initialization of
     * a language's type environment (e.g. to add type constructors corresponding
     * to arrays, functions, etc) and during the processing of a program to
     * add user-defined types and type constructors to this environment
     * @param tc the new type constructor to add to this type environment
     */
    public void addLocalTypeCon(TypeCon tc) {
        typeCons.put(tc.getName(), tc);
    }

    /**
     * The <code>addGlobalTypeCon()</code> method adds a new, named, type constructor
     * to this type environment. This method is used in the initialization of
     * a language's type environment (e.g. to add type constructors corresponding
     * to arrays, functions, etc) and during the processing of a program to
     * add user-defined types and type constructors to this environment
     * @param tc the new type constructor to add to this type environment
     */
    public void addGlobalTypeCon(TypeCon tc) {
        if ( parent != null ) parent.addGlobalTypeCon(tc);
        else typeCons.put(tc.getName(), tc);
    }

    public void addRelation(Relation r) {
        relations.put(r.name, r);
    }

    public void addBinOp(TypeCon a, TypeCon b, TypeCon.BinOp binop) {
        String op = binop.getOperation();
        binops.put(new Tuple3<String, TypeCon, TypeCon>(op, a, b), binop);
    }

    public void addUnOp(TypeCon a, TypeCon.UnOp unop) {
        String op = unop.getOperation();
        unops.put(new Tuple2<String, TypeCon>(op, a), unop);
    }

    public TypeCon.BinOp resolveBinOp(Type a, Type b, String op) {
        return resolveBinOp(a.getTypeCon(), b.getTypeCon(), op);
    }

    public TypeCon.BinOp resolveBinOp(TypeCon a, TypeCon b, String op) {
        return binops.get(new Tuple3<String, TypeCon, TypeCon>(op, a, b));
    }

    public TypeCon.UnOp resolveUnOp(Type a, String op) {
        return resolveUnOp(a.getTypeCon(), op);
    }

    public TypeCon.UnOp resolveUnOp(TypeCon a, String op) {
        return unops.get(new Tuple2<String, TypeCon>(op, a));
    }
}
