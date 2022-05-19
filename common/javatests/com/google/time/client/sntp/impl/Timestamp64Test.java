/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.time.client.sntp.impl.NtpDateTimeUtils.MAX_32BIT_SECONDS_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.time.client.base.Instant;
import com.google.time.client.base.impl.DateTimeConstants;
import com.google.time.client.base.testing.PredictableRandom;
import org.junit.Test;

public class Timestamp64Test {

  @Test
  public void fromComponents() {
    long minNtpEraSeconds = 0;
    long maxNtpEraSeconds = MAX_32BIT_SECONDS_VALUE;

    assertThrows(
        IllegalArgumentException.class, () -> Timestamp64.fromComponents(minNtpEraSeconds - 1, 0));
    assertThrows(
        IllegalArgumentException.class, () -> Timestamp64.fromComponents(maxNtpEraSeconds + 1, 0));

    assertComponentCreation(minNtpEraSeconds, 0);
    assertComponentCreation(maxNtpEraSeconds, 0);
    assertComponentCreation(maxNtpEraSeconds, Integer.MIN_VALUE);
    assertComponentCreation(maxNtpEraSeconds, Integer.MAX_VALUE);
  }

  private static void assertComponentCreation(long ntpEraSeconds, int fractionBits) {
    Timestamp64 value = Timestamp64.fromComponents(ntpEraSeconds, fractionBits);
    assertEquals(ntpEraSeconds, value.getEraSeconds());
    assertEquals(fractionBits, value.getFractionBits());
  }

  @Test
  public void equalsAndHashcode() {
    assertEqualsAndHashcode(0, 0);
    assertEqualsAndHashcode(1, 0);
    assertEqualsAndHashcode(0, 1);
  }

  private static void assertEqualsAndHashcode(int eraSeconds, int fractionBits) {
    Timestamp64 one = Timestamp64.fromComponents(eraSeconds, fractionBits);
    Timestamp64 two = Timestamp64.fromComponents(eraSeconds, fractionBits);
    assertEquals(one, two);
    assertEquals(one.hashCode(), two.hashCode());
  }

  @Test
  public void compareTo() {
    Timestamp64 lowFrac = Timestamp64.fromString("11111111.00000000");
    Timestamp64 midFrac = Timestamp64.fromString("11111111.77777777");
    Timestamp64 highFrac = Timestamp64.fromString("11111111.FFFFFFFF");

    assertOrdering(lowFrac, midFrac, highFrac);

    Timestamp64 lowSecs = Timestamp64.fromString("00000000.11111111");
    Timestamp64 midSecs = Timestamp64.fromString("77777777.11111111");
    Timestamp64 highSecs = Timestamp64.fromString("FFFFFFFF.11111111");

    assertOrdering(lowSecs, midSecs, highSecs);

    Timestamp64 lowHigh = Timestamp64.fromString("00000000.FFFFFFFF");
    Timestamp64 mid = Timestamp64.fromString("77777777.77777777");
    Timestamp64 highLow = Timestamp64.fromString("FFFFFFFF.00000000");

    assertOrdering(lowHigh, mid, highLow);
  }

  private static void assertOrdering(Timestamp64 low, Timestamp64 medium, Timestamp64 high) {
    assertThat(low).isEquivalentAccordingToCompareTo(low);
    assertThat(low).isLessThan(medium);
    assertThat(low).isLessThan(high);

    assertThat(medium).isEquivalentAccordingToCompareTo(medium);
    assertThat(medium).isGreaterThan(low);
    assertThat(medium).isLessThan(high);

    assertThat(high).isEquivalentAccordingToCompareTo(high);
    assertThat(high).isGreaterThan(low);
    assertThat(high).isGreaterThan(medium);
  }

