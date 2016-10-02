package ar.edu.itba.ss.time_driven_simulation.services;

class MathUtils {
  static int factorial(final int n) {
    if(n < 0) {
      throw new IllegalArgumentException("Number must be greater or equal than 0");
    }
    if(n == 0 || n == 1) {
      return 1;
    }
    return n * factorial(n-1);
  }
}
