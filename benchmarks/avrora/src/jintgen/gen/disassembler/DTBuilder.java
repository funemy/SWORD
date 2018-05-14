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

import cck.text.Verbose;
import cck.util.Util;
import java.util.*;

/**
 * The <code>DTBuilder</code> class represents a node in the decoding tree for a particular
 * architecture. Each node in the decoding tree matches on a contiguous field of bits within
 * the instruction's encoding. The value of these bits are used to determine which child
 * node the matching should continue to.
 * Each node has a starting bit (left_bit) and an ending bit (right_bit) that
 * denote the start and end of the bit field that this node matches on.
 *
 * @author Ben L. Titzer
 */
public class DTBuilder {

    class EncodingSet {
        HashSet<EncodingInst> lowPrio = new HashSet<EncodingInst>();
        HashSet<EncodingInst> highPrio = new HashSet<EncodingInst>();
        HashMap<Integer, EncodingSet> children = new HashMap<Integer, EncodingSet>();
        private int minlength = 128;
        private int minprio = 128;
        private int maxprio = 0;
        int left_bit;
        int right_bit;

        int[] prio;
        int[] length;
        int[] counts;

        void addEncoding(EncodingInst ei) {
            lowPrio.add(ei);
            int prio = ei.encoding.getPriority();
            int length = ei.getLength();
            if ( length < minlength ) minlength = length;
            if ( prio < minprio ) minprio = prio;
            if ( prio > maxprio ) maxprio = prio;
        }

        int[] newPriorityArray() {
            int[] prio = new int[minlength];
            Arrays.fill(prio, maxprio);
            return prio;
        }

        int[] newLengthArray() {
            int[] la = new int[minlength];
            for ( int cntr = 0; cntr < minlength; cntr++ ) {
                la[cntr] = minlength - cntr;
            }
            return la;
        }

        void buildArrays() {
            prio = newPriorityArray();
            length = newLengthArray();
            counts = new int[minlength];
        }

        DTNode build() {
            HashMap<Integer, DTNode> c = new HashMap<Integer, DTNode>();
            for ( Map.Entry<Integer, EncodingSet> e : children.entrySet() ) {
                c.put(e.getKey(), e.getValue().build());
            }
            int num = (nodeNumber++);
            String label = "node"+num;
            DTNode node = new DTNode(label, left_bit, right_bit, c);
            node.number = num;
            node.encodings.addAll(lowPrio);
            node.encodings.addAll(highPrio);
            return node;
        }
    }

    int nodeNumber;
    EncodingSet root;
    private Verbose.Printer verbose;

    public DTBuilder() {
        verbose = Verbose.getVerbosePrinter("jintgen.disassem");
        root = new EncodingSet();
    }

    public void addEncoding(EncodingInst ei) {
        root.addEncoding(ei);
    }

    void computeRange(EncodingSet set, int depth) {
        // there should be at least one encoding in every decoding tree
        assert !set.lowPrio.isEmpty();

        set.buildArrays();
        verbose.println("--> scanning...");
        // for each encoding, increment the count of each concrete bit
        for ( EncodingInst ei : set.lowPrio ) {
            if ( ei.encoding.getPriority() == set.minprio ) set.highPrio.add(ei);
            ei.printVerbose(depth, verbose);
            // scan for the most lucrative bit range
            scanForBitRange(set, ei);
        }

        // scan from the left for the bit that is most often concrete
        int max = getLeftBit(set);

        verbose.println("result: decode["+set.left_bit+ ':' +set.right_bit+ ']');

        // problem: no encodings have any concrete bits left
        if ( max == 0 ) {
            if ( set.highPrio.size() > 1 ) {
                DGUtil.ambiguous(set.highPrio);
            } else {
                set.left_bit = set.right_bit = 0;
            }
        }

        // remove all the high priority encodings from the main encoding set
        set.lowPrio.removeAll(set.highPrio);
    }

    int getLeftBit(EncodingSet set) {
        int max = 0;
        for ( int cntr = 0; cntr < set.minlength; cntr++ ) {
            int count = set.counts[cntr];
            // only select bit ranges that are concrete in the highest priority level
            if ( set.prio[cntr] == set.minprio && count > max ) {
                set.left_bit = cntr;
                max = count;
            }
        }
        assert set.length[set.left_bit] > 0;
        set.right_bit = set.left_bit + set.length[set.left_bit] - 1;
        return max;
    }

