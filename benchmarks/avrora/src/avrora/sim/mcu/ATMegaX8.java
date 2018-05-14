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
 * The <code>ATMegaX8</code> class represents the ATMega8 series microcontrollers from Atmel. These
 * microcontrollers have various amounts of memory, and a host of internal devices such as
 * ADC, SPI, and timers.
 *
 * @author Ben L. Titzer
 * @author Pekka Nikander
 * @author Bastian Schlich
 * @author John F. Schommer
 *
 */
public abstract class ATMegaX8 extends ATMegaFamilyNew {

    public static final int _1kb = 1024;
    public static final int _512b = 512;

    public static final int ATMEGAX8_IOREG_SIZE = 256;
    public static final int ATMEGAX8_NUM_PINS = 32; // 28 in PDIP, missing ADC6-7.
    public static final int ATMEGAX8_NUM_INTS = 24;

    public static final int MODE_IDLE       = 1;
    public static final int MODE_ADCNRED    = 2;
    public static final int MODE_POWERDOWN  = 3;
    public static final int MODE_POWERSAVE  = 4;
    public static final int MODE_RESERVED1  = 5;
    public static final int MODE_RESERVED2  = 6;
    public static final int MODE_STANDBY    = 7;
    public static final int MODE_RESERVED3  = 8;

    protected static final String[] idleModeNames = {
        "Active",
        "Idle",
        "ADC Noise Reduction",
        "Power Down",
        "Power Save",
        "RESERVED 1",
        "RESERVED 2",
        "Standby",
        "RESERVED 3"
    };

    protected final ActiveRegister MCUCR_reg;

    public static final int pkgPDIP = 0;
    public static final int pkgTQFP = 1;
    public static final int pkgMLF  = 2;

    public static int pkg = pkgPDIP;

    protected static final HashMap pinAssignments = new HashMap(150);
    protected static final RegisterLayout rl = new RegisterLayout(ATMEGAX8_IOREG_SIZE, 8);
    protected static final HashMap interruptAssignments = new HashMap(30);

