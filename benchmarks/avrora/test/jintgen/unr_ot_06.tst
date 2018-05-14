// @Harness: verifier
// @Purpose: "Test for unresolved operand types"
// @Result: "UnresolvedOperandType @ 8:23"

architecture unr_ot_02 {
    enum A { r = 0 }
    operand-type AT {
        sub-operand a: A;
    }
}
