// @Harness: verifier
// @Purpose: "Test for variable resolution"
// @Result: "UnresolvedVariable @ 9:15"

architecture _ {
   instruction "I" {
       execute {
           local e: int = 0;
           e = a[3:0];
       }
   }
}
