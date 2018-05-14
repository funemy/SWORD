// @Harness: verifier
// @Purpose: "Test for redefinitions of addressing modes"
// @Result: "RedefinedAddrMode @ 7:13"

architecture redef_am_01 {
   addr-mode A1 { }
   addr-mode A1 { }
}
