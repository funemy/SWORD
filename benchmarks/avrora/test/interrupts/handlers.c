/****************** TODO, NOTES, & BUGS************************
 * 
 * according to avrora the enables for 24 and 34 are off but all
 * others should be on - YET 24 & 32-35 do not fire (these nubers
 * all might be one too high)
 **************************************************************/

#include <avr/interrupt.h>
#include <avr/io.h>

volatile char x[1];
volatile char z;

int main (void) {
  cli(); //disable global interrupts
  
  enable_external_int(0xff); // set all bits in EIMSK to 1; enable external interrupts
  timer_enable_int(0xff); //  enable timer outputcompare and overflow (TIMSK) 
  
  // Two wire control register
  sbi(TWCR, TWIE);
  sbi(TWCR, TWEN);

  sbi(SPCR, SPE);
  sbi(SPMCSR,SPMIE);
  sbi(SPMCSR,SPMEN);
  sbi(SPMCR,SPMIE);
  
  outp(0x3f,ETIMSK); //ETIMSK
  outp((1<<SPIE),SPCR); //SPCR
  
  outp((1<<ADIE),ADCSR); // enable ADC
  outp((1<<ADIE),ADCSRA); // enable ADC

  // disabled for tst file ... too hard to guess when it will go
  // off the first time
  //sbi(EECR,EERIE); //Enable EEPROM
  
  sbi(SFIOR, ACME);
  sbi(ACSR, ADC); 
  sbi(ACSR, ACI);
  sbi(ACSR, ACIE);
  sbi(ACSR, ACIC);//Analog comparator control and status register

  sei(); // turn on global interrupt enable

  return 0;
}

// interrupt handlers

void SIG_INTERRUPT0 (void) __attribute__ ((signal));
void SIG_INTERRUPT1 (void) __attribute__ ((signal));
void SIG_INTERRUTPT2 (void) __attribute__ ((signal));
void SIG_INTERRUPT3 (void) __attribute__ ((signal));
void SIG_INTERRUPT0 (void) __attribute__ ((signal));
void SIG_INTERRUPT1 (void) __attribute__ ((signal));
void SIG_INTERRUPT2 (void) __attribute__ ((signal));
void SIG_INTERRUPT3 (void) __attribute__ ((signal));
void SIG_INTERRUPT4 (void) __attribute__ ((signal));
void SIG_INTERRUPT5 (void) __attribute__ ((signal));
void SIG_INTERRUPT6 (void) __attribute__ ((signal));
void SIG_INTERRUPT7 (void) __attribute__ ((signal));
void SIG_OUTPUT_COMPARE2 (void) __attribute__ ((signal));
void SIG_OVERFLOW2 (void) __attribute__ ((signal));
void SIG_INPUT_CAPTURE1 (void) __attribute__ ((signal));
void SIG_OUTPUT_COMPARE1A (void) __attribute__ ((signal));
void SIG_OUTPUT_COMPARE1B (void) __attribute__ ((signal));
void SIG_OVERFLOW1 (void) __attribute__ ((signal));
void SIG_OUTPUT_COMPARE0 (void) __attribute__ ((signal));
void SIG_OVERFLOW0 (void) __attribute__ ((signal));
void SIG_SPI (void) __attribute__ ((signal));
void SIG_UART0_RECV (void) __attribute__ ((signal));
void SIG_UART0_DATA (void) __attribute__ ((signal));
void SIG_UART0_TRANS (void) __attribute__ ((signal));
void SIG_ADC (void) __attribute__ ((signal));
void SIG_EEPROM_READY (void) __attribute__ ((signal));
void SIG_COMPARATOR (void) __attribute__ ((signal));
void SIG_OUTPUT_COMPARE1C (void) __attribute__ ((signal));
void SIG_INPUT_CAPTURE3  (void) __attribute__ ((signal));
void SIG_OUTPUT_COMPARE3A (void) __attribute__ ((signal));
void SIG_OUTPUT_COMPARE3B (void) __attribute__ ((signal));
void SIG_OUTPUT_COMPARE3C (void) __attribute__ ((signal));
void SIG_OVERFLOW3 (void) __attribute__ ((signal));
void SIG_UART1_RECV (void) __attribute__ ((signal));
void SIG_UART1_DATA (void) __attribute__ ((signal));
void SIG_UART1_TRANS (void) __attribute__ ((signal));
void SIG_2WIRE_SERIAL (void) __attribute__ ((signal));
void SIG_SPM_READY (void) __attribute__ ((signal));

