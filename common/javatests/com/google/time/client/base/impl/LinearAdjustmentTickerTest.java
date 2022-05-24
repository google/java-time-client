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

package com.google.time.client.base.impl;

import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_SECOND;
import static org.junit.Assert.assertEquals;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.testing.FakeClocks;
import org.junit.Test;

public class LinearAdjustmentTickerTest {

  @Test
  public void zeroAdjustment() {
    int adjustmentTicksPerGigaTick = 0;
    testAdjustmentCalcs(adjustmentTicksPerGigaTick);
  }

  @Test
  public void positiveAdjustment() {
    long adjustmentTicksPerGigaTick = 50;
    testAdjustmentCalcs(adjustmentTicksPerGigaTick);
  }

  @Test
  public void negativeAdjustment() {
    int adjustmentTicksPerGigaTick = -50;
    testAdjustmentCalcs(adjustmentTicksPerGigaTick);
  }

  @Test
  public void largePositiveAdjustment() {
    // Base clock is 50% slow!
    long adjustmentTicksPerGigaTick = 500_000_000L;
    testAdjustmentCalcs(adjustmentTicksPerGigaTick);
  }

  @Test
  public void largeNegativeAdjustment() {
    // Base clock is 50% fast!
    long adjustmentTicksPerGigaTick = -500_000_000L;
    testAdjustmentCalcs(adjustmentTicksPerGigaTick);
  }

  private static void testAdjustmentCalcs(long adjustmentTicksPerGigaTick) {
    FakeClocks fakeClocks = new FakeClocks();
    FakeClocks.FakeTicker fakeTicker = fakeClocks.getFakeTicker();
    long baseTickerStartValue = 1_000_000_000;
    fakeTicker.setTicksValue(baseTickerStartValue);

    LinearAdjustmentTicker linearAdjustmentTicker =
        new LinearAdjustmentTicker(fakeTicker, adjustmentTicksPerGigaTick);
    Ticks actualStartTicks = linearAdjustmentTicker.ticks();
    Ticks expectedStartTicks = Ticks.fromTickerValue(linearAdjustmentTicker, 0);
    assertEquals(expectedStartTicks, actualStartTicks);

    // Advance the base ticker by a full second. Internal LinearAdjustmentTicker work in nanos, s
    // we should see adjustmentTicksPerGigaTick.
    long baseTickerAdvanceNanos = NANOS_PER_SECOND;
    fakeTicker.advanceNanos(baseTickerAdvanceNanos);
    long elapsedFakeTickerNanos = baseTickerAdvanceNanos;
    long expectedIncrementalAdjustment = adjustmentTicksPerGigaTick;
    long expectedTotalAdjustmentNanos = expectedIncrementalAdjustment;
    checkTicksAndDuration(
        linearAdjustmentTicker,
        elapsedFakeTickerNanos,
        expectedTotalAdjustmentNanos,
        actualStartTicks);

    // Try advancing by a fractions of a second that the adjustmentTicksPerGigaTick can be divided
    // by exactly.
    assertEquals("Test logic assumes a multiple of 10", 0, adjustmentTicksPerGigaTick % 10);
    baseTickerAdvanceNanos = NANOS_PER_SECOND / 10;
    fakeTicker.advanceNanos(baseTickerAdvanceNanos);
    elapsedFakeTickerNanos += baseTickerAdvanceNanos;
    expectedIncrementalAdjustment = adjustmentTicksPerGigaTick / 10;
    expectedTotalAdjustmentNanos += expectedIncrementalAdjustment;
    checkTicksAndDuration(
        linearAdjustmentTicker,
        elapsedFakeTickerNanos,
        expectedTotalAdjustmentNanos,
        actualStartTicks);

    // Try advancing by a large duration.
    baseTickerAdvanceNanos = NANOS_PER_SECOND * 24L * 60 * 60 * 365;
    fakeTicker.advanceNanos(baseTickerAdvanceNanos);
    elapsedFakeTickerNanos += baseTickerAdvanceNanos;
    expectedIncrementalAdjustment = adjustmentTicksPerGigaTick * 24L * 60 * 60 * 365;
    expectedTotalAdjustmentNanos += expectedIncrementalAdjustment;
    checkTicksAndDuration(
        linearAdjustmentTicker,
        elapsedFakeTickerNanos,
        expectedTotalAdjustmentNanos,
        actualStartTicks);
  }

  private static void checkTicksAndDuration(
      LinearAdjustmentTicker linearAdjustmentTicker,
      long elapsedFakeTickerNanos,
      long expectedTotalAdjustmentNanos,
      Ticks actualStartTicks) {
    Ticks actualTicks = linearAdjustmentTicker.ticks();
    Ticks expectedTicks =
        Ticks.fromTickerValue(
            linearAdjustmentTicker, elapsedFakeTickerNanos + expectedTotalAdjustmentNanos);
    assertEquals(expectedTicks, actualTicks);
    Duration expectedDurationFromStart =
        Duration.ofNanos(elapsedFakeTickerNanos + expectedTotalAdjustmentNanos);
    assertEquals(expectedDurationFromStart, actualStartTicks.durationUntil(actualTicks));
  }
}
