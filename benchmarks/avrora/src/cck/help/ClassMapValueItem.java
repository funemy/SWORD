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
import cck.util.ClassMap;

/**
 * The <code>ClassMapValueItem</code> is a help item representing one possible value
 * for an option, where the value of the option is used to access a class map. For
 * example, the "-action" option accepts a value where the user specifies what
 * action the main program should perform. In this case, for each of the possible
 * values of this option, a <code>ClassMapValueItem</code> instance will be created
 * that is used in constructing the help text for the actions help category.
 *
 * @author Ben L. Titzer
 */
public class ClassMapValueItem implements HelpItem {

    public final int indent;
    public final String optname;
    public final String optvalue;
    public final ClassMap map;
    boolean isHelpCategory;
    protected String help;

    /**
     * The constructor for the <code>ClassMapValueItem</code> class creates a new instance of
     * a help item for the specified option and value. The classmap passed is used to
     * get an instance of the object corresponding to that value so that any help that it has
     * can be added underneath the option.
     *
     * @param indent   the number of spaces to indent when printing the help
     * @param optname  the name of the option
     * @param optvalue the string value of the option
     * @param map      the classmap which is used to get instances for this value
     */
    public ClassMapValueItem(int indent, String optname, String optvalue, ClassMap map) {
        this.optname = optname;
        this.optvalue = optvalue;
        this.map = map;
        this.indent = indent;
    }

    /**
     * The <code>getHelp()</code> method returns a help string for this help item.
     *
     * @return a string representing the help for this item
     */
    public String getHelp() {
        if (help != null) return help;
        else return computeHelp();
    }

    /**
     * The <code>printHelp()</code> method prints out a well-formatted paragraph for this option
     * and the value specified. If there is help for the value associated with this option, it
     * will format it as part of the help for this item.
     */
    public void printHelp() {
        String h = getHelp();
        Terminal.print(StringUtil.space(indent));
        String name;
        if (isHelpCategory && Terminal.htmlColors) {
            name = "<a href=" + optvalue + ".html>" + optvalue + "</a>";
        } else {
            name = optvalue;
        }
        Terminal.printPair(Terminal.COLOR_GREEN, Terminal.COLOR_YELLOW, optname, "=", name);
        Terminal.nextln();
        Terminal.println(StringUtil.formatParagraphs(h, indent + 4, 0, Terminal.MAXLINE));
    }

    /**
     * The <code>computeHelp()</code> method looks up this value in the specified class map,
     * and if the object is a help item itself, will return its help. If it is not a help item,
     * then it will return a default string.
     *
     * @return help for this item
     */
    private String computeHelp() {
        try {
            HelpItem item = (HelpItem) map.getObjectOfClass(optvalue);
            help = item.getHelp();
            if (item instanceof HelpCategory) isHelpCategory = true;
        } catch (Throwable t) {
            return "(No help available for this item.)";
        }
        return help;
    }
}
