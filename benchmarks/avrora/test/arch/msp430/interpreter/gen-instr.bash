#!/bin/bash

AWK_COMMAND="{ printf(\"%s %s         \\\"$2\\\"\n\", \$1, \$2) }"

echo ".org 0x$1" > /tmp/instr.s
echo "$2" >> /tmp/instr.s

msp430-as -o /tmp/instr.o /tmp/instr.s
cp /tmp/instr.o /tmp/instr.bin
msp430-objcopy -O binary /tmp/instr.bin
hexdump -e '"0x%04_ax: " 16/1 "%02x " "\n"' -s $((0x$1)) /tmp/instr.bin
