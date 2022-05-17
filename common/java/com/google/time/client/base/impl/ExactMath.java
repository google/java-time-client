/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.time.client.base.impl;

/**
 * Stand-ins for {@link Math} methods which are not available on all platform versions supported by
 * java-time-client.
 */
public class ExactMath {

  private ExactMath() {}

  // A copy of Guava's LongMath.checkedAdd() method.
  /**
   * Returns the sum of {@code a} and {@code b}, provided it does not overflow.
   *
   * @throws ArithmeticException if {@code a + b} overflows in signed {@code long} arithmetic
   */
  public static long addExact(long a, long b) {
    long result = a + b;
    checkNoOverflow((a ^ b) < 0 | (a ^ result) >= 0, "checkedAdd", a, b);
    return result;
  }

  // A copy of Guava's LongMath.checkedSubtract() method.
  /**
   * Returns the difference of {@code a} and {@code b}, provided it does not overflow.
   *
   * @throws ArithmeticException if {@code a - b} overflows in signed {@code long} arithmetic
   */
  public static long subtractExact(long a, long b) {
    long result = a - b;
    checkNoOverflow((a ^ b) >= 0 | (a ^ result) >= 0, "checkedSubtract", a, b);
    return result;
  }

  // A copy of Guava's LongMath.checkedMultiply() method.
  /**
   * Returns the product of {@code a} and {@code b}, provided it does not overflow.
   *
   * @throws ArithmeticException if {@code a * b} overflows in signed {@code long} arithmetic
   */
  public static long multiplyExact(long a, long b) {
    // Hacker's Delight, Section 2-12
    int leadingZeros =
        Long.numberOfLeadingZeros(a)
            + Long.numberOfLeadingZeros(~a)
            + Long.numberOfLeadingZeros(b)
            + Long.numberOfLeadingZeros(~b);
    /*
     * If leadingZeros > Long.SIZE + 1 it's definitely fine, if it's < Long.SIZE it's definitely
     * bad. We do the leadingZeros check to avoid the division below if at all possible.
     *
     * Otherwise, if b == Long.MIN_VALUE, then the only allowed values of a are 0 and 1. We take
     * care of all a < 0 with their own check, because in particular, the case a == -1 will
     * incorrectly pass the division check below.
     *
     * In all other cases, we check that either a is 0 or the result is consistent with division.
     */
    if (leadingZeros > Long.SIZE + 1) {
      return a * b;
    }
    checkNoOverflow(leadingZeros >= Long.SIZE, "checkedMultiply", a, b);
    checkNoOverflow(a >= 0 | b != Long.MIN_VALUE, "checkedMultiply", a, b);
    long result = a * b;
    checkNoOverflow(a == 0 || result / a == b, "checkedMultiply", a, b);
    return result;
  }

  // A copy of Guava's MathPreconditions.checkNoOverflow().
  private static void checkNoOverflow(boolean condition, String methodName, long a, long b) {
    if (!condition) {
      throw new ArithmeticException("overflow: " + methodName + "(" + a + ", " + b + ")");
    }
  }
}
