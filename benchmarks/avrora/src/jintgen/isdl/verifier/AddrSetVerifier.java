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
 * Creation date: Nov 3, 2005
 */

package jintgen.isdl.verifier;

import jintgen.isdl.*;
import jintgen.isdl.parser.Token;
import jintgen.types.Type;
import java.util.*;

/**
 * @author Ben L. Titzer
 */
public class AddrSetVerifier extends VerifierPass {

    public AddrSetVerifier(ArchDecl arch) {
        super(arch);
    }

    public void verify() {

        for ( AddrModeSetDecl as: arch.addrSets ) {
            HashMap<String, OperandTypeDecl.Union> unions = new HashMap<String, OperandTypeDecl.Union>();
            HashSet<String> alloperands = new HashSet<String>();
            for ( Token t : as.list ) {
                AddrModeDecl am = arch.getAddressingMode(t.image);
                if ( am == null )
                    ERROR.UnresolvedAddressingMode(t);
                as.addrModes.add(am);
                am.joinSet(as);
                unifyAddressingMode(unions, am, as, alloperands, t);
            }

            buildOperandList(unions, as);
        }


        // build the accessor methods for unions
        for ( AddrModeSetDecl as : arch.addrSets ) {
            for ( AddrModeDecl.Operand o : as.unionOperands )
                computeAccessorUnion((OperandTypeDecl.Union)o.getOperandType());
        }
    }

    private void buildOperandList(HashMap<String, OperandTypeDecl.Union> unions, AddrModeSetDecl as) {
        // now that we verified the unification of the operands, create a list of operands for
        // this addressing mode with names and union types
        List<AddrModeDecl.Operand> operands = new LinkedList<AddrModeDecl.Operand>();
        for ( Map.Entry<String, OperandTypeDecl.Union> e : unions.entrySet() ) {
            Token n = new Token();
            n.image = e.getKey();
            OperandTypeDecl.Union unionType = e.getValue();
            OperandTypeRef t = new OperandTypeRef(unionType.name);
            t.resolve(typeEnv);
            AddrModeDecl.Operand operand = new AddrModeDecl.Operand(n, t);
            //operand.setOperandType(unionType);
            operands.add(operand);
        }

        as.unionOperands = operands;
    }

    private void computeAccessorUnion(OperandTypeDecl.Union union) {
        int goal = union.types.size();
        HashMap<Type, HashSet<OperandTypeDecl.AccessMethod>> rmeths = new HashMap<Type, HashSet<OperandTypeDecl.AccessMethod>>();
        HashMap<Type, HashSet<OperandTypeDecl.AccessMethod>> wmeths = new HashMap<Type, HashSet<OperandTypeDecl.AccessMethod>>();
        for ( OperandTypeDecl ot : union.types ) {
            addAccessorMethods(ot.readDecls, rmeths);
            addAccessorMethods(ot.writeDecls, wmeths);
        }
        addAccessors(rmeths, union.readAccessors, goal);
        addAccessors(wmeths, union.writeAccessors, goal);
    }

    private void addAccessors(HashMap<Type, HashSet<OperandTypeDecl.AccessMethod>> rmeths, HashMap<Type, OperandTypeDecl.Accessor> accessors, int goal) {
        for ( Map.Entry<Type, HashSet<OperandTypeDecl.AccessMethod>> e : rmeths.entrySet() ) {
            if (e.getValue().size() == goal) {
                // only unify "complete" accessor method sets
                OperandTypeDecl.PolymorphicAccessor polymorphicAccessor = new OperandTypeDecl.PolymorphicAccessor(e.getKey());
                for ( OperandTypeDecl.AccessMethod m : e.getValue() )
                    polymorphicAccessor.addTarget(m);
                accessors.put(e.getKey(), polymorphicAccessor);
            }
        }
    }

    private void addAccessorMethods(List<OperandTypeDecl.AccessMethod> entries, HashMap<Type, HashSet<OperandTypeDecl.AccessMethod>> rmeths) {
        for ( OperandTypeDecl.AccessMethod m : entries ) {
            Type key = m.typeRef.resolve(arch.typeEnv);
            m.type = key;
            HashSet<OperandTypeDecl.AccessMethod> set = rmeths.get(key);
            if ( set == null ) {
                set = new HashSet<OperandTypeDecl.AccessMethod>();
                rmeths.put(key, set);
            }
            set.add(m);
            m.usedPolymorphically = true;
        }
    }

    private void unifyAddressingMode(HashMap<String, OperandTypeDecl.Union> unions, AddrModeDecl am, AddrModeSetDecl as, HashSet<String> alloperands, Token t) {
        if ( as.addrModes.size() == 1 ) {
            // for the first addressing mode, put the union types in the map
            for ( AddrModeDecl.Operand o : am.operands ) {
                Token tok = new Token();
                tok.image = as.name.image+ '_' +o.name.image+"_union";
                OperandTypeDecl.Union ut = new OperandTypeDecl.Union(tok);
                ut.addType(o.typeRef.getOperandTypeDecl());
                unions.put(o.name.image, ut);
                alloperands.add(o.name.image);
                arch.typeEnv.addOperandType(ut);
            }
        } else {
            // for each operand in this addressing mode, check that it exists and add
            // it to the types to be unified.
            HashSet<String> operands = new HashSet<String>();
            for ( AddrModeDecl.Operand o : am.operands ) {
                OperandTypeDecl.Union ut = unions.get(o.name.image);
                if ( ut == null )
                    ERROR.ExtraOperandInAddrModeUnification(as.name, t, o.name);

                ut.addType(o.typeRef.getOperandTypeDecl());
                operands.add(o.name.image);
            }
            if ( !operands.containsAll(alloperands) ) {
                alloperands.removeAll(operands);
                String oneop = alloperands.iterator().next();
                ERROR.MissingOperandInAddrModeUnification(as.name, t, oneop);
            }
        }
    }

}
