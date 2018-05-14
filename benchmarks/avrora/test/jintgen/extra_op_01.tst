// @Harness: verifier
// @Purpose: "Test that no addressing mode has more operands than others in unification"
// @Result: "ExtraOperandInAddrModeUnification @ 9:22"

architecture extra_op_01 {
    operand-type A[5]: int [0,31];
    addr-mode A1 { }
    addr-mode A2 a: A { }
    addr-set AS { A1, A2 }
}