  @Test
  public void stringForm() {
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString(""));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("."));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("1234567812345678"));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("12345678?12345678"));
    assertThrows(
        IllegalArgumentException.class, () -> Timestamp64.fromString("12345678..12345678"));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("1.12345678"));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("12.12345678"));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("123456.12345678"));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("1234567.12345678"));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("12345678.1"));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("12345678.12"));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("12345678.123456"));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("12345678.1234567"));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("X2345678.12345678"));
    assertThrows(IllegalArgumentException.class, () -> Timestamp64.fromString("12345678.X2345678"));

    assertStringCreation("00000000.00000000", 0, 0);
    assertStringCreation("00000001.00000001", 1, 1);
    assertStringCreation("ffffffff.ffffffff", 0xFFFFFFFFL, 0xFFFFFFFF);
  }

  private static void assertStringCreation(
      String string, long expectedSeconds, int expectedFractionBits) {
    Timestamp64 timestamp64 = Timestamp64.fromString(string);
    assertEquals(string, timestamp64.toString());
    assertEquals(expectedSeconds, timestamp64.getEraSeconds());
    assertEquals(expectedFractionBits, timestamp64.getFractionBits());
  }

  @Test
  public void stringForm_lenientHexCasing() {
    Timestamp64 mixedCaseValue = Timestamp64.fromString("AaBbCcDd.EeFf1234");
    assertEquals(0xAABBCCDDL, mixedCaseValue.getEraSeconds());
    assertEquals(0xEEFF1234, mixedCaseValue.getFractionBits());
  }

  @Test
  public void fromInstant_secondsHandling() {
    final int era0 = 0;
    final int eraNeg1 = -1;
    final int eraNeg2 = -2;
    final int era1 = 1;

    assertInstantCreationOnlySeconds(-Timestamp64.OFFSET_1900_TO_1970, 0, era0);
    assertInstantCreationOnlySeconds(
        -Timestamp64.OFFSET_1900_TO_1970 - Timestamp64.SECONDS_IN_ERA, 0, eraNeg1);
    assertInstantCreationOnlySeconds(
        -Timestamp64.OFFSET_1900_TO_1970 + Timestamp64.SECONDS_IN_ERA, 0, era1);

    assertInstantCreationOnlySeconds(
        -Timestamp64.OFFSET_1900_TO_1970 - 1, MAX_32BIT_SECONDS_VALUE, -1);
    assertInstantCreationOnlySeconds(
        -Timestamp64.OFFSET_1900_TO_1970 - Timestamp64.SECONDS_IN_ERA - 1,
        MAX_32BIT_SECONDS_VALUE,
        eraNeg2);
    assertInstantCreationOnlySeconds(
        -Timestamp64.OFFSET_1900_TO_1970 + Timestamp64.SECONDS_IN_ERA - 1,
        MAX_32BIT_SECONDS_VALUE,
        era0);

    assertInstantCreationOnlySeconds(-Timestamp64.OFFSET_1900_TO_1970 + 1, 1, era0);
    assertInstantCreationOnlySeconds(
        -Timestamp64.OFFSET_1900_TO_1970 - Timestamp64.SECONDS_IN_ERA + 1, 1, eraNeg1);
    assertInstantCreationOnlySeconds(
        -Timestamp64.OFFSET_1900_TO_1970 + Timestamp64.SECONDS_IN_ERA + 1, 1, era1);

    assertInstantCreationOnlySeconds(0, Timestamp64.OFFSET_1900_TO_1970, era0);
    assertInstantCreationOnlySeconds(
        -Timestamp64.SECONDS_IN_ERA, Timestamp64.OFFSET_1900_TO_1970, eraNeg1);
    assertInstantCreationOnlySeconds(
        Timestamp64.SECONDS_IN_ERA, Timestamp64.OFFSET_1900_TO_1970, era1);

    assertInstantCreationOnlySeconds(1, Timestamp64.OFFSET_1900_TO_1970 + 1, era0);
    assertInstantCreationOnlySeconds(
        -Timestamp64.SECONDS_IN_ERA + 1, Timestamp64.OFFSET_1900_TO_1970 + 1, eraNeg1);
    assertInstantCreationOnlySeconds(
        Timestamp64.SECONDS_IN_ERA + 1, Timestamp64.OFFSET_1900_TO_1970 + 1, era1);

    assertInstantCreationOnlySeconds(-1, Timestamp64.OFFSET_1900_TO_1970 - 1, era0);
    assertInstantCreationOnlySeconds(
        -Timestamp64.SECONDS_IN_ERA - 1, Timestamp64.OFFSET_1900_TO_1970 - 1, eraNeg1);
    assertInstantCreationOnlySeconds(
        Timestamp64.SECONDS_IN_ERA - 1, Timestamp64.OFFSET_1900_TO_1970 - 1, era1);
  }

  private static void assertInstantCreationOnlySeconds(
      long epochSeconds, long expectedNtpEraSeconds, int ntpEra) {
    int nanosOfSecond = 0;
    Instant instant = Instant.ofEpochSecond(epochSeconds, nanosOfSecond);
    Timestamp64 timestamp = Timestamp64.fromInstant(instant);
    assertEquals(expectedNtpEraSeconds, timestamp.getEraSeconds());

    int expectedFractionBits = 0;
    assertEquals(expectedFractionBits, timestamp.getFractionBits());

    // Confirm the Instant can be round-tripped if we know the era. Also assumes the nanos can be
    // stored precisely; 0 can be.
    Instant roundTrip = timestamp.toInstant(ntpEra);
    assertEquals(instant, roundTrip);
  }

  @Test
  public void fromInstant_fractionHandling() {
    // Try some values we know can be represented exactly.
    assertInstantCreationOnlyFractionExact(0x0, 0);
    assertInstantCreationOnlyFractionExact(0x80000000, 500_000_000L);
    assertInstantCreationOnlyFractionExact(0x40000000, 250_000_000L);

    // Test the limits of precision.
    assertInstantCreationOnlyFractionExact(0x00000006, 1L);
    assertInstantCreationOnlyFractionExact(0x00000005, 1L);
    assertInstantCreationOnlyFractionExact(0x00000004, 0L);
    assertInstantCreationOnlyFractionExact(0x00000002, 0L);
    assertInstantCreationOnlyFractionExact(0x00000001, 0L);

    // Confirm nanosecond storage / precision is within 1ns.
    final boolean exhaustive = false;
    for (int i = 0; i < DateTimeConstants.NANOS_PER_SECOND; i++) {
      Instant instant = Instant.ofEpochSecond(0, i);
      Instant roundTripped = Timestamp64.fromInstant(instant).toInstant(0);
      assertNanosWithTruncationAllowed(i, roundTripped);
      if (!exhaustive) {
        i += 999_999;
      }
    }
  }

  @SuppressWarnings("JavaInstantGetSecondsGetNano")
  private static void assertInstantCreationOnlyFractionExact(int fractionBits, long expectedNanos) {
    Timestamp64 timestamp64 = Timestamp64.fromComponents(0, fractionBits);

    final int ntpEra = 0;
    Instant instant = timestamp64.toInstant(ntpEra);

    assertEquals(expectedNanos, instant.getNano());
  }

  @SuppressWarnings("JavaInstantGetSecondsGetNano")
  private static void assertNanosWithTruncationAllowed(long expectedNanos, Instant instant) {
    // Allow for < 1ns difference due to truncation.
    long actualNanos = instant.getNano();
    assertTrue(
        "expectedNanos=" + expectedNanos + ",  actualNanos=" + actualNanos,
        actualNanos == expectedNanos || actualNanos == expectedNanos - 1);
  }

  @SuppressWarnings("JavaInstantGetSecondsGetNano")
  @Test
  public void millisRandomizationConstant() {
    // Mathematically, we can say that to represent 1000 different values, we need 10 binary
    // digits (2^10 = 1024). The same is true whether we're dealing with integers or fractions.
    // Unfortunately, for fractions those 1024 values do not correspond to discrete decimal values.
    // Discrete millisecond values as fractions (e.g. 0.001 - 0.999) cannot be represented exactly
    // except where the value can also be represented as some combination of powers of -2. When
    // we convert back and forth, we truncate, so millisecond decimal fraction N represented as a
    // binary fraction will always be equal to or lower than N. If we are truncating correctly it
    // will never be as low as (N-0.001). N -> [N-0.001, N].

    // We need to keep 10 bits to hold millis (inaccurately, since there are numbers that
    // cannot be represented exactly), leaving us able to randomize the remaining 22 bits of the
    // fraction part without significantly affecting the number represented.
    assertEquals(22, Timestamp64.SUB_MILLIS_BITS_TO_RANDOMIZE);

    // Brute force proof that randomization logic will keep the timestamp within the range
    // [N-0.001, N] where x is in milliseconds.
    int smallFractionRandomizedLow = 0;
    int smallFractionRandomizedHigh = 0b00000000_00111111_11111111_11111111;
    int largeFractionRandomizedLow = 0b11111111_11000000_00000000_00000000;
    int largeFractionRandomizedHigh = 0b11111111_11111111_11111111_11111111;

    long smallLowNanos =
        Timestamp64.fromComponents(0, smallFractionRandomizedLow).toInstant(0).getNano();
    long smallHighNanos =
        Timestamp64.fromComponents(0, smallFractionRandomizedHigh).toInstant(0).getNano();
    long smallDelta = smallHighNanos - smallLowNanos;
    long millisInNanos = 1_000_000_000 / 1_000;
    assertTrue(smallDelta >= 0 && smallDelta < millisInNanos);

    long largeLowNanos =
        Timestamp64.fromComponents(0, largeFractionRandomizedLow).toInstant(0).getNano();
    long largeHighNanos =
        Timestamp64.fromComponents(0, largeFractionRandomizedHigh).toInstant(0).getNano();
    long largeDelta = largeHighNanos - largeLowNanos;
    assertTrue(largeDelta >= 0 && largeDelta < millisInNanos);

    PredictableRandom random = new PredictableRandom();
    random.setIntSequence(new int[] {0xFFFF_FFFF});
    Timestamp64 zero = Timestamp64.fromComponents(0, 0);
    Timestamp64 zeroWithFractionRandomized = zero.randomizeSubMillis(random);
    assertEquals(zero.getEraSeconds(), zeroWithFractionRandomized.getEraSeconds());
    assertEquals(smallFractionRandomizedHigh, zeroWithFractionRandomized.getFractionBits());
  }
}
