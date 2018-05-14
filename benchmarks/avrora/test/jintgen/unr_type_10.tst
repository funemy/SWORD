// @Harness: verifier
// @Purpose: "Test for unresolved types"
// @Result: "UnresolvedType @ 7:16"

architecture unr_type_10 {
    operand-type A {
        write : duck { }
    }
}

