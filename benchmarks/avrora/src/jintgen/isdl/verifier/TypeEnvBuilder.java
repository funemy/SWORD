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
import jintgen.jigir.JIGIRTypeEnv;
import jintgen.types.Type;
import jintgen.types.TypeCon;

/**
 * @author Ben L. Titzer
 */
public class TypeEnvBuilder extends VerifierPass {

    public TypeEnvBuilder(ArchDecl arch) {
        super(arch);
    }

    public void verify() {
        uniqueCheck("UserType", "User type", arch.userTypes);
        uniqueCheck("Subroutine", "Subroutine", arch.subroutines);
        uniqueCheck("Global", "Global", arch.globals);

        addUserTypes();       // add the enum and operand types to the type env
        computeEnumTypes();   // compute enum representation types
        resolveEnumParents(); // resolve parent types for all subsets
        buildGlobalEnv();     // build the environment with all global vars
        addConversions();     // add conversions to type environment
    }

    void addUserTypes() {
        // add the enum to the type env and remember its kind
        for ( EnumDecl d : arch.enums ) {
            d.kind = typeEnv.addEnum(d);
            arch.globalEnv.addDecl(d);
        }
        // add each operand type to the type environment
        for ( OperandTypeDecl d : arch.operandTypes ) {
            typeEnv.addOperandType(d);
        }
    }

    void resolveEnumParents() {
        for ( EnumDecl ed : arch.enums ) {
            if (ed instanceof EnumDecl.Subset) {
                // for each subset declaration, resolve the parent type
                EnumDecl.Subset dd = (EnumDecl.Subset)ed;
                dd.parentRef.resolve(typeEnv);
                EnumDecl parent = dd.getParent();
                TypeCon tc = arch.typeEnv.resolveTypeCon(ed.name.image);
                TypeCon ptc = arch.typeEnv.resolveTypeCon(parent.name.image);
                arch.typeEnv.ASSIGNABLE.add(tc, ptc);
                arch.typeEnv.COMPARABLE.add(tc, ptc);
                arch.typeEnv.COMPARABLE.add(ptc, tc);
                dd.setRepType(parent.getRepType());
            }
        }
    }

    void computeEnumTypes() {
        for ( EnumDecl ed : arch.enums ) {
            if (!(ed instanceof EnumDecl.Subset)) {
                // for each enum declaration, compute the representation type
                boolean first = true;
                int min = 0;
                int max = 0;
                // find the maximum and minimum symbol values
                for ( SymbolMapping.Entry e : ed.map.getEntries() ) {
                    if ( first ) {
                        min = max = e.value;
                    } else {
                        if ( e.value > max ) max = e.value;
                        if ( e.value < min ) min = e.value;
                    }
                    first = false;
                }
                // compute the types of the maximum and minimum values
                Type t = computeRepType(min, max);
                ed.setRepType(t);
            }
        }
    }

    Type computeRepType(int min, int max) {
        JIGIRTypeEnv.TYPE_int mint = TypeChecker.getTypeOfLiteral(typeEnv, min);
        JIGIRTypeEnv.TYPE_int maxt = TypeChecker.getTypeOfLiteral(typeEnv, max);
        // if the sign bits are the same, return the greater
        if ( maxt.isSigned() == mint.isSigned() )
            return maxt.getSize() > mint.getSize() ? maxt : mint;
        else {
            // min type is signed, max type is not signed (because max val > min val)
            if ( mint.getSize() > maxt.getSize() ) return mint;
            else return typeEnv.newIntType(true, maxt.getSize() + 1);
        }
    }

    void buildGlobalEnv() {
        for ( GlobalDecl d : arch.globals ) {
            // resolve the type and add to global environment
            d.typeRef.resolve(arch.typeEnv);
            arch.globalEnv.addDecl(d);
        }
        for ( SubroutineDecl d : arch.subroutines ) {
            // resolve all the types of parameters
            for ( SubroutineDecl.Parameter p : d.getParams() ) p.type.resolve(typeEnv);
            // resolve the return type
            d.ret.resolve(arch.typeEnv);
            // add to the global type env
            arch.globalEnv.addSubroutine(d);
        }
    }

    void addConversions() {
        for ( EnumDecl d: arch.enums ) {
            typeEnv.CONVERTIBLE.add(typeEnv.resolveTypeCon(d.name.image), d.getRepType().getTypeCon());
        }
    }
}
