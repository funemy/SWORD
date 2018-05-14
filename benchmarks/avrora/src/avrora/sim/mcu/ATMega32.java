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

import avrora.arch.avr.AVRProperties;
import avrora.arch.legacy.LegacyInterpreter;
import avrora.core.Program;
import avrora.sim.*;
import avrora.sim.clock.ClockDomain;
import cck.util.Arithmetic;
import java.util.HashMap;

/**
 * The <code>ATMega32</code> class represents the ATMega32 microcontroller from Atmel. This
 * microcontroller has 32Kb code, 2KB SRAM, 1KB EEPROM, and a host of internal devices such as
 * ADC, SPI, and timers.
 *
 * @author Ben L. Titzer
 */
public class ATMega32 extends ATMegaFamily {

    public static final int _1kb = 1024;

    public static final int ATMEGA32_IOREG_SIZE = 64;
    public static final int ATMEGA32_SRAM_SIZE = 2 * _1kb;
    public static final int ATMEGA32_FLASH_SIZE = 32 * _1kb;
    public static final int ATMEGA32_EEPROM_SIZE = _1kb;
    public static final int ATMEGA32_NUM_PINS = 45;
    public static final int ATMEGA32_NUM_INTS = 22;

    public static final int MODE_IDLE       = 1;
    public static final int MODE_RESERVED1  = 2;
    public static final int MODE_ADCNRED    = 3;
    public static final int MODE_RESERVED2  = 4;
    public static final int MODE_POWERDOWN  = 5;
    public static final int MODE_STANDBY    = 6;
    public static final int MODE_POWERSAVE  = 7;
    public static final int MODE_EXTSTANDBY = 8;

    protected static final String[] idleModeNames = {
        "Active",
        "Idle",
        "RESERVED 1",
        "ADC Noise Reduction",
        "RESERVED 2",
        "Power Down",
        "Standby",
        "Power Save",
        "Extended Standby"
    };

    protected static final int[] wakeupTimes = {
        0, 0, 0, 0, 0, 1000, 6, 1000, 6
    };

    protected final ActiveRegister MCUCR_reg;

    private static final int[][] transitionTimeMatrix  = FiniteStateMachine.buildBimodalTTM(idleModeNames.length, 0, wakeupTimes, new int[wakeupTimes.length]);


    /**
     * The <code>props</code> field stores a static reference to a properties
     * object shared by all of the instances of this microcontroller. This object
     * stores the IO register size, SRAM size, pin assignments, etc.
     */
    public static final AVRProperties props;

