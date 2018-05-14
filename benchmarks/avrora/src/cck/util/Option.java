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
import cck.text.Terminal;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;

/**
 * The <code>Option</code> class represents an option that has been given on the command line. The inner
 * classes represent specific types of options such as integers, booleans, and strings.
 *
 * @author Ben L. Titzer
 * @see Options
 */
public abstract class Option {
    /**
     * The <code>name</code> field stores a reference to the string name of this option.
     */
    protected final String name;

    /**
     * The <code>description</code> field stores a reference to the string that represents the help item for
     * this option.
     */
    protected final String description;

    /**
     * The <code>OptionComparator</code> is an implementation of the <code>java.util.Comparator</code>
     * interface that is used to sort options alphabetically for printing in the help system.
     */
    public static final Comparator COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            Option opt1 = (Option) o1;
            Option opt2 = (Option) o2;
            return String.CASE_INSENSITIVE_ORDER.compare(opt1.getName(), opt2.getName());
        }
    };

    /**
     * The constructor of the <code>Option</code> class creates a new option with the specified name and
     * description.
     *
     * @param n a string name of the option
     * @param d the description, as an unformatted string, of the help item for this option
     */
    public Option(String n, String d) {
        name = n;
        description = d;
    }

    /**
     * The <code>getName()</code> method returns the string name of the option. This name is the same name
     * used at the command line; i.e. -name=value.
     *
     * @return a string that is the name of this option
     */
    public String getName() {
        return name;
    }

    /**
     * The <code>set()</code> method updates the value of the option. It is passed a string that is converted
     * to the option's value by each respective option implementation. For example, an integer option converts
     * the string from an integer into an integer value.
     *
     * @param val the string value passed at the command line
     */
    public abstract void set(String val);

    /**
     * The <code>stringValue()</code> method returns a string representation of the value of the option. This
     * is used in debugging and reporting purposes.
     *
     * @return a string representation of the value of the option.
     */
    public abstract String stringValue();

    /**
     * The <code>printHelp()</code> method prints out a textual paragraph of the help item for this option to
     * the terminal.
     */
    public abstract void printHelp();

    /**
     * The <code>printDescription()</code> method prints out a well-formatted representation of the
     * description of the item to the terminal.
     */
    public void printDescription() {
        Terminal.print(StringUtil.formatParagraphs(description, 8, 0, Terminal.MAXLINE));
        Terminal.nextln();
    }

    /**
     * The <code>printHeader()</code> method prints out the first line of the help text for this item. This
     * includes the option's name, type, and its default value.
     *
     * @param type     the type of the item as a string
     * @param defvalue the default value for the item as a string
     */
    public void printHeader(String type, String defvalue) {
        Terminal.printGreen("    -" + name);
        Terminal.print(": ");
        Terminal.printBrightCyan(type);
        Terminal.print(" = ");
        Terminal.printYellow(defvalue);
        Terminal.nextln();
    }

    /**
     * The <code>parseError()</code> method is called by an option implementation when there is a problem
     * parsing the value for an option supplied by the user on the command line. For example, if an integer is
     * not in the correct format, this method will be called, which will report an error.
     *
     * @param name the name of the option
     * @param val  the (invalid) value passed
     */
    protected void parseError(String name, String type, String val) {
        Util.userError("Option Error", "invalid value for " + type + " option " + StringUtil.quote(name) + " = " + StringUtil.quote(val));
    }

    /**
     * The <code>Option.Long</code> class is an implementation of the <code>Option</code> class that
     * encapsulates a long integer value.
     */
    public static class Long extends Option {
        /**
         * The <code>defvalue</code> field stores the default (initial) value for this option. It is used in
         * reporting the help item.
         */
        protected final long defvalue;
        protected long value;

        /**
         * The constructor for the <code>Option.Long</code> class creates a new option that can store long
         * integers. It is given an option name, a help description, and a default value.
         *
         * @param nm   the string name of the option
         * @param val  the default value of the option
         * @param desc the description of the option
         */
        public Long(String nm, long val, String desc) {
            super(nm, desc);
            value = val;
            defvalue = val;
        }

        /**
         * The <code>set()</code> method updates the value of the option.
         *
         * @param val a string representation of the new value of the option.
         */
        public void set(String val) {
            try {
                value = java.lang.Long.parseLong(val);
            } catch (Exception e) {
                parseError(name, "long", val);
            }
        }

        /**
         * The <code>get()</code> method returns the current value of the option.
         *
         * @return the value of the option as a long integer.
         */
        public long get() {
            return value;
        }

        /**
         * The <code>stringValue()</code> method returns a string representation of the value of the option.
         * This is used in debugging and reporting purposes.
         *
         * @return a string representation of the value of the option.
         */
        public String stringValue() {
            return String.valueOf(value);
        }

        /**
         * The <code>printHelp()</code> method prints out a textual paragraph of the help item for this option
         * to the terminal.
         */
        public void printHelp() {
            printHeader("long", String.valueOf(defvalue));
            printDescription();
        }
    }

    /**
     * The <code>Option.Double</code> class is an implementation of the <code>Option</code> class that
     * encapsulates a double value.
     */
    public static class Double extends Option {
        /**
         * The <code>defvalue</code> field stores the default (initial) value for this option. It is used in
         * reporting the help item.
         */
        protected final double defvalue;
        protected double value;

        /**
         * The constructor for the <code>Option.Double</code> class creates a new option that can store long
         * integers. It is given an option name, a help description, and a default value.
         *
         * @param nm   the string name of the option
         * @param val  the default value of the option
         * @param desc the description of the option
         */
        public Double(String nm, double val, String desc) {
            super(nm, desc);
            value = val;
            defvalue = val;
        }

        /**
         * The <code>set()</code> method updates the value of the option.
         *
         * @param val a string representation of the new value of the option.
         */
        public void set(String val) {
            try {
                value = java.lang.Double.parseDouble(val);
            } catch (Exception e) {
                parseError(name, "double", val);
            }
        }

        /**
         * The <code>get()</code> method returns the current value of the option.
         *
         * @return the value of the option as a double.
         */
        public double get() {
            return value;
        }

        /**
         * The <code>stringValue()</code> method returns a string representation of the value of the option.
         * This is used in debugging and reporting purposes.
         *
         * @return a string representation of the value of the option.
         */
        public String stringValue() {
            return String.valueOf(value);
        }

        /**
         * The <code>printHelp()</code> method prints out a textual paragraph of the help item for this option
         * to the terminal.
         */
        public void printHelp() {
            printHeader("double", String.valueOf(defvalue));
            printDescription();
        }
    }

    /**
     * The <code>Option.Long</code> class is an implementation of the <code>Option</code> class that
     * encapsulates a long integer value.
     */
    public static class Interval extends Option {
        /**
         * The <code>default_low</code> field stores the default (initial) low value for this option. It is
         * used in reporting the help item.
         */
        protected final long default_low;
        /**
         * The <code>default_high</code> field stores the default (initial) high value for this option. It is
         * used in reporting the help item.
         */
        protected final long default_high;
        protected long low;
        protected long high;

        /**
         * The constructor for the <code>Option.Interval</code> class creates a new option that can store an
         * interval which is denoted by a low integer and a high integer. It is given an option name, a help
         * description, and a default value.
         *
         * @param nm   the string name of the option
         * @param l    the default lowest value of the interval
         * @param h    the default highest value of the interval
         * @param desc the description of the option
         */
        public Interval(String nm, long l, long h, String desc) {
            super(nm, desc);
            default_low = low = l;
            default_high = high = h;
        }

        /**
         * The <code>set()</code> method updates the value of the option.
         *
         * @param val a string representation of the new value of the option.
         */
        public void set(String val) {
            CharacterIterator iter = new StringCharacterIterator(val);
            try {
                // check for leading [
                if (!StringUtil.peekAndEat(iter, '[')) parseError(name, "interval", val);

                String lstr = StringUtil.readDecimalString(iter, 12);
                low = java.lang.Long.parseLong(lstr);

                // check for ',' separator
                if (!StringUtil.peekAndEat(iter, ',')) parseError(name, "interval", val);

                String hstr = StringUtil.readDecimalString(iter, 12);
                high = java.lang.Long.parseLong(hstr);

                // check for trailing ]
                if (!StringUtil.peekAndEat(iter, ']')) parseError(name, "interval", val);

            } catch (NumberFormatException e) {
                // in case of NumberFormatException
                parseError(name, "interval", val);
            }
        }

        /**
         * The <code>getLow()</code> method returns the current lowest value of the interval for this option.
         *
         * @return the lowest value of the interval as a long.
         */
        public long getLow() {
            return low;
        }

        /**
         * The <code>getHigh()</code> method returns the current highest value of the interval for this
         * option.
         *
         * @return the highest value of the interval as a long.
         */
        public long getHigh() {
            return high;
        }

        /**
         * The <code>stringValue()</code> method returns a string representation of the value of the option.
         * This is used in debugging and reporting purposes.
         *
         * @return a string representation of the value of the option.
         */
        public String stringValue() {
            return "[" + low + ", " + high + ']';
        }

        /**
         * The <code>printHelp()</code> method prints out a textual paragraph of the help item for this option
         * to the terminal.
         */
        public void printHelp() {
            printHeader("interval", "[" + default_low + ',' + default_high + ']');
            printDescription();
        }
    }

    /**
     * The <code>Option.Str</code> class is an implementation of the <code>Option</code> class that
     * encapsulates a string.
     */
    public static class Str extends Option {
        /**
         * The <code>defvalue</code> field stores the default (initial) value for this option. It is used in
         * reporting the help item.
         */
        protected String defvalue;
        protected String value;

        /**
         * The constructor for the <code>Option.Str</code> class creates a new option that can store a string.
         * It is given an option name, a help description, and a default value.
         *
         * @param nm   the string name of the option
         * @param val  the default value of the option
         * @param desc the description of the option
         */
        public Str(String nm, String val, String desc) {
            super(nm, desc);
            value = val;
            defvalue = value;
        }

        /**
         * The <code>set()</code> method updates the value of the option.
         *
         * @param val a string representation of the new value of the option.
         */
        public void set(String val) {
            value = val;
        }

        /**
         * The <code>setNewDefault()</code> method sets a new default value for this option. This is useful
         * for inherited options that might have a different default value for different actions or different
         * simulations.
         *
         * @param val the new default value for this option
         */
        public void setNewDefault(String val) {
            defvalue = val;
            value = val;
        }

        /**
         * The <code>get()</code> method returns the current value of the option.
         *
         * @return the value of the option as a string.
         */
        public String get() {
            return value;
        }

        /**
         * The <code>stringValue()</code> method returns a string representation of the value of the option.
         * This is used in debugging and reporting purposes.
         *
         * @return a string representation of the value of the option.
         */
        public String stringValue() {
            return value;
        }

        /**
         * The <code>printHelp()</code> method prints out a textual paragraph of the help item for this option
         * to the terminal.
         */
        public void printHelp() {
            printHeader("string", defvalue);
            printDescription();
        }

        public boolean isBlank() {
            return "".equals(value);
        }
    }

    /**
     * The <code>Option.List</code> class is an implementation of the <code>Option</code> class that
     * encapsulates a list.
     */
    public static class List extends Option {
        protected java.util.List value;
        protected String orig;

        /**
         * The constructor for the <code>Option.List</code> class creates a new option that can store a list
         * of strings. It is given an option name, a help description, and a default value.
         *
         * @param nm   the string name of the option
         * @param val  the default value of the option
         * @param desc the description of the option
         */
        public List(String nm, String val, String desc) {
            super(nm, desc);
            parseString(val);
        }

        /**
         * The <code>set()</code> method updates the value of the option.
         *
         * @param val a string representation of the new value of the option.
         */
        public void set(String val) {
            parseString(val);
        }

        /**
         * The <code>setNewDefault()</code> method sets a new default value for this option. This is useful
         * for inherited options that might have a different default value for different actions or different
         * simulations.
         *
         * @param val the new default value for this option
         */
        public void setNewDefault(String val) {
            parseString(val);
        }

        /**
         * The <code>get()</code> method returns the current value of the option.
         *
         * @return the value of the option as a <code>java.util.List</code>.
         */
        public java.util.List get() {
            return value;
        }

        /**
         * The <code>stringValue()</code> method returns a string representation of the value of the option.
         * This is used in debugging and reporting purposes.
         *
         * @return a string representation of the value of the option.
         */
        public String stringValue() {
            return orig;
        }

        private void parseString(String val) {
            orig = val;
            value = StringUtil.toList(val);
        }

        public String[] toArray() {
            String[] result = new String[value.size()];
            Iterator i = value.iterator();
            int cntr = 0;
            while ( i.hasNext() ) {
                result[cntr++] = (String)i.next();
            }
            return result;
        }

        /**
         * The <code>printHelp()</code> method prints out a textual paragraph of the help item for this option
         * to the terminal.
         */
        public void printHelp() {
            String defvalue = "".equals(orig) ? "(null)" : orig;
            printHeader("list", defvalue);
            printDescription();
        }
    }

    /**
     * The <code>Option.Bool</code> class is an implementation of the <code>Option</code> class that
     * encapsulates a boolean.
     */
    public static class Bool extends Option {
        /**
         * The <code>defvalue</code> field stores the default (initial) value for this option. It is used in
         * reporting the help item.
         */
        protected final boolean defvalue;
        protected boolean value;

        /**
         * The constructor for the <code>Option.Long</code> class creates a new option that can store a
         * boolean value. It is given an option name, a help description, and a default value.
         *
         * @param nm   the string name of the option
         * @param val  the default value of the option
         * @param desc the description of the option
         */
        public Bool(String nm, boolean val, String desc) {
            super(nm, desc);
            value = val;
            defvalue = val;
        }

        /**
         * The <code>set()</code> method updates the value of the option.
         *
         * @param val a string representation of the new value of the option.
         */
        public void set(String val) {
            if ("true".equals(val) || "".equals(val)) {
                value = true;
            } else if ("false".equals(val)) {
                value = false;
            } else parseError(name, "boolean", val);
        }

        /**
         * The <code>get()</code> method returns the current value of the option.
         *
         * @return the value of the option as a boolean.
         */
        public boolean get() {
            return value;
        }

        /**
         * The <code>stringValue()</code> method returns a string representation of the value of the option.
         * This is used in debugging and reporting purposes.
         *
         * @return a string representation of the value of the option.
         */
        public String stringValue() {
            return String.valueOf(value);
        }

        /**
         * The <code>printHelp()</code> method prints out a textual paragraph of the help item for this option
         * to the terminal.
         */
        public void printHelp() {
            printHeader("boolean", String.valueOf(defvalue));
            printDescription();
        }
    }
}
