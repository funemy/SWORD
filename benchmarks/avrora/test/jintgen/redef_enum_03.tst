// @Harness: verifier
// @Purpose: "Test for redefinitions of enums"
// @Result: "RedefinedUserType @ 10:15"

architecture redef_enum_01 {
   enum E {
      foo = 0
   }

   enum-subset E: E {
      foo = 1
   }
}