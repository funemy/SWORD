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

package avrora.sim.mcu;

import cck.text.StringUtil;
import cck.util.Arithmetic;
import cck.util.Util;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;

/**
 * The <code>RegisterLayout</code> class stores information about the IO register addresses for a particular
 * microcontroller model. It maps the names of the IO registers to their addresses and also contains information
 * about which fields are stored in which registers. When a microcontroller is instantiated, this class is used
 * to create an instance of <code>RegisterSet</code> that corresponds to the layout of the registers on
 * this device.
 *
 * @author Ben L. Titzer
 */
public class RegisterLayout {

    protected static class RegisterInfo {
        final String name;
        final int ior_num;
        SubField[] subfields;

        RegisterInfo(String n, int ior) {
            name = n;
            ior_num = ior;
        }
    }

    protected static class Field {
        final String name;
        int length;
        SubField[] subfields;

        Field(String n) {
            name = n;
        }

        void add(SubField sf) {
            if ( subfields == null ) {
                subfields = new SubField[1];
            } else {
                SubField[] nsf = new SubField[subfields.length+1];
                System.arraycopy(subfields, 0, nsf, 0, subfields.length);
                subfields = nsf;
            }
            subfields[subfields.length - 1] = sf;
            int highbit = sf.field_low_bit + sf.length;
            if ( highbit > length ) length = highbit;
        }
    }

    public static final Field UNUSED = new Field("UNUSED");
    public static final Field RESERVED = new Field("RESERVED");

    protected static class SubField {
        final Field field;       // the name of the field
        final int ior;            // the IO register containing this subfield
        int length;
        int ior_low_bit;    // low bit in the IO register
        int field_low_bit;  // offset (low bit) in the field
        int mask;           // low bit in the IO register
        boolean commit;     // true if a write to this subfield should commit

        SubField(Field f, int ior) {
            this.field = f;
            this.ior = ior;
        }
    }

    /**
     * The <code>ioreg_size</code> field stores the number of IO registers on this microcontroller.
     */
    public final int ioreg_size;

    /**
     * The <code>ioreg_length</code> field stores the length of each register in bits.
     */
    public final int ioreg_length;

    /**
     * The <code>ioregAssignments</code> field stores a reference to a hashmap from IO register names to their
     * addresses.
     */
    protected final HashMap ioregAssignments;

    protected final RegisterInfo[] info;

    /**
     * The <code>fields</code> field stores a reference to a hashmap that maps from a field name to a representation
     * of the field.
     */
    protected final HashMap fields;

    /**
     * The constructor for the <code>RegisterLayout</code> class creates a new register layout with the specified
     * size.
     * @param is the number of registers in this register layout
     */
    public RegisterLayout(int is, int rlength) {
        ioreg_size = is;
        ioregAssignments = new HashMap();
        fields = new HashMap();
        info = new RegisterInfo[is];
        ioreg_length = rlength;
    }

    /**
     * The <code>addIOReg()</code> method adds a new IO register with the specified name and address to this
     * register layout.
     * @param n the name of the IO register
     * @param ior_num the address of the IO register
     */
    public void addIOReg(String n, int ior_num) {
        if ( ior_num >= ioreg_size )
            throw new Util.Error("Layout Error", "invalid register address "+ior_num+" for register "+ StringUtil.quote(n));
        RegisterInfo i = new RegisterInfo(n, ior_num);
        info[ior_num] = i;
        ioregAssignments.put(n, i);
    }

    /**
     * The <code>addIOReg()</code> method adds a new IO register with the specified name and address to this
     * register layout. This variant of the method also accepts a format string describing which fields are
     * in this IO register. The format description is used to automatically generate an instance of the IO
     * register that puts the right bits of the right fields in the right places when the register is
     * written to.
     * @param n the name of the IO register
     * @param ior_num the address of the IO register
     * @param format the format of the IO register as a string describing which fields and which bits of
     * which fields are present in this IO register
     */
    public void addIOReg(String n, int ior_num, String format) {
        if ( ior_num >= ioreg_size )
            throw new Util.Error("Layout Error", "invalid register address "+ior_num+" for register "+ StringUtil.quote(n));
        RegisterInfo i = new RegisterInfo(n, ior_num);
        i.subfields = parseSubFields(n, ior_num, format);
        info[ior_num] = i;
        ioregAssignments.put(n, i);
    }

    /**
     * The <code>getIOReg()</code> method retrieves the IO register number for the given IO
     * Register name for this microcontroller.
     * @param n the name of the IO register such as "TCNT0"
     * @return an integer representing the IO register number if it exists
     * @throws NoSuchElementException if the specified IO register name does not have an assignment
     */
    public int getIOReg(String n) {
        RegisterInfo i = (RegisterInfo)ioregAssignments.get(n);
        if ( i == null )
            throw new NoSuchElementException(StringUtil.quote(n)+" IO register not found");
        return i.ior_num;
    }

