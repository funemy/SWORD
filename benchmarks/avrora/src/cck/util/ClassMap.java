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

package cck.util;

import cck.text.StringUtil;

import java.util.*;

/**
 * The <code>ClassMap</code> is a class that maps short names (i.e. short, lower case strings) to java classes
 * and can instantiate them. This is useful for dynamic resolution of classes but with a small set of known
 * defaults that have a short name. If the short name is not in the default set, this class will treat the
 * short name as a fully qualified Java class name and load it. This class does the requisite checking--that
 * the class exists, that it can be loaded, that it is of the appropriate type, that it can be instantiated,
 * etc.
 *
 * @author Ben L. Titzer
 */
public class ClassMap {

    /**
     * The <code>type</code> field stores a string that represents the name of the "type" that this map
     * contains. For example, a class map for actions might be called "Action" and for input formats might be
     * called "Input Format".
     */
    protected final String type;

    /**
     * The <code>clazz</code> field stores a reference to the Java class of which the objects stored in this
     * map are instances of.
     */
    protected final Class clazz;

    /**
     * The <code>classMap</code> field is a hash map that maps a string to a Java class.
     */
    protected final HashMap classMap;

    /**
     * The <code>reverseMap</code> field is a hash map that maps a Java class back to its alias.
     */
    protected final HashMap reverseMap;

    /**
     * The <code>objMap</code> field is a hash map that maps a string to an instance of a particular class,
     * i.e. an object.
     */
    protected final HashMap objMap;

    /**
     * The constructor for the <code>ClassMap</code> class creates a new class map with the specified type,
     * which maps strings to instances of the specified class.
     *
     * @param t   the name of the type of this class as a string
     * @param clz the class which objects should be instances of
     */
    public ClassMap(String t, Class clz) {
        clazz = clz;
        classMap = new HashMap();
        reverseMap = new HashMap();
        objMap = new HashMap();
        type = t;
    }

    /**
     * The <code>addClass()</code> method adds a short name (alias) for the specified class to the set of
     * default class names.
     *
     * @param alias the string representation of the alias of the class
     * @param clz   the class to which the alias maps
     */
    public void addClass(String alias, Class clz) {
        classMap.put(alias, clz);
        reverseMap.put(clz, alias);
    }

    /**
     * The <code>addInstance()</code> method adds a mapping between a short name (alias) and an object that is
     * the instance of the class represented by that short name.
     *
     * @param alias the alias for the instance
     * @param o     the actual object that will be returned from <code>getObjectOfClass()</code> when the
     *              parameters is equal to the alias.
     */
    public void addInstance(String alias, Object o) {
        Class cz = o.getClass();
        if (!(clazz.isAssignableFrom(cz)))
            throw Util.failure("Object of class " + StringUtil.quote(cz) + " is not an instance of " + clazz.getName());

        objMap.put(alias, o);
        classMap.put(alias, cz);
        reverseMap.put(o, alias);
    }

    /**
     * The <code>getClass()</code> method gets the Java class representing the class returned for a given
     * short name. If there is no short name (alias) for the passed argument, this method will assume that the
     * parameter is the fully qualified name of a class. It will then load that class and instantiate an
     * instance of that class. That instance will be returned in subsequent calls to
     * <code>getObjectOfClass()</code>.
     *
     * @param shortName the short name of the class
     * @return a Java class representing the class for that alias or fully qualified name
     */
    public Class getClass(String shortName) {
        Object o = objMap.get(shortName);
        if (o != null) return o.getClass();
        return (Class) classMap.get(shortName);
    }

    /**
     * The <code>getObjectOfClass()</code> method looks up the string name of the class in the alias map
     * first, and if not found, attempts to load the class using <code>Class.forName()</code> and instantiates
     * one object.
     *
     * @param name the name of the class or alias
     * @return an instance of the specified class
     * @throws Util.Error if there is a problem finding or instantiating the class
     */
    public Object getObjectOfClass(String name) {
        Object o = objMap.get(name);
        if (o != null) return o;

        String clname = StringUtil.quote(name);

        Class c = (Class) classMap.get(name);
        if (c == null) {
            try {
                c = Class.forName(name);
            } catch (ClassNotFoundException e) {
                Util.userError(type + " class not found", clname);
            }
        } else {
            clname = clname + " (" + c.toString() + ')';
        }

        if (!(clazz.isAssignableFrom(c)))
            Util.userError("The specified class does not extend " + clazz.getName(), clname);

        try {
            return c.newInstance();
        } catch (InstantiationException e) {
            Util.userError("The specified class does not have a default constructor", clname);
        } catch (IllegalAccessException e) {
            Util.userError("Illegal access to class", clname);
        }

        // UNREACHABLE
        throw Util.unreachable();
    }

    /**
     * The <code>getAlias()</code> method returns the alias of a particular class or object
     * that corresponds to the short name for the class. If there is no such alias for this
     * object, this method will return the name of the class of the object.
     * @param o the object for which to look up the alias
     * @return the string name of the alias for the object
     */
    public String getAlias(Object o) {
        String s = (String)reverseMap.get(o);
        if ( s == null ) s = (String)reverseMap.get(o.getClass());
        if ( s == null ) s = o.getClass().getName();
        return s;
    }

    /**
     * The <code>getSortedList()</code> method returns a sorted list of the short names (aliases) stored in
     * this class map.
     *
     * @return an alphabetically sorted list that contains all the aliases in this map
     */
    public List getSortedList() {
        List list = Collections.list(Collections.enumeration(classMap.keySet()));
        Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    /**
     * The <code>getIterator()</code> method returns an interator over all of the key values (short
     * names or aliases) of this class map.
     *
     * @return an iterator over all the short names in this map
     */
    public Iterator getIterator() {
        return classMap.keySet().iterator();
    }

    /**
     * The <code>iterator()</code> method returns an interator over the short names (aliases) stored in this
     * map.
     *
     * @return an instance of <code>java.util.Iterator</code> which can be used to iterate over each alias in
     *         this map.
     */
    public Iterator iterator() {
        return classMap.keySet().iterator();
    }
}
