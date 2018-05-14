// @Harness: verifier
// @Purpose: "Test for unresolved addressing modes"
// @Result: "UnresolvedAddrMode @ 6:21"

architecture unr_am_01 {
    operand-type AM[5]: int [0,31];
    instruction "I": AM {
    }
}
