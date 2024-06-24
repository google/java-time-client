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

package com.google.time.client.base;

// import androidx.annotation.NonNull;

import com.google.time.client.base.impl.ExactMath;

/**
 * Ticker is an abstraction for a clock that can be used to track elapsed time. A {@link Ticker}
 * returns unit-less readings from the clock as {@link Ticks}. Callers can use {@link Ticks} to
 * derive times in seconds without knowing details of the underlying clock, such as its precision /
 * resolution or whether it can be reset or has monotonic behavior.
 *
 * <p>Public implementation classes and methods returning {@link Ticker} implementations should be
 * clear about the clock behavior.
 */
public abstract class Ticker {

  /** Returns a reading from the clock in ticks. */
  /*@NonNull*/ public abstract Ticks ticks();

  /**
   * Calculates the duration between two Ticks.
   *
   * <p>The accuracy and precision of the {@link Duration} will depend on the origin {@link
   * Ticker}'s accuracy and precision. If the origin {@link Ticker} has low accuracy, the resulting
   * {@link Duration} will be inaccurate. If the ticks do not originate from the same ticker, then a
   * IllegalArgumentException is thrown.
   *
   * @param start - the start ticks, not null
   * @param end - the end ticks, not null
   * @return a non null Duration that is the period of time between the two ticks. If start was read
   *     after end then the return value will be a negative period else it is positive.
   * @throws IllegalArgumentException - if the ticks do not originate from the same ticker
   * @throws ArithmeticException - if the calculation results in an overflow
   */
  /*@NonNull*/ public abstract Duration durationBetween(
      /*@NonNull*/ Ticks start, /*@NonNull*/ Ticks end) throws IllegalArgumentException;

  /**
   * Confirms the supplied {@link Ticks} is from this ticker. If not, {@link
   * IllegalArgumentException} is thrown.
   */
  protected final void checkThisIsTicksOrigin(/*@NonNull*/ Ticks ticks) {
    if (ticks.getOriginTicker() != this) {
      throw new IllegalArgumentException(
          String.format("Ticks (%s) must be from this Ticker (%s)", ticks, this));
    }
  }

  /**
   * Returns the number of increments between two Ticks.
   *
   * @param start - the start ticks, not null
   * @param end - the end ticks, not null
   * @return a long that is the number of increments between two ticks. If start was read after end
   *     then the return value will be a negative value else it is positive.
   * @throws IllegalArgumentException - if the ticks do not originate from the same ticker
   * @throws ArithmeticException - if the calculation results in an overflow
   */
  protected final long incrementsBetween(
      /*@NonNull*/ Ticks start, /*@NonNull*/ Ticks end) throws IllegalArgumentException {
    checkThisIsTicksOrigin(start);
    checkThisIsTicksOrigin(end);

    return ExactMath.subtractExact(end.getValue(), start.getValue());
  }

  /** Factory method for a {@link Ticks} that can be used by subclasses. */
  protected final Ticks createTicks(long value) {
    return Ticks.fromTickerValue(this, value);
  }

  /** Extract the value from a {@link Ticks} created by this ticker. */
  protected final long valueForTicks(Ticks ticks) {
    checkThisIsTicksOrigin(ticks);
    return ticks.getValue();
  }
}
