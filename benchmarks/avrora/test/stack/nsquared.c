volatile int array[100];

int main() {
  int i = 0;
  int j = 100;

  for ( i = 0; i < 100; i++ ) {
    proc(j);
  }

  return array[0];
}

int proc(int j) {
  int k = 0;
  for ( k = 0; k < j; k++ ) {
    array[k] = j;
  }

  return 0;
}
