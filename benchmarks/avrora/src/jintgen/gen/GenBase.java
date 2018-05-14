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

package jintgen.gen;

import cck.text.Printer;
import cck.text.StringUtil;
import jintgen.isdl.AddrModeDecl;
import jintgen.jigir.JIGIRTypeEnv;
import jintgen.types.*;
import java.util.*;

/**
 * The <code>GenBase</code> class contains a number of utility methods that help
 * in generating Java code, including the ability to do large-sacale string
 * replacement with a map of properties.
 *
 * @author Ben L. Titzer
 */
public class GenBase {

    public final Properties properties;
    public Printer p;

    protected String tr(String t, Object... o) {
        int cntr = 0;
        for ( Object obj : o ) {
            String str = obj == null ? "null" : obj.toString();
            properties.setProperty(""+(++cntr), str);
        }
        return StringUtil.stringReplace(t, properties);
    }

    protected GenBase(Properties prop) {
        properties = prop;
    }

    protected GenBase() {
        properties = new Properties();
    }

    protected void setPrinter(Printer printer) {
        p = printer;
    }

    protected void generateJavaDoc(String doc) {
        List lines = StringUtil.trimLines(doc, 0, 70);
        p.println("");
        p.println("/**");
        for ( Object l : lines ) {
            p.print(" * ");
            p.println(l.toString());
        }
        p.println(" */");
    }


    protected void print(String f, Object... args) {
        p.print(tr(f, args));
    }

    protected void println(String f, Object... args) {
        p.println(tr(f, args));
    }

    protected void beginList(String f, Object... args) {
        p.beginList(tr(f, args));
    }

    protected void endList(String f, Object... args) {
        p.endList(tr(f, args));
    }

    protected void beginList() {
        p.beginList();
    }

    protected void endList() {
        p.endList();
    }

    protected void endListln(String f, Object... args) {
        p.endListln(tr(f, args));
    }

    protected void startblock(String f, Object... args) {
        p.startblock(tr(f, args));
    }

    protected void endblock(String f, Object... args) {
        p.endblock(tr(f, args));
    }

    protected void startblock() {
        p.startblock();
    }

    protected void endblock() {
        p.endblock();
    }

    protected void nextln() {
        p.nextln();
    }

    protected void close() {
        p.close();
    }

    protected String javaName(String n) {
        return n.replace('"', '$').replace('.', '_');
    }

    protected LinkedList<String> nameList(List<AddrModeDecl.Operand> ol) {
        LinkedList<String> list = new LinkedList<String>();
        for ( AddrModeDecl.Operand o : ol ) list.add(o.name.image);
        return list;
    }

    protected LinkedList<String> nameTypeList(List<AddrModeDecl.Operand> ol) {
        LinkedList<String> list = new LinkedList<String>();
        for ( AddrModeDecl.Operand o : ol ) list.add("$operand."+o.typeRef.getTypeConName()+' '+o.name.image);
        return list;
    }

    protected LinkedList<String> nameList(String a, List<AddrModeDecl.Operand> ol) {
        LinkedList<String> list = new LinkedList<String>();
        list.add(a);
        for ( AddrModeDecl.Operand o : ol ) list.add(o.name.image);
        return list;
    }

    protected LinkedList<String> nameTypeList(String t, String n, List<AddrModeDecl.Operand> ol) {
        LinkedList<String> list = new LinkedList<String>();
        list.add(t+ ' ' +n);
        for ( AddrModeDecl.Operand o : ol ) list.add("$operand."+o.typeRef.getTypeConName()+' '+o.name.image);
        return list;
    }

    protected void printParams(List<String> list) {
        beginList("(");
        for ( String str : list ) print(str);
        endList(")");
    }

    public String renderType(Type t) {
        // TODO: compute the correct java type depending on the situation
        return renderTypeCon(t.getTypeCon());
    }

    public String renderTypeCon(TypeCon tc) {
        if ( tc instanceof JIGIRTypeEnv.TYPE_enum )
            return tr("$symbol.$1", tc.getName());
        else if ( tc instanceof JIGIRTypeEnv.TYPE_operand )
            return tr("$operand.$1", tc.getName());
        else return tc.toString();
    }

    public String renderType(TypeRef t) {
        // TODO: compute the correct java type depending on the situation
        return renderTypeCon(t.getType().getTypeCon());
    }


}
