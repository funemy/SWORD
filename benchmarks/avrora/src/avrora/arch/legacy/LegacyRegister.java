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

package avrora.arch.legacy;

import java.util.HashMap;
import java.util.HashSet;

/**
 * The <code>LegacyRegister</code> class represents a register available on the AVR instruction set. All registers
 * in the instruction set architecture are represented as objects that have a name and a number. Those objects
 * are singletons and are public static final fields of this class.<br><br>
 * <p/>
 * Additionally, the <code>LegacyRegister</code> class contains sets of registers that are used in verifying the
 * operand constraints of each individual instruction as defined in the AVR instruction set reference. An
 * example of an operand constraint is that ldi (load immediate) takes as operands one of the general purpose
 * registers {r17...r31} and an immediate. Other instructions take certain subsets of the instructions. Those
 * register sets are allocated once here and are exposed as static fields in this class.
 *
 * @author Ben L. Titzer
 * @see LegacyOperand
 * @see LegacyInstr
 */
public class LegacyRegister {

    private static final HashMap registers = initializeRegisterMap();

    public static final LegacyRegister R0 = getRegisterByNumber(0);
    public static final LegacyRegister R1 = getRegisterByNumber(1);
    public static final LegacyRegister R2 = getRegisterByNumber(2);
    public static final LegacyRegister R3 = getRegisterByNumber(3);
    public static final LegacyRegister R4 = getRegisterByNumber(4);
    public static final LegacyRegister R5 = getRegisterByNumber(5);
    public static final LegacyRegister R6 = getRegisterByNumber(6);
    public static final LegacyRegister R7 = getRegisterByNumber(7);
    public static final LegacyRegister R8 = getRegisterByNumber(8);
    public static final LegacyRegister R9 = getRegisterByNumber(9);
    public static final LegacyRegister R10 = getRegisterByNumber(10);
    public static final LegacyRegister R11 = getRegisterByNumber(11);
    public static final LegacyRegister R12 = getRegisterByNumber(12);
    public static final LegacyRegister R13 = getRegisterByNumber(13);
    public static final LegacyRegister R14 = getRegisterByNumber(14);
    public static final LegacyRegister R15 = getRegisterByNumber(15);
    public static final LegacyRegister R16 = getRegisterByNumber(16);
    public static final LegacyRegister R17 = getRegisterByNumber(17);
    public static final LegacyRegister R18 = getRegisterByNumber(18);
    public static final LegacyRegister R19 = getRegisterByNumber(19);
    public static final LegacyRegister R20 = getRegisterByNumber(20);
    public static final LegacyRegister R21 = getRegisterByNumber(21);
    public static final LegacyRegister R22 = getRegisterByNumber(22);
    public static final LegacyRegister R23 = getRegisterByNumber(23);
    public static final LegacyRegister R24 = getRegisterByNumber(24);
    public static final LegacyRegister R25 = getRegisterByNumber(25);
    public static final LegacyRegister R26 = getRegisterByNumber(26);
    public static final LegacyRegister R27 = getRegisterByNumber(27);
    public static final LegacyRegister R28 = getRegisterByNumber(28);
    public static final LegacyRegister R29 = getRegisterByNumber(29);
    public static final LegacyRegister R30 = getRegisterByNumber(30);
    public static final LegacyRegister R31 = getRegisterByNumber(31);

    public static final LegacyRegister X = getRegisterByName("x");
    public static final LegacyRegister Y = getRegisterByName("y");
    public static final LegacyRegister Z = getRegisterByName("z");

    private static final LegacyRegister[] REGS_0_31 = {
        R0, R1, R2, R3, R4, R5, R6, R7,
        R8, R9, R10, R11, R12, R13, R14, R15,
        R16, R17, R18, R19, R20, R21, R22, R23,
        R24, R25, R26, R27, R28, R29, R30, R31
    };
    private static final LegacyRegister[] EREGS = {
        R0, R2, R4, R6, R8, R10, R12, R14,
        R16, R18, R20, R22, R24, R26, R28, R30,
    };
    private static final LegacyRegister[] REGS_16_31 = {
        R16, R17, R18, R19, R20, R21, R22, R23,
        R24, R25, R26, R27, R28, R29, R30, R31
    };
    private static final LegacyRegister[] REGS_16_23 = {
        R16, R17, R18, R19,
        R20, R21, R22, R23,
    };
    private static final LegacyRegister[] REGS_XYZ = {
        X, Y, Z
    };
    private static final LegacyRegister[] REGS_YZ = {
        Y, Z
    };
    private static final LegacyRegister[] REGS_Z = {
        Z
    };
    private static final LegacyRegister[] REGS_RDL = {
        R24, R26, R28, R30
    };

    public static final Set GPR_set = new Set(REGS_0_31);
    public static final Set HGPR_set = new Set(REGS_16_31);
    public static final Set MGPR_set = new Set(REGS_16_23);
    public static final Set EGPR_set = new Set(EREGS);
    public static final Set ADR_set = new Set(REGS_XYZ);
    public static final Set RDL_set = new Set(REGS_RDL);
    public static final Set YZ_set = new Set(REGS_YZ);
    public static final Set Z_set = new Set(REGS_Z);

