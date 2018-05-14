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

// TODO: correct register mappings
// TODO: remove extended standby mode
// TODO: check everything else

package avrora.sim.mcu;

import java.util.HashMap;

import avrora.arch.avr.AVRProperties;
import avrora.arch.legacy.LegacyInterpreter;
import avrora.core.Program;
import avrora.sim.*;
import avrora.sim.clock.ClockDomain;
import cck.util.Arithmetic;

/**
 * The <code>ATMega169</code> class represents the ATMega169 microcontroller
 * from Atmel. This microcontroller has 16Kb code, 1KB SRAM, 512 Byte EEPROM,
 * LCD driver, and a host of internal devices such as ADC, SPI, and timers.
 *
 * @author Anttu Koski
 */
public class ATMega169 extends ATMegaFamilyNew {

    public static final int _1kb = 1024;
    public static final int _512b = 512;

    public static final int ATMEGA169_IOREG_SIZE = 256 - 32;
    public static final int ATMEGA169_SRAM_SIZE = _1kb;
    public static final int ATMEGA169_FLASH_SIZE = 16 * _1kb;
    public static final int ATMEGA169_EEPROM_SIZE = _512b;
    public static final int ATMEGA169_NUM_PINS = 64;
    public static final int ATMEGA169_NUM_INTS = 22;

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

    //FIXME: these need to be checked
    protected static final int[] wakeupTimes = {
        0, 0, 0, 1000, 1000, 0, 0, 1000, 6
    };

    protected final ActiveRegister MCUCR_reg;

    private static final int[][] transitionTimeMatrix  =
    	FiniteStateMachine.buildBimodalTTM(idleModeNames.length, 0,
    			wakeupTimes, new int[wakeupTimes.length]);


    /**
     * The <code>props</code> field stores a static reference to a properties
     * object shared by all of the instances of this microcontroller.
     * This object stores the IO register size, SRAM size, pin assignments, etc.
     */
    public static final AVRProperties props;

