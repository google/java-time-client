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

/**
 * A {@link Ticker} that provides nanosecond-precision access to Java's {@link System#nanoTime()}
 * clock.
 */
public final class PlatformTicker extends Ticker {

  private static final PlatformTicker INSTANCE = new PlatformTicker();

  public static Ticker instance() {
    return INSTANCE;
  }

  private PlatformTicker() {}

  @Override
  public Ticks ticks() {
    return Ticks.fromTickerValue(this, System.nanoTime());
  }

  @Override
  public String toString() {
    return "PlatformTicker";
  }

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
  /*@NonNull*/ public Duration durationBetween(
      /*@NonNull*/ Ticks start, /*@NonNull*/ Ticks end) throws IllegalArgumentException {
    return Duration.ofNanos(incrementsBetween(start, end));
  }
}