    static {
        // statically initialize the pin assignments for this microcontroller
        HashMap pinAssignments = new HashMap(150);
        RegisterLayout rl = new RegisterLayout(ATMEGA32_IOREG_SIZE, 8);
        HashMap interruptAssignments = new HashMap(30);

        addPin(pinAssignments, 1, "MOSI", "PB5");
        addPin(pinAssignments, 2, "MISO", "PB6");
        addPin(pinAssignments, 3, "SCK", "PB7");
        addPin(pinAssignments, 4, "RESET");
        addPin(pinAssignments, 5, "VCC.1");
        addPin(pinAssignments, 6, "GND.1");
        addPin(pinAssignments, 7, "XTAL2");
        addPin(pinAssignments, 8, "XTAL1");
        addPin(pinAssignments, 9, "RXD", "PD0");
        addPin(pinAssignments, 10, "TXD", "PD1");
        addPin(pinAssignments, 11, "INT0", "PD2");
        addPin(pinAssignments, 12, "INT1", "PD3");
        addPin(pinAssignments, 13, "OC1B", "PD4");
        addPin(pinAssignments, 14, "OC1A", "PD5");
        addPin(pinAssignments, 15, "ICP1", "PD6");
        addPin(pinAssignments, 16, "OC2", "PD7");
        addPin(pinAssignments, 17, "VCC.2");
        addPin(pinAssignments, 18, "GND.2");
        addPin(pinAssignments, 19, "SCL", "PC0");
        addPin(pinAssignments, 20, "SDA", "PC1");
        addPin(pinAssignments, 21, "TCK", "PC2");
        addPin(pinAssignments, 22, "TMS", "PC3");
        addPin(pinAssignments, 23, "TDO", "PC4");
        addPin(pinAssignments, 24, "TDI", "PC5");
        addPin(pinAssignments, 25, "TOSC1", "PC6");
        addPin(pinAssignments, 26, "TOSC2", "PC7");
        addPin(pinAssignments, 27, "AVCC");
        addPin(pinAssignments, 28, "GND.3");
        addPin(pinAssignments, 29, "AREF");
        addPin(pinAssignments, 30, "ADC7", "PA7");
        addPin(pinAssignments, 31, "ADC6", "PA6");
        addPin(pinAssignments, 32, "ADC5", "PA5");
        addPin(pinAssignments, 33, "ADC4", "PA4");
        addPin(pinAssignments, 34, "ADC3", "PA3");
        addPin(pinAssignments, 35, "ADC2", "PA2");
        addPin(pinAssignments, 36, "ADC1", "PA1");
        addPin(pinAssignments, 37, "ADC0", "PA0");
        addPin(pinAssignments, 38, "VCC.3");
        addPin(pinAssignments, 39, "GND.4");
        addPin(pinAssignments, 40, "XCK", "T0", "PB0");
        addPin(pinAssignments, 41, "T1", "PB1");
        addPin(pinAssignments, 42, "AIN0", "INT2", "PB2");
        addPin(pinAssignments, 43, "AIN1", "OC0", "PB3");
        addPin(pinAssignments, 44, "SS", "PB4");

        // lower 64 IO registers
        rl.addIOReg("SREG", 0x3F);
        rl.addIOReg("SPH", 0x3E);
        rl.addIOReg("SPL", 0x3D);
        rl.addIOReg("OCR0", 0x3C);
        rl.addIOReg("GICR", 0x3B);
        rl.addIOReg("GIFR", 0x3A);
        rl.addIOReg("TIMSK", 0x39);
        rl.addIOReg("TIFR", 0x38);
        rl.addIOReg("SPMCR", 0x37);
        // TODO: this this register is called different names on different models
        rl.addIOReg("SPMCSR", 0x37);
        rl.addIOReg("TWCR", 0x36);
        rl.addIOReg("MCUCR", 0x35);
        rl.addIOReg("MCUCSR", 0x34);
        rl.addIOReg("TCCR0", 0x33);
        rl.addIOReg("TCNT0", 0x32);
        rl.addIOReg("OSCCAL", 0x31);
        rl.addIOReg("SFIOR", 0x30);
        rl.addIOReg("TCCR1A", 0x2F, "COM1A[1:0],COM1B[1:0],FOC1A,FOC1B,WGM1[1:0]");
        rl.addIOReg("TCCR1B", 0x2E, "...,WGM1[3:2],CS1[2:0]");
        rl.addIOReg("TCNT1H", 0x2D);
        rl.addIOReg("TCNT1L", 0x2C);
        rl.addIOReg("OCR1AH", 0x2B);
        rl.addIOReg("OCR1AL", 0x2A);
        rl.addIOReg("OCR1BH", 0x29);
        rl.addIOReg("OCR1BL", 0x28);
        rl.addIOReg("ICR1H", 0x27);
        rl.addIOReg("ICR1L", 0x26);
        rl.addIOReg("TCCR2", 0x25);
        rl.addIOReg("TCNT2", 0x24);
        rl.addIOReg("OCR2", 0x23);
        rl.addIOReg("ASSR", 0x22);
        rl.addIOReg("WDTCR", 0x21);
        rl.addIOReg("UBRRH", 0x20);
        // TODO: the UCSRC register is shared!
        rl.addIOReg("UCSRC", 0x20);
        rl.addIOReg("EEARH", 0x1F);
        rl.addIOReg("EEARL", 0x1E);
        rl.addIOReg("EEDR", 0x1D);
        rl.addIOReg("EECR", 0x1C);
        rl.addIOReg("PORTA", 0x1B);
        rl.addIOReg("DDRA", 0x1A);
        rl.addIOReg("PINA", 0x19);
        rl.addIOReg("PORTB", 0x18);
        rl.addIOReg("DDRB", 0x17);
        rl.addIOReg("PINB", 0x16);
        rl.addIOReg("PORTC", 0x15);
        rl.addIOReg("DDRC", 0x14);
        rl.addIOReg("PINC", 0x13);
        rl.addIOReg("PORTD", 0x12);
        rl.addIOReg("DDRD", 0x11);
        rl.addIOReg("PIND", 0x10);
        rl.addIOReg("SPDR", 0x0F);
        rl.addIOReg("SPSR", 0x0E);
        rl.addIOReg("SPCR", 0x0D);
        rl.addIOReg("UDR", 0x0C);
        rl.addIOReg("UCSRA", 0x0B);
        rl.addIOReg("UCSRB", 0x0A);
        rl.addIOReg("UBRRL", 0x09);
        rl.addIOReg("ACSR", 0x08);
        rl.addIOReg("ADMUX", 0x07);
        rl.addIOReg("ADCSRA", 0x06);
        rl.addIOReg("ADCH", 0x05);
        rl.addIOReg("ADCL", 0x04);
        rl.addIOReg("TWDR", 0x03);
        rl.addIOReg("TWAR", 0x02);
        rl.addIOReg("TWSR", 0x01);
        rl.addIOReg("TWBR", 0x00);

        addInterrupt(interruptAssignments, "RESET", 1);
        addInterrupt(interruptAssignments, "INT0", 2);
        addInterrupt(interruptAssignments, "INT1", 3);
        addInterrupt(interruptAssignments, "INT2", 4);
        addInterrupt(interruptAssignments, "TIMER2 COMP", 5);
        addInterrupt(interruptAssignments, "TIMER2 OVF", 6);
        addInterrupt(interruptAssignments, "TIMER1 CAPT", 7);
        addInterrupt(interruptAssignments, "TIMER1 COMPA", 8);
        addInterrupt(interruptAssignments, "TIMER1 COMPB", 9);
        addInterrupt(interruptAssignments, "TIMER1 OVF", 10);
        addInterrupt(interruptAssignments, "TIMER0 COMP", 11);
        addInterrupt(interruptAssignments, "TIMER0 OVF", 12);
        addInterrupt(interruptAssignments, "SPI, STC", 13);
        addInterrupt(interruptAssignments, "USART, RX", 14);
        addInterrupt(interruptAssignments, "USART, UDRE", 15);
        addInterrupt(interruptAssignments, "USART, TX", 16);
        addInterrupt(interruptAssignments, "ADC", 17);
        addInterrupt(interruptAssignments, "EE READY", 18);
        addInterrupt(interruptAssignments, "ANALOG COMP", 19);
        addInterrupt(interruptAssignments, "TWI", 20);
        addInterrupt(interruptAssignments, "SPM READY", 21);

        props = new AVRProperties(ATMEGA32_IOREG_SIZE, // number of io registers
                ATMEGA32_SRAM_SIZE, // size of sram in bytes
                ATMEGA32_FLASH_SIZE, // size of flash in bytes
                ATMEGA32_EEPROM_SIZE, // size of eeprom in bytes
                ATMEGA32_NUM_PINS, // number of pins
                ATMEGA32_NUM_INTS, // number of interrupts
                new ReprogrammableCodeSegment.Factory(ATMEGA32_FLASH_SIZE, 6),
                pinAssignments, // the assignment of names to physical pins
                rl, // the assignment of names to IO registers
                interruptAssignments);

    }

