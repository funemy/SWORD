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

package avrora.core;

import cck.text.StringUtil;

import java.util.*;

/**
 * The <code>SourceMapping</code> class embodies the concept of mapping machine code level
 * addresses and constructions in the <code>Program</code> class back to a source code program,
 * either in assembly language (labels), or a high-level programming lagnguage like C. This
 * class is used by the simulator to report information about the program in a higher-level
 * way more readibly understandable, for example to report calls / returns between functions
 * by their names rather than their machine code addresses.
 *
 * @author Ben L. Titzer
 */
public class SourceMapping {

    /**
     * The <code>program</code> field stores a reference to the program for this source mapping.
     */
    protected final Program program;
    protected final HashMap labels;
    protected final HashMap reverseMap;

    /**
     * The <code>LOCATION_COMPARATOR</code> comparator is used in order to sort locations
     * in the program from lowest address to highest address.
     */
    public static Comparator LOCATION_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            Location l1 = (Location)o1;
            Location l2 = (Location)o2;

            if (l1.lma_addr == l2.lma_addr) {
                if (l1.name == null) return 1;
                if (l2.name == null) return -1;
                return l1.name.compareTo(l2.name);
            }
            return l1.lma_addr - l2.lma_addr;
        }
    };

    /**
     * The <code>Location</code> class represents a location in the program; either named by
     * a label, or an unnamed integer address. The location may refer to any of the code, data,
     * or eeprom segments.
     */
    public class Location {

        /**
         * The <code>section</code> field records the name of the segment that this label refers to,
         * such as ".text" or ".data".
         */
        public final String section;

        /**
         * The <code>name</code> field records the name of this label.
         */
        public final String name;

        /**
         * The <code>address</code> field records the address of this label as a byte address.
         */
        public final int vma_addr;

        /**
         * The <code>address</code> field records the address of this label as a byte address.
         */
        public final int lma_addr;

        /**
         * The constructor for the <code>Location</code> class creates a new location for the
         * specified lable and address. It is used internally to create labels.
         * @param s the name of the segment as a string
         * @param n the name of the label as a string
         * @param vma_addr the virtual memory address
         * @param lma_addr the linear memory address (physical)
         */
        Location(String s, String n, int vma_addr, int lma_addr) {
            section = s;
            if ( n == null ) name = StringUtil.addrToString(lma_addr);
            else name = n;
            this.vma_addr = vma_addr;
            this.lma_addr = lma_addr;
        }

        /**
         * The <code>hashCode()</code> method computes the hash code of this location so that
         * it can be used in any of the standard collection libraries.
         * @return an integer value that represents the hash code
         */
        public int hashCode() {
            if (name == null) return lma_addr;
            else return name.hashCode();
        }

        /**
         * The <code>equals()</code> method compares this location against another object. It will return
         * true if and only if the specified object is an instance of <code>Location</code>, the addresses
         * match, and the names match.
         * @param o the other object to test this location for equality
         * @return true if the other object is equal to this label; false otherwise
         */
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Location)) return false;
            Location l = (Location)o;
            return l.name.equals(this.name) && l.lma_addr == this.lma_addr;
        }

        public String toString() {
            return name;
        }
    }

    /**
     * The <code>getName()</code> method translates a code address into a name that is more useful to
     * the user, such as a label. In the implementation of the label mapping, this method will return
     * the label name for this address if there is one. If there is no label for the specified address,
     * this method will render the address as a hexadecimal string via the <code>StringUtil.addrToString()<.code>
     * method.
     * @param address the address of an instruction in the program
     * @return a string representation of the address as a label or a hexadecimal string
     */
    public String getName(int address) {
        String s = (String)reverseMap.get(new Integer(address));
        return s == null ? StringUtil.addrToString(address) : s;
    }

    /**
     * The constructor for the <code>SourceMapping</code> base class creates a new instance of source mapping
     * information for the specified program. The mapping is tied to the program throughout its lifetime.
     * @param p the program to create the source mapping for
     */
    public SourceMapping(Program p) {
        program = p;
        labels = new HashMap();
        reverseMap = new HashMap();
    }

    /**
     * The <code>getProgram()</code> class returns a reference to the program for which this class
     * provides source information.
     * @return the program associated with this source mapping
     */
    public Program getProgram() {
        return program;
    }

    /**
     * The <code>getLocation()</cdoe> method retrieves an object that represents a location for the given name,
     * if the name exists in the program. If the name does not exist in the program, this method will return null.
     * For strings beginning with "0x", this method will evaluate them as hexadecimal literals and return a
     * location corresponding to an unnamed location at that address.
     * @param name the name of a program location as a label or a hexadecimal constant
     * @return a <code>Location</code> object representing that program location; <code>null</code> if the
     * specified label is not contained in the program
     */
    public Location getLocation(String name) {
        if ( StringUtil.isHex(name) ) {
            int val = StringUtil.evaluateIntegerLiteral(name);
            return new Location(null, null, val, val);
        }
        return (Location)labels.get(name);
    }

    /**
     * The <code>newLocation()</code> method creates a new program location with the specified label name that
     * is stored internally.
     * @param section the name of the section which contains this label
     * @param name the name of the label
     * @param vma_addr the virtual address in the program
     * @param lma_addr the address in the program for which to create and store a new location
     */
    public void newLocation(String section, String name, int vma_addr, int lma_addr) {
        Location l = new Location(section, name, vma_addr, lma_addr);
        labels.put(name, l);
        reverseMap.put(new Integer(lma_addr), name);
    }

    /**
     * The <code>getIterator()</code> method creates an iterator over the labels
     * in this source mapping.
     * @return an iterator that will allow iterating over all of the labels in this source mapping
     */
    public Iterator getIterator() {
        return labels.values().iterator();
    }
}
