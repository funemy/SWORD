# @Harness: probes
# @Result: 
# @Program: array.asm
# @Purpose: Tests watches on reading of memory locations that are enabled by events

watch A { | | | }
event B { 
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

event C {
  remove A 0x100;
  remove A 0x101;
  remove A 0x102;
  remove A 0x103;
  remove A 0x104;
  remove A 0x105;
  remove A 0x106;
  remove A 0x107;
  remove A 0x108;
  remove A 0x109;
  remove A 0x10a;
  remove A 0x10b;
  remove A 0x10c;
  remove A 0x10d;
  remove A 0x10e;
  remove A 0x10f;
}

main {
  insert B 50;
  insert C 100;
}

result {
  50 B;
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
  100 C;
}