    public static class Factory implements MicrocontrollerFactory {

        /**
         * The <code>newMicrocontroller()</code> method is used to instantiate a microcontroller instance for the
         * particular program. It will construct an instance of the <code>Simulator</code> class that has all the
         * properties of this hardware device and has been initialized with the specified program.
         *
         * @param sim
         *@param p the program to load onto the microcontroller @return a <code>Microcontroller</code> instance that represents the specific hardware device with the
         *         program loaded onto it
         */
        public Microcontroller newMicrocontroller(int id, Simulation sim, ClockDomain cd, Program p) {
            return new ATMega32(id, sim, cd, p);
        }

    }

    public ATMega32(int id, Simulation sim, ClockDomain cd, Program p) {
        super(cd, props, new FiniteStateMachine(cd.getMainClock(), MODE_ACTIVE, idleModeNames, transitionTimeMatrix));
        simulator = sim.createSimulator(id, LegacyInterpreter.FACTORY, this, p);
        interpreter = (AtmelInterpreter)simulator.getInterpreter();
        MCUCR_reg = getIOReg("MCUCR");
        installPins();
        installDevices();
    }

    protected void installPins() {
        for (int cntr = 0; cntr < properties.num_pins; cntr++)
            pins[cntr] = new Pin(cntr);
    }

    protected void installDevices() {
        // set up the external interrupt mask and flag registers and interrupt range
        int[] mapping = new int[] { -1, -1, -1, -1, -1, 4, 2, 3 };
        FlagRegister fr = new FlagRegister(interpreter, mapping);
        MaskRegister mr = new MaskRegister(interpreter, mapping);
        installIOReg("GICR", mr);
        installIOReg("GIFR", fr);
        EIFR_reg = fr;

        // set up the timer mask and flag registers and interrupt range
        TIFR_reg = buildInterruptRange(false, "TIMSK", "TIFR", 12, 8);
        TIMSK_reg = (MaskRegister)getIOReg("TIMSK");


        addDevice(new Timer0());
        addDevice(new Timer1(2));
        addDevice(new Timer2());

        buildPort('A');
        buildPort('B');
        buildPort('C');
        buildPort('D');

        addDevice(new EEPROM(properties.eeprom_size, this));
        addDevice(new USART("", this));

        addDevice(new SPI(this));
        addDevice(new ADC(this, 8));
    }


    // permutation of sleep mode bits in the register (high order bits first)
    private static final int[] MCUCR_sm_perm = { 2, 4, 3 };

    protected int getSleepMode() {
        byte value = MCUCR_reg.read();
        boolean sleepEnable = Arithmetic.getBit(value, 5);

        if ( sleepEnable )
            return Arithmetic.getBitField(value, MCUCR_sm_perm) + 1;
        else
            return MODE_IDLE;
    }

}
