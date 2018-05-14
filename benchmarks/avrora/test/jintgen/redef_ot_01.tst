// @Harness: verifier
// @Purpose: "Test for redefinitions of operand types"
// @Result: "RedefinedUserType @ 8:16"

architecture redef_ot_01 {

   operand-type A[5]: int [0, 7];
   operand-type A[3]: int [0, 5];

}