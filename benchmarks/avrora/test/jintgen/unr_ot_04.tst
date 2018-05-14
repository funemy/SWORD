// @Harness: verifier
// @Purpose: "Test for unresolved operand types"
// @Result: "UnresolvedOperandType @ 7:20"

architecture unr_ot_02 {
    enum A { r = 0 }
    instruction "I" a: A {
    }
}
