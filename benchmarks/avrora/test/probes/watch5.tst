# @Harness: probes
# @Result: 
# @Program: array.asm
# @Purpose: Tests whether probe inserted from within watch (on same instruction) works correctly

watch A { insert B 0x08; | | | }
probe B { | }

main {
  insert A 0x10e;
}

result {
  102 A.beforeRead;
  102 A.afterRead;
  109 B.before;
  111 B.after;
}