    static {
        // statically initialize the pin assignments for this microcontroller

	switch (pkg) {
	case pkgPDIP:
	    addPin(pinAssignments, 1, "RESET", "PC6");
	    addPin(pinAssignments, 2, "RXD", "PD0");
	    addPin(pinAssignments, 3, "TXD", "PD1");
	    addPin(pinAssignments, 4, "INT0", "PD2");
	    addPin(pinAssignments, 5, "INT1", "OC2B", "PD3");
	    addPin(pinAssignments, 6, "T0", "XCK", "PD4");
	    addPin(pinAssignments, 7, "VCC");
	    addPin(pinAssignments, 8, "GND.1");
	    addPin(pinAssignments, 9, "TOSC1", "XTAL1", "PB6");
	    addPin(pinAssignments, 10, "TOSC2", "XTAL2", "PB7");
	    addPin(pinAssignments, 11, "T1", "OC0B", "PD5");
	    addPin(pinAssignments, 12, "AIN0", "OC0A", "PD6");
	    addPin(pinAssignments, 13, "AIN1", "PD7");
	    addPin(pinAssignments, 14, "ICP1", "CLK0", "PB0");
	    addPin(pinAssignments, 15, "OC1A", "PB1");
	    addPin(pinAssignments, 16, "SS", "OC1B", "PB2");
	    addPin(pinAssignments, 17, "MOSI", "OC2A", "PB3");
	    addPin(pinAssignments, 18, "MISO", "PB4");
	    addPin(pinAssignments, 19, "SCK", "PB5");
	    addPin(pinAssignments, 20, "AVCC");
	    addPin(pinAssignments, 21, "AREF");
	    addPin(pinAssignments, 22, "GND.2");
	    addPin(pinAssignments, 23, "ADC0", "PC0");
	    addPin(pinAssignments, 24, "ADC1", "PC1");
	    addPin(pinAssignments, 25, "ADC2", "PC2");
	    addPin(pinAssignments, 26, "ADC3", "PC3");
	    addPin(pinAssignments, 27, "ADC4", "SDA", "PC4");
	    addPin(pinAssignments, 28, "ADC5", "SLC", "PC5");
	    break;
	case pkgTQFP:
	case pkgMLF:
	    addPin(pinAssignments, 1, "INT1", "OC2B", "PD3");
	    addPin(pinAssignments, 2, "T0", "XCK", "PD4");
	    addPin(pinAssignments, 3, "GND.1");
	    addPin(pinAssignments, 4, "VCC.1");
	    addPin(pinAssignments, 5, "GND.2");
	    addPin(pinAssignments, 6, "VCC.2");
	    addPin(pinAssignments, 7, "TOSC1", "XTAL1", "PB6");
	    addPin(pinAssignments, 8, "TOSC2", "XTAL2", "PB7");

	    addPin(pinAssignments, 9, "T1", "OC0B", "PD5");
	    addPin(pinAssignments, 10, "AIN0", "OC0A", "PD6");
	    addPin(pinAssignments, 11, "AIN1", "PD7");
	    addPin(pinAssignments, 12, "ICP1", "CLK0", "PB0");
	    addPin(pinAssignments, 13, "OC1A", "PB1");
	    addPin(pinAssignments, 14, "SS", "OC1B", "PB2");
	    addPin(pinAssignments, 15, "MOSI", "OC2A", "PB3");
	    addPin(pinAssignments, 16, "MISO", "PB4");

	    addPin(pinAssignments, 17, "SCK", "PB5");
	    addPin(pinAssignments, 18, "AVCC");
	    addPin(pinAssignments, 19, "ADC6");
	    addPin(pinAssignments, 20, "AREF");
	    addPin(pinAssignments, 21, "GND.3");
	    addPin(pinAssignments, 22, "ADC7");
	    addPin(pinAssignments, 23, "ADC0", "PC0");
	    addPin(pinAssignments, 24, "ADC1", "PC1");

	    addPin(pinAssignments, 25, "ADC2", "PC2");
	    addPin(pinAssignments, 26, "ADC3", "PC3");
	    addPin(pinAssignments, 27, "ADC4", "SDA", "PC4");
	    addPin(pinAssignments, 28, "ADC5", "SLC", "PC5");
	    addPin(pinAssignments, 29, "RESET", "PC6");
	    addPin(pinAssignments, 30, "RXD", "PD0");
	    addPin(pinAssignments, 31, "TXD", "PD1");
	    break;
	}

	// High IO register
	rl.addIOReg("UDR0",   0xC6);
	rl.addIOReg("UBRR0H", 0xC5);
	rl.addIOReg("UBRR0L", 0xC4);
	rl.addIOReg("UCSR0C", 0xC2);
	rl.addIOReg("UCSR0B", 0xC1);
	rl.addIOReg("UCSR0A", 0xC0);
	rl.addIOReg("TWAMR",  0xBD);
	rl.addIOReg("TWCR",   0xBC);
	rl.addIOReg("TWDR",   0xBB);
	rl.addIOReg("TWAR",   0xBA);
	rl.addIOReg("TWSR",   0xB9);
	rl.addIOReg("TWBR",   0xB8);
	rl.addIOReg("ASSR",   0xB6);
	rl.addIOReg("OCR2B",  0xB4);
	rl.addIOReg("OCR2A",  0xB3);
	rl.addIOReg("TCNT2",  0xB2);
	rl.addIOReg("TCCR2B", 0xB1, "FOC2A,FOC2B,..,WGM2[2],CS2[2:0]");
	rl.addIOReg("TCCR2A", 0xB0, "COM2A[1:0],COM2B[1:0],..,WGM2[1:0]");

	rl.addIOReg("OCR1BH", 0x8B);
	rl.addIOReg("OCR1BL", 0x8A);
	rl.addIOReg("OCR1AH", 0x89);
	rl.addIOReg("OCR1AL", 0x88);
	rl.addIOReg("ICR1H",  0x87);
	rl.addIOReg("ICR1L",  0x86);
	rl.addIOReg("TCNT1H", 0x85);
	rl.addIOReg("TCNT1L", 0x84);
	rl.addIOReg("TCCR1C", 0x82, "FOC1A,FOC1B");
	rl.addIOReg("TCCR1B", 0x81, "ICNC1,ICES1,.,WGM1[3:2],CS1[2:0]");
	rl.addIOReg("TCCR1A", 0x80, "COM1A[1:0],COM1B[1:0],..,WGM1[1:0]");
	rl.addIOReg("DIDR1",  0x7F);
	rl.addIOReg("DIDR0",  0x7E);
	rl.addIOReg("ADMUX",  0x7C);
	rl.addIOReg("ADCSRB", 0x7B);
	rl.addIOReg("ADCSRA", 0x7A);
	rl.addIOReg("ADCH",   0x79);
	rl.addIOReg("ADCL",   0x78);

	rl.addIOReg("TIMSK2", 0x70, ".....,OCIE2B,OCIE2A,TOIE2");
	rl.addIOReg("TIMSK1", 0x6F, "..,ICIE1,..,OCIE1B,OCIE1A,TOIE1");
	rl.addIOReg("TIMSK0", 0x6E, "..,OCIE0B,OCIE0A,TOIE0");
	rl.addIOReg("PCMSK2", 0x6D);
	rl.addIOReg("PCMSK1", 0x6C);
	rl.addIOReg("PCMSK0", 0x6B);
	rl.addIOReg("EICRA",  0x69);
	rl.addIOReg("PCICR",  0x68);
	rl.addIOReg("OSCCAL", 0x66);
	rl.addIOReg("PRR",    0x64);
	rl.addIOReg("CLKPR",  0x61);
	rl.addIOReg("WDTCSR", 0x60);

        // Low IO registers
        rl.addIOReg("SREG",   0x3F);
        rl.addIOReg("SPH",    0x3E);
        rl.addIOReg("SPL",    0x3D);
        rl.addIOReg("SPMCSR", 0x37);
        rl.addIOReg("MCUCR",  0x35);
        rl.addIOReg("MCUCSR", 0x34);
        rl.addIOReg("SMCR",   0x33);
        rl.addIOReg("ACSR",   0x30);
	rl.addIOReg("SPDR",   0x2E);
	rl.addIOReg("SPSR",   0x2D);
	rl.addIOReg("SPCR",   0x2C);
	rl.addIOReg("GPIOR2", 0x2B);
	rl.addIOReg("GPIOR1", 0x2A);
        rl.addIOReg("OCR0B",  0x28);
        rl.addIOReg("OCR0A",  0x27);
        rl.addIOReg("TCNT0" , 0x26, "FOC0A,FOC0B,..,WGM0[2],CS0[2:0]");
        rl.addIOReg("TCCR0B", 0x25, "COM0A[1:0],COM0B[1:0],..,WGM0[1:0]");
        rl.addIOReg("TCCR0A", 0x24);
        rl.addIOReg("GTCCR",  0x23);
        rl.addIOReg("EEARH",  0x22);
        rl.addIOReg("EEARL",  0x21);
        rl.addIOReg("EEDR",   0x20);
        rl.addIOReg("EECR",   0x1F);
        rl.addIOReg("GPIOR0", 0x1E);
        rl.addIOReg("EIMSK",  0x1D);
        rl.addIOReg("EIFR",   0x1C);
        rl.addIOReg("PCIFR",  0x1B);
        rl.addIOReg("TIFR2",  0x17, ".....,OCF2B,OCF2A,TOV2");
        rl.addIOReg("TIFR1",  0x16, "..,ICF1,..,OCF1B,OCF1A,TOV1");
        rl.addIOReg("TIFR0",  0x15, ".....,OCF0B,OCF0A,TOV0");
	rl.addIOReg("PORTD",  0x0B);
	rl.addIOReg("DDRD",   0x0A);
	rl.addIOReg("PIND",   0x09);
	rl.addIOReg("PORTC",  0x08);
	rl.addIOReg("DDRC",   0x07);
	rl.addIOReg("PINC",   0x06);
	rl.addIOReg("PORTB",  0x05);
	rl.addIOReg("DDRB",   0x04);
	rl.addIOReg("PINB",   0x03);

        addInterrupt(interruptAssignments, "RESET", 1);
        addInterrupt(interruptAssignments, "INT0", 2);
        addInterrupt(interruptAssignments, "INT1", 3);
	addInterrupt(interruptAssignments, "PCINT0", 4);
	addInterrupt(interruptAssignments, "PCINT1", 5);
	addInterrupt(interruptAssignments, "PCINT2", 6);
	addInterrupt(interruptAssignments, "WDT", 7);
        addInterrupt(interruptAssignments, "TIMER2 COMPA", 8);
        addInterrupt(interruptAssignments, "TIMER2 COMPB", 9);
        addInterrupt(interruptAssignments, "TIMER2 OVF", 10);
        addInterrupt(interruptAssignments, "TIMER1 CAPT", 11);
        addInterrupt(interruptAssignments, "TIMER1 COMPA", 12);
        addInterrupt(interruptAssignments, "TIMER1 COMPB", 13);
        addInterrupt(interruptAssignments, "TIMER1 OVF", 14);
        addInterrupt(interruptAssignments, "TIMER0 COMPA", 15);
        addInterrupt(interruptAssignments, "TIMER0 COMPB", 16);
        addInterrupt(interruptAssignments, "TIMER0 OVF", 17);
        addInterrupt(interruptAssignments, "SPI, STC", 18);
        addInterrupt(interruptAssignments, "USART0, RX", 19);
        addInterrupt(interruptAssignments, "USART0, UDRE", 20);
        addInterrupt(interruptAssignments, "USART0, TX", 21);
        addInterrupt(interruptAssignments, "ADC", 22);
        addInterrupt(interruptAssignments, "EE READY", 23);

    }

