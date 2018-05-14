// @Harness: verifier
// @Purpose: "Test for redefinitions of formats"
// @Result: "RedefinedFormat @ 7:10"

architecture redef_enc_01 {
   format F = { a[3:0] }
   format F = { a[3:0] }
}