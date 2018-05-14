# @Harness: probes
# @Result: 
# @Program: simple.asm
# @Purpose: this tests the operation of the low-level probing infrastructure

probe A { | }
probe B { | }

main {
  insert A 0x000;
  insert B 0x000;
}

result {
  0 A.before;
  0 B.before;
  1 A.after;
  1 B.after;
}
