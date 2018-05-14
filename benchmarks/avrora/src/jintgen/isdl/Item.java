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

package jintgen.isdl;

import jintgen.isdl.parser.Token;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * The <code>Item</code> class represents the base class of many different types of items
 * that can occur in an ISDL file. Each item has a unique name and a list of properties
 * associated with it.
 *
 * @author Ben L. Titzer
 */
public class Item {

    /**
     * The <code>name</code> field stores a reference to the token that names this
     * item. The token contains information about which where in the file it occured.
     */
    public final Token name;

    /**
     * The <code>properties</code> field stores a reference to a hash map that can
     * retrieve the properties by their name.
     */
    public final HashMap<String, Property> properties;

    Item(Token n) {
        name = n;
        properties = new LinkedHashMap<String, Property>();
    }

    public String getName() {
        return name.image;
    }

    /**
     * The <code>addProperty()</code> method adds a property to this syntactic item. The
     * property can be retrieved by its name as a string.
     * @param p the property to add to this item
     */
    public void addProperty(Property p) {
        properties.put(p.name.image, p);
    }

    /**
     * The <code>getProperty()</code> method retrieves a property by its name, if it exists,
     * and returns it.
     * @param name the name of the property as a string
     * @return a reference to the property if it exists; null otherwise
     */
    public Property getProperty(String name) {
        return properties.get(name);
    }
}
