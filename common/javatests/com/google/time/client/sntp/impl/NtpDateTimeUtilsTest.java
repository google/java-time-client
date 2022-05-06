/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.time.client.sntp.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

public class NtpDateTimeUtilsTest {

  @Test
  public void randomizeLowestBits() {
    Random random = new Random(1);
    {
      int fractionBits = 0;
      assertThrows(
          IllegalArgumentException.class,
          () -> NtpDateTimeUtils.randomizeLowestBits(random, fractionBits, -1));
      assertThrows(
          IllegalArgumentException.class,
          () -> NtpDateTimeUtils.randomizeLowestBits(random, fractionBits, 0));
      assertThrows(
          IllegalArgumentException.class,
          () -> NtpDateTimeUtils.randomizeLowestBits(random, fractionBits, Integer.SIZE));
      assertThrows(
          IllegalArgumentException.class,
          () -> NtpDateTimeUtils.randomizeLowestBits(random, fractionBits, Integer.SIZE + 1));
    }

    // Check the behavior looks correct from a probabilistic point of view.
    for (int input : new int[] {0, 0xFFFFFFFF}) {
      for (int bitCount = 1; bitCount < Integer.SIZE; bitCount++) {
        int upperBitMask = 0xFFFFFFFF << bitCount;
        int expectedUpperBits = input & upperBitMask;

        Set<Integer> values = new HashSet<>();
        values.add(input);

        int trials = 100;
        for (int i = 0; i < trials; i++) {
          int outputFractionBits = NtpDateTimeUtils.randomizeLowestBits(random, input, bitCount);

          // Record the output value for later analysis.
          values.add(outputFractionBits);

          // Check upper bits did not change.
          assertEquals(expectedUpperBits, outputFractionBits & upperBitMask);
        }

        // It's possible to be more rigorous here, perhaps with a histogram. As bitCount rises,
        // values.size() quickly trend towards the value of trials + 1. For now, this mostly just
        // guards against a no-op implementation.
        assertTrue(bitCount + ":" + values.size(), values.size() > 1);
      }
    }
  }
}
