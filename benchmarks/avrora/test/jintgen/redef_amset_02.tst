// @Harness: verifier
// @Purpose: "Test for redefinitions of addressing mode sets"
// @Result: "RedefinedAddrModeSet @ 10:12"

architecture redef_amset_01 {
   addr-mode A1 { }
   addr-mode A2 { }

   addr-set AS { A1 }
   addr-set AS { A1, A2 }
}
