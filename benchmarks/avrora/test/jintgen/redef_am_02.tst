// @Harness: verifier
// @Purpose: "Test for redefinitions of addressing modes"
// @Result: "RedefinedAddrMode @ 8:13"

architecture redef_am_01 {
   operand-type A[5]: int [0,31];
   addr-mode A1 { }
   addr-mode A1 foo: A { }
}
