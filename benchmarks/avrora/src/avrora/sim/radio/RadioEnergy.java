/*
 * Created on 26.10.2004
 *
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
package avrora.sim.radio;

import cck.text.StringUtil;

/**
 * Constants for CC1000 radio energy consumption
 *
 * @author Olaf Landsiedel
 */
public abstract class RadioEnergy {

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
        "Crystal:              ",
        "Crystal + Bias:       ",
        "Crystal + Bias + Syn: ",
        "Receive (Rx):         ",
        "Transmit (Tx):        "
    };


    /**
     * <code>modeAmpere</code> power consumption of the operating modes
     */
    public static final double[] modeAmpere = {
        0.0, //off
        0.0000002, //power down
        0.00006, //crystal
        0.00138, //crystal + bias
        0.0055, //crystal + bias + syn
        0.0096, //receive
        0.00630, //transmit 0
        0.00882, //transmit 1
        0.00903, //...
        0.00915,
        0.00935,
        0.00956,
        0.00976,
        0.00992,
        0.01007,
        0.01022,
        0.01038,
        0.01057,
        0.01079,
        0.01101,
        0.01120,
        0.01134,
        0.01144,
        0.01153,
        0.01163,
        0.01172,
        0.01181,
        0.01189,
        0.01198,
        0.01206,
        0.01214,
        0.01222,
        0.01230,
        0.01237,
        0.01245,
        0.01252,
        0.01259,
        0.01266,
        0.01273,
        0.01279,
        0.01286,
        0.01292,
        0.01298,
        0.01304,
        0.01310,
        0.01316,
        0.01321,
        0.01327,
        0.01332,
        0.01338,
        0.01343,
        0.01348,
        0.01353,
        0.01358,
        0.01362,
        0.01367,
        0.01372,
        0.01376,
        0.01380,
        0.01385,
        0.01389,
        0.01393,
        0.01397,
        0.01401,
        0.01405,
        0.01409,
        0.01413,
        0.01417,
        0.01421,
        0.01425,
        0.01429,
        0.01432,
        0.01436,
        0.01440,
        0.01443,
        0.01447,
        0.01451,
        0.01454,
        0.01458,
        0.01462,
        0.01465,
        0.01469,
        0.01473,
        0.01476,
        0.01480,
        0.01484,
        0.01487,
        0.01491,
        0.01495,
        0.01499,
        0.01503,
        0.01507,
        0.01511,
        0.01515,
        0.01519,
        0.01523,
        0.01528,
        0.01532,
        0.01536,
        0.01541,
        0.01546,
        0.01550,
        0.01555,
        0.01560,
        0.01565,
        0.01569,
        0.01574,
        0.01579,
        0.01583,
        0.01588,
        0.01593,
        0.01597,
        0.01602,
        0.01606,
        0.01611,
        0.01615,
        0.01620,
        0.01624,
        0.01629,
        0.01633,
        0.01638,
        0.01642,
        0.01647,
        0.01651,
        0.01656,
        0.01660,
        0.01665,
        0.01669,
        0.01674,
        0.01679,
        0.01683,
        0.01688,
        0.01693,
        0.01697,
        0.01702,
        0.01707,
        0.01712,
        0.01716,
        0.01721,
        0.01726,
        0.01731,
        0.01736,
        0.01741,
        0.01746,
        0.01751,
        0.01756,
        0.01761,
        0.01766,
        0.01771,
        0.01776,
        0.01781,
        0.01786,
        0.01791,
        0.01796,
        0.01801,
        0.01806,
        0.01811,
        0.01816,
        0.01821,
        0.01826,
        0.01831,
        0.01837,
        0.01842,
        0.01847,
        0.01852,
        0.01857,
        0.01862,
        0.01867,
        0.01872,
        0.01878,
        0.01883,
        0.01888,
        0.01893,
        0.01898,
        0.01903,
        0.01908,
        0.01913,
        0.01919,
        0.01924,
        0.01929,
        0.01934,
        0.01939,
        0.01944,
        0.01949,
        0.01954,
        0.01959,
        0.01964,
        0.01969,
        0.01974,
        0.01979,
        0.01984,
        0.01989,
        0.01994,
        0.01999,
        0.02004,
        0.02009,
        0.02014,
        0.02019,
        0.02024,
        0.02029,
        0.02033,
        0.02038,
        0.02042,
        0.02046,
        0.02050,
        0.02054,
        0.02058,
        0.02061,
        0.02065,
        0.02068,
        0.02072,
        0.02075,
        0.02078,
        0.02082,
        0.02085,
        0.02088,
        0.02091,
        0.02095,
        0.02098,
        0.02101,
        0.02105,
        0.02108,
        0.02112,
        0.02115,
        0.02119,
        0.02123,
        0.02127,
        0.02131,
        0.02135,
        0.02140,
        0.02144,
        0.02149,
        0.02154,
        0.02159,
        0.02165,
        0.02171,
        0.02177,
        0.02183,
        0.02189,
        0.02196,
        0.02203,
        0.02211,
        0.02219,
        0.02227,
        0.02235,
        0.02244,
        0.02253,
        0.02265,
        0.02280,
        0.02300,
        0.02323,
        0.02349,
        0.02377,
        0.02408,
        0.02441,
        0.02475,
        0.02511,
        0.02547,
        0.02584,
        0.02620,
        0.02656,
        0.02692
    };

    public static String[] allModeNames() {
        String[] modeName = new String[262];

        System.arraycopy(RadioEnergy.modeName, 0, modeName, 0, 6);
        for (int i = 0; i < 256; i++) {
            modeName[i + 6] = RadioEnergy.modeName[6] + StringUtil.leftJustify(i+":   ", 3);
        }
        return modeName;
    }
}