    static {
        // statically initialize the pin assignments for this microcontroller
        HashMap pinAssignments = new HashMap(150);
        RegisterLayout rl = new RegisterLayout(ATMEGA169_IOREG_SIZE, 8);
        HashMap interruptAssignments = new HashMap(30);

        addPin(pinAssignments, 1, "LCDCAP");
        addPin(pinAssignments, 2, "RXD", "PCINT0", "PE0");
        addPin(pinAssignments, 3, "TXD", "PCINT1", "PE1");
        addPin(pinAssignments, 4, "XCK", "AIN0", "PCINT2", "PE2");
        addPin(pinAssignments, 5, "AIN1", "PCINT3", "PE3");
        addPin(pinAssignments, 6, "USCK", "SCL", "PCINT4", "PE4");
        addPin(pinAssignments, 7, "DI", "SDA", "PCINT5", "PE5");
        addPin(pinAssignments, 8, "DO", "PCINT6", "PE6");
        addPin(pinAssignments, 9, "CLK0", "PCINT7", "PE7");
        addPin(pinAssignments, 10, "SS", "PCINT8", "PB0");
        addPin(pinAssignments, 11, "SCK", "PCINT9", "PB1");
        addPin(pinAssignments, 12, "MOSI", "PCINT10", "PB2");
        addPin(pinAssignments, 13, "MISO", "PCINT11", "PB3");
        addPin(pinAssignments, 14, "OC0A", "PCINT12", "PB4");
        addPin(pinAssignments, 15, "OC1A", "PCINT13", "PB5");
        addPin(pinAssignments, 16, "OC1B", "PCINT14", "PB6");
        addPin(pinAssignments, 17, "OC2A", "PCINT15", "PB7");
        addPin(pinAssignments, 18, "T1", "SEG24", "PG3");
        addPin(pinAssignments, 19, "T0", "SEG23", "PG4");
        addPin(pinAssignments, 20, "RESET");
        addPin(pinAssignments, 21, "VCC.1");
        addPin(pinAssignments, 22, "GND.1");
        addPin(pinAssignments, 23, "TOSC2", "XTAL2");
        addPin(pinAssignments, 24, "TOSC1", "XTAL1");
        addPin(pinAssignments, 25, "ICP1", "SEG22", "PD0");
        addPin(pinAssignments, 26, "INT0", "SEG21", "PD1");
        addPin(pinAssignments, 27, "SEG20", "PD2");
        addPin(pinAssignments, 28, "SEG19", "PD3");
        addPin(pinAssignments, 29, "SEG18", "PD4");
        addPin(pinAssignments, 30, "SEG17", "PD5");
        addPin(pinAssignments, 31, "SEG16", "PD6");
        addPin(pinAssignments, 32, "SEG15", "PD7");
        addPin(pinAssignments, 33, "SEG14", "PG0");
        addPin(pinAssignments, 34, "SEG13", "PG1");
        addPin(pinAssignments, 35, "SEG12", "PC0");
        addPin(pinAssignments, 36, "SEG11", "PC1");
        addPin(pinAssignments, 37, "SEG10", "PC2");
        addPin(pinAssignments, 38, "SEG9", "PC3");
        addPin(pinAssignments, 39, "SEG8", "PC4");
        addPin(pinAssignments, 40, "SEG7", "PC5");
        addPin(pinAssignments, 41, "SEG6", "PC6");
        addPin(pinAssignments, 42, "SEG5", "PC7");
        addPin(pinAssignments, 43, "SEG4", "PG2");
        addPin(pinAssignments, 44, "SEG3", "PA7");
        addPin(pinAssignments, 45, "SEG2", "PA6");
        addPin(pinAssignments, 46, "SEG1", "PA5");
        addPin(pinAssignments, 47, "SEG0", "PA4");
        addPin(pinAssignments, 48, "COM3", "PA3");
        addPin(pinAssignments, 49, "COM2", "PA2");
        addPin(pinAssignments, 50, "COM1", "PA1");
        addPin(pinAssignments, 51, "COM0", "PA0");
        addPin(pinAssignments, 52, "VCC.2");
        addPin(pinAssignments, 53, "GND.2");
        addPin(pinAssignments, 54, "ADC7", "TDI", "PF7");
        addPin(pinAssignments, 55, "ADC6", "TDO", "PF6");
        addPin(pinAssignments, 56, "ADC5", "TMS", "PF5");
        addPin(pinAssignments, 57, "ADC4", "TCK", "PF4");
        addPin(pinAssignments, 58, "ADC3", "PF3");
        addPin(pinAssignments, 59, "ADC2", "PF2");
        addPin(pinAssignments, 60, "ADC1", "PF1");
        addPin(pinAssignments, 61, "ADC0", "PF0");
        addPin(pinAssignments, 62, "AREF");
        addPin(pinAssignments, 63, "GND.3");
        addPin(pinAssignments, 64, "AVCC");

        // extended IO registers
        rl.addIOReg("LCDDR18", 0xDE, ".......,SEG324");
        rl.addIOReg("LCDDR17", 0xDD, "SEG32[3:0],SEG31[9:6]");
        rl.addIOReg("LCDDR16", 0xDC, "SEG31[5:0],SEG30[9:8]");
        rl.addIOReg("LCDDR15", 0xDB, "SEG30[7:0]");
        rl.addIOReg("LCDDR13", 0xD9, ".......,SEG224");
        rl.addIOReg("LCDDR12", 0xD8, "SEG22[3:0],SEG21[9:6]");
        rl.addIOReg("LCDDR11", 0xD7, "SEG21[5:0],SEG20[9:8]");
        rl.addIOReg("LCDDR10", 0xD6, "SEG20[7:0]");
        rl.addIOReg("LCDDR8", 0xD4, ".......,SEG114");
        rl.addIOReg("LCDDR7", 0xD3, "SEG12[3:0],SEG11[9:6]");
        rl.addIOReg("LCDDR6", 0xD2, "SEG11[5:0],SEG10[9:8]");
        rl.addIOReg("LCDDR5", 0xD1, "SEG10[7:0]");
        rl.addIOReg("LCDDR3", 0xCF, ".......,SEG014");
        rl.addIOReg("LCDDR2", 0xCE, "SEG02[3:0],SEG01[9:6]");
        rl.addIOReg("LCDDR1", 0xCD, "SEG01[5:0],SEG00[9:8]");
        rl.addIOReg("LCDDR0", 0xCC, "SEG00[7:0]");
        rl.addIOReg("LCDCCR", 0xC7, "LCDDC[2:0],.,LCDCC[3:0]");
        rl.addIOReg("LCDFRR", 0xC6, ".,LCDPS[2:0],.,LCDCD[2:0]");
        rl.addIOReg("LCDCRB", 0xC5, "LCDCS,LCD2B,LCDMUX[1:0],.,LCDPM[2:0]");
        rl.addIOReg("LCDCRA", 0xC4, "LCDEN,LCDAB,.,LCDIF,LCDIE,..,LCDBL");
        rl.addIOReg("UDR", 0xA6);
        rl.addIOReg("UBRRH", 0xA5);
        rl.addIOReg("UBRRL", 0xA4);
        rl.addIOReg("UCSRC", 0xA2, ".,UMSEL,UPM[1:0],USBS,UCSZ[1:0],UCDPOL");
        rl.addIOReg("UCSRB", 0xA1, "RXCIE,TXCIE,UDRIE,RXEN,TXEN,UCSZ2,RXB6,TXB6");
        rl.addIOReg("UCSRA", 0xA0, "RXC,TXC,UDRE,FE,DOR,UPE,U2X,MPCM");
        rl.addIOReg("USIDR", 0x9A);
        rl.addIOReg("USISR", 0x99, "USISIF,USIOIF,USIPF,USIDC,USICNT[3:0]");
        rl.addIOReg("USICR", 0x98, "USISIE,USIOIE,USIWM[1:0],USICS[1:0],USICLK,USITC");
        rl.addIOReg("ASSR", 0x96, "...,EXCLK,AS2,TCN2UB,OCR2UB,TCR2UB");
        rl.addIOReg("OCR2A", 0x93);
        rl.addIOReg("TCNT2", 0x92);
        rl.addIOReg("TCCR2A", 0x90, "FOC2A,WGM20,COM2A[1:0],WGM21,CS2[2:0]");
        rl.addIOReg("OCR1BH", 0x6B);
        rl.addIOReg("OCR1BL", 0x6A);
        rl.addIOReg("OCR1AH", 0x69);
        rl.addIOReg("OCR1AL", 0x68);
        rl.addIOReg("ICR1H", 0x67);
        rl.addIOReg("ICR1L", 0x66);
        rl.addIOReg("TCNT1H", 0x65);
        rl.addIOReg("TCNT1L", 0x64);
        rl.addIOReg("TCCR1C", 0x62, "FOC1A,FOC1B,......");
        rl.addIOReg("TCCR1B", 0x61, "ICNC1,ICES1,.,WGM1[3:2],CS1[2:0]");
        rl.addIOReg("TCCR1A", 0x60, "COM1A[1:0],COM1B[1:0],..,WGM1[1:0]");
        rl.addIOReg("DIDR1", 0x5F, "......,AIN1D,AIN0D");
        rl.addIOReg("DIDR0", 0x5E, "ADC7D,ADC6D,ADC5D,ADC4D,ADC3D,ADC2D,ADC1D,ADC0D");
        rl.addIOReg("ADMUX", 0x5C, "REFS[1:0],ADLAR,MUX[4:0]");
        rl.addIOReg("ADCSRB", 0x5B, ".,ACME,...,ADTS[2:0]");
        rl.addIOReg("ADCSRA", 0x5A, "ADEN,ADSC,ADATE,ADIF,ADIE,ADPS[2:0]");
        rl.addIOReg("ADCH", 0x59);
        rl.addIOReg("ADCL", 0x58);
        rl.addIOReg("TIMSK2", 0x50, "......,OCIE2A,TOIE2");
        rl.addIOReg("TIMSK1", 0x4F, "..,ICIE1,..,OCIE1B,OCIE1A,TOIE1");
        rl.addIOReg("TIMSK0", 0x4E, "......,OCIE0A,TOIE0");
        rl.addIOReg("PCMSK1", 0x4C, "PCINT[15:8]");
        rl.addIOReg("PCMSK0", 0x4B, "PCINT[7:0]");
        rl.addIOReg("EICRA", 0x49, "......,ISC0[1:0]");
        rl.addIOReg("OSCCALC", 0x46);
        rl.addIOReg("PRR", 0x44, "...,PRLCD,PRTIM1,PRSPI,PRUSART0,PRADC");
        rl.addIOReg("CLKPR", 0x41, "CLKPCE,...,CLKPS[3:0]");
        rl.addIOReg("WDTCR", 0x40, "...,WDCE,WDE,WDP[2:0]");

        // lower 64 IO registers
        rl.addIOReg("SREG", 0x3F);
        rl.addIOReg("SPH", 0x3E);
        rl.addIOReg("SPL", 0x3D);
        rl.addIOReg("SPMCSR", 0x37);
        rl.addIOReg("MCUCR", 0x35);
        rl.addIOReg("MCUSR", 0x34);
        rl.addIOReg("SMCR", 0x33);
        rl.addIOReg("OCDR", 0x31);
        rl.addIOReg("ACSR", 0x30);
        rl.addIOReg("SPDR", 0x2E);
        rl.addIOReg("SPSR", 0x2D);
        rl.addIOReg("SPCR", 0x2C);
        rl.addIOReg("GPIOR2", 0x2B);
        rl.addIOReg("GPIOR1", 0x2A);
        rl.addIOReg("OCR0A", 0x27);
        rl.addIOReg("TCNT0", 0x26);
        rl.addIOReg("TCCR0A", 0x24, "FOC0A,WGM00,COM0A[1:0],WGM01,CS0[2:0]");
        rl.addIOReg("GTCCR", 0x23, "TSM,.....,PSR2,PSR10");
        rl.addIOReg("EEARH", 0x22);
        rl.addIOReg("EEARL", 0x21);
        rl.addIOReg("EEDR", 0x20);
        rl.addIOReg("EECR", 0x1F);
        rl.addIOReg("GPIOR0", 0x1E);
        rl.addIOReg("EIMSK", 0x1D, "PCIE[1:0],.....,INT0");
        rl.addIOReg("EIFR", 0x1C, "PCIF[1:0],.....,INTF0");
        rl.addIOReg("TIFR2", 0x17, "......,OCF2A,TOV2");
        rl.addIOReg("TIFR1", 0x16, "..,ICF1,..OCF1B,OCF1A,TOV1");
        rl.addIOReg("TIFR0", 0x15, "......,OCF0A,TOV0");
        rl.addIOReg("PORTG", 0x14);
        rl.addIOReg("DDRG", 0x13);
        rl.addIOReg("PING", 0x12);
        rl.addIOReg("PORTF", 0x11);
        rl.addIOReg("DDRF", 0x10);
        rl.addIOReg("PINF", 0x0F);
        rl.addIOReg("PORTE", 0x0E);
        rl.addIOReg("DDRE", 0x0D);
        rl.addIOReg("PINE", 0x0C);
        rl.addIOReg("PORTD", 0x0B);
        rl.addIOReg("DDRD", 0x0A);
        rl.addIOReg("PIND", 0x09);
        rl.addIOReg("PORTC", 0x08);
        rl.addIOReg("DDRC", 0x07);
        rl.addIOReg("PINC", 0x06);
        rl.addIOReg("PORTB", 0x05);
        rl.addIOReg("DDRB", 0x04);
        rl.addIOReg("PINB", 0x03);
        rl.addIOReg("PORTA", 0x02);
        rl.addIOReg("DDRA", 0x01);
        rl.addIOReg("PINA", 0x00);

        addInterrupt(interruptAssignments, "RESET", 1);
        addInterrupt(interruptAssignments, "INT0", 2);
        addInterrupt(interruptAssignments, "PCINT0", 3);
        addInterrupt(interruptAssignments, "PCINT1", 4);
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
        addInterrupt(interruptAssignments, "USI START", 17);
        addInterrupt(interruptAssignments, "USI OVERFLOW", 18);
        addInterrupt(interruptAssignments, "ANALOG COMP", 19);
        addInterrupt(interruptAssignments, "ADC", 20);
        addInterrupt(interruptAssignments, "EE READY", 21);
        addInterrupt(interruptAssignments, "SPM READY", 22);
        addInterrupt(interruptAssignments, "LCD", 23);

        props = new AVRProperties(ATMEGA169_IOREG_SIZE, // number of io registers
                ATMEGA169_SRAM_SIZE, // size of sram in bytes
                ATMEGA169_FLASH_SIZE, // size of flash in bytes
                ATMEGA169_EEPROM_SIZE, // size of eeprom in bytes
                ATMEGA169_NUM_PINS, // number of pins
                ATMEGA169_NUM_INTS, // number of interrupts
                new ReprogrammableCodeSegment.Factory(ATMEGA169_FLASH_SIZE, 6),
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
         * @param sim the simulation
         * @param p the program to load onto the microcontroller @return a <code>Microcontroller</code> instance that represents the specific hardware device with the
         *         program loaded onto it
         */
        public Microcontroller newMicrocontroller(int id, Simulation sim, ClockDomain cd, Program p) {
            return new ATMega169(id, sim, cd, p);
        }

    }

    public ATMega169(int id, Simulation sim, ClockDomain cd, Program p) {
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

    protected FlagRegister TIFR0_reg;
    protected MaskRegister TIMSK0_reg;

    protected FlagRegister TIFR1_reg;
    protected MaskRegister TIMSK1_reg;

    protected FlagRegister TIFR2_reg;
    protected MaskRegister TIMSK2_reg;

    protected void installDevices() {
        // set up the external interrupt mask and flag registers and interrupt range
        int[] EIFR_mapping = {2, -1, -1, -1, -1, -1, 4, 3};
        EIFR_reg = new FlagRegister(interpreter, EIFR_mapping);

        int[] TIFR0_mapping = {12, 11, -1, -1, -1, -1, -1, -1 };
        int[] TIFR1_mapping = {10, 8, 9, -1, -1, 7, -1, -1 };
        int[] TIFR2_mapping = {6, 5, -1, -1, -1, -1, -1, -1 };

        // set up the timer mask and flag registers and interrupt range
        TIFR0_reg = new FlagRegister(interpreter, TIFR0_mapping);
        TIMSK0_reg = new MaskRegister(interpreter, TIFR0_mapping);
        TIFR1_reg = new FlagRegister(interpreter, TIFR1_mapping);
        TIMSK1_reg = new MaskRegister(interpreter, TIFR1_mapping);
        TIFR2_reg = new FlagRegister(interpreter, TIFR2_mapping);
        TIMSK2_reg = new MaskRegister(interpreter, TIFR2_mapping);

        installIOReg("TIFR0", TIFR0_reg);
        installIOReg("TIFR1", TIFR1_reg);
        installIOReg("TIFR2", TIFR2_reg);
        installIOReg("TIMSK0", TIMSK0_reg);
        installIOReg("TIMSK1", TIMSK1_reg);
        installIOReg("TIMSK2", TIMSK2_reg);

        //int[] ETIFR_mapping = {25, 29, 30, 28, 27, 26, -1, -1};
        //ETIFR_reg = new FlagRegister(interpreter, ETIFR_mapping);
        //ETIMSK_reg = new MaskRegister(interpreter, ETIFR_mapping);

        //installIOReg("ETIMSK", ETIMSK_reg);
        //installIOReg("ETIFR", ETIFR_reg);

        //addDevice(new Timer0());
        //addDevice(new Timer1(3));
        //addDevice(new Timer2());
        //addDevice(new Timer3(3));

        buildPort('A');
        buildPort('B');
        buildPort('C');
        buildPort('D');
        buildPort('E');
        buildPort('F');
        buildPort('G', 5);

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
