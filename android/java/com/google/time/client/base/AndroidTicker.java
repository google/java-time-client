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

package com.google.time.client.base;

/**
 * An extension interface for {@link Ticker}s on the Android platform. The current purpose is to
 * enable interoperability with the elapsed time clock that is available on the Android platform but
 * which is not part of the Java standard API. See {@link android.os.SystemClock}.
 */
public abstract class AndroidTicker extends Ticker {

  /**
   * Creates a {@link Ticks} from a value supplied by the device's elapsed realtime nanos clock, or
   * throws {@link UnsupportedOperationException} if the ticker implementation isn't able to convert
   * elapsed realtime values.
   *
   * <p>The {@link Ticker} may use a different underlying clock than the device's elapsed realtime
   * clock, or it may adjust for known frequency error in the elapsed realtime clock, so the {@link
   * Ticks} returned may only be an approximation. Ordering between {@link Ticks} and other
   * properties of the returned {@link Ticks} depend on the precision of the {@link Ticker}
   * implementation.
   *
   * <p>This method is useful to bridge between code that uses {@link
   * android.os.SystemClock#elapsedRealtimeNanos} and similar methods, and code that uses {@link
   * Ticks} or {@link Ticker}. For example, if code has a location with an elapsed realtime clock
   * timestamp, the timestamp can be converted to a {@link Ticks} using this method and then code
   * like {@code locationTicks.durationUntil(ticker.ticks())} can be used to determine approximately
   * how old it is according to the {@code ticker}.
   */
  /*@NonNull*/ public abstract Ticks ticksForElapsedRealtimeNanos(long elapsedRealtimeNanos);

  /**
   * Creates a {@link Ticks} from a value supplied by the device's elapsed realtime millis clock, or
   * throws {@link UnsupportedOperationException} if the ticker implementation isn't able to convert
   * elapsed realtime values.
   *
   * <p>See {@link #ticksForElapsedRealtimeNanos(long)} for details.
   */
  /*@NonNull*/ public abstract Ticks ticksForElapsedRealtimeMillis(long elapsedRealtimeMillis);

  /**
   * Calculates an elapsed realtime clock value in nanoseconds from the supplied {@link Ticks}, or
   * throws {@link UnsupportedOperationException} if the ticker implementation isn't able to convert
   * to elapsed realtime values.
   *
   * <p>The {@link Ticker} may use a different underlying clock than the device's elapsed realtime
   * nanos clock, or it may adjust for known frequency error in the elapsed realtime clock, so the
   * elapsed realtime clock value returned may only be an approximation. Ordering between returned
   * values from two {@link Ticks} depend on the precision of the {@link Ticker} implementation.
   *
   * <p>This method is useful to bridge between code that uses {@link
   * android.os.SystemClock#elapsedRealtimeNanos} and similar methods, and code that uses {@link
   * Ticks} or {@link Ticker}. For example, if code requires an elapsed realtime clock value and it
   * has a {@link Ticks}.
   */
  public abstract long elapsedRealtimeNanosForTicks(/*@NonNull*/ Ticks ticks);

  /**
   * Calculates an elapsed realtime clock value in milliseconds from the supplied {@link Ticks}, or
   * throws {@link UnsupportedOperationException} if the ticker implementation isn't able to convert
   * to elapsed realtime values.
   *
   * <p>See {@link #elapsedRealtimeNanosForTicks(Ticks)} for details.
   */
  public abstract long elapsedRealtimeMillisForTicks(/*@NonNull*/ Ticks ticks);
}