    /**
     * The <code>hasIOReg()</code> method simply checks whether this register layout has a register with
     * the specified name.
     * @param n the name of the IO register
     * @return true if this layout has a register with the specified name; false otherwise
     */
    public boolean hasIOReg(String n) {
        return ioregAssignments.containsKey(n);
    }

    /**
     * The <code>instantiate()</code> method creates a new register set that contains the actual register
     * implementations that can be used in simulation.
     * @return a new register set to be used in simulation
     */
    public RegisterSet instantiate() {
        return new RegisterSet(this);
    }

    public String getRegisterName(int ior) {
        RegisterInfo registerInfo = info[ior];
        return registerInfo != null ? registerInfo.name : "";
    }

    private SubField[] parseSubFields(String name, int ior, String desc) {
        int totalbits = 0;
        int count = 0;
        SubField[] sfs = new SubField[8];
        StringCharacterIterator i = new StringCharacterIterator(desc);
        int ior_hbit = 7;
        while ( ior_hbit >= 0 && i.current() != CharacterIterator.DONE ) {
            if ( i.current() == '.') {
                ior_hbit = readUnusedField(i, sfs, count, ior_hbit);
                totalbits += sfs[count].length;
            } else if ( i.current() == 'x') {
                ior_hbit = readReservedField(i, sfs, count, ior_hbit);
                totalbits += sfs[count].length;
            } else {
                ior_hbit = readNamedField(i, ior, sfs, count, ior_hbit);
                totalbits += sfs[count].length;
            }
            count++;
            StringUtil.peekAndEat(i, ',');
            StringUtil.skipWhiteSpace(i);
        }

        // check that there are exactly 8 bits
        if ( totalbits != ioreg_length ) {
            throw new Util.Error("Layout Error", "expected "+ioreg_length+" bits, found: "+totalbits+" in "+StringUtil.quote(name));
        }

        // resize the array to be smaller
        SubField[] subFields = new SubField[count];
        System.arraycopy(sfs, 0, subFields, 0, count);
        // calculate the commit points (i.e. last write to the field)
        HashSet fs = new HashSet();
        for ( int cntr = subFields.length - 1; cntr >= 0; cntr-- ) {
            SubField subField = subFields[cntr];
            if ( !fs.contains(subField.field) ) subField.commit = true;
            fs.add(subField.field);
        }
        return subFields;
    }

    private int readNamedField(StringCharacterIterator i, int ior, SubField[] sfs, int count, int ior_hbit) {
        // named field is specified
        String fid = StringUtil.readIdentifier(i);
        Field field = getField(fid);
        SubField sf = new SubField(field, ior);
        field.add(sf);
        sfs[count] = sf;
        if ( StringUtil.peekAndEat(i, '[') ) {
            ior_hbit = readBitRange(i, ior_hbit, sf);
        } else {
            ior_hbit = readBit(ior_hbit, sf);
        }
        return ior_hbit;
    }

    private int readReservedField(StringCharacterIterator i, SubField[] sfs, int count, int ior_hbit) {
        // reserved field is specified
        SubField sf = new SubField(RESERVED, -1);
        sfs[count] = sf;
        ior_hbit = eat(ior_hbit, i, sf, 'x');
        return ior_hbit;
    }

    private int readUnusedField(StringCharacterIterator i, SubField[] sfs, int count, int ior_hbit) {
        // unused field is specified
        SubField sf = new SubField(UNUSED, -1);
        sfs[count] = sf;
        ior_hbit = eat(ior_hbit, i, sf, '.');
        return ior_hbit;
    }

    private int readBit(int ior_hbit, SubField sf) {
        // no bit range is specified; assume only one bit
        sf.ior_low_bit = ior_hbit;
        sf.field_low_bit = 0;
        sf.mask = 0x1;
        sf.length = 1;
        ior_hbit--;
        return ior_hbit;
    }

    private int readBitRange(StringCharacterIterator i, int ior_hbit, SubField sf) {
        // a bit range is specified; create a subfield
        int fhbit = StringUtil.readDecimalValue(i, 1);
        int flbit = fhbit;
        if ( StringUtil.peekAndEat(i, ':')) {
            flbit =  StringUtil.readDecimalValue(i, 1);
        }
        int length = fhbit - flbit + 1;
        sf.ior_low_bit = ior_hbit - length + 1;
        sf.field_low_bit = flbit;
        sf.mask = 0xff >> (8 - length);
        sf.length = length;
        StringUtil.peekAndEat(i, ']');
        ior_hbit -= length;
        return ior_hbit;
    }

    private int eat(int ior_hbit, StringCharacterIterator i, SubField sf, char c) {
        int hbit = ior_hbit;
        while ( i.current() == c ) {
            sf.length++;
            i.next();
        }
        sf.ior_low_bit = hbit - sf.length + 1;
        sf.mask = Arithmetic.getBitMask(sf.length);
        ior_hbit -= sf.length;
        return ior_hbit;
    }

    private Field getField(String name) {
        Field f = (Field)fields.get(name);
        if ( f == null ) {
            f = new Field(name);
            fields.put(name, f);
        }
        return f;
    }
}
