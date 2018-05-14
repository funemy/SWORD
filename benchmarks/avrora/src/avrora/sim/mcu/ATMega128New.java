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
import avrora.sim.energy.Energy;
import cck.util.Arithmetic;
import java.util.HashMap;

/**
 * The <code>ATMega128</code> class represents the ATMega128 microcontroller from Atmel. This
 * microcontroller has 128Kb code, 4KB SRAM, 4KB EEPROM, and a host of internal devices such as
 * ADC, SPI, and timers.
 *
 * @author Ben L. Titzer
 * @author Pekka Nikander
 */
public class ATMega128New extends ATMegaClassic {

    public static final int _1kb = 1024;

    public static final int ATMEGA128_IOREG_SIZE = 256 - 32;
    public static final int ATMEGA128_SRAM_SIZE = 4 * _1kb;
    public static final int ATMEGA128_FLASH_SIZE = 128 * _1kb;
    public static final int ATMEGA128_EEPROM_SIZE = 4 * _1kb;
    public static final int ATMEGA128_NUM_PINS = 65;
    public static final int ATMEGA128_NUM_INTS = 36;

    public static final int MODE_IDLE       = 1;
    public static final int MODE_ADCNRED    = 2;
    public static final int MODE_POWERDOWN  = 3;
    public static final int MODE_POWERSAVE  = 4;
    public static final int MODE_RESERVED1  = 5;
    public static final int MODE_RESERVED2  = 6;
    public static final int MODE_STANDBY    = 7;
    public static final int MODE_EXTSTANDBY = 8;

    protected static final String[] idleModeNames = {
        "Active",
        "Idle",
        "ADC Noise Reduction",
        "Power Down",
        "Power Save",
        "RESERVED 1",
        "RESERVED 2",
        "Standby",
        "Extended Standby"
    };

    //power consumption of each mode (ATMEGA128L)
    private static final double[] modeAmpere = {
        0.0075667,
        0.0033433,
        0.0009884,
        0.0001158,
        0.0001237,
        0.0,
        0.0,
        0.0002356,
        0.0002433
    };


