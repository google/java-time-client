/*
 * Copyright 2024 Google LLC
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
package com.google.time.client.base.testing;

import static org.junit.Assert.assertEquals;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Ticker;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.impl.Objects;

/**
 * A fake {@link Ticker} that can be used for tests. See {@link FakeClocks} for how to obtain an
 * instance. This ticker simulates one that increments the tick value every nanosecond.
 */
public final class FakeTicker extends Ticker implements Advanceable {

  private final FakeClocks fakeClocks;

  private long ticksValue;

  FakeTicker(FakeClocks fakeClocks) {
    this.fakeClocks = Objects.requireNonNull(fakeClocks);
  }

  @Override
  public Duration durationBetween(Ticks start, Ticks end) throws IllegalArgumentException {
    return Duration.ofNanos(incrementsBetween(start, end));
  }

  @Override
  public Ticks ticks() {
    ticksValue += fakeClocks.autoAdvanceDuration.toNanos();
    return getCurrentTicks();
  }

  /** Asserts the current ticks value matches the one supplied. Does not auto advance. */
  public void assertCurrentTicks(Ticks actual) {
    assertEquals(createTicks(ticksValue), actual);
  }

  /** Returns the current ticks value. Does not auto advance. */
  public Ticks getCurrentTicks() {
    return createTicks(ticksValue);
  }

  /** Returns a ticks value. Does not auto advance. */
  public Ticks ticksForValue(long value) {
    return createTicks(value);
  }

  /** Sets the current ticks value. Does not auto advance. */
  public void setTicksValue(long ticksValue) {
    this.ticksValue = ticksValue;
  }

  public void advanceNanos(long nanos) {
    // FakeTicker.ticksValue is fixed to nanoseconds.
    ticksValue += nanos;
  }

  @Override
  public void advance(Duration duration) {
    advanceNanos(duration.toNanos());
  }

  @Override
  public String toString() {
    return "FakeTicker{"
        + "ticksValue="
        + ticksValue
        + ", fakeClocks.autoAdvanceDuration="
        + fakeClocks.autoAdvanceDuration
        + '}';
  }
}
