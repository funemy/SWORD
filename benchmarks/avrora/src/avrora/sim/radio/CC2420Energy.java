/*
 * "Copyright (c) 2009 Cork Institute of Technology, Ireland
 * All rights reserved."
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE CORK INSTITUTE OF TECHNOLOGY BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE CORK INSTITUTE
 * OF TECHNOLOGY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE CORK INSTITUTE OF TECHNOLOGY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 */

package avrora.sim.radio;

import cck.text.StringUtil;
/**
 * Constants for CC2420 radio energy consumption
 * 
 * @author Rodolfo de Paz 
 */
public abstract class CC2420Energy {  
   
    /**
     * <code>startMode</code> the default operating mode after turning on / reset
     */
    public static final int startMode = 0;

    /**
     * <code>modeName</code> names of the operating modes
     */
    public static final String[] modeName = {
        "Power Off:            ",
        "Power Down:           ",
        "Idle:              ",
        "Receive (Rx):         ",
        "Transmit (Tx):        "
    };


    /**
     * <code>modeAmpere</code> power consumption of the operating modes
     */
    public static final double[] modeAmpere = {                                             
        0.00000002, //off
        0.000020, //power down
        0.000426, //idle
        0.0188,  //receive
        0.0073421, // (PA_LEVEL=0)
        0.0077405, // (PA_LEVEL=1)
        0.0081261, // (PA_LEVEL=2)
        0.0085000, //transmit -25dBm (PA_LEVEL = 3)
        0.0088632, // (PA_LEVEL=4)
        0.0092169, // (PA_LEVEL=5)
        0.0095621, // (PA_LEVEL=6)
        0.0099000,// transmit -15dBm (PA_LEVEL = 7)
        0.0102316, // (PA_LEVEL=8)
        0.0105581, // (PA_LEVEL=9)
        0.0108805, // (PA_LEVEL=10)
        0.0112000, //transmit -10 dBm (PA_LEVEL = 11)
        0.0115181, // (PA_LEVEL=12)
        0.0118382, // (PA_LEVEL=13)
        0.0121642, // (PA_LEVEL=14)
        0.0125000, //transmit -7 dBm  (PA_LEVEL = 15)
        0.0128476, // (PA_LEVEL=12)
        0.0132016, // (PA_LEVEL=13)
        0.0135549, // (PA_LEVEL=14)
        0.0139000, // (PA_LEVEL=15)
        0.0142321, // (PA_LEVEL=16)
        0.0145552, // (PA_LEVEL=17)
        0.0135549, // (PA_LEVEL=18)
        0.0139000, // transmit -5 dBm (PA_LEVEL = 19)
        0.0142321, // (PA_LEVEL=20)
        0.0145552 , // (PA_LEVEL=21)
        0.0148758, // (PA_LEVEL=22)              
        0.0152000, // transmit -3 dBm (PA_LEVEL = 23)
        0.0155318, // (PA_LEVEL=24)
        0.0158649, // (PA_LEVEL=25)
        0.0161906, // (PA_LEVEL=26)
        0.0165000, // transmit -1 dBm (PA_LEVEL = 27)
        0.0167844, // (PA_LEVEL=28)
        0.0170351, // (PA_LEVEL=29)
        0.0172432, // (PA_LEVEL=30)
        0.0174000,// transmit  0 dBm (PA_LEVEL = 31)         
    };

    public static String[] allModeNames() {
        String[] modeName = new String[262];

        System.arraycopy(CC2420Energy.modeName, 0, modeName, 0, 4);
        for (int i = 0; i < 256; i++) {
            modeName[i + 4] = CC2420Energy.modeName[4] + StringUtil.leftJustify(i+":   ", 3);
        }
        return modeName;
    }
}
