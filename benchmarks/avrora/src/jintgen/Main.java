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

package jintgen;

import cck.help.HelpCategory;
import cck.test.TestEngine;
import cck.text.*;
import cck.util.*;
import jintgen.gen.*;
import jintgen.gen.disassembler.DisassemblerGenerator;
import jintgen.isdl.ArchDecl;
import jintgen.isdl.parser.ISDLParser;
import jintgen.isdl.parser.ParseException;
import jintgen.isdl.verifier.Verifier;
import jintgen.isdl.verifier.VerifierTestHarness;
import java.io.*;
import java.util.Iterator;
import java.util.List;

/**
 * This is the main entrypoint to Jintgen. It is responsible for parsing the options to the main program and
 * loading the architecture description.
 *
 * @author Ben L. Titzer
 */
public class Main {

    static final Options mainOptions = new Options();

    public static final Option.Str CODEMAP = mainOptions.newOption("codemap", "",
            "This option specifies the file to generate the codemap into. The codemap is used in a" +
            "dynamic basic block compiler and dependency analysis of instructions.");
    public static final Option.Bool INLINE = mainOptions.newOption("inline", true,
            "This option controls whether the ISDL processor will inline all subroutines marked as " +
            "\"inline\" in their declaration.");

    public static final Option.Bool COLORS = mainOptions.newOption("colors", true,
            "This option is used to enable or disable the terminal colors.");
    public static final Option.Bool BANNER = mainOptions.newOption("banner", true,
            "This option is used to enable or disable the printing of the banner.");
    public static final Option.List VERBOSE = mainOptions.newOptionList("verbose", "",
            "This option allows users to enable verbose printing of individual " +
            "subsystems within jIntGen. A list can be given with individual items separated " +
            "by commas. For example: -verbose=loader,optimizer");
    public static final Option.Bool HELP = mainOptions.newOption("help", false,
            "Displays this help message.");
    public static final Option.Bool LICENSE = mainOptions.newOption("license", false,
            "Display the detailed copyright and license text.");
    public static final Option.Bool HTML = mainOptions.newOption("html", false,
            "For terminal colors. Display terminal colors as HTML tags for " +
            "easier inclusion in webpages.");
    public static final Option.List GENERATORS = mainOptions.newOptionList("generate", "",
            "This option accepts a list of generators to be applied to the specified architecture " +
            "description file. Each generator may generate a tool, such as an assembler or disassembler " +
            "or perform an analysis on the architecture description. Each generator may support " +
            "further options that allow its operation to be customized.");
    public static final Option.Bool TEST = mainOptions.newOption("test", false,
            "When specified, this option activates the internal testing framework of jIntGen.");

    public static ClassMap generatorMap = new ClassMap("Generator", Generator.class);

    static {
        generatorMap.addClass("ir", InstrIRGenerator.class);
        generatorMap.addClass("codemap", CodemapGenerator.class);
        generatorMap.addClass("disassembler", DisassemblerGenerator.class);
        generatorMap.addClass("interpreter", InterpreterGenerator.class);
    }

    /**
     * The <code>main()</code> method is the entrypoint into jIntGen. It processes the command line options,
     * looks up the action, and prints help (if there are no arguments or the <code>-help</code> option is
     * specified.
     *
     * @param args an array of strings representing the command line arguments passed by the user
     */
    public static void main(String[] args) {
        try {
            // parse the command line options
            parseOptions(args);

            if ( TEST.get() ) {
                ClassMap harnessMap = new ClassMap("Test Harness", TestEngine.Harness.class);
                harnessMap.addClass("verifier", VerifierTestHarness.class);
                new TestEngine(harnessMap).runTests(mainOptions.getArguments());
            } else if (args.length == 0 || HELP.get()) {
                // print the help if there are no arguments or -help is specified
                printHelp(mainOptions.getArguments());
            } else {
                // otherwise run the specified action
                run(mainOptions.getArguments());
            }

        } catch (Util.Error e) {
            // report any internal errors
            e.report();
        } catch (Exception e) {
            // report any other exceptions
            e.printStackTrace();
        }
    }

    private static void run(String[] args) throws Exception {
        banner();
        if (args.length < 1)
            Util.userError("Usage: jintgen <arch.isdl>");

        ArchDecl.INLINE = INLINE.get();

        try {
            ArchDecl a = loadArchitecture(args[0]);
            runGenerators(a);
        } catch ( Util.Error t) {
            Status.error(t);
            t.report();
        } catch ( Throwable t) {
            Status.error(t);
            t.printStackTrace();
        }
    }

    private static ArchDecl loadArchitecture(String fname) throws FileNotFoundException, ParseException {
        Status.begin("Loading architecture description "+fname);
        checkFileExists(fname);
        File archfile = new File(fname);
        FileInputStream fis = new FileInputStream(archfile);
        ISDLParser parser = new ISDLParser(fis);
        ArchDecl a = parser.ArchDecl();
        Status.success();
        Status.begin("Verifying "+fname);
        new Verifier(a).verify();
        Status.success();
        return a;
    }

    private static void runGenerators(ArchDecl a) throws Exception {
        for ( Object o : GENERATORS.get() ) {
            String str = (String)o;
            Status.begin("Running "+str+" generator");
            Generator g = (Generator)generatorMap.getObjectOfClass(str);
            g.setArchitecture(a);
            g.processOptions(mainOptions);
            g.generate();
            Status.success();
        }
    }

