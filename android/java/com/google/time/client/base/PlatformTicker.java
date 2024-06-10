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

import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_MILLISECOND;

import android.os.SystemClock;
import com.google.time.client.base.impl.ExactMath;

/**
 * A {@link Ticker} that provides nanosecond-precision access to the Android kernel's CLOCK_BOOTTIME
 * clock.
 */
public final class PlatformTicker extends AndroidTicker {

  private static final PlatformTicker INSTANCE = new PlatformTicker();

  public static AndroidTicker instance() {
    return INSTANCE;
  }

  private PlatformTicker() {}

  @Override
  public Ticks ticks() {
    return createTicks(SystemClock.elapsedRealtimeNanos());
  }

  @Override
  public Ticks ticksForElapsedRealtimeNanos(long elapsedRealtimeNanos) {
    // This ticker uses the SystemClock.elapsedRealtimeNanos() value with no adjustments.
    return createTicks(elapsedRealtimeNanos);
  }

  @Override
  public Ticks ticksForElapsedRealtimeMillis(long elapsedRealtimeMillis) {
    long nanosValue = ExactMath.multiplyExact(elapsedRealtimeMillis, NANOS_PER_MILLISECOND);
    return createTicks(nanosValue);
  }

  @Override
  public long elapsedRealtimeNanosForTicks(Ticks ticks) {
    checkThisIsTicksOrigin(ticks);
    return ticks.getValue();
  }

  @Override
  public long elapsedRealtimeMillisForTicks(Ticks ticks) {
    checkThisIsTicksOrigin(ticks);
    return ticks.getValue() / NANOS_PER_MILLISECOND;
  }

  @Override
  public Duration durationBetween(Ticks start, Ticks end) {
    return Duration.ofNanos(incrementsBetween(start, end));
  }

  @Override
  public String toString() {
    return "PlatformTicker";
  }
}
