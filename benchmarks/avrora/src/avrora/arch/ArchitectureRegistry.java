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
 *
 * Creation date: Nov 16, 2005
 */

package avrora.arch;

import avrora.Defaults;
import avrora.arch.avr.AVRArchitecture;
import avrora.arch.legacy.LegacyArchitecture;
import avrora.arch.msp430.MSP430Architecture;
import cck.help.HelpCategory;
import cck.util.ClassMap;

/**
 * The <code>ArchitectureRegistry</code> class implements a registry of all the instruction set architectures
 * supported by Avrora.
 *
 * @author Ben L. Titzer
 */
public class ArchitectureRegistry {

    private static ClassMap archMap;

    public static synchronized void addArchitectures() {
        if (archMap == null) {
            archMap = new ClassMap("Architecture", AbstractArchitecture.class);
            //-- DEFAULT ACTIONS
            archMap.addInstance("avr", LegacyArchitecture.INSTANCE);
            archMap.addInstance("avr-new", AVRArchitecture.INSTANCE);
            archMap.addInstance("msp430", MSP430Architecture.INSTANCE);

            // plug in a new help category for simulations accesible with "-help simulations"
            HelpCategory hc = new HelpCategory("architectures", "Help for supported instruction set architectures.");
            hc.addOptionValueSection("ARCHITECTURES",
                    "When running a simulation or other program analysis tool, Avrora derives information " +
                    "about the CPU architecture from an internal specification. In order to select the appropriate " +
                    "architecture, each one is named and can be selected with command line options.",
                    "-arch", archMap);
            Defaults.addMainCategory(hc);
            Defaults.addSubCategories(archMap);
        }
    }

    public static AbstractArchitecture getArchitecture(String s) {
        addArchitectures();
        return (AbstractArchitecture)archMap.getObjectOfClass(s);
    }
}
