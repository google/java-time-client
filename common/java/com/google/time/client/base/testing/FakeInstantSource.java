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

import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_MILLISECOND;
import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_SECOND;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.impl.Objects;

/**
 * A fake {@link InstantSource} that can be used for tests. See {@link FakeClocks} for how to obtain
 * an instance.
 *
 * <p>By default, this instant source simulates one that returns instants with millisecond
 * precision, but this can be changed.
 */
public final class FakeInstantSource extends InstantSource implements Advanceable {

  private Instant instantSourceNow = Instant.ofEpochMilli(0);
  private int precision = InstantSource.PRECISION_MILLIS;

  private final FakeClocks fakeClocks;

  FakeInstantSource(FakeClocks fakeClocks) {
    this.fakeClocks = Objects.requireNonNull(fakeClocks);
  }

  @Override
  public int getPrecision() {
    return precision;
  }

  @Override
  public Instant instant() {
    instantSourceNow = instantSourceNow.plus(fakeClocks.autoAdvanceDuration);
    if (precision == PRECISION_MILLIS) {
      return Instant.ofEpochMilli(instantSourceNow.toEpochMilli());
    } else if (precision == PRECISION_NANOS) {
      return instantSourceNow;
    }
    throw new IllegalStateException("Unknown resolution=" + precision);
  }

  /** Advance this instant source by the specified number of milliseconds. */
  public void advanceMillis(long millis) {
    advance(Duration.ofNanos(millis * NANOS_PER_MILLISECOND));
  }

  /** Advance this instant source by the specified duration. */
  @Override
  public void advance(Duration duration) {
    instantSourceNow = instantSourceNow.plus(duration);
  }

  public void setEpochMillis(long epochMillis) {
    instantSourceNow = Instant.ofEpochMilli(epochMillis);
  }

  public void setInstant(Instant instant) {
    instantSourceNow = instant;
  }

  /** Returns the current instant. Does not auto advance. */
  public Instant getCurrentInstant() {
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
        + ", fakeClocks.autoAdvanceDuration="
        + fakeClocks.autoAdvanceDuration
        + '}';
  }
}
