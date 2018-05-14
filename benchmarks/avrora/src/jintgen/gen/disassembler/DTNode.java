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

import java.util.*;

/**
 * The <code>DTNode</code> class represents a node in a decoding tree or graph.
 * An instance of this class requires that its children be created first; thus
 * it is not directly possible to create a cyclic reference graph.
 *
 * Once created, a <code>DTNode</code> instance represents an immutable part
 * of the subgraph. Neither it nor its subgraph will be altered.
 *
 * The <code>DTNode</code> instance can, however, be relabelled and renumbered.
 * Since labels are considered for equality and hash code computation purposes,
 * hashcodes may be recomputed. Thus membership in sets and maps will be stale
 * after relabelling.
 *
 * @author Ben L. Titzer
 */
public class DTNode {

    public static Comparator<Map.Entry<Integer, DTNode>> EDGE_COMPARATOR = new Comparator<Map.Entry<Integer, DTNode>>() {
        public int compare(Map.Entry<Integer, DTNode> a, Map.Entry<Integer, DTNode> b) {
            return a.getKey() - b.getKey();
        }
    };

    public final int left_bit;
    public final int right_bit;

    public int number;

    public final HashSet <EncodingInst> encodings;

    private final HashMap<Integer, DTNode> children;
    private boolean needHash = true;
    private int hashCode;
    private String label;

    /**
     * The constructor for the <code>DTNode</code> class creates a new node instance with the
     * specified left and right bits and specified children. The implementation expects that the
     * children map will NOT be altered following construction of this node. Furthermore, this
     * implementation protects the children map from being altered. In essence, this class
     * enforces a fixed topology subgraph upon creation. Labelling and numbering can be
     * altered, however.
     * @param l the initial string label of the node
     * @param lb the (logical) left bit of the comparison
     * @param rb the (logical) right bit of the comparison
     * @param c the map which maps values of the bits to successor nodes
     */
    public DTNode(String l, int lb, int rb, HashMap<Integer, DTNode> c) {
        label = l;
        left_bit = lb;
        right_bit = rb;
        children = c;
        encodings = new HashSet<EncodingInst>();
    }

    /**
     * The <code>setLabel()</code> method relabels this node. This causes the hash code to be changed
     * lazily.
     * @param l the new label for this node
     */
    public void setLabel(String l) {
        label = l;
        needHash = true;
    }

    /**
     * The <code>getLabel()</code> method returns the string label of this node.
     * @return a string representing the label of this node
     */
    public String getLabel() {
        return label;
    }

    /**
     * The <code>hashCode()</code> method computes the hash code of this node. The hash code is
     * derived from the label and the hash codes of the children nodes. Thus, when the label is
     * changed, the hash code changes. Care should be taken to avoid stale sets and maps when
     * altering the labels of a tree.
     * @return an integer hash code for this node
     */
    public int hashCode() {
        if ( needHash ) {
            if ( label != null ) hashCode = label.hashCode();
            for ( DTNode n : children.values() ) hashCode += n.hashCode() * 37;
            needHash = false;
        }
        return hashCode;
    }

    /**
     * The <code>equals()</code> method compares two <code>DTNode</code> instances. Two nodes are
     * considered equal if they share the same label, left and right bits, and all children are
     * equal (by reference equality only). This definition of equals is therefore convenient
     * for recursive subtree equality testing, and sufficient for hash sets and topological orders.
     * @param o the object to test equality against
     * @return true if the nodes are equal; false otherwise
     */
    public boolean equals(Object o) {
        if ( !(o instanceof DTNode) ) return false;
        if ( o == this ) return true;
        DTNode n = (DTNode)o;
        if ( n.left_bit != left_bit ) return false;
        if ( n.right_bit != right_bit ) return false;
        if ( !n.label.equals(label) ) return false;
        if ( children.size() != n.children.size() ) return false;
        for ( Map.Entry<Integer, DTNode> e : children.entrySet() ) {
            int value = e.getKey();
            DTNode cdt = e.getValue();
            DTNode odt = n.children.get(value);
            if ( cdt != odt ) return false;
        }
        return true;
    }

    /**
     * The <code>isLeaf()</code> method returns true if this node has no children.
     * @return true if this node has no children; false otherwise
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * The <code>isTree()</code> method checks whether the subgraph rooted at this node is a tree, according
     * to the set passed, which contains nodes already present in this graph. This node and all of its subnodes
     * will be added to the set.
     * @param set the set of nodes already in the graph
     * @return true if this node is a tree with respect to the given set; false otherwise
     */
    public boolean isTree(Set<DTNode> set) {
        for ( DTNode n : children.values() )
            if ( !n.isTree(set) ) return false;
        if ( set.contains(this) ) return false;
        set.add(this);
        return true;
    }

