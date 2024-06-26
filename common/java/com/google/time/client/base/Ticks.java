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

import com.google.time.client.base.impl.Objects;

/**
 * A value type for tracking elapsed time without committing to specific epoch baseline value or
 * relationship to physical seconds.
 *
 * <p>The value held by a Ticks is a reading from a {@link Ticker}, and can be converted to and from
 * a value in physical seconds. The seconds values calculated may be inaccurate depending on the
 * accuracy and precision of the underlying Ticker.
 *
 * <p>Each object holds a reference to an {@code originTicker} which is intended to prevent
 * comparisons between, and calculations using, ticks originating from different {@link Ticker}
 * (which may be using different epochs, have different resolutions, and so on).
 */
public final class Ticks implements Comparable<Ticks> {

  /*@NonNull*/ private final Ticker originTicker;
  private final long value;

  private Ticks(/*@NonNull*/ Ticker originTicker, long value) {
    this.originTicker = Objects.requireNonNull(originTicker, "originTicker");
    this.value = value;
  }

  /**
   * Creates a {@link Ticks} from an originating ticker and a value. This method should only be
   * called from {@link Ticker#ticks()} implementations and in tests. Look for {@link
   * Ticker}-specific methods if you want a {@link Ticks} with a value for comparison with a raw
   * clock value.
   *
   * @param originTicker - the originating ticker
   * @param value the value from a Ticker
   */
  static /*@NonNull*/ Ticks fromTickerValue(/*@NonNull*/ Ticker originTicker, long value) {
    return new Ticks(originTicker, value);
  }

  /**
   * Calculates the duration between two Ticks.
   *
   * <p>The accuracy and precision of the {@link Duration} will depend on the origin {@link
   * Ticker}'s accuracy and precision. If the origin {@link Ticker} has low accuracy, the resulting
   * {@link Duration} will be inaccurate. If the ticks do not originate from the same ticker, then a
   * IllegalArgumentException is thrown.
   *
   * @param other - the other ticks, not null
   * @return a non null Duration that is the period of time between the two ticks. If start was read
   *     after end then the return value will be a negative period else it is positive.
   * @throws IllegalArgumentException - if the ticks do not originate from the same ticker
   * @throws ArithmeticException - if the calculation results in an overflow
   */
  public /*@NonNull*/ Duration durationUntil(
      /*@NonNull*/ Ticks other) throws IllegalArgumentException {
    return this.getOriginTicker().durationBetween(this, other);
  }

  /**
   * Returns {@code true} if this ticks was read after the specified ticks.
   *
   * @param t1 the ticks to compare to
   * @throws IllegalArgumentException - if the ticks do not originate from the same ticker
   */
  public boolean isAfter(/*@NonNull*/ Ticks t1) {
    if (this.originTicker != t1.originTicker) {
      throw new IllegalArgumentException("Ticks must be from the same origin");
    }

    return this.value > t1.value;
  }

  /**
   * Returns {@code true} if this ticks was read before the specified ticks.
   *
   * @param t1 the ticks to compare to
   * @throws IllegalArgumentException - if the ticks do not originate from the same ticker
   */
  public boolean isBefore(/*@NonNull*/ Ticks t1) {
    if (this.originTicker != t1.originTicker) {
      throw new IllegalArgumentException("Ticks must be from the same origin");
    }

    return this.value < t1.value;
  }

  /** Returns the ticker that originated the ticks. */
  /*@NonNull*/ public Ticker getOriginTicker() {
    return originTicker;
  }

  /** Returns the wrapped ticks value. */
  long getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Ticks)) {
      return false;
    }
    Ticks ticks = (Ticks) o;
    return this.value == ticks.value && Objects.equals(originTicker, ticks.originTicker);
  }

  @Override
  public int hashCode() {
    return Objects.hash(originTicker, value);
  }

  @Override
  public String toString() {
    return "Ticks{" + "originalTicker=" + originTicker + ", value=" + value + '}';
  }

  @Override
  public int compareTo(Ticks other) {
    if (!Objects.equals(this.originTicker, other.originTicker)) {
      // It's a very bad idea to compare Ticks with different Tickers, and we cannot compare tickers
      // in a stable way. Throwing an exception is only broadly in line with the contract.
      throw new ClassCastException(
          "Ticks from different tickers are not comparable: this=" + this + ", other=" + other);
    }
    return Long.compare(this.value, other.value);
  }
}