    private static HashMap initializeRegisterMap() {
        HashMap map = new HashMap();

        for (int cntr = 0; cntr < 32; cntr++) {
            LegacyRegister reg = new LegacyRegister("r" + cntr, cntr, 8);
            map.put("r" + cntr, reg);
            map.put("R" + cntr, reg);
        }

        LegacyRegister reg = new LegacyRegister("X", 26, 16);
        map.put("x", reg);
        map.put("X", reg);

        reg = new LegacyRegister("Y", 28, 16);
        map.put("y", reg);
        map.put("Y", reg);

        reg = new LegacyRegister("Z", 30, 16);
        map.put("z", reg);
        map.put("Z", reg);

        return map;
    }

    /**
     * The <code>getRegisterByName()</code> method retrieves a reference to the <code>LegacyRegister</code> instance
     * with the given string name. This method is not case sensitive.
     *
     * @param name the name of the register as a string
     * @return a reference to the <code>LegacyRegister</code> object representing the register if a register of that
     *         name exists; null otherwise
     */
    public static LegacyRegister getRegisterByName(String name) {
        return (LegacyRegister)registers.get(name);
    }

    /**
     * The <code>getRegisterByNumber()</code> method retrieves a reference to the <code>LegacyRegister</code>
     * instance with the given offset in the register file.
     *
     * @param num the integer number of the register to retrieve
     * @return a reference to the <code>LegacyRegister</code> object representing the chosen register
     */
    public static LegacyRegister getRegisterByNumber(int num) {
        return getRegisterByName("r" + num);
    }


    private final String name;
    private final int number;
    private final int width;

    private LegacyRegister(String nm, int num, int w) {
        name = nm;
        number = num;
        width = w;
    }

    /**
     * The <code>hashCode()</code> computes the hash code of this register so that registers can be inserted
     * in hashmaps and hashsets. This implementation of register simply uses the hash code of the name of the
     * register as its hash code.
     *
     * @return an integer that represents the hash code of this register
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * The <code>toString()</code> method coverts this register to a string. This implementation simply
     * returns the name of the register.
     *
     * @return a string representation of this register
     */
    public String toString() {
        return name;
    }

    /**
     * The <code>getName()</code> method returns the name of the instruction as a string.
     *
     * @return the name of the instruction
     */
    public String getName() {
        return name;
    }

    /**
     * The <code>getNumber()</code> method returns the "number" of this register, meaning the offset into the
     * register file.
     *
     * @return the number of this register
     */
    public int getNumber() {
        return number;
    }

    /**
     * The <code>getWidth()</code> method returns the width of the register in bits.
     *
     * @return the number of bits in this register
     */
    public int getWidth() {
        return width;
    }

    /**
     * The <code>nextRegister()</code> method returns a reference to the register that immediately follows
     * this register in the register file. This is needed when treating multiple registers as a single value,
     * etc.
     *
     * @return the register immediately following this register in the register file
     */
    public LegacyRegister nextRegister() {
        return REGS_0_31[number + 1];
    }

    /**
     * The <code>Set</code> class represents a set of registers. This is used to represent classes of
     * registers that are used as operands to various instructions. For example, an instruction might expect
     * one of its operands to be a general purpose register that has a number greater than 15; a set of those
     * registers can be constructed and then a membership test performed.
     * <p/>
     * In practice, the needed register sets are all allocated statically.
     */
    public static class Set {

        /**
         * The <code>contents</code> field stores a string that represents a summary of the registers that are
         * in this set. An example string for the even registers would be <code>"{r0, r2, ..., r30}"</code>.
         */
        public final String contents;

        private final HashSet registerSet;

        /**
         * The constructor for the <code>Set</code> class takes a string that represents the contents of the
         * registers and an array of registers that are members of the set. It then constructs an internal
         * hash set for fast membership tests.
         *
         * @param regs an array of registers that are members of this set
         */
        Set(LegacyRegister[] regs) {
            registerSet = new HashSet(2 * regs.length);
            for (int cntr = 0; cntr < regs.length; cntr++) {
                registerSet.add(regs[cntr]);
            }

            StringBuffer buf = new StringBuffer("{");
            for (int cntr = 0; cntr < regs.length; cntr++) {
                registerSet.add(regs[cntr]);
                // abreviate large sets
                if (cntr == 2 && regs.length > 4) buf.append("..., ");
                // print first two, and last, or all if the set is fewer than five
                if (cntr < 2 || cntr == regs.length - 1 || regs.length < 5) {
                    buf.append(regs[cntr]);
                    if (cntr < regs.length - 1) buf.append(", ");
                }
            }
            buf.append('}');
            contents = buf.toString();
        }

        /**
         * The <code>contains()</code> method tests for membership. Given a register, it will return true if
         * that register is a member of this set, and false otherwise.
         *
         * @param reg the register to test membership of
         * @return true if the specified register is a member of this set; false otherwise
         */
        public boolean contains(LegacyRegister reg) {
            return registerSet.contains(reg);
        }

        /**
         * The <code>toString()</code> method converts this set to a string representation. In this
         * implementation, it simply returns the string representation of the contents.
         *
         * @return a string representation of this register set
         */
        public String toString() {
            return contents;
        }
    }

}
