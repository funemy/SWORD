#!/bin/bash

. ./msp430init.bash

# add                     A     B C =>   A     B C N Z V
#--------------------------------------------------------
gen-REGREG        add     4     4 0      4     8 0 0 0 0
gen-REGREG        add 32000   768 0  32000 32768 0 1 0 1
gen-REGREG        add 65000   536 0  65000     0 1 0 1 0

# addc                     A     B C =>   A     B C N Z V
#--------------------------------------------------------
gen-REGREG        addc     4     4 1      4     9 0 0 0 0
gen-REGREG        addc 32000   767 1  32000 32768 0 1 0 1
gen-REGREG        addc 65000   535 1  65000     0 1 0 1 0

# and                      A     B C =>   A     B C N Z V
#--------------------------------------------------------
gen-REGREG        and      4     4 0      4     4 0 0 0 0
gen-REGREG        and   0xf0   0xf 0   0xf0     0 0 0 1 0
gen-REGREG        and  32768 65535 0  32768 32768 0 1 0 0

# bic                      A     B C =>   A     B C N Z V
#--------------------------------------------------------
gen-REGREG        bic      4     4 0      4     0 0 0 0 0
gen-REGREG        bic   0xf0   0xf 0   0xf0   0xf 0 0 1 0


# bis                      A     B C =>   A      B C N Z V
#--------------------------------------------------------
gen-REGREG        bis      8     4 0      8     12 0 0 0 0
gen-REGREG        bis   0xf0   0xf 0   0xf0   0xff 0 0 1 0
gen-REGREG        bis  0xff0   0xf 0   0xf0  0xfff 0 0 1 0
gen-REGREG        bis 0xfff0   0xf 0   0xf0 0xffff 0 0 1 0

# bit                      A     B C =>   A     B C N Z V
#--------------------------------------------------------
gen-REGREG        and      4     4 0      4     4 0 0 0 0
gen-REGREG        and   0xf0   0xf 0   0xf0   0xf 0 0 1 0
gen-REGREG        and  32768 65535 0  32768 65536 0 1 0 0

# cmp                      A     B C =>   A     B C N Z V
#--------------------------------------------------------
gen-REGREG        cmp      4     4 0      4     4 0 0 1 0

# dadd

# mov                      A     B C =>   A     B C N Z V
#--------------------------------------------------------
gen-REGREG        mov      4     7 0      4     4 0 0 0 0
gen-REGREG        mov      1     7 0      1     1 0 0 0 0
gen-REGREG        mov    256     7 0    256   256 0 0 0 0
gen-REGREG        mov.b  256     7 0    256     0 0 0 0 0

# rra                      A C =>   A C N Z V
#-------------------------------------------------------------
gen-REG           rra      4 0      2 0 0 0 0
gen-REG           rra      8 0      4 0 0 0 0
gen-REG           rra  32768 0  16384 0 0 0 0

# rra                    A C =>    A C N Z V
#-------------------------------------------------------------
gen-REG           rrc    4 1   32770 0 0 0 0

# sub

# subc

# swpb                     A C =>    A C N Z V
#-------------------------------------------------------------
gen-REG          swpb 0x00ff 1  0xff00 0 1 0 0
gen-REG          swpb 0x00fe 1  0xfe00 0 1 0 0
gen-REG          swpb 0xeeff 1  0xffee 0 1 0 0
gen-REG          swpb 0x11ff 1  0xff11 0 1 0 0
gen-REG          swpb 0x1001 1  0x0110 0 0 0 0

# sxt                A C      A C N Z V
#-------------------------------------------
gen-REG        sxt   4 0      4 0 0 0 0
gen-REG        sxt 128 0  65408 0 0 0 0

# xor

# IMM addressing mode     A B   C     B C N Z V
#-----------------------------------------------------
gen-IMMREG        add     1 4   0     5 0 0 0 0
gen-IMMREG        add     2 4   0     6 0 0 0 0
gen-IMMREG        add     4 4   0     8 0 0 0 0
gen-IMMREG        add     8 4   0    12 0 0 0 0
gen-IMMREG        add    -1 4   0     3 0 0 0 0
gen-IMMREG        add   330 4   0   334 0 0 0 0
gen-IMMREG        mov     1 0   0     1 0 0 0 0
gen-IMMREG        mov     2 0   0     2 0 0 0 0
gen-IMMREG        mov     4 0   0     4 0 0 0 0
gen-IMMREG        mov     8 0   0     8 0 0 0 0
gen-IMMREG        mov    -1 0   0 65535 0 0 0 0
gen-IMMREG        mov   290 0   0   290 0 0 0 0
gen-IMMREG        mov.b 290 0   0    34 0 0 0 0


