// @Harness: verifier
// @Purpose: "Test for unresolved types"
// @Result: "UnresolvedType @ 7:15"

architecture unr_type_09 {
    operand-type A {
        read : duck { }
    }
}
