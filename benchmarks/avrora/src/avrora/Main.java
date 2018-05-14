/**
 * Copyright (c) 2004-2006, Regents of the University of California
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

package avrora;

import avrora.actions.Action;
import avrora.core.Program;
import avrora.core.ProgramReader;
import cck.help.HelpCategory;
import cck.text.*;
import cck.util.*;
import java.io.*;
import java.util.*;

/**
 * This is the main entrypoint to Avrora. It is responsible for parsing the options to the main program and
 * selecting the appropriate action. Currently, it also implements the help system.
 *
 * @author Ben L. Titzer
 */
public class Main {

    static final Options mainOptions = new Options();

    public static final Option.Str INPUT = mainOptions.newOption("input", "auto",
            "This option selects among the available program formats as input to Avrora. " +
            "For example, the default input format, \"atmel\" selects the assembly " +
            "language format supported by Atmel's assembler.");
    public static final Option.Str ACTION = mainOptions.newOption("action", "simulate",
            "This option selects the action to perform. For example, an action might " +
            "be to load a program into the simulator and run it. For more information, " +
            "see the section on actions.");
    public static final Option.Bool COLORS = mainOptions.newOption("colors", true,
            "This option is used to enable or disable the terminal colors.");
    public static final Option.Bool BANNER = mainOptions.newOption("banner", true,
            "This option is used to enable or disable the printing of the banner.");
    public static final Option.Bool STATUS = mainOptions.newOption("status", true,
            "This option enables and disables printing of status information, for example " +
            "when the simulator is loading a program.");
    public static final Option.Bool STATUS_TIMING = mainOptions.newOption("status-timing", false,
            "This option enables and disables printing of timing with status information, for " +
            "example when the simulator is loading a program.");
    public static final Option.List VERBOSE = mainOptions.newOptionList("verbose", "",
            "This option allows users to enable verbose printing of individual " +
            "subsystems within Avrora. A list can be given with individual items separated " +
            "by commas. For example: -verbose=loader,atmel.flash");
    public static final Option.Bool HELP = mainOptions.newOption("help", false,
            "Displays this help message.");
    public static final Option.Bool LICENSE = mainOptions.newOption("license", false,
            "Display the detailed copyright and license text.");
    public static final Option.Bool HTML = mainOptions.newOption("html", false,
            "For terminal colors. Display terminal colors as HTML tags for " +
            "easier inclusion in webpages.");
    public static final Option.Str CONFIGFILE = mainOptions.newOption("config-file", "",
            "This option can be used to specify a file that contains additional command " +
            "line options to Avrora. Any command-line option can be specified in this " +
            "file. For repeated runs with similar options, the common options can be stored " +
            "in this file for use over multiple runs. Options are processed in the following " +
            "order: " +
            "\n   1) The .avrora file in your home directory " +
            "\n   2) A configuration file specified on the command line " +
            "\n   3) Command line options to Avrora");

    /**
     * The <code>main()</code> method is the entrypoint into Avrora. It processes the command line options,
     * looks up the action, and prints help (if there are no arguments or the <code>-help</code> option is
     * specified.
     *
     * @param args an array of strings representing the command line arguments passed by the user
     */
    public static void main(String[] args) {
        try {
            // try to load from ~/.avrora if it exists
            loadUserDefaults();

            // parse the command line options
            parseOptions(args);

            if ( !CONFIGFILE.isBlank() ) {
                // if the config-file option is specified, load config file and then re-parse
                // the arguments specified on the command line
                loadFile(CONFIGFILE.get());
                parseOptions(args);
            }

            if (args.length == 0 || HELP.get()) {
                // print the help if there are no arguments or -help is specified
                printHelp(mainOptions.getArguments());
            } else {
                // otherwise run the specified action
                runAction();
            }

        } catch (Util.Error e) {
            // report any internal Avrora errors
            e.report();
        } catch (Exception e) {
            // report any other exceptions
            e.printStackTrace();
        }
    }

    private static void runAction() throws Exception {
        banner();

        Action a = Defaults.getAction(ACTION.get());
        if (a == null)
            Util.userError("Unknown Action", StringUtil.quote(ACTION.get()));

        a.options.process(mainOptions);
        a.run(mainOptions.getArguments());
    }

    private static void loadUserDefaults() throws IOException {
        String hdir = System.getProperty("user.home");
        if (hdir == null || "".equals(hdir)) return;

        File f = new File(hdir + "/.avrora");
        if (f.exists()) {
            Properties defs = new Properties();
            defs.load(new FileInputStream(f));
            mainOptions.process(defs);
        }
    }

    private static void loadFile(String fname) throws IOException {
        checkFileExists(fname);
        File f = new File(fname);
        Properties defs = new Properties();
        defs.load(new FileInputStream(f));
        mainOptions.process(defs);
    }

