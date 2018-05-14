// @Harness: verifier
// @Purpose: "Test for redefinitions of enums"
// @Result: "RedefinedUserType @ 10:15"

architecture redef_enum_01 {
   enum E {
      foo = 0
   }

   operand-type E[5]: int [0,31];
}