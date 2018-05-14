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

import java.util.HashMap;
import java.util.Map;

/**
 * The <code>TreeFactorer</code> class implements a factoring algorithm that reduces
 * the size of a decoding tree and turns it into directed acyclic graph. It does this
 * by recursively rebuilding the tree and caching equivalent subtrees. The algorithm
 * guarantees that the new DAG that is created will be equivalent.
 *
 * @author Ben L. Titzer
 */
public class TreeFactorer {

    final DTNode oldRoot;
    DTNode newRoot;
    final HashMap<Long, HashMap<DTNode, DTNode>> pathMap;

    public TreeFactorer(DTNode dt) {
        oldRoot = dt;
        pathMap = new HashMap<Long, HashMap<DTNode, DTNode>>();
    }

    /**
     * The <code>getNewTree()</code> method rebuilds the tree and returns a reference to the new
     * root.
     * @return a reference to the new root of the tree
     */
    public DTNode getNewTree() {
        if ( newRoot == null ) newRoot = rebuild(0, oldRoot);
        return newRoot;
    }

    private DTNode rebuild(long state, DTNode n) {
        long nstate = DGUtil.getBitStates(state, n);
        // rebuild each of the children
        HashMap<Integer, DTNode> nc = new HashMap<Integer, DTNode>();
        for ( Map.Entry<Integer, DTNode> e : n.getEdges() ) {
            int value = e.getKey();
            DTNode child = e.getValue();
            DTNode nchild = rebuild(nstate, child);
            nc.put(new Integer(value), nchild);
        }

        DTNode nn = n.shallowCopy(nc);
        DTNode prev = getPrevNode(state, nn);
        if ( prev != null ) return prev;

        // cache this node and (and its subgraph)
        addNewNode(state, nn);
        return nn;
    }

    private void addNewNode(long state, DTNode ndt) {
        HashMap<DTNode, DTNode> set = getTreeMap(state);
        set.put(ndt, ndt);
    }

    private HashMap<DTNode, DTNode> getTreeMap(long state) {
        HashMap<DTNode, DTNode> set = pathMap.get(state);
        if ( set == null ) {
            set = new HashMap<DTNode, DTNode>();
            pathMap.put(state, set);
        }
        return set;
    }

    private DTNode getPrevNode(long state, DTNode dt) {
        HashMap<DTNode, DTNode> set = getTreeMap(state);
        return set.get(dt);
    }
}
