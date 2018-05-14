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

package cck.help;

import cck.text.StringUtil;
import cck.text.Terminal;

/**
 * The <code>ValueItem</code> method represents a help item that is used in a list. It represents
 * a help item for a possible value assignment to a specific option.
 *
 * @author Ben L. Titzer
 */
public class ValueItem implements HelpItem {

    public final int indent;
    public final String optname;
    public final String optvalue;
    public final String help;

    /**
     * The constructor for the <code>ValueItem</code> class creates a new instance of this help item
     * for the specified option and option value, with the given help text.
     *
     * @param indent   the number of spaces to indent when printing to the terminal
     * @param optname  the name of the option
     * @param optvalue the value of the option
     * @param help     the help text for this option
     */
    public ValueItem(int indent, String optname, String optvalue, String help) {
        this.optname = optname;
        this.optvalue = optvalue;
        this.help = help;
        this.indent = indent;
    }

    /**
     * The <code>getHelp()</code> method returns a help string for this help item.
     *
     * @return a help string
     */
    public String getHelp() {
        return help;
    }

    /**
     * The <code>printHelp()</code> method prints out a well-formatted help paragraph for this
     * option and its value, containing the specified help text.
     */
    public void printHelp() {
        Terminal.print(StringUtil.space(indent));
        Terminal.printPair(Terminal.COLOR_GREEN, Terminal.COLOR_YELLOW, optname, "=", optvalue);
        Terminal.nextln();
        Terminal.println(StringUtil.formatParagraphs(help, indent + 4, 0, Terminal.MAXLINE));
    }

}
