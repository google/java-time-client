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

import static com.google.time.client.base.AndroidDateTimeUtils.checkPositiveSubSecondNanos;
import static com.google.time.client.base.impl.DateTimeConstants.MILLISECONDS_PER_SECOND;
import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_MILLISECOND;
import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_SECOND;

import android.os.Build;
import androidx.annotation.RequiresApi;
import com.google.time.client.base.impl.ExactMath;
import com.google.time.client.base.impl.Objects;

/**
 * A stand-in for java.time.Instant, which cannot be used because it is not available on all
 * platforms supported by java-time-client.
 *
 * <p>This is the Android-specific version which provides a basic implementation of methods.
 */
public final class Instant implements Comparable<Instant> {

  private static final long MAX_EPOCH_SECONDS = 31556889864403199L;
  private static final long MIN_EPOCH_SECONDS = -31557014167219200L;

  public static final Instant MAX = Instant.ofEpochSecond(MAX_EPOCH_SECONDS, NANOS_PER_SECOND - 1);
  public static final Instant MIN = Instant.ofEpochSecond(MIN_EPOCH_SECONDS, 0);

  private final long seconds;
  private final int nanosOfSecond;

  public static Instant ofEpochSecond(long epochSeconds, long nanoAdjustment) {
    long instantEpochSeconds = ExactMath.addExact(epochSeconds, nanoAdjustment / NANOS_PER_SECOND);
    int instantNanosOfSecond = (int) (nanoAdjustment % NANOS_PER_SECOND);
    if (instantNanosOfSecond < 0) {
      instantEpochSeconds = ExactMath.subtractExact(instantEpochSeconds, 1);
      instantNanosOfSecond = NANOS_PER_SECOND + instantNanosOfSecond;
    }
    return new Instant(instantEpochSeconds, instantNanosOfSecond);
  }

  public static Instant ofEpochMilli(long epochMillis) {
    long seconds = epochMillis / MILLISECONDS_PER_SECOND;
    int nanosOfSeconds = (int) ((epochMillis % MILLISECONDS_PER_SECOND) * NANOS_PER_MILLISECOND);
    if (nanosOfSeconds < 0) {
      seconds--;
      nanosOfSeconds = NANOS_PER_SECOND + nanosOfSeconds;
    }
    return new Instant(seconds, nanosOfSeconds);
  }

  private Instant(long seconds, int nanosOfSecond) {
    this.seconds = checkSecondsInAllowedRange(seconds);
    this.nanosOfSecond = checkPositiveSubSecondNanos(nanosOfSecond);
  }

  public Instant plus(Duration adjustment) {
    long seconds = ExactMath.addExact(this.seconds, adjustment.getSeconds());
    checkSecondsInAllowedRange(seconds);

    int nanosOfSecond = this.nanosOfSecond + adjustment.getNano();
    if (nanosOfSecond >= NANOS_PER_SECOND) {
      nanosOfSecond -= NANOS_PER_SECOND;
      seconds = ExactMath.addExact(seconds, 1);
    }
    return new Instant(seconds, nanosOfSecond);
  }

  public long getEpochSecond() {
    return seconds;
  }

  public int getNano() {
    return nanosOfSecond;
  }

  public long toEpochMilli() {
    long seconds = this.seconds;
    int millisAdjustmentForNanos = nanosOfSecond / NANOS_PER_MILLISECOND;

    // Deal with the special case of negatives close to Long.MIN_VALUE to avoid an arithmetic
    // exception. Example: Instant.ofEpochMillis(Long.MIN_VALUE).toEpochMillis() should not throw.
    if (seconds < 0 && millisAdjustmentForNanos > 0) {
      millisAdjustmentForNanos -= MILLISECONDS_PER_SECOND;
      seconds++;
    }
    return ExactMath.multiplyExact(seconds, MILLISECONDS_PER_SECOND) + millisAdjustmentForNanos;
  }

  @Override
  public int compareTo(Instant instant) {
    if (this.seconds < instant.seconds) {
      return -1;
    } else if (this.seconds > instant.seconds) {
      return 1;
    } else {
      return Long.compare(this.nanosOfSecond, instant.nanosOfSecond);
    }
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
    return this.seconds == instant.seconds && this.nanosOfSecond == instant.nanosOfSecond;
  }

  @Override
  public int hashCode() {
    return Objects.hash(seconds, nanosOfSecond);
  }

  @Override
  public String toString() {
    // TODO ISO format would be nicer.
    return "Instant{seconds=" + seconds + ", nanosOfSecond=" + nanosOfSecond + "}";
  }

  private static long checkSecondsInAllowedRange(long seconds) {
    if (seconds < MIN_EPOCH_SECONDS || seconds > MAX_EPOCH_SECONDS) {
      throw new DateTimeException("seconds is out of allowed range");
    }
    return seconds;
  }

  /**
   * Converts an {@link java.time.Instant} to an {@link Instant}.
   *
   * <p>Interoperability with {@code java.time} classes for platforms that support it.
   */
  @RequiresApi(Build.VERSION_CODES.O)
  public static Instant ofJavaTime(java.time.Instant javaTimeInstant) {
    return Instant.ofEpochSecond(javaTimeInstant.getEpochSecond(), javaTimeInstant.getNano());
  }

  /**
   * Converts an {@link Instant} to an {@link java.time.Instant}.
   *
   * <p>Interoperability with {@code java.time} classes for platforms that support it.
   */
  @RequiresApi(Build.VERSION_CODES.O)
  public java.time.Instant toJavaTime() {
    return java.time.Instant.ofEpochSecond(getEpochSecond(), getNano());
  }
}
