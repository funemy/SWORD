// @Harness: verifier
// @Purpose: "Test for redefinitions of format"
// @Result: "RedefinedFormat @ 7:10"

architecture redef_enc_01 {
   format F = { a[3:0] }
   format F = { a[7:0] }
}
