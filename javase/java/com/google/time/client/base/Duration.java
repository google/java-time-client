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
 * A stand-in for java.time.Duration, which is not available on all platform versions supported by
 * java-time-client.
 *
 * <p>This is the Java SE-specific version which delegates calls to the Java SE java.time equivalent
 * class. Using java.time rather than using the Android variant everywhere serves one main purpose:
 * The test is common, so passing tests on both platforms provides confidence that the Android
 * variant's behavior is going to be close enough to what developers expect.
 */
public final class Duration implements Comparable<Duration> {

  public static final Duration ZERO = Duration.ofSeconds(0, 0);

  public static Duration ofNanos(long nanos) {
    return new Duration(java.time.Duration.ofNanos(nanos));
  }

  public static Duration ofSeconds(long seconds, long nanoAdjustment) {
    return new Duration(java.time.Duration.ofSeconds(seconds, nanoAdjustment));
  }

  public static Duration between(Instant t1, Instant t2) {
    try {
      return new Duration(java.time.Duration.between(t1.delegate, t2.delegate));
    } catch (java.time.DateTimeException e) {
      throw new DateTimeException(e);
    }
  }

  final java.time.Duration delegate;

  private Duration(java.time.Duration delegate) {
    this.delegate = Objects.requireNonNull(delegate);
  }

  public Duration minus(Duration toSubtract) {
    return new Duration(delegate.minus(toSubtract.delegate));
  }

  public Duration plus(Duration toAdd) {
    return new Duration(delegate.plus(toAdd.delegate));
  }

  public Duration dividedBy(long divisor) {
    return new Duration(delegate.dividedBy(divisor));
  }

  public long toMillis() {
    return delegate.toMillis();
  }

  public long toNanos() {
    return delegate.toNanos();
  }

  public long getSeconds() {
    return delegate.getSeconds();
  }

  public int getNano() {
    return delegate.getNano();
  }

  @Override
  public int compareTo(Duration other) {
    return this.delegate.compareTo(other.delegate);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Duration duration = (Duration) o;
    return delegate.equals(duration.delegate);
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
