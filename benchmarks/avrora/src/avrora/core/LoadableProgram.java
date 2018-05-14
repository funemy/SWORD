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

import avrora.Main;
import cck.util.Util;
import java.io.File;

/**
 * The <code>LoadableProgram</code> class represents a reference to a program on the disk.
 * Since the user may want to reload the program (after recompiling it, for example),
 * this class supports the ability to reload the program from disk.
 *
 * @author Ben L. Titzer
 */
public class LoadableProgram {

    public final String fname;
    public final File file;
    protected Program program;

    /**
     * The constructor for the <code>LoadableProgram</code> class creates a new instance with
     * a reference to the file on the disk. The program is NOT automatically loaded.
     * @param f the file containing the program
     */
    public LoadableProgram(File f) {
        file = f;
        fname = f.getAbsolutePath();
    }

    /**
     * The constructor for the <code>LoadableProgram</code> class creates a new instance with
     * a reference to the file on the disk. The program is NOT automatically loaded.
     * @param fname the filename of the file containing the program
     */
    public LoadableProgram(String fname) {
        file = new File(fname);
        this.fname = fname;
    }

    /**
     * The <code>getProgram()</code> method gets the current representation of the program stored
     * in this object. It will NOT load the program if it has not been loaded yet.
     * @return A program representing a "compiled" version of the file
     */
    public Program getProgram() {
        if ( program == null )
            throw Util.failure("Program "+file+" must be loaded before use");
        return program;
    }

    /**
     * The <code>load()</code> method loads (or reloads) the program from the disk.
     */
    public void load() throws Exception {
        program = Main.loadProgram(new String[] { fname });
    }

    /**
     * The <code>getName()</code> method returns the name of the program, i.e. the name of the file
     * containing the program.
     * @return the name of the file without its absolute path
     */
    public String getName() {
        return file.getName();
    }
}