    /**
     * The <code>getChildren()</code> method returns a collection of all the children nodes of this
     * node.
     * @return a collection of the children of this node
     */
    public Collection<DTNode> getChildren() {
        return children.values();
    }

    /**
     * The <code>getEdges()</code> method returns a collection of the edges that are outgoing from this
     * node, including the integer value and the <code>DTNode</code> target of the edge.
     * @return a collection of the edges outgoing from this node
     */
    public Collection<Map.Entry<Integer, DTNode>> getEdges() {
        return children.entrySet();
    }

    /**
     * The <code>getSortedEdges()</code> method returns a collection of the edges that are outgoing from
     * this node, including the integer value and the <code>DTNode</code> target of the edge. The edges
     * are sorted by the value of the integer on the edge.
     * @return a collection of the edges outgoing from this node, sorted by value
     */
    public Collection<Map.Entry<Integer, DTNode>> getSortedEdges() {
        Set<Map.Entry<Integer, DTNode>> set = children.entrySet();
        List<Map.Entry<Integer, DTNode>> children = new LinkedList<Map.Entry<Integer, DTNode>>(set);
        Collections.sort(children, EDGE_COMPARATOR);
        return children;
    }

    /**
     * The <code>addTopologicalOrder()</code> method recursively adds this node and its
     * subnodes to the specified list, provided that the nodes are not already in the
     * <code>seen</code> set that is passed as a parameter. For graphs that are not trees,
     * this will correctly add the nodes in topological order so that no node appears more
     * than once in the list.
     * @param list the list to add this node and its subnodes to
     * @param seen a set containing any nodes already in the topological list
     */
    public void addTopologicalOrder(List<DTNode> list, Set<Integer> seen) {
        if ( seen.contains(number) ) return;
        seen.add(number);
        for ( DTNode n : children.values() )
            n.addTopologicalOrder(list, seen);
        list.add(this);
    }

    /**
     * The <code>addPreOrder()</code> method recursively adds this node and its
     * subnodes to the specified list, provided that the nodes are not already in the
     * <code>seen</code> set that is passed as a parameter. For graphs that are not trees,
     * this will correctly add the nodes in pre-order so that no node appears more
     * than once in the list.
     * @param list the list to add this node and its subnodes to
     * @param seen a set containing any nodes already in the pre-order list
     */
    public void addPreOrder(List<DTNode> list, Set<Integer> seen) {
        if ( seen.contains(this) ) return;
        seen.add(number);
        list.add(this);
        for ( DTNode n : children.values() )
            n.addTopologicalOrder(list, seen);
    }

    /**
     * The <code>addTopologicalOrder()</code> method recursively adds this node and its
     * subnodes to the specified list, regardless of whether these nodes already appear
     * in the list. Thus, if the graph rooted at this node is not a tree, or if it references
     * nodes already in the list, the nodes will appear multiple times in the resulting list.
     * @param list the list to which to add this node and its subnodes
     */
    public void addTopologicalOrder(List<DTNode> list) {
        for ( DTNode n : children.values() )
            n.addTopologicalOrder(list);
        list.add(this);
    }

    /**
     * The <code>deepCopy()</code> method creates a complete duplicate of this DTNode and its
     * children. It recursively creates the deep copies of all the children.
     * @return a new <code>DTNode</code> instance that is equivalent to this one, with all
     * children copied as well
     */
    public DTNode deepCopy() {
        HashMap<DTNode, DTNode> nn = new HashMap<DTNode, DTNode>();
        return deepCopy(nn);
    }

    private DTNode deepCopy(HashMap<DTNode, DTNode> nn) {
        DTNode newThis = nn.get(this);
        if ( newThis == null ) {
            // create a new map for the children
            HashMap<Integer, DTNode> nc = new HashMap<Integer, DTNode>();
            for ( Map.Entry<Integer, DTNode> e : children.entrySet() ) {
                // recursively create / get a new child instance
                DTNode newC = e.getValue().deepCopy(nn);
                nc.put(e.getKey(), newC);
            }
            // create a new shallow copy of this node
            newThis = shallowCopy(nc);
            // put it in the map so it is not duplicated over and over
            nn.put(this, newThis);
        }
        return newThis;
    }

    public DTNode shallowCopy(HashMap<Integer, DTNode> nc) {
        DTNode newThis = new DTNode(label, left_bit, right_bit, nc);
        newThis.number = number;
        newThis.label = label;
        newThis.encodings.addAll(encodings);
        return newThis;
    }

    public DTNode shallowCopy(int left_bit, int right_bit, HashMap<Integer, DTNode> nc) {
        DTNode newThis = new DTNode(label, left_bit, right_bit, nc);
        newThis.number = number;
        newThis.label = label;
        newThis.encodings.addAll(encodings);
        return newThis;
    }
}
