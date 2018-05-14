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

package avrora.monitors;

import avrora.sim.Simulator;
import avrora.sim.util.MemPrint;
import avrora.core.Program;
import avrora.core.SourceMapping;

import cck.text.StringUtil;
import cck.util.Option;

import java.text.StringCharacterIterator;
import java.util.Iterator;

/**
 * The <code>PrintMonitor</code> gives apps a simple way to tell the
 * simulator to print a string or int to the screen
 *
 * @author John Regehr
 * @author Rodolfo de Paz
 */
public class PrintMonitor extends MonitorFactory {

    protected final Option.Str VARIABLENAME = newOption("VariableName","debugbuf1" ,
            "This option specifies the name of the variable to print");
    protected final Option.Str MAX = newOption("max","30" ,
            "This option specifies the length of the variable to print");
    protected final Option.Str LOG = newOption("printlogfile", "",
            "This option specifies whether the print monitor should log changes to each " +
            "node's energy state. If this option is specified, then each node's print " +
            "statements will be written to <option>.#, where '#' represents the " +
            "node ID.");
    protected final Option.Str BASEADDR = newOption("base", "",
            "This option specifies the base direction of the SRAM to watch. ");
    
    public class Monitor implements avrora.monitors.Monitor {
        public final MemPrint memprofile;        
        private final String varname = VARIABLENAME.get();             
        int LEN = Integer.parseInt(MAX.get());       
        int BASE;
        protected Simulator simulator;
        private String fileName;        

        Monitor(Simulator s) {
            this.simulator = s;           
            Program p = s.getProgram();               
            Iterator it = p.getSourceMapping().getIterator();        
            //If you enter the variable name will look into the map file fo
            while (it.hasNext()) {
                SourceMapping.Location tempLoc = (SourceMapping.Location)it.next();
                //Look for the label that equals the desired variable name inside the map file
                if (varname.equals(tempLoc.name)){            
                    String st = StringUtil.toHex((long)tempLoc.vma_addr,3);
                    st = st.substring(3,6);
                    BASE = StringUtil.readHexValue(new StringCharacterIterator(st),3);
                }                                         
            }
            //If you enter directly the addr will look into that addr
            if (!BASEADDR.isBlank()) BASE = Integer.parseInt(BASEADDR.get());
            if ( !LOG.isBlank() ) fileName = LOG.get() + simulator.getID();  
            else fileName = "";
            memprofile = new MemPrint(BASE, LEN, fileName);
            s.insertWatch(memprofile, BASE);                                    
        }

        public void report() {
            // do nothing.
        }
    }
     

    public PrintMonitor() {
        super("The \"print\" monitor watches a dedicated range of SRAM for instructions " +
                "to print a string or int to the screen.  Set the VariableName and avrora " +
                "will look directly inside the map file the part of the SRAM to print");
    }

    public avrora.monitors.Monitor newMonitor(Simulator s) {
        return new Monitor(s);
    }
}