    protected static final int[] wakeupTimes = {
        0, 0, 0, 1000, 1000, 0, 0, 6, 6
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
        HashMap interruptAssignments = new HashMap(50);

        addPin(pinAssignments, 1, "PEN");
        addPin(pinAssignments, 2, "PE0", "RXD0", "PDI");
        addPin(pinAssignments, 3, "PE1", "TXD0", "PDO");
        addPin(pinAssignments, 4, "PE2", "XCK0", "AIN0");
        addPin(pinAssignments, 5, "PE3", "OC3A", "AIN1");
        addPin(pinAssignments, 6, "PE4", "OC3B", "INT4");
        addPin(pinAssignments, 7, "PE5", "OC3C", "INT5");
        addPin(pinAssignments, 8, "PE6", "T3", "INT6");
        addPin(pinAssignments, 9, "PE7", "IC3", "INT7");
        addPin(pinAssignments, 10, "PB0", "SS");
        addPin(pinAssignments, 11, "PB1", "SCK");
        addPin(pinAssignments, 12, "PB2", "MOSI");
        addPin(pinAssignments, 13, "PB3", "MISO");
        addPin(pinAssignments, 14, "PB4", "OC0");
        addPin(pinAssignments, 15, "PB5", "OC1A");
        addPin(pinAssignments, 16, "PB6", "OC1B");
        addPin(pinAssignments, 17, "PB7", "OC2", "OC1C");
        addPin(pinAssignments, 18, "PG3", "TOSC2");
        addPin(pinAssignments, 19, "PG4", "TOSC1");
        addPin(pinAssignments, 20, "RESET");
        addPin(pinAssignments, 21, "VCC");
        addPin(pinAssignments, 22, "GND");
        addPin(pinAssignments, 23, "XTAL2");
        addPin(pinAssignments, 24, "XTAL1");
        addPin(pinAssignments, 25, "PD0", "SCL", "INT0");
        addPin(pinAssignments, 26, "PD1", "SDA", "INT1");
        addPin(pinAssignments, 27, "PD2", "RXD1", "INT2");
        addPin(pinAssignments, 28, "PD3", "TXD1", "INT3");
        addPin(pinAssignments, 29, "PD4", "IC1");
        addPin(pinAssignments, 30, "PD5", "XCK1");
        addPin(pinAssignments, 31, "PD6", "T1");
        addPin(pinAssignments, 32, "PD7", "T2");
        addPin(pinAssignments, 33, "PG0", "WR");
        addPin(pinAssignments, 34, "PG1", "RD");
        addPin(pinAssignments, 35, "PC0", "A8");
        addPin(pinAssignments, 36, "PC1", "A9");
        addPin(pinAssignments, 37, "PC2", "A10");
        addPin(pinAssignments, 38, "PC3", "A11");
        addPin(pinAssignments, 39, "PC4", "A12");
        addPin(pinAssignments, 40, "PC5", "A13");
        addPin(pinAssignments, 41, "PC6", "A14");
        addPin(pinAssignments, 42, "PC7", "A15");
        addPin(pinAssignments, 43, "PG2", "ALE");
        addPin(pinAssignments, 44, "PA7", "AD7");
        addPin(pinAssignments, 45, "PA6", "AD5");
        addPin(pinAssignments, 46, "PA5", "AD5");
        addPin(pinAssignments, 47, "PA4", "AD4");
        addPin(pinAssignments, 48, "PA3", "AD3");
        addPin(pinAssignments, 49, "PA2", "AD2");
        addPin(pinAssignments, 50, "PA1", "AD1");
        addPin(pinAssignments, 51, "PA0", "AD0");
        addPin(pinAssignments, 52, "VCC.b");
        addPin(pinAssignments, 53, "GND.b");
        addPin(pinAssignments, 54, "PF7", "ADC7", "TDI");
        addPin(pinAssignments, 55, "PF6", "ADC6", "TDO");
        addPin(pinAssignments, 56, "PF5", "ADC5", "TMS");
        addPin(pinAssignments, 57, "PF4", "ADC4", "TCK");
        addPin(pinAssignments, 58, "PF3", "ADC3");
        addPin(pinAssignments, 59, "PF2", "ADC2");
        addPin(pinAssignments, 60, "PF1", "ADC1");
        addPin(pinAssignments, 61, "PF0", "ADC0");
        addPin(pinAssignments, 62, "AREF");
        addPin(pinAssignments, 63, "GND.c");
        addPin(pinAssignments, 64, "AVCC");

        RegisterLayout rl = new RegisterLayout(ATMEGA128_IOREG_SIZE, 8);

        // extended IO registers
        rl.addIOReg("UCSR1C", 0x7D);
        rl.addIOReg("UDR1", 0x7C);
        rl.addIOReg("UCSR1A", 0x7B);
        rl.addIOReg("UCSR1B", 0x7A);
        rl.addIOReg("UBRR1L", 0x79);
        rl.addIOReg("UBRR1H", 0x78);

        rl.addIOReg("UCSR0C", 0x75);

        rl.addIOReg("UBRR0H", 0x70);

        rl.addIOReg("TCCR3C", 0x6C, "FOC3A,FOC3B,FOC3C,.....");
        rl.addIOReg("TCCR3A", 0x6B, "COM3A[1:0],COM3B[1:0],COM3C[1:0],WGM3[1:0]");
        rl.addIOReg("TCCR3B", 0x6A, "ICNC3,ICES3,.,WGM3[3:2],CS3[2:0]");
        rl.addIOReg("TCNT3H", 0x69);
        rl.addIOReg("TCNT3L", 0x68);
        rl.addIOReg("OCR3AH", 0x67);
        rl.addIOReg("OCR3AL", 0x66);
        rl.addIOReg("OCR3BH", 0x65);
        rl.addIOReg("OCR3BL", 0x64);
        rl.addIOReg("OCR3CH", 0x63);
        rl.addIOReg("OCR3CL", 0x62);
        rl.addIOReg("ICR3H", 0x61);
        rl.addIOReg("ICR3L", 0x60);

        rl.addIOReg("ETIMSK", 0x5D, "..,TICIE3,OCIE3A,OCIE3B,TOIE3,OCIE3C,OCIE1C");
        rl.addIOReg("ETIFR", 0x5C,  "..,ICF3,OCF3A,OCF3B,TOV3,OCF3C,OCF1C");

        rl.addIOReg("TCCR1C", 0x5A, "FOC1A,FOC1B,FOC1C,.....");
        rl.addIOReg("OCR1CH", 0x59);
        rl.addIOReg("OCR1CL", 0x58);

        rl.addIOReg("TWCR", 0x54);
        rl.addIOReg("TWDR", 0x53);
        rl.addIOReg("TWAR", 0x52);
        rl.addIOReg("TWSR", 0x51);
        rl.addIOReg("TWBR", 0x50);
        rl.addIOReg("OSCCAL", 0x4F);

        rl.addIOReg("XMCRA", 0x4D);
        rl.addIOReg("XMCRB", 0x4C);

        rl.addIOReg("EICRA", 0x4A);

        rl.addIOReg("SPMCSR", 0x48);

        rl.addIOReg("PORTG", 0x45);
        rl.addIOReg("DDRG", 0x44);
        rl.addIOReg("PING", 0x43);
        rl.addIOReg("PORTF", 0x42);
        rl.addIOReg("DDRF", 0x41);

        // lower 64 IO registers
        rl.addIOReg("SREG", 0x3F);
        rl.addIOReg("SPH", 0x3E);
        rl.addIOReg("SPL", 0x3D);
        rl.addIOReg("XDIV", 0x3C);
        rl.addIOReg("RAMPZ", 0x3B);
        rl.addIOReg("EICRB", 0x3A);
        rl.addIOReg("EIMSK", 0x39);
        rl.addIOReg("EIFR", 0x38);
        rl.addIOReg("TIMSK", 0x37, "OCIE2,TOIE2,ICIE1,OCIE1A,OCIE1B,TOIE1,OCIE0,TOIE0");
        rl.addIOReg("TIFR", 0x36,  "OCF2,TOV2,ICF1,OCF1A,OCF1B,TOV1,OCF0,TOV0");
        rl.addIOReg("MCUCR", 0x35);
        rl.addIOReg("MCUCSR", 0x34);
        rl.addIOReg("TCCR0", 0x33, "FOC0,WGM0[0],COM0[1:0],WGM0[1],CS0[2:0]");
        rl.addIOReg("TCNT0", 0x32);
        rl.addIOReg("OCR0", 0x31);
        rl.addIOReg("ASSR", 0x30, "....,AS0,TCN0UB,OCR0UB,TCR0UB");
        rl.addIOReg("TCCR1A", 0x2F, "COM1A[1:0],COM1B[1:0],COM1C[1:0],WGM1[1:0]");
        rl.addIOReg("TCCR1B", 0x2E, "ICNC1,ICES1,.,WGM1[3:2],CS1[2:0]");
        rl.addIOReg("TCNT1H", 0x2D);
        rl.addIOReg("TCNT1L", 0x2C);
        rl.addIOReg("OCR1AH", 0x2B);
        rl.addIOReg("OCR1AL", 0x2A);
        rl.addIOReg("OCR1BH", 0x29);
        rl.addIOReg("OCR1BL", 0x28);
        rl.addIOReg("ICR1H", 0x27);
        rl.addIOReg("ICR1L", 0x26);
        rl.addIOReg("TCCR2", 0x25, "FOC2,WGM2[0],COM2[1:0],WGM2[1],CS2[2:0]");
        rl.addIOReg("TCNT2", 0x24);
        rl.addIOReg("OCR2", 0x23);
        rl.addIOReg("OCDR", 0x22);
        rl.addIOReg("WDTCR", 0x21);
        rl.addIOReg("SFIOR", 0x20, "TSM,...,ACME,PUD,PSR0,PSR321");
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
        rl.addIOReg("UDR0", 0x0C);
        rl.addIOReg("UCSR0A", 0x0B);
        rl.addIOReg("UCSR0B", 0x0A);
        rl.addIOReg("UBRR0L", 0x09);
        rl.addIOReg("ACSR", 0x08);
        rl.addIOReg("ADMUX", 0x07);
        rl.addIOReg("ADCSRA", 0x06);
        rl.addIOReg("ADCH", 0x05);
        rl.addIOReg("ADCL", 0x04);
        rl.addIOReg("PORTE", 0x03);
        rl.addIOReg("DDRE", 0x02);
        rl.addIOReg("PINE", 0x01);
        rl.addIOReg("PINF", 0x00);

        addInterrupt(interruptAssignments, "RESET", 1);
        addInterrupt(interruptAssignments, "INT0", 2);
        addInterrupt(interruptAssignments, "INT1", 3);
        addInterrupt(interruptAssignments, "INT2", 4);
        addInterrupt(interruptAssignments, "INT3", 5);
        addInterrupt(interruptAssignments, "INT4", 6);
        addInterrupt(interruptAssignments, "INT5", 7);
        addInterrupt(interruptAssignments, "INT6", 8);
        addInterrupt(interruptAssignments, "INT7", 9);
        addInterrupt(interruptAssignments, "TIMER2 COMP", 10);
        addInterrupt(interruptAssignments, "TIMER2 OVF", 11);
        addInterrupt(interruptAssignments, "TIMER1 CAPT", 12);
        addInterrupt(interruptAssignments, "TIMER1 COMPA", 13);
        addInterrupt(interruptAssignments, "TIMER1 COMPB", 14);
        addInterrupt(interruptAssignments, "TIMER1 OVF", 15);
        addInterrupt(interruptAssignments, "TIMER0 COMP", 16);
        addInterrupt(interruptAssignments, "TIMER0 OVF", 17);
        addInterrupt(interruptAssignments, "SPI, STC", 18);
        addInterrupt(interruptAssignments, "USART0, RX", 19);
        addInterrupt(interruptAssignments, "USART0, UDRE", 20);
        addInterrupt(interruptAssignments, "USART0, TX", 21);
        addInterrupt(interruptAssignments, "ADC", 22);
        addInterrupt(interruptAssignments, "EE READY", 23);
        addInterrupt(interruptAssignments, "ANALOG COMP", 24);
        addInterrupt(interruptAssignments, "TIMER1 COMPC", 25);
        addInterrupt(interruptAssignments, "TIMER3 CAPT", 26);
        addInterrupt(interruptAssignments, "TIMER3 COMPA", 27);
        addInterrupt(interruptAssignments, "TIMER3 COMPB", 28);
        addInterrupt(interruptAssignments, "TIMER3 COMPC", 29);
        addInterrupt(interruptAssignments, "TIMER3 OVF", 30);
        addInterrupt(interruptAssignments, "USART1, RX", 31);
        addInterrupt(interruptAssignments, "USART1, UDRE", 32);
        addInterrupt(interruptAssignments, "USART1, TX", 33);
        addInterrupt(interruptAssignments, "TWI", 34);
        addInterrupt(interruptAssignments, "SPM READY", 35);

        props = new AVRProperties(ATMEGA128_IOREG_SIZE, // number of io registers
                ATMEGA128_SRAM_SIZE, // size of sram in bytes
                ATMEGA128_FLASH_SIZE, // size of flash in bytes
                ATMEGA128_EEPROM_SIZE, // size of eeprom in bytes
                ATMEGA128_NUM_PINS, // number of pins
                ATMEGA128_NUM_INTS, // number of interrupts
                new ReprogrammableCodeSegment.Factory(ATMEGA128_FLASH_SIZE, 7),
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
            return new ATMega128New(id, sim, cd, p);
        }

    }

