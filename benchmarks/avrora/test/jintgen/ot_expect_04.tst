// @Harness: verifier
// @Purpose: "Test for places where operand types are expected"
// @Result: "OperandTypeExpected @ 7:13"

architecture ot_expect_01 {
   enum E {
       r = 0
   }
   
   subroutine foo(e: E): void {
       local f: int = read(e);
   }
}
