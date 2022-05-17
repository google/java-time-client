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
package com.google.time.client.base.testing;

import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_MILLISECOND;
import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_SECOND;
import static org.junit.Assert.assertEquals;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.Ticker;
import com.google.time.client.base.Ticks;

/**
 * A source of fake {@link Ticker} and {@link InstantSource} objects that can be made to advance
 * with each other automatically each time they are accessed via methods on this class. The
 * individual clocks can be advanced independently via methods on each.
 */
public class FakeClocks {

  private final FakeInstantSource fakeInstantSource = new FakeInstantSource();
  private final FakeTicker fakeTicker = new FakeTicker();

  private Duration autoAdvanceDuration = Duration.ZERO;

  /** Clock is automatically advanced <em>before</em> the time is read. */
  public void setAutoAdvanceNanos(long autoAdvanceNanos) {
    this.autoAdvanceDuration = Duration.ofNanos(autoAdvanceNanos);
  }

  /** Clock is automatically advanced <em>before</em> the time is read. */
  public void setAutoAdvanceDuration(Duration autoAdvanceDuration) {
    this.autoAdvanceDuration = autoAdvanceDuration;
  }

  /** Returns the fake {@link FakeInstantSource}. */
  public FakeInstantSource getInstantSource() {
    return fakeInstantSource;
  }

  /** Returns the fake {@link Ticker}. */
  public FakeTicker getTicker() {
    return fakeTicker;
  }

  /**
   * A fake {@link Ticker} that can be used for tests. This ticker simulates one that increments the
   * tick value every nanosecond.
   */
  public class FakeTicker extends Ticker {

    private long ticksValue;

    private FakeTicker() {}

    @Override
    public Duration durationBetween(Ticks start, Ticks end) throws IllegalArgumentException {
      return Duration.ofNanos(incrementsBetween(start, end));
    }

    @Override
    public Ticks ticks() {
      ticksValue += autoAdvanceDuration.toNanos();
      return Ticks.fromTickerValue(this, ticksValue);
    }

    /** Asserts the current ticks value matches the one supplied. Does not auto advance. */
    public void assertCurrentTicks(Ticks actual) {
      assertEquals(Ticks.fromTickerValue(this, ticksValue), actual);
    }

    /** Returns the current ticks value. Does not auto advance. */
    public long getTicksValue() {
      return ticksValue;
    }

    /** Sets the current ticks value. Does not auto advance. */
    public void setTicksValue(long ticksValue) {
      this.ticksValue = ticksValue;
    }

    public void advanceNanos(int nanos) {
      // FakeTicker.ticksValue is fixed to nanoseconds.
      ticksValue += nanos;
    }

    @Override
    public String toString() {
      return "FakeTicker{"
          + "ticksValue="
          + ticksValue
          + ", FakeClocks.this.autoAdvanceDuration="
          + FakeClocks.this.autoAdvanceDuration
          + '}';
    }
  }

  /**
   * A fake {@link InstantSource} that can be used for tests.
   *
   * <p>By default, this instant source simulates one that returns instants with millisecond
   * precision, but this can be changed.
   */
  public class FakeInstantSource extends InstantSource {

    private Instant instantSourceNow = Instant.ofEpochMilli(0);
    private int precision = InstantSource.PRECISION_MILLIS;

    private FakeInstantSource() {}

    @Override
    public int getPrecision() {
      return precision;
    }

    @Override
    public Instant instant() {
      instantSourceNow = instantSourceNow.plus(autoAdvanceDuration);
      if (precision == PRECISION_MILLIS) {
        return Instant.ofEpochMilli(instantSourceNow.toEpochMilli());
      } else if (precision == PRECISION_NANOS) {
        return instantSourceNow;
      }
      throw new IllegalStateException("Unknown resolution=" + precision);
    }

    /** Advance this instant source by the specified number of milliseconds. */
    public void advanceMillis(long millis) {
      instantSourceNow = instantSourceNow.plus(Duration.ofNanos(millis * NANOS_PER_MILLISECOND));
    }

    public void setEpochMillis(long epochMillis) {
      instantSourceNow = Instant.ofEpochMilli(epochMillis);
    }

    public void setInstant(Instant instant) {
      instantSourceNow = instant;
    }

    public Instant getFakeClockInstant() {
      return instantSourceNow;
    }

    public void setPrecision(int precision) {
      this.precision = precision;
    }

    public void setEpochNanos(long epochNanos) {
      long seconds = epochNanos / NANOS_PER_SECOND;
      long nanos = epochNanos % NANOS_PER_SECOND;
      if (epochNanos < 0) {
        seconds--;
        nanos += NANOS_PER_SECOND;
      }
      instantSourceNow = Instant.ofEpochSecond(seconds, nanos);
    }

    @Override
    public String toString() {
      return "FakeInstantSource{"
          + "instantSourceNow="
          + instantSourceNow
          + ", resolution="
          + precision
          + ", FakeClocks.this.autoAdvanceDuration="
          + FakeClocks.this.autoAdvanceDuration
          + '}';
    }
  }
}