    static HelpCategory buildHelpCategory() {
        HelpCategory hc = new HelpCategory("main", "");
        hc.addSection("OVERVIEW","Avrora is a tool for working with " +
                "assembly language programs for the AVR architecture microcontrollers " +
                "and building simulations of hardward devices based on this microcontroller." +
                "It contains tools to read AVR programs in multiple formats, perform " +
                "actions on them, and generate output in multiple formats.\n" +
                "Typical usage is to specify a list of files that contain a program " +
                "in some format supported by Avrora and then specifying the action " +
                "to perform on that program. For example, giving the name of a file " +
                "that contains a program written in assembly language and a simulate " +
                "action might look like: \n\n" +
                "avrora -action=simulate -input=atmel program.asm \n\n" +
                "Other actions that are available include generating a control flow graph of the " +
                "program or running one of the analysis tools on the program. See the " +
                "actions section for more information.");
        hc.addOptionSection("The main options to Avrora specify the action to be performed as well as the input " +
                "format and any general configuration parameters for " +
                "Avrora. The available main options are listed below along with their types and default " +
                "values. Each action also has its own set of options. To access help for the options " +
                "related to an action, specify the name of the action along with the \"help\" option.", mainOptions);

        hc.addSubcategorySection("ADDITIONAL HELP CATEGORIES", "Additional help is available on a category by category " +
                "basis. Below is a list of the additional categories available to provide help with actions, " +
                "input formats, monitors, and more. To access help for a specific category, specify the " +
                "\"-help\" option followed by the name of category.", Defaults.getMainCategories());

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
        HelpCategory hc = new HelpCategory("all", "Print a list of all categories for which help is available.");
        hc.addSection("OVERVIEW", "Avrora provides help in many categories that are all accessible from the command " +
                "line.");
        hc.addSubcategorySection("ALL HELP CATEGORIES", "Below is a listing of all the help categories available.",
                Defaults.getAllCategories());
        Defaults.addMainCategory(hc);
    }

    private static void printUsage() {
        int[] colors = {Terminal.COLOR_RED,
                        -1,
                        Terminal.COLOR_GREEN,
                        -1,
                        Terminal.COLOR_GREEN,
                        -1,
                        Terminal.COLOR_YELLOW,
                        -1,
                        Terminal.COLOR_YELLOW,
                        -1,
                        Terminal.COLOR_YELLOW,
                        -1};

        String[] strs = {"Usage", ": ", "avrora", " [", "-action", "=", "action", "] [", "options", "] ", "<files>"};
        Terminal.print(colors, strs);
        Terminal.nextln();

        int[] colors2 = {Terminal.COLOR_RED,
                         -1,
                         Terminal.COLOR_GREEN,
                         -1,
                         Terminal.COLOR_YELLOW,
                         -1};

        String[] strs2 = {"Usage", ": ", "avrora -help", " [", "category", "]"};
        Terminal.print(colors2, strs2);
        Terminal.println("\n");
    }

    private static void printFooter() {
        Terminal.println("For more information, see the online documentation at ");
        Terminal.printCyan("http://compilers.cs.ucla.edu/avrora");
        Terminal.nextln();
        Terminal.println("To report bugs or seek help, consult the Avrora mailing list: ");
        Terminal.printCyan("http://lists.ucla.edu/cgi-bin/mailman/listinfo/avrora");
        Terminal.nextln();
        Terminal.print("Please include the version number [");
        Terminal.printBrightBlue(Version.TAG.toString());
        Terminal.print("] when posting to the list.");
        Terminal.nextln();
    }

    private static void printHelp(String a) {
        HelpCategory hc = Defaults.getHelpCategory(a);
        hc.printHelp();
    }

    static void banner() {
        if (!BANNER.get() ) return;

        title();

        if (LICENSE.get()) {
            String notice =
                    "Copyright (c) 2003-2007, Regents of the University of California \n" +
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

    }

    static void title() {
        Terminal.printBrightBlue("Avrora ");
        Terminal.print("[");
        Terminal.printBrightBlue(Version.TAG.toString());
        Terminal.print("]");
        Terminal.print(" - (c) 2003-2007 UCLA Compilers Group\n\n");
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
        Status.ENABLED = STATUS.get();
        Status.TIMING = STATUS_TIMING.get();
        List verbose = VERBOSE.get();
        Iterator i = verbose.iterator();
        while (i.hasNext())
            Verbose.setVerbose((String)i.next(), true);
    }

    /**
     * The <code>readProgram()</code> method reads a program from the command line arguments given the format
     * specified at the command line. It will also process the indirect-call edge information and add it to
     * the <code>Program</code> instance returned. This method is primarily used by actions that manipulate
     * programs.
     *
     * @param args an array of strings representing command line arguments with the options removed
     * @return an instance of the <code>Program</code> class if the program can be loaded correctly
     * @throws Exception if there is an error loading the program, such as a file not found exception, parse
     *                   error, etc
     */
    public static Program loadProgram(String[] args) throws Exception {
        Status.begin("Loading "+args[0]);
        ProgramReader reader = Defaults.getProgramReader(INPUT.get());
        reader.options.process(mainOptions);
        Program program = reader.read(args);
        Status.success();
        return program;
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
