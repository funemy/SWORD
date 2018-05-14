# @Harness: probes
# @Result: 
# @Program: array.asm
# @Purpose: Tests whether probes inserted from watch (not on same instruction) work correctly

watch A { insert B 0x0A; | | | }
probe B { | }

main {
  insert A 0x10e;
}

result {
  102 A.beforeRead;
  102 A.afterRead;
  104 B.before;
  106 B.after;
  111 B.before;
  113 B.after;
}
