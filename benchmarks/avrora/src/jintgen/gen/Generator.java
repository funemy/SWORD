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

import cck.text.*;
import cck.util.*;
import jintgen.Main;
import jintgen.isdl.ArchDecl;
import java.io.*;
import java.util.List;

/**
 * The <code>Generator</code> class represents a class that generates
 * some tool from an instruction set description, such as an interpreter, disassembler,
 * assembler, etc.
 *
 * @author Ben L. Titzer
 */
public abstract class Generator extends GenBase {

    public ArchDecl arch;
    protected final Options options = new Options();

    public final Option.Str DEST_PACKAGE = options.newOption("package", "",
            "This option specifies the name of the destination java package for generators.");
    public final Option.Str CLASS_PREFIX = options.newOption("class-prefix", "*",
            "This option specifies a prefix for each class name to be generated. When this option " +
            "is set to \"*\", the generators will append the name of the architecture to the beginning " +
            "of each class generated; otherwise generators will append the specified string.");
    public final Option.Str ABSTRACT = options.newOption("abstract-package", "",
            "This option specifies the name of the java package that contains the abstract " +
            "versions of the instructions and architecture.");

    public void setArchitecture(ArchDecl a) {
        arch = a;
    }

    public void processOptions(Options o) {
        options.process(o);
    }

    public abstract void generate() throws Exception;

    protected SectionFile createSectionFile(String fname, String sect) throws IOException {
        Main.checkFileExists(fname);
        return new SectionFile(fname, sect);
    }

    protected String className(String cls) {
        String prefix = CLASS_PREFIX.get();
        if ( "*".equals(prefix) ) return arch.getName().toUpperCase()+cls;
        else return prefix+cls;
    }

    protected Printer newClassPrinter(String name, List<String> imports, String sup, List<String> impl, String jdoc) throws IOException {
        return newJavaPrinter(name, imports, sup, "class", impl, jdoc);
    }

    protected Printer newAbstractClassPrinter(String name, List<String> imports, String sup, List<String> impl, String jdoc) throws IOException {
        return newJavaPrinter(name, imports, sup, "abstract class", impl, jdoc);
    }

    private Printer newJavaPrinter(String n, List<String> imports, String sup, String type, List<String> impl, String jdoc) throws FileNotFoundException {
        String name = properties.getProperty(n);
        if ( name == null ) throw Util.failure("unknown class template: "+n);
        File f = new File(name + ".java");
        Printer printer = new Printer(new PrintStream(f));
        String pname = this.DEST_PACKAGE.get();
        if ( !"".equals(pname) ) {
            printer.println("package "+pname+ ';');
            printer.nextln();
        }
        String pabs = ABSTRACT.get();
        if ( !"".equals(pabs))
            printer.println("import "+pabs+".*;");
        if ( imports != null ) for ( String s : imports ) {
            printer.println("import "+s+ ';');
        }
        if ( jdoc != null )
            generateJavaDoc(printer, jdoc);
        String ec = sup == null ? "" : " extends "+sup;
        printer.print("public "+type+ ' ' +name+ec+' ');
        if ( impl != null ) {
            printer.beginList("implements ");
            for ( String str : impl ) printer.print(str);
            printer.endList(" ");
        }
        printer.startblock();
        return printer;
    }

    protected Printer newInterfacePrinter(String name, List<String> imports, String sup, String jdoc) throws IOException {
        return newJavaPrinter(name, imports, sup, "interface", null, jdoc);
    }

    protected void generateJavaDoc(Printer printer, String p) {
        List lines = StringUtil.trimLines(p, 0, 70);
        printer.println("");
        printer.println("/**");
        for ( Object l : lines ) {
            printer.print(" * ");
            printer.println(l.toString());
        }
        printer.println(" */");
    }

}
