# @Harness: eventqueue
# @Result: 5 A B; 10 A; 20 B
# @Purpose: this tests the operation of the delta queue using simple events

event A { insert B 10; }
event B { insert A 5; remove B; }

main {
  insert A 5;
  insert B 5;
  advance 25;
}