    void scanForBitRange(EncodingSet set, EncodingInst ei) {
        int len = 1;
        int p = ei.encoding.getPriority();
        // scan backwards through the bit states. for each
        // concrete bit range, record the number of bits until it meets a non-concrete bit.
        // this limits the length of a concrete bit match so that all encodings with
        // that concrete bit set have the entire bit range set
        for ( int cntr = set.prio.length - 1; cntr >= 0; cntr--, len++ ) {
            if ( ei.isConcrete(cntr) ) {
                if ( len < set.length[cntr] ) set.length[cntr] = len;
                set.counts[cntr]++;
                if ( p < set.prio[cntr] ) set.prio[cntr] = p;
            } else {
                len = 0;
            }
        }
    }

    void createChildren(EncodingSet set) {
        List<EncodingInst> unmatched = new LinkedList<EncodingInst>();

        // if this node is a singleton, remove all but highest encoding
        if ( createSingleton(set) ) return;

        // create the main branches
        createMainBranches(set, unmatched);

        // create the default branch
        createDefaultBranch(set, unmatched);

        assert !set.children.isEmpty();
    }

    private boolean createSingleton(EncodingSet set) {
        if ( set.highPrio.size() == 1 ) {
            // get the encoding info of the singleton
            EncodingInst ei = set.highPrio.iterator().next();
            // if the left bit is not concrete, there are no bits left
            if ( !ei.isConcrete(set.left_bit) ) {
                // this node represents a terminal node (no children)
                // and overrides all of the lower priority encodings
                set.lowPrio.clear();
                return true;
            }
        }
        return false;
    }

    private void createMainBranches(EncodingSet set, List<EncodingInst> unmatched) {
        // all of the encodings at the high priority must have the bits set
        for ( EncodingInst ei : set.highPrio ) {
            if ( ei.isConcrete(set.left_bit) ) createChild(set, ei);
            else DGUtil.ambiguous(set.highPrio);
        }
        // for the rest of the encodings, add them to children or unmatched list
        for ( EncodingInst ei : set.lowPrio ) {
            if ( ei.isConcrete(set.left_bit) ) createChild(set, ei);
            else unmatched.add(ei);
        }
    }

    private void createDefaultBranch(EncodingSet set, List<EncodingInst> unmatched) {
        if (!unmatched.isEmpty() ) {
            // replicate the unmatched encodings over all branches
            for ( EncodingInst ei : unmatched ) {
                for ( EncodingSet c : set.children.values() )
                    c.addEncoding(ei.copy());
            }
            // if the tree is not complete, add a default branch
            if ( set.children.size() < 1 << (set.right_bit - set.left_bit + 1) ) {
                EncodingSet def = new EncodingSet();
                set.children.put(new Integer(-1), def);
                for ( EncodingInst ei : unmatched ) {
                    def.addEncoding(ei.copy());
                }
            }
        }
    }

    private void createChild(EncodingSet set, EncodingInst ei) {
        // get the value of the bits and add to corresponding subtree
        Integer iv = new Integer(extractValue(set, ei));
        EncodingSet c = set.children.get(iv);
        if ( c == null ) {
            c = new EncodingSet();
            set.children.put(iv, c);
        }
        c.addEncoding(ei);
    }

    private int extractValue(EncodingSet set, EncodingInst ei) {
        int value = 0;
        for ( int cntr = set.left_bit; cntr <= set.right_bit; cntr++ ) {
            byte bitState = ei.bitStates[cntr];
            switch (bitState) {
                case EncodingInst.ENC_ZERO:
                    value = value << 1;
                    ei.bitStates[cntr] = EncodingInst.ENC_MATCHED_ZERO;
                    break;
                case EncodingInst.ENC_ONE:
                    value = value << 1 | 1;
                    ei.bitStates[cntr] = EncodingInst.ENC_MATCHED_ONE;
                    break;
                default:
                    throw Util.failure("invalid bit state at "+cntr+" in "+ei);
            }
        }
        return value;
    }

    DTNode compute() {
        compute(root, 0);
        return root.build();
    }

    private void compute(EncodingSet set, int depth) {
        computeRange(set, depth);
        createChildren(set);
        for ( EncodingSet dt : set.children.values() ) {
            compute(dt, depth+1);
        }
    }
}
