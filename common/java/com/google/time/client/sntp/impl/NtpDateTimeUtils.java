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

import com.google.time.client.base.impl.DateTimeConstants;
import java.util.Random;

/** Utility functions associated with NTP data types. */
final class NtpDateTimeUtils {

  /**
   * The maximum value for the number of seconds in NTP data types when represented as a 32-bit
   * unsigned value.
   */
  static final long MAX_32BIT_SECONDS_VALUE = 0xFFFF_FFFFL;

  private NtpDateTimeUtils() {}

  static int fractionBitsToNanos(int fractionBits) {
    long fractionBitsLong = fractionBits & 0xFFFF_FFFFL;
    return (int) ((fractionBitsLong * DateTimeConstants.NANOS_PER_SECOND) >>> 32);
  }

  static int nanosToFractionBits(long nanos) {
    if (nanos > DateTimeConstants.NANOS_PER_SECOND) {
      throw new IllegalArgumentException();
    }
    return (int) ((nanos << 32) / DateTimeConstants.NANOS_PER_SECOND);
  }

  static int randomizeLowestBits(Random random, int value, int bitsToRandomize) {
    if (bitsToRandomize < 1 || bitsToRandomize >= Integer.SIZE) {
      // There's no point in randomizing all bits or none of the bits.
      throw new IllegalArgumentException(Integer.toString(bitsToRandomize));
    }

    int upperBitMask = 0xFFFF_FFFF << bitsToRandomize;
    int lowerBitMask = ~upperBitMask;

    int randomValue = random.nextInt();
    return (value & upperBitMask) | (randomValue & lowerBitMask);
  }
}
