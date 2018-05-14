// @Harness: verifier
// @Purpose: "Test for extra operand in addressing mode unification"
// @Result: "ExtraOperandInAddrModeUnification @ 9:22"

architecture extra_op_02 {
    operand-type A[5]: int [0,31];
    addr-mode A1 a: A { }
    addr-mode A2 a: A, b: A { }
    addr-set AS { A1, A2 }
}
