# @Harness: probes
# @Result: 
# @Program: array.asm
# @Purpose: Tests watches on reading of memory locations

watch A { | | | }

main {
  insert A 0x100;
  insert A 0x101;
  insert A 0x102;
  insert A 0x103;
  insert A 0x104;
  insert A 0x105;
  insert A 0x106;
  insert A 0x107;
  insert A 0x108;
  insert A 0x109;
  insert A 0x10a;
  insert A 0x10b;
  insert A 0x10c;
  insert A 0x10d;
  insert A 0x10e;
  insert A 0x10f;
}

result {
  4 A.beforeRead;
  4 A.afterRead;
  11 A.beforeRead;
  11 A.afterRead;
  18 A.beforeRead;
  18 A.afterRead;
  25 A.beforeRead;
  25 A.afterRead;
  32 A.beforeRead;
  32 A.afterRead;
  39 A.beforeRead;
  39 A.afterRead;
  46 A.beforeRead;
  46 A.afterRead;
  53 A.beforeRead;
  53 A.afterRead;
  60 A.beforeRead;
  60 A.afterRead;
  67 A.beforeRead;
  67 A.afterRead;
  74 A.beforeRead;
  74 A.afterRead;
  81 A.beforeRead;
  81 A.afterRead;
  88 A.beforeRead;
  88 A.afterRead;
  95 A.beforeRead;
  95 A.afterRead;
  102 A.beforeRead;
  102 A.afterRead;
  109 A.beforeRead;
  109 A.afterRead;
}
