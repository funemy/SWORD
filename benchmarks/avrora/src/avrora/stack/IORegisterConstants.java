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

package avrora.stack;

/**
 * The <code>IORegisterConstants</code> interface is used to centralize the numeric values of the IO registers
 * of the AVR architecture.
 *
 * @author Ben L. Titzer
 */
public interface IORegisterConstants {

    public final int UCSR1C = 0x9D;
    public final int UDR1 = 0x9C;
    public final int UCSR1A = 0x9B;
    public final int UCSR1B = 0x9A;
    public final int UBRR1L = 0x99;
    public final int UBRR1H = 0x98;

    public final int UCSR0C = 0x95;

    public final int UBRR0H = 0x90;

    public final int TCCR3C = 0x8C;
    public final int TCCR3A = 0x8B;
    public final int TCCR3B = 0x8A;
    public final int TCNT3H = 0x89;
    public final int TCNT3L = 0x88;
    public final int OCR3AH = 0x87;
    public final int OCR3AL = 0x86;
    public final int OCR3BH = 0x85;
    public final int OCR3BL = 0x84;
    public final int OCR3CH = 0x83;
    public final int OCR3CL = 0x82;
    public final int ICR3H = 0x81;
    public final int ICR3L = 0x80;

    public final int ETIMSK = 0x7D;
    public final int ETIFR = 0x7C;

    public final int TCCR1C = 0x7A;
    public final int OCR1CH = 0x79;
    public final int OCR1CL = 0x78;

    public final int TWCR = 0x74;
    public final int TWDR = 0x73;
    public final int TWAR = 0x72;
    public final int TWSR = 0x71;
    public final int TWBR = 0x70;
    public final int OSCCAL = 0x6F;

    public final int XMCRA = 0x6D;
    public final int XMCRB = 0x6C;

    public final int EICRA = 0x6A;

    public final int SPMCSR = 0x68;

    public final int PORTG = 0x65;
    public final int DDRG = 0x64;
    public final int PING = 0x63;
    public final int PORTF = 0x62;
    public final int DDRF = 0x61;

    public final int SREG = 0x3F;
    public final int SPH = 0x3E;
    public final int SPL = 0x3D;
    public final int XDIV = 0x3C;
    public final int RAMPZ = 0x3B;
    public final int EICRB = 0x3A;
    public final int EIMSK = 0x39;
    public final int EIFR = 0x38;
    public final int TIMSK = 0x37;
    public final int TIFR = 0x36;
    public final int MCUCR = 0x35;
    public final int MCUCSR = 0x34;
    public final int TCCR0 = 0x33;
    public final int TCNT0 = 0x32;
    public final int OCR0 = 0x31;
    public final int ASSR = 0x30;
    public final int TCCR1A = 0x2F;
    public final int TCCR1B = 0x2E;
    public final int TCNT1H = 0x2D;
    public final int TCNT1L = 0x2C;
    public final int OCR1AH = 0x2B;
    public final int OCR1AL = 0x2A;
    public final int OCR1BH = 0x29;
    public final int OCR1BL = 0x28;
    public final int ICR1H = 0x27;
    public final int ICR1L = 0x26;
    public final int TCCR2 = 0x25;
    public final int TCNT2 = 0x24;
    public final int OCR2 = 0x23;
    public final int OCDR = 0x22;
    public final int WDTCR = 0x21;
    public final int SFIOR = 0x20;
    public final int EEARH = 0x1F;
    public final int EEARL = 0x1E;
    public final int EEDR = 0x1D;
    public final int EECR = 0x1C;
    public final int PORTA = 0x1B;
    public final int DDRA = 0x1A;
    public final int PINA = 0x19;
    public final int PORTB = 0x18;
    public final int DDRB = 0x17;
    public final int PINB = 0x16;
    public final int PORTC = 0x15;
    public final int DDRC = 0x14;
    public final int PINC = 0x13;
    public final int PORTD = 0x12;
    public final int DDRD = 0x11;
    public final int PIND = 0x10;
    public final int SPDR = 0x0F;
    public final int SPSR = 0x0E;
    public final int SPCR = 0x0D;
    public final int UDR0 = 0x0C;
    public final int UCSR0A = 0x0B;
    public final int UCSR0B = 0x0A;
    public final int UBRR0L = 0x09;
    public final int ACSR = 0x08;
    public final int ADMUX = 0x07;
    public final int ADCSRA = 0x06;
    public final int ADCH = 0x05;
    public final int ADCL = 0x04;
    public final int PORTE = 0x03;
    public final int DDRE = 0x02;
    public final int PINE = 0x01;
    public final int PINF = 0x00;

    public final int SREG_I = 7;
    public final int SREG_T = 6;
    public final int SREG_H = 5;
    public final int SREG_S = 4;
    public final int SREG_V = 3;
    public final int SREG_N = 2;
    public final int SREG_Z = 1;
    public final int SREG_C = 0;

    public final int NUM_REGS = 32;
}
