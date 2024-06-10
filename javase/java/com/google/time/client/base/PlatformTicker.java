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
    return createTicks(System.nanoTime());
  }

  @Override
  /*@NonNull*/ public Duration durationBetween(/*@NonNull*/ Ticks start, /*@NonNull*/ Ticks end) {
    return Duration.ofNanos(incrementsBetween(start, end));
  }

  @Override
  public String toString() {
    return "PlatformTicker";
  }
}
