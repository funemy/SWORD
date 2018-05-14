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
 * The <code>SubcategoryItem</code> class implements a help item that represents a subcategory in
 * a help category. This class is used to add a list of other related help categories at the end
 * of a section within another help category.
 *
 * @author Ben L. Titzer
 */
public class SubcategoryItem implements HelpItem {

    public final int indent;
    public final HelpCategory helpCat;
    protected String help;

    /**
     * The constructor for the <code>SubcategoryItem</code> method creates a new instance that represents
     * a help item that can be added to the end of a section in another help category.
     *
     * @param indent the number of spaces to indent when printing to the terminal
     * @param hc     the help category that this item refers to
     */
    public SubcategoryItem(int indent, HelpCategory hc) {
        this.helpCat = hc;
        this.indent = indent;
    }

    /**
     * The <code>getHelp()</code> method returns the help string of the underlying help category.
     *
     * @return a help string for this item.
     */
    public String getHelp() {
        return helpCat.getHelp();
    }

    /**
     * The <code>printHelp()</code> method prints out a well-formatted paragraph containing the help
     * for this subcategory.
     */
    public void printHelp() {
        String h = getHelp();
        Terminal.print(StringUtil.space(indent));
        String name;
        if (Terminal.htmlColors) {
            name = "<a href=" + helpCat.name + ".html>" + helpCat.name + "</a>";
        } else {
            name = helpCat.name;
        }
        Terminal.printPair(Terminal.COLOR_GREEN, Terminal.COLOR_YELLOW, "-help", " ", name);
        Terminal.nextln();
        Terminal.println(StringUtil.formatParagraphs(h, indent + 4, 0, Terminal.MAXLINE));
    }
}