    public ATMega128New(int id, Simulation sim, ClockDomain cd, Program p) {
        super(cd, props, new FiniteStateMachine(cd.getMainClock(), MODE_ACTIVE, idleModeNames, transitionTimeMatrix));
        simulator = sim.createSimulator(id, LegacyInterpreter.FACTORY, this, p);
        interpreter = (AtmelInterpreter)simulator.getInterpreter();
        MCUCR_reg = getIOReg("MCUCR");
        installPins();
        installDevices();
        new Energy("CPU", modeAmpere, sleepState, sim.getEnergyControl());

        // Jacob's temporary addition for bootloader
        //interpreter.setBootPC(0x1E000);

    }

    protected void installPins() {
        for (int cntr = 0; cntr < properties.num_pins; cntr++)
            pins[cntr] = new ATMegaFamily.Pin(cntr);
    }

    protected void installDevices() {
        // set up the external interrupt mask and flag registers and interrupt range
        EIFR_reg = buildInterruptRange(true, "EIMSK", "EIFR", 2, 8);

        // set up the timer mask and flag registers and interrupt range
        RWRegister TIFR_reg = buildInterruptRange(false, "TIMSK", "TIFR", 17, 8);
        RWRegister TIMSK_reg = (MaskRegister)getIOReg("TIMSK");

        int[] ETIFR_mapping = {25, 29, 30, 28, 27, 26, -1, -1};
        RWRegister ETIFR_reg = new FlagRegister(interpreter, ETIFR_mapping);
        RWRegister ETIMSK_reg = new MaskRegister(interpreter, ETIFR_mapping);

        installIOReg("ETIMSK", ETIMSK_reg);
        installIOReg("ETIFR", ETIFR_reg);

        addDevice(new Timer0());
        addDevice(new Timer1(3));
        addDevice(new Timer2());
        addDevice(new Timer3(3));

        buildPort('A');
        buildPort('B');
        buildPort('C');
        buildPort('D');
        buildPort('E');
        buildPort('F');

        addDevice(new EEPROM(properties.eeprom_size, this));
        addDevice(new USART("0", this));
        addDevice(new USART("1", this));

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
