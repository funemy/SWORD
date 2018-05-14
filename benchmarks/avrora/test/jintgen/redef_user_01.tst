// @Harness: verifier
// @Purpose: "Test for redefinitions of enums"
// @Result: "RedefinedUserType @ 10:8"

architecture redef_enum_01 {
   enum E {
      foo = 0
   }

   operand-type E { }
}