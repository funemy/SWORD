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

import avrora.arch.AbstractArchitecture;
import avrora.arch.ArchitectureRegistry;
import cck.help.HelpCategory;
import cck.text.StringUtil;
import cck.util.*;
import java.util.Iterator;

/**
 * The <code>ProgramReader</code> class represents an object capable of reading a program given the special
 * command line arguments. It may for example read source assembly and produce a simplified program.
 */
public abstract class ProgramReader extends HelpCategory {

    public final Option.Str ARCH = newOption("arch", "avr",
            "This option specifies the name of the instruction set architecture for the " +
            "specified program. This architecture option is used to retrieve an appropriate " +
            "disassembler and interpreter for the program.");
    public final Option.List INDIRECT_EDGES = newOptionList("indirect-edges", "",
            "This option can be used to specify the possible targets of indirect calls and " +
            "jumps within a program, which may be needed in performing stack analysis or " +
            "building a control flow graph. Each element of the list is a pair of " +
            "program addresses separated by a colon, where a program address can be a " +
            "label or a hexadecimal number preceded by \"0x\". The first program address " +
            "is the address of the indirect call or jump instruction and the second program " +
            "address is a possible target.");
    /**
     * The <code>read()</code> method will read a program in and produce a simplified format.
     *
     * @param args the command line arguments
     * @return a program instance representing the program
     * @throws Exception
     */
    public abstract Program read(String[] args) throws Exception;

    /**
     * The constructor for the <code>ProgramReader</code> class builds a new reader with the specified
     * help text. Since a program reader is also a help category, the constructor will also add an options
     * section containing help for each specific option.
     * @param h the help text for this reader
     */
    protected ProgramReader(String h) {
        super("reader", h);

        addSection("OVERVIEW", help);
        addOptionSection("Help for specific options is below.", options);
    }

    /**
     * The <code>addIndirectEdges()</code> method adds any indirect edges specified in the "-indirect-edges"
     * option to the program representation.
     * @param p the program to add indirect edges to
     */
    protected void addIndirectEdges(Program p) {
        Iterator i = INDIRECT_EDGES.get().iterator();
        while (i.hasNext()) {
            String s = (String)i.next();
            int ind = s.indexOf(':');
            if (ind <= 0)
                throw Util.failure("invalid indirect edge format: " + StringUtil.quote(s));
            SourceMapping sm = p.getSourceMapping();
            SourceMapping.Location loc = sm.getLocation(s.substring(0, ind));
            SourceMapping.Location tar = sm.getLocation(s.substring(ind + 1));
            p.addIndirectEdge(loc.lma_addr, tar.lma_addr);
        }
    }

    /**
     * The <code>getArchitecture()</code> method returns a reference to the architecture specified
     * at the command line. It uses the values in <code>Defaults</code> to translate a string name
     * specified as the option's value to an <code>AbstractArchitecture</code> instance.
     *
     * @return a reference to the architecture instance specified as an option
     */
    public AbstractArchitecture getArchitecture() {
        return ArchitectureRegistry.getArchitecture(ARCH.get());
    }

}
