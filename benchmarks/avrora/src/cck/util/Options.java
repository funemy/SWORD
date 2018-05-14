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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

/**
 * The <code>Options</code> class represents a collection of command line options and utility methods for
 * parsing the command line. Very useful for getting cheap and powerful parsing of command line options.
 *
 * @author Ben L. Titzer
 */
public class Options {

    protected final HashMap knownValues;

    protected String[] arguments;

    public Options() {
        knownValues = new HashMap();
    }

    public Option.Bool newOption(String name, boolean val, String desc) {
        Option.Bool o = new Option.Bool(name, val, desc);
        knownValues.put(name, o);
        return o;
    }

    public Option.Str newOption(String name, String val, String desc) {
        Option.Str o = new Option.Str(name, val, desc);
        knownValues.put(name, o);
        return o;
    }

    public Option.List newOptionList(String name, String val, String desc) {
        Option.List o = new Option.List(name, val, desc);
        knownValues.put(name, o);
        return o;
    }

    public Option.Long newOption(String name, long val, String desc) {
        Option.Long o = new Option.Long(name, val, desc);
        knownValues.put(name, o);
        return o;
    }

    public Option.Double newOption(String name, double val, String desc) {
        Option.Double o = new Option.Double(name, val, desc);
        knownValues.put(name, o);
        return o;
    }

    public Option.Interval newOption(String name, long l, long h, String desc) {
        Option.Interval o = new Option.Interval(name, l, h, desc);
        knownValues.put(name, o);
        return o;
    }

    public String getOptionValue(String name) {
        Option option = (Option) knownValues.get(name);
        return option == null ? null : option.stringValue();
    }

    public String getOptionValue(String name, String def) {
        Option option = (Option) knownValues.get(name);
        return option == null ? def : option.stringValue();
    }

    public Option getOption(String name) {
        return (Option) knownValues.get(name);
    }

    public boolean hasOption(String name) {
        return knownValues.get(name) != null;
    }

    public String[] getArguments() {
        return arguments;
    }

    public void parseCommandLine(String[] args) {
        // parse the options
        int cntr = 0;
        for (; cntr < args.length; cntr++) {
            if (args[cntr].charAt(0) != '-') break;
            parseOption(args[cntr]);
        }

        int left = args.length - cntr;

        arguments = new String[left];
        System.arraycopy(args, cntr, arguments, 0, left);
    }

    protected void parseOption(String opt) {
        String optname, value;

        int index = opt.indexOf('=');
        if (index < 0) { // naked option
            optname = opt.substring(1, opt.length());
            value = "";
        } else {
            value = opt.substring(index + 1);
            optname = opt.substring(1, index);
        }

        setOption(optname, value);

    }

    public void setOption(String optname, String value) {
        Option option = (Option) knownValues.get(optname);

        if (option == null) {
            option = new Option.Str(optname, value, "");
            knownValues.put(optname, option);
        }
        option.set(value);
    }

    public Collection getAllOptions() {
        return knownValues.values();
    }

    public void process(Options o) {
        Iterator i = o.knownValues.keySet().iterator();

        while (i.hasNext()) {
            String name = (String) i.next();
            String val = ((Option) o.knownValues.get(name)).stringValue();
            setOption(name, val);
        }
    }

    public void process(Properties p) {
        Iterator i = p.keySet().iterator();

        while (i.hasNext()) {
            String name = (String) i.next();
            String val = p.getProperty(name);
            setOption(name, val);
        }
    }

    public void loadFile(String fname) throws IOException {
        //checkFileExists(fname);
        File f = new File(fname);
        Properties defs = new Properties();
        defs.load(new FileInputStream(f));
        process(defs);
    }

}