    static HelpCategory buildHelpCategory() {
        HelpCategory hc = new HelpCategory("main", "");
        hc.addSection("OVERVIEW","jIntGen is a tool for generating interpreters, assemblers, " +
                "disassemblers, and other tools for " +
                "assembly language and machine code programs. jIntGen has a domain-specific language " +
                "for describing instruction sets from which it can generate interpreters and " +
                "other tools.");
        hc.addOptionSection("The main options to jIntGen specify the file containing the architecture description " +
                "to be processed and what tools to generate. To access help for the options " +
                "related to other subcategories, specify the name of the subcategory along with " +
                "the \"help\" option.", mainOptions);

        // TODO: add subcategory section for generators
        return hc;
    }

    static void printHelp(String[] args) {
        title();
        printUsage();

        buildAllCategories();

        if (args.length == 0) {
            buildHelpCategory().printHelp();
        } else if (args.length == 1) {
            printHelp(args[0]);
        } else {
            Util.userError("help available for only one category at a time.");
        }

        printFooter();
    }

    private static void buildAllCategories() {
        // TODO: add all subcategories
    }

    private static void printUsage() {
        int[] colors = {Terminal.COLOR_RED,
                        -1,
                        Terminal.COLOR_GREEN,
                        -1,
                        Terminal.COLOR_YELLOW,
                        -1,
                        Terminal.COLOR_YELLOW,
                        -1};

        String[] strs = {"Usage", ": ", "jintgen", " [", "options", "] ", "<architecture file>"};
        Terminal.print(colors, strs);
        Terminal.nextln();

        int[] colors2 = {Terminal.COLOR_RED,
                         -1,
                         Terminal.COLOR_GREEN,
                         -1,
                         Terminal.COLOR_YELLOW,
                         -1};

        String[] strs2 = {"Usage", ": ", "jintgen -help", " [", "category", "]"};
        Terminal.print(colors2, strs2);
        Terminal.println("\n");
    }

    private static void printFooter() {
        Terminal.println("For more information, see the online documentation at ");
        Terminal.printCyan("http://compilers.cs.ucla.edu/jintgen");
        Terminal.nextln();
        Terminal.println("To report bugs or seek help, consult the jIntGen mailing list: ");
        Terminal.printCyan("http://lists.ucla.edu/cgi-bin/mailman/listinfo/jintgen");
        Terminal.nextln();
        Terminal.print("Please include the version number [");
        Terminal.printBrightBlue(Version.TAG.toString());
        Terminal.print("] when posting to the list.");
        Terminal.nextln();
    }

    private static void printHelp(String a) {
        // TODO: get help for main categories
    }

    static void banner() {
        if (! BANNER.get() ) return;

        title();
        String notice;
        if (!LICENSE.get())
            notice =
                    "This interpreter generator tool is provided with absolutely no " +
                    "warranty, either expressed or implied. It is provided to you with the hope " +
                    "that it be useful for generating tools to work with machine code programs. " +
                    "For more information about the license " +
                    "that this software is provided to you under, specify the \"license\" " +
                    "option.\n\n";
        else
            notice =
                    "Copyright (c) 2005, Regents of the University of California \n" +
                    "All rights reserved.\n\n" +

                    "Redistribution and use in source and binary forms, with or without " +
                    "modification, are permitted provided that the following conditions " +
                    "are met:\n\n" +

                    "Redistributions of source code must retain the above copyright notice, " +
                    "this list of conditions and the following disclaimer.\n\n" +

                    "Redistributions in binary form must reproduce the above copyright " +
                    "notice, this list of conditions and the following disclaimer in the " +
                    "documentation and/or other materials provided with the distribution.\n\n" +

                    "Neither the name of the University of California, Los Angeles nor the " +
                    "names of its contributors may be used to endorse or promote products " +
                    "derived from this software without specific prior written permission.\n\n" +

                    "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS " +
                    "\"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT " +
                    "LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR " +
                    "A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT " +
                    "OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, " +
                    "SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT " +
                    "LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, " +
                    "DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY " +
                    "THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT " +
                    "(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE " +
                    "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n\n";

        Terminal.print(StringUtil.formatParagraphs(notice, 0, 0, Terminal.MAXLINE));
    }

    static void title() {
        Terminal.printBrightBlue("jIntGen ");
        Terminal.print("[");
        Terminal.printBrightBlue(Version.TAG.toString());
        Terminal.print("]");
        Terminal.print(" - (c) 2005-2007 UCLA Compilers Group\n\n");
    }

    /**
     * The <code>parseOptions()</code> method takes an array of strings and parses it, extracting the options
     * and storing the option values in the internal state of main.
     *
     * @param args the array of strings to parse into options
     */
    public static void parseOptions(String[] args) {
        mainOptions.parseCommandLine(args);
        Terminal.useColors = COLORS.get();
        Terminal.htmlColors = HTML.get();
        List verbose = VERBOSE.get();
        Iterator i = verbose.iterator();
        while (i.hasNext())
            Verbose.setVerbose((String)i.next(), true);
    }

    public static void checkFilesExist(String[] files) {
        for ( int cntr = 0; cntr < files.length; cntr++ ) {
            checkFileExists(files[cntr]);
        }
    }

    public static void checkFileExists(String fstr) {
        File f = new File(fstr);
        if ( !f.exists() ) {
            Util.userError("File not found", fstr);
        }
    }
}
