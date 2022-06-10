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

import com.google.time.client.base.impl.Objects;

/**
 * A stand-in for java.time.Instant, which is not available on all platform versions supported by
 * java-time-client.
 *
 * <p>This is the Java SE-specific version which delegates calls to the Java SE java.time equivalent
 * class. Using java.time rather than using the Android variant everywhere serves one main purpose:
 * The test is common, so passing tests on both platforms provides confidence that the Android
 * variant's behavior is going to be close enough to what developers expect.
 */
public final class Instant implements Comparable<Instant> {

  public static final Instant MAX = new Instant(java.time.Instant.MAX);
  public static final Instant MIN = new Instant(java.time.Instant.MIN);

  public static Instant ofEpochSecond(long epochSeconds, long nanoAdjustment) {
    try {
      return new Instant(java.time.Instant.ofEpochSecond(epochSeconds, nanoAdjustment));
    } catch (java.time.DateTimeException e) {
      throw new DateTimeException(e);
    }
  }

  public static Instant ofEpochMilli(long epochMillis) {
    try {
      return new Instant(java.time.Instant.ofEpochMilli(epochMillis));
    } catch (java.time.DateTimeException e) {
      throw new DateTimeException(e);
    }
  }

  final java.time.Instant delegate;

  private Instant(java.time.Instant delegate) {
    this.delegate = Objects.requireNonNull(delegate);
  }

  public Instant plus(Duration adjustment) {
    try {
      return new Instant(this.delegate.plus(adjustment.delegate));
    } catch (java.time.DateTimeException e) {
      throw new DateTimeException(e);
    }
  }

  public long getEpochSecond() {
    return delegate.getEpochSecond();
  }

  public long toEpochMilli() {
    return delegate.toEpochMilli();
  }

  @SuppressWarnings("JavaInstantGetSecondsGetNano")
  public int getNano() {
    return delegate.getNano();
  }

  @Override
  public int compareTo(Instant instant) {
    return delegate.compareTo(instant.delegate);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Instant instant = (Instant) o;
    return delegate.equals(instant.delegate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
