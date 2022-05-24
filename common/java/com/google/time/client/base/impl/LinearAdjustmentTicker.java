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

import com.google.time.client.base.Duration;
import com.google.time.client.base.Ticker;
import com.google.time.client.base.Ticks;

/**
 * A {@link Ticker} implementation that applies a linear adjustment to a "base" ticker. Used for
 * situations where the base ticker has been calibrated and found to be inaccurate due to a constant
 * clock frequency error.
 *
 * <p>Calculations take place in the nanosecond scale, but results may vary depending on the
 * precision of the base ticker, i.e. if the underlying ticker ticks in milliseconds, then ticks,
 * and therefore durations, produced by this ticker will have similar precision and adjustments may
 * be stepped accordingly.
 */
public final class LinearAdjustmentTicker extends Ticker {

  private final Ticker baseTicker;
  private final long adjustmentTicksPerGigaTick;
  private final Ticks baseAnchorTicks;

  /** Creates a new ticker using a base ticker and a linear adjustment value. The adjustm */
  public LinearAdjustmentTicker(Ticker baseTicker, long adjustmentTicksPerGigaTick) {
    this.baseTicker = Objects.requireNonNull(baseTicker);
    this.adjustmentTicksPerGigaTick = adjustmentTicksPerGigaTick;
    // Capture a base value. All adjustments are made relative to this. This value is this
    // ticker's "zero point".
    baseAnchorTicks = baseTicker.ticks();
  }

  @Override
  public Ticks ticks() {
    Ticks baseTicks = baseTicker.ticks();

    // All values are related back to the zero point.
    Duration duration = baseAnchorTicks.durationUntil(baseTicks);

    // Use nanos for the calculations for best possible precision.
    long durationNanos = duration.toNanos();

    // Long.MAX_VALUE nanos == ~292 years. The multiplication below would over/underflow in long
    // arithmetic within a year if adjustmentTicksPerGigaTick exceeds ~292. Double is used to avoid
    // that.
    long linearAdjustmentNanos =
        (long) ((durationNanos * ((double) adjustmentTicksPerGigaTick)) / 1_000_000_000.0);
    long adjustedTicksValue = ExactMath.addExact(durationNanos, linearAdjustmentNanos);
    return Ticks.fromTickerValue(this, adjustedTicksValue);
  }

  @Override
  public Duration durationBetween(Ticks start, Ticks end) throws IllegalArgumentException {
    return Duration.ofNanos(incrementsBetween(start, end));
  }
}
