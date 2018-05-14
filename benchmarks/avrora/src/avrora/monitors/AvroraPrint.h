/*
 * Copyright (c) 2009 Cork Institute of Technology, Ireland
 * All rights reserved."
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written
 * agreement is hereby granted, provided that the above copyright notice, the
 * following two paragraphs and the author appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE CORK INSTITUTE OF TECHNOLOGY BE LIABLE TO ANY
 * PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE
 * CORK INSTITUTE OF TECHNOLOGY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 * 
 * THE CORK INSTITUTE OF TECHNOLOGY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE CORK INSTITUTE OF TECHNOLOGY HAS NO OBLIGATION 
 * TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 */                   

/* AvroraPrint.h
 *
 * This is the C code to print variables to the Avrora emulator
 *
 * How to use:
 *   (1) Include this file "AvroraPrintf.h" in your WSN application 
 *   (2) Send print statements like this:
 *	  
 *	  printChar('a');
 *
 *	  printInt8(44);
 *	  printInt16(3333);
 *	  printInt32(55556666);
 *
 *	  printStr("hello world");
 *
 *	  printHex8(0xFF);
 *    printHex16(0xFFFF);
 *	  printHex32(0xFFFFAAAA);
 *
 *	 (3) Compile and run the code with Avrora including the c-print option.
 *
 * Known bugs/limitations:
 *
 * 	 - If you include many print statements the emulator will slow down 
 * 	 - Print statements in arrow, without any operation in the middle, will 
 * 	 only print the last statement. I think this is beacuse the memory location 
 * 	 is re-written before printing the value in Avrora. 	 
 * 
 * Notes:	 
 * 	
 * 	 - You can log the print statements to a file including the avrora
 * 	 option printlogfile="logfile.log". The saved file will be in the format
 * 	 logfile.log+nodeid 
 *
 *
 * @author AWS / Rodolfo De Paz http://www.aws.cit.ie/rodolfo
 * @contact avrora@lists.ucla.edu
 */
#define DEBUGBUF_SIZE 64
#include <stdarg.h>
char debugbuf1[DEBUGBUF_SIZE+1];
char *debugbuf;

#define printChar(__char) {	\
	init();	\
	debugbuf[0] = __char;	\
	debugbuf[1] = 0;		\
	vartype(2);	\
}
#define printInt8(__char) {	\
	init(); \
	debugbuf[0] = __char;	\
	debugbuf[1] = 0;		\
	vartype(3);			\
}
#define printInt16(__int) {	\
	init(); \
	debugbuf[0] = (uint8_t)(uint16_t)__int&0x00ff;	\
	debugbuf[1] = (uint8_t)((uint16_t)__int>>8)&0x00ff;		\
	vartype(3);		\
}
#define printInt32(__int) {	\
	init();\
	debugbuf[0] = (uint8_t)((uint32_t)__int)&0x00ff;	\
	debugbuf[1] = (uint8_t)((uint32_t)__int>>8)&0x00ff;	\
	debugbuf[2] = (uint8_t)((uint32_t)__int>>16)&0x00ff;	\
	debugbuf[3] = (uint8_t)((uint32_t)__int>>24)&0x00ff;	\
	vartype(5);		\
}
#define printStr(__str) { \
	init();	\
	strcpy(debugbuf, __str);	\
	vartype(2);	\
}
#define printHex8(__char) { \
	init(); \
	debugbuf[0] = __char;   \
	debugbuf[1] = 0;        \
	vartype(1);		\
}
#define printHex16(__int) { \
	init(); \
	debugbuf[0] = __int&0x00ff; \
	debugbuf[1] = (__int>>8)&0x00ff;     \
	vartype(1);		\
}
#define printHex32(__int) { \
	init();\
	debugbuf[0] = (uint8_t)((uint32_t)__int)&0x00ff; \
	debugbuf[1] = (uint8_t)((uint32_t)__int>>8)&0x00ff;    \
	debugbuf[2] = (uint8_t)((uint32_t)__int>>16)&0x00ff;   \
	debugbuf[3] = (uint8_t)((uint32_t)__int>>24)&0x00ff;   \
	vartype(4);	\
}
void init(){
	debugbuf = &debugbuf1[1];
}
void vartype(uint8_t a)
{
	debugbuf1[0] = a;
}

