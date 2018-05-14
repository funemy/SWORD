#!/bin/bash

gen_header() {
    echo '; @Harness: simulator' > $fname
    echo '; @Arch: msp430' >> $fname
    echo '; @Format: raw' >> $fname
}

gen_init() {
    echo "; @Init: \"$1\"" >> $fname
}

gen_final() {
    echo "; @Result: \"$1\"" >> $fname
}

gen_comm() {
    echo "; $1" >> $fname
    echo >> $fname
    echo ".code " >> $fname
}

get_fname() {
number=1
export fname="${1}_${number}.tst"
while [ -f $fname ]; do
    number=`expr $number + 1`
    export fname="${1}_${number}.tst"
done
}

checkParams() {
   if [ "$1" -ne "$2" ]; then
       echo "Usage: gen-$AMODE <instr> $3"
       exit
   fi
}

gen-test() {
    INAME=$1
    PARAMS=$2
    
    echo "gen-$AMODE $PARAMS"
    INSTR=`./gen-instr.bash 4000 "$SYNTAX"`

    get_fname ${INAME}_${AMODE}
    echo "  $INSTR => $fname"
    gen_header
    gen_init "$INIT"
    gen_final "$RESULT"
    gen_comm "gen-$AMODE $PARAMS"
    echo $INSTR >> $fname 
}

