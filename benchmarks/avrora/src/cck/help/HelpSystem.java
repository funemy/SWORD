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

import cck.util.ClassMap;

import java.util.List;

/**
 * The <code>HelpSystem</code> is the global repository for help information, indexing
 * categories for help, while each category can have sub categories, etc.
 *
 * @author Ben L. Titzer
 */
public class HelpSystem {

    static final ClassMap categoryMap = new ClassMap("HelpCategory", HelpCategory.class);

    /**
     * The <code>getCategory()</code> method gets a help category for the specified short name.
     *
     * @param name the name of the help category
     * @return a help category for the specified name if it exists
     */
    public static HelpCategory getCategory(String name) {
        HelpCategory helpCategory = (HelpCategory) categoryMap.getObjectOfClass(name);
        if (helpCategory != null) helpCategory.setName(name);
        return helpCategory;
    }

    /**
     * The <code>addCategory()</code> method adds a help category to the help system.
     *
     * @param name the short name of the help category
     * @param cat  the category
     */
    public static void addCategory(String name, HelpCategory cat) {
        cat.setName(name);
        categoryMap.addInstance(name, cat);
    }

    /**
     * The <code>addCategory()</code> method adds a help category to the help system.
     *
     * @param name the short name of the help category
     * @param cz   the class for this help category
     */
    public static void addCategory(String name, Class cz) {
        categoryMap.addClass(name, cz);
    }

    /**
     * The <code>getSortedList()</code> returns a sorted list of all of the help categories.
     *
     * @return a sorted list of all help categories
     */
    public static List getSortedList() {
        return categoryMap.getSortedList();
    }
}