# ABS addressing mode
#-----------------------------------------------------
gen-ABSREG        add 0x400 4 4 0 8 0 0 0 0
gen-ABSREG        mov 0x400 2 0 0 2 0 0 0 0

gen-ABSABS        add 0x400 0x500 4 4 0 8 0 0 0 0
gen-ABSABS        mov 0x400 0x500 2 0 0 2 0 0 0 0

gen-IMMABS        add  1 0x400 4 0  5 0 0 0 0
gen-IMMABS        add  8 0x400 4 0 12 0 0 0 0
gen-IMMABS        add 30 0x400 4 0 34 0 0 0 0
gen-IMMABS        mov  1 0x400 4 0  1 0 0 0 0
gen-IMMABS        mov 30 0x400 4 0 30 0 0 0 0

gen-REGABS        add 0x400  1 4 0  5 0 0 0 0
gen-REGABS        add 0x400  8 4 0 12 0 0 0 0
gen-REGABS        add 0x400 30 4 0 34 0 0 0 0
gen-REGABS        mov 0x400 30 4 0 30 0 0 0 0


# SYM (relative) addressing mode
#--------------------------------------------------
gen-SYMSYM       add 0x400 0x500 4 4 0 8 0 0 0 0
gen-SYMSYM       mov 0x400 0x500 2 0 0 2 0 0 0 0

gen-SYMABS        add 0x400 0x500 4 4 0 8 0 0 0 0
gen-SYMABS        mov 0x400 0x500 2 0 0 2 0 0 0 0

gen-SYMREG        add 0x400 4 4 0 8 0 0 0 0
gen-SYMREG        mov 0x400 2 0 0 2 0 0 0 0

gen-REGSYM        add  1 0x400 4 0  5 0 0 0 0
gen-REGSYM        add  8 0x400 4 0 12 0 0 0 0
gen-REGSYM        add 30 0x400 4 0 34 0 0 0 0
gen-REGSYM        mov 30 0x400 4 0 30 0 0 0 0

# IND (indexed) addressing mode
#--------------------------------------------------
gen-INDIND        add 100 0x400 102 0x400 4 4 0 8 0 0 0 0
gen-INDIND        mov 100 0x400 102 0x400 2 0 0 2 0 0 0 0

gen-INDABS        add 100 0x400 0x500 4 4 0 8 0 0 0 0
gen-INDABS        mov 100 0x400 0x500 2 0 0 2 0 0 0 0

gen-INDREG        add 100 0x400 4 4 0 8 0 0 0 0
gen-INDREG        mov 100 0x400 2 0 0 2 0 0 0 0

gen-REGIND        add  1 100 0x400 4 0  5 0 0 0 0
gen-REGIND        add  8 100 0x400 4 0 12 0 0 0 0
gen-REGIND        add 30 100 0x400 4 0 34 0 0 0 0
gen-REGIND        mov 30 100 0x400 4 0 30 0 0 0 0

# IREG (indirect register) addressing mode
#---------------------------------------------------------

gen-IREGREG        add 0x400 4 4 0 8 0 0 0 0
gen-IREGREG        mov 0x400 2 0 0 2 0 0 0 0

gen-IREGSYM        add 0x400 0x500 4 4 0 8 0 0 0 0
gen-IREGSYM        mov 0x400 0x500 2 0 0 2 0 0 0 0

gen-IREGABS        add 0x400 0x500 4 4 0 8 0 0 0 0
gen-IREGABS        mov 0x400 0x500 2 0 0 2 0 0 0 0


# AUTO_W (auto-increment) addressing mode
#---------------------------------------------------------

# AUTO_B (auto-increment) addressing mode
#---------------------------------------------------------

# calls
#---------------------------------------------------------

# ret, reti
#---------------------------------------------------------

# branches (jc, jeq, jge, jl, jn, jnc, jne)
#---------------------------------------------------------

# jmp
#---------------------------------------------------------

# push, pop
#---------------------------------------------------------

