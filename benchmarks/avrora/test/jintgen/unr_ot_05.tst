// @Harness: verifier
// @Purpose: "Test for unresolved operand types"
// @Result: "UnresolvedOperandType @ 7:23"

architecture unr_type_01 {
    operand-type AT {
        sub-operand a: A;
    }
}