    public ATMegaX8(int id, Simulation sim, AVRProperties props, ClockDomain cd, Program p,
                    int[][] transitionTimeMatrix) {
        super(cd, props,
	      new FiniteStateMachine(cd.getMainClock(), MODE_ACTIVE, idleModeNames, transitionTimeMatrix));
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

    protected FlagRegister TIFR0_reg;
    protected MaskRegister TIMSK0_reg;

    protected FlagRegister TIFR1_reg;
    protected MaskRegister TIMSK1_reg;

    protected FlagRegister TIFR2_reg;
    protected MaskRegister TIMSK2_reg;

    protected void installDevices() {
        // set up the external interrupt mask and flag registers and interrupt range
        EIFR_reg = buildInterruptRange(true, "EIMSK", "EIFR", 2 /* INT0 */, 5 /* INT0-PCINT2 */);

        // set up the timer mask and flag registers and interrupt range
	int[] TIFR0_mapping = { 10, 8, 9, -1, -1, -1, -1, -1, -1 };
        TIFR0_reg = new FlagRegister(interpreter, TIFR0_mapping);
        TIMSK0_reg = new MaskRegister(interpreter, TIFR0_mapping);

        installIOReg("TIMSK0", TIMSK0_reg);
        installIOReg("TIFR0",  TIFR0_reg);

        int[] TIFR1_mapping = { 14, 12, 13, -1, -1, -1, 11, -1, -1};
        TIFR1_reg = new FlagRegister(interpreter, TIFR1_mapping);
        TIMSK1_reg = new MaskRegister(interpreter, TIFR1_mapping);

        installIOReg("TIMSK1", TIMSK1_reg);
        installIOReg("TIFR1", TIFR1_reg);

        // set up the timer mask and flag registers and interrupt range
	int[] TIFR2_mapping = { 17, 15, 16, -1, -1, -1, -1, -1, -1 };
        TIFR2_reg = new FlagRegister(interpreter, TIFR2_mapping);
        TIMSK2_reg = new MaskRegister(interpreter, TIFR2_mapping);

        installIOReg("TIMSK2", TIMSK2_reg);
        installIOReg("TIFR2",  TIFR2_reg);

//      addDevice(new Timer0());
//      addDevice(new Timer1(2));
//      addDevice(new Timer2());

        buildPort('B');
        buildPort('C', 7);
        buildPort('D');

        addDevice(new EEPROM(properties.eeprom_size, this));
        addDevice(new USART("0", this));

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