void SIG_INTERRUPT0 (void) {
  z=x[0x802];
}

void SIG_INTERRUPT1 (void) {
  z=x[0x803];
}

void SIG_INTERRUPT2 (void) {
  z=x[0x804];
}

void SIG_INTERRUPT3 (void) {
  z= x[0x805];
}

void SIG_INTERRUPT4 (void) {
  z= x[0x806];
}

void SIG_INTERRUPT5 (void) {
  z= x[0x807];
}

void SIG_INTERRUPT6 (void) {
  z=x[0x808];
}

void SIG_INTERRUPT7 (void) {
  z= x[0x809];
}

void SIG_OUTPUT_COMPARE2 (void) {
  z=x[0x810]; 
} 

void SIG_OVERFLOW2 (void) {
  z=x[0x811]; 
}

void SIG_INPUT_CAPTURE1 (void) {
  z= x[0x812]; 
}

void SIG_OUTPUT_COMPARE1A (void) {
  z=x[0x813]; 
}

void SIG_OUTPUT_COMPARE1B (void) {
  z=x[0x814]; 
} 

void SIG_OVERFLOW1 (void) {
  z=x[0x815]; 
} 

void SIG_OUTPUT_COMPARE0 (void) {
  z= x[0x816]; 
} 

void SIG_OVERFLOW0 (void) {
  z=x[0x817]; 
} 

void SIG_SPI (void) {
  z=x[0x818]; 
}

void SIG_UART0_RECV (void) {
  z=x[0x819]; 
}

void SIG_UART0_DATA (void) {
  z=x[0x820]; 
} 

void SIG_UART0_TRANS (void) {
  z=x[0x821]; 
} 

void SIG_ADC (void) {
  sbi(ADCSR, ADIF);
  cbi(ADCSR, ADEN);
  sei();
  z=x[0x822];
} 

// works, but might want to make sure that
// all of it is correct 
void SIG_EEPROM_READY (void) {
  z=x[0x823];
  cli();
  sbi(EECR,EEMWE);
  cbi(EECR,EEWE);
  sbi(EECR, EEWE);
  sei();
} 

void SIG_COMPARATOR (void) {       //  <---- Can't get this enabled
  z=x[0x824]; 
} 

void SIG_OUTPUT_COMPARE1C (void) {
  z=x[0x825]; 
} 

void SIG_INPUT_CAPTURE3  (void) {
  z=x[0x826]; 
} 

void SIG_OUTPUT_COMPARE3A (void) {
  z=x[0x827]; 
} 

void SIG_OUTPUT_COMPARE3B (void) {
  z=x[0x828]; 
} 

void SIG_OUTPUT_COMPARE3C (void) {
  z= x[0x829]; 
} 

void SIG_OVERFLOW3 (void) {
  z=x[0x830];
  asm volatile ("break"); // <----------------- halt. added for tst file. 31-35
                          //    Didn't completely work corretly anyways (fire too
  //    often or not at all).
} 

void SIG_UART1_RECV (void) {
  z=x[0x831]; 
} 

void SIG_UART1_DATA (void) {
  z=x[0x832]; 
} 

void SIG_UART1_TRANS (void) {
  z=x[0x833]; 
} 

void SIG_2WIRE_SERIAL (void) {
  z= x[0x834]; 
} 

void SIG_SPM_READY (void) {
  z= x[0x835]; 
 
}