# generate a register-register test case
#-----------------------------------------------------------------------------------
gen-REGREG() {
    AMODE=REGREG
    checkParams $# 10 "<A> <B> <C> <A'> <B'> <C'> <N'> <Z'> <V'>"
    A=$2; B=$3; C=$4; Ap=$5; Bp=$6; Cp=$7; Np=$8; Zp=$9; Vp=${10}
    SYNTAX="$1 r4, r5"
    INIT="r4 = $A, r5 = $B, C = $C"
    RESULT="r4 = $Ap, r5 = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate a register (source/dest) test case
#-----------------------------------------------------------------------------------
gen-REG() {
    AMODE=REG
    checkParams $# 8 "<A> <C> <A'> <C'> <N'> <Z'> <V'>"
    A=$2; C=$3; Ap=$4; Cp=$5; Np=$6; Zp=$7; Vp=$8
    SYNTAX="$1 r4"
    INIT="r4 = $A, C = $C"
    RESULT="r4 = $Ap, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate a immediate-register test case
#-----------------------------------------------------------------------------------
gen-IMMREG() {
    AMODE=IMMREG
    checkParams $# 9 "<IMM> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    IMM=$2; B=$3; C=$4; Bp=$5; Cp=$6; Np=$7; Zp=$8; Vp=$9
    SYNTAX="$1 #$IMM, r5"
    INIT="r4 = $A, r5 = $B, C = $C"
    RESULT="r5 = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate a absolute-register test case
#-----------------------------------------------------------------------------------
gen-ABSREG() {
    AMODE=ABSREG
    checkParams $# 10 "<ABS> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    ABS=$2; A=$3; B=$4; C=$5; Bp=$6; Cp=$7; Np=$8; Zp=$9; Vp=${10}
    SYNTAX="$1 &$ABS, r5"
    INIT="data[$ABS] = $A, r5 = $B, C = $C"
    RESULT="r5 = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate a immediate-absolute test case
#-----------------------------------------------------------------------------------
gen-IMMABS() {
    AMODE=IMMABS
    checkParams $# 10 "<IMM> <ABS> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    IMM=$2; ABS=$3; B=$4; C=$5; Bp=$6; Cp=$7; Np=$8; Zp=$9; Vp=${10}
    SYNTAX="$1 #$IMM, &$ABS"
    INIT="data[$ABS] = $B, C = $C"
    RESULT="data[$ABS] = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an absolute-absolute test case
#-----------------------------------------------------------------------------------
gen-ABSABS() {
    AMODE=ABSABS
    checkParams $# 11 "<ABSA> <ABSB> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    ABSA=$2; ABSB=$3; A=$4; B=$5; C=$6; Bp=$7; Cp=$8; Np=$9; Zp=${10}; Vp=${11}
    SYNTAX="$1 &$ABSA, &$ABSB"
    INIT="data[$ABSA] = $A, data[$ABSB] = $B, C = $C"
    RESULT="data[$ABSB] = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an register-absolute test case
#-----------------------------------------------------------------------------------
gen-REGABS() {
    AMODE=REGABS
    checkParams $# 10 "<ABS> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    ABS=$2; A=$3; B=$4; C=$5; Bp=$6; Cp=$7; Np=$8; Zp=$9; Vp=${10}
    SYNTAX="$1 r4, &$ABS"
    INIT="r4 = $A, data[$ABS] = $B, C = $C"
    RESULT="data[$ABS] = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate a absolute (source/dest) test case
#-----------------------------------------------------------------------------------
gen-ABS() {
    AMODE=ABS
    checkParams $# 9 "<ABS> <A> <C> <A'> <C'> <N'> <Z'> <V'>"
    ABS=$2; A=$3; C=$4; Ap=$5; Cp=$6; Np=$7; Zp=$8; Vp=$9
    SYNTAX="$1 &$ABS"
    INIT="data[$ABS] = $A, C = $C"
    RESULT="data[$ABS] = $Ap, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an symbolic-symbolic test case
#-----------------------------------------------------------------------------------
gen-SYMSYM() {
    AMODE=SYMSYM
    checkParams $# 11 "<SYMA> <SYMB> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    SYMA=$2; SYMB=$3; A=$4; B=$5; C=$6; Bp=$7; Cp=$8; Np=$9; Zp=${10}; Vp=${11}
    SYNTAX="$1 $SYMA, $SYMB"
    INIT="data[$SYMA] = $A, data[$SYMB] = $B, C = $C"
    RESULT="data[$SYMB] = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an symbolic-absolute test case
#-----------------------------------------------------------------------------------
gen-SYMABS() {
    AMODE=SYMABS
    checkParams $# 11 "<SYMA> <ABSB> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    SYMA=$2; ABSB=$3; A=$4; B=$5; C=$6; Bp=$7; Cp=$8; Np=$9; Zp=${10}; Vp=${11}
    SYNTAX="$1 $SYMA, &$ABSB"
    INIT="data[$SYMA] = $A, data[$ABSB] = $B, C = $C"
    RESULT="data[$ABSB] = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate a symbolic-register test case
#-----------------------------------------------------------------------------------
gen-SYMREG() {
    AMODE=SYMREG
    checkParams $# 10 "<SYM> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    SYM=$2; A=$3; B=$4; C=$5; Bp=$6; Cp=$7; Np=$8; Zp=$9; Vp=${10}
    SYNTAX="$1 $SYM, r5"
    INIT="data[$SYM] = $A, r5 = $B, C = $C"
    RESULT="r5 = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an register-symbolic test case
#-----------------------------------------------------------------------------------
gen-REGSYM() {
    AMODE=REGSYM
    checkParams $# 10 "<SYM> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    SYM=$2; A=$3; B=$4; C=$5; Bp=$6; Cp=$7; Np=$8; Zp=$9; Vp=${10}
    SYNTAX="$1 $SYM, r5"
    INIT="r4 = $A, data[$SYM] = $B, C = $C"
    RESULT="data[$SYM] = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an immediate-symbolic test case
#-----------------------------------------------------------------------------------
gen-IMMSYM() {
    AMODE=IMMSYM
    checkParams $# 10 "<IMM> <SYM> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    IMM=$2; SYM=$3; B=$4; C=$5; Bp=$6; Cp=$7; Np=$8; Zp=$9; Vp=${10}
    SYNTAX="$1 #$IMM, &$SYM"
    INIT="data[$SYM] = $B, C = $C"
    RESULT="data[$SYM] = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate a symbolic (source/dest) test case
#-----------------------------------------------------------------------------------
gen-SYM() {
    AMODE=SYM
    checkParams $# 9 "<SYM> <A> <C> <A'> <C'> <N'> <Z'> <V'>"
    SYM=$2; A=$3; C=$4; Ap=$5; Cp=$6; Np=$7; Zp=$8; Vp=$9
    SYNTAX="$1 $SYM"
    INIT="data[$SYM] = $A, C = $C"
    RESULT="data[$SYM] = $Ap, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}


# generate a indirect-register test case
#-----------------------------------------------------------------------------------
gen-IREGREG() {
    AMODE=IREGREG
    checkParams $# 10 "<ADDR> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    ADDR=$2; A=$3; B=$4; C=$5; Bp=$6; Cp=$7; Np=$8; Zp=$9; Vp=${10}
    SYNTAX="$1 @r4, r5"
    INIT="r4 = $ADDR, data[$ADDR] = $A, r5 = $B, C = $C"
    RESULT="r5 = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an absolute-absolute test case
#-----------------------------------------------------------------------------------
gen-IREGABS() {
    AMODE=IREGABS
    checkParams $# 11 "<ADDR> <ABSB> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    ADDR=$2; ABSB=$3; A=$4; B=$5; C=$6; Bp=$7; Cp=$8; Np=$9; Zp=${10}; Vp=${11}
    SYNTAX="$1 @r4, &$ABSB"
    INIT="r4 = $ADDR, data[$ADDR] = $A, data[$ABSB] = $B, C = $C"
    RESULT="data[$ABSB] = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an symbolic-symbolic test case
#-----------------------------------------------------------------------------------
gen-IREGSYM() {
    AMODE=IREGSYM
    checkParams $# 11 "<ADDR> <SYMB> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    ADDR=$2; SYMB=$3; A=$4; B=$5; C=$6; Bp=$7; Cp=$8; Np=$9; Zp=${10}; Vp=${11}
    SYNTAX="$1 @r4, $SYMB"
    INIT="r4 = $ADDR, data[$ADDR] = $A, data[$SYMB] = $B, C = $C"
    RESULT="data[$SYMB] = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an indirect (source/dest) test case
#-----------------------------------------------------------------------------------
gen-IREG() {
    AMODE=IREG
    checkParams $# 9 "<ADDR> <A> <C> <A'> <C'> <N'> <Z'> <V'>"
    ADDR=$2; A=$3; C=$4; Ap=$5; Cp=$6; Np=$7; Zp=$8; Vp=$9
    SYNTAX="$1 @r4"
    INIT="data[$ADDR] = $A, r4 = $ADDR, C = $C"
    RESULT="data[$ADDR] = $Ap, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an index-index test case
#-----------------------------------------------------------------------------------
gen-INDIND() {
    AMODE=INDIND
    checkParams $# 13 "<OFFA> <BASEA> <OFFB> <BASEB> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    OFFA=$2; BASEA=$3; OFFB=$4; BASEB=$5; A=$6; B=$7; C=$8; Bp=$9; Cp=${10}; Np=${11}; Zp=${12}; Vp=${13}
    ADDRA=$(($BASEA + $OFFA))
    ADDRB=$(($BASEB + $OFFB))
    SYNTAX="$1 $OFFA(r4), $OFFB(r5)"
    INIT="r4 = $BASEA, r5 = $BASEB, data[$ADDRA] = $A, data[$ADDRB] = $B, C = $C"
    RESULT="data[$ADDRB] = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an index-absolute test case
#-----------------------------------------------------------------------------------
gen-INDABS() {
    AMODE=INDABS
    checkParams $# 12 "<OFFA> <BASEA> <ABSB> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    OFFA=$2; BASEA=$3; ABSB=$4; A=$5; B=$6; C=$7; Bp=$8; Cp=$9; Np=${10}; Zp=${11}; Vp=${12}
    ADDRA=$(($BASEA + $OFFA))
    SYNTAX="$1 $OFFA(r4), &$ABSB"
    INIT="r4 = $BASEA, data[$ADDRA] = $A, data[$ABSB] = $B, C = $C"
    RESULT="data[$ABSB] = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an index-register test case
#-----------------------------------------------------------------------------------
gen-INDREG() {
    AMODE=INDREG
    checkParams $# 11 "<OFFA> <BASEA> <A> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    OFFA=$2; BASEA=$3; A=$4; B=$5; C=$6; Bp=$7; Cp=$8; Np=$9; Zp=${10}; Vp=${11}
    ADDRA=$(($BASEA + $OFFA))
    SYNTAX="$1 $OFFA(r4), r5"
    INIT="r4 = $BASEA, r5 = $B, data[$ADDRA] = $A, C = $C"
    RESULT="r5 = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

# generate an register-index test case
#-----------------------------------------------------------------------------------
gen-REGIND() {
    AMODE=REGIND
    checkParams $# 11 "<A> <OFFB> <BASEB> <B> <C> <B'> <C'> <N'> <Z'> <V'>"
    A=$2; OFFB=$3; BASEB=$4; B=$5; C=$6; Bp=$7; Cp=$8; Np=$9; Zp=${10}; Vp=${11}
    ADDRB=$(($BASEB + $OFFB))
    SYNTAX="$1 r4, $OFFB(r5)"
    INIT="r4 = $A, r5 = $BASEB, data[$ADDRB] = $B, C = $C"
    RESULT="data[$ADDRB] = $Bp, C = $Cp, N = $Np, Z = $Zp, V = $Vp"
    
    gen-test $1 "$*"
}

