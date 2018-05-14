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

package jintgen.gen.disassembler;

import cck.text.Printer;
import cck.util.Util;
import jintgen.isdl.*;
import java.io.*;
import java.util.*;

/**
 * @author Ben L. Titzer
 */
public class DisassemblerTestGenerator{

    ArchDecl archDecl;
    File directory;
    String dname;
    Printer printer;
    HashMap<OperandTypeDecl, ValueSet> operandValues;

    public abstract class ValueSet {

        abstract void getRepresentative(String name, HashMap<String, String> props);
        abstract void enumerate(InstrDecl d, String syntax, String name, HashMap<String, String> props);
    }

    class SimpleValues extends ValueSet {
        final List<String> list;
        SimpleValues() {
            list = new LinkedList<String>();
        }
        void getRepresentative(String name, HashMap<String, String> props) {
            props.put(name, list.iterator().next());
        }
        void enumerate(InstrDecl d, String syntax, String name, HashMap<String, String> props) {
            for ( String str : list ) {
                props.put(name, str);
                generate(d, syntax, props);
            }
        }
    }

    class CompoundValues extends ValueSet {
        List<HashMap<String, String>> values;
        CompoundValues() {
            values = new LinkedList<HashMap<String, String>>();
        }
        void getRepresentative(String name, HashMap<String, String> props) {
            HashMap<String, String> map = values.iterator().next();
            putSubValues(map, props, name);
        }

        private void putSubValues(HashMap<String, String> map, HashMap<String, String> props, String name) {
            for ( Map.Entry<String, String> e: map.entrySet() ) {
                props.put(name+ '.' +e.getKey(), e.getValue());
            }
        }

        void enumerate(InstrDecl d, String syntax, String name, HashMap<String, String> props) {
            for ( HashMap<String, String> map : values ) {
                putSubValues(map, props, name);
                generate(d, syntax, props);
            }
        }
    }

    public DisassemblerTestGenerator(ArchDecl a, File dir) {
        archDecl = a;
        if ( !dir.isDirectory() )
            Util.userError("must specify directory for testcases");
        dname = dir.getAbsolutePath();
        directory = dir;
    }

    public void generate() {
        for ( OperandTypeDecl d : archDecl.operandTypes ) getOperandValueSet(d);
        for ( InstrDecl d : archDecl.instructions ) visit(d);
    }

    ValueSet getOperandValueSet(OperandTypeDecl o) {
        ValueSet vs = operandValues.get(o);
        if ( vs != null ) return vs;
        if ( o.isCompound() ) {
            CompoundValues cv = new CompoundValues();
            operandValues.put(o, cv);
            OperandTypeDecl.Compound ct = (OperandTypeDecl.Compound)o;
            return cv;
        } else {
            SimpleValues sv = new SimpleValues();
            operandValues.put(o, sv);
            sv.list.add("0");
            sv.list.add("11");
            return sv;
        }
    }

    public void visit(InstrDecl d) {
        for ( AddrModeDecl am : d.addrMode.addrModes ) {
            Property p = am.getProperty("syntax");
            if ( p == null ) p = d.getProperty("syntax");
            String syntax = p != null ? p.value.image : generateSyntax(am);
            for ( AddrModeDecl.Operand o : am.operands ) {
                HashMap<String, String> map = new HashMap<String, String>();
                for ( AddrModeDecl.Operand ot : am.operands) {
                    if ( o != ot ) {
                        ValueSet values = operandValues.get(ot.getOperandType());
                        values.getRepresentative(ot.name.image, map);
                    }
                }
                ValueSet values = operandValues.get(o.getOperandType());
                values.enumerate(d, syntax, o.name.image, null);
            }
        }
    }

    public void visit(InstrDecl d, AddrModeDecl am) {
        Property p = am.getProperty("syntax");
        if ( p == null ) p = d.getProperty("syntax");
        String syntax = p != null ? p.value.image : generateSyntax(am);
        for ( AddrModeDecl.Operand o : am.operands ) {
            HashMap<String, String> map = new HashMap<String, String>();
            for ( AddrModeDecl.Operand ot : am.operands) {
                if ( o != ot ) {
                    ValueSet values = operandValues.get(ot.getOperandType());
                    values.getRepresentative(ot.name.image, map);
                }
            }
            ValueSet values = operandValues.get(o.getOperandType());
            values.enumerate(d, syntax, o.name.image, null);
        }
    }

    private String generateSyntax(AddrModeDecl am) {
        StringBuffer buf = new StringBuffer();
        for ( AddrModeDecl.Operand o : am.operands ) {
            if ( buf.length() == 0 ) buf.append(", ");
            buf.append('%');
            buf.append(o.name.image);
        }
        return buf.toString();
    }

    void generate(InstrDecl d, String syntax, HashMap<String, String> props) {
        for ( Map.Entry<String, String> e : props.entrySet() ) {
            syntax = syntax.replaceAll('%' +e.getKey(), e.getValue());
        }
        printer.print(d.name+" "+syntax);
    }

}
