// @Harness: verifier
// @Purpose: "Test for redefinitions of enums"
// @Result: "RedefinedUserType @ 10:15"

architecture redef_enum_01 {
   enum A {
      foo = 0
   }

   enum E {
      foo = 0
   }

   enum-subset E: A {
      foo = 1
   }
}