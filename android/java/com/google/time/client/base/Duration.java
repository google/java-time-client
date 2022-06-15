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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * A stand-in for java.time.Duration, which cannot be used because it is not available on all
 * platforms supported by java-time-client.
 *
 * <p>This is the Android-specific version which provides a basic implementation of methods.
 */
public final class Duration implements Comparable<Duration> {

  private static final int NANO_DECIMAL_DIGITS = 9;
  private static final BigInteger BIGINT_NANOS_PER_SECOND = BigInteger.valueOf(NANOS_PER_SECOND);

  public static final Duration ZERO = Duration.ofSeconds(0, 0);

  private final long seconds;
  private final int nanosOfSecond;

  public static Duration ofNanos(long nanos) {
    long seconds = nanos / NANOS_PER_SECOND;
    long nanosAdjustment = nanos % NANOS_PER_SECOND;
    return ofSeconds(seconds, nanosAdjustment);
  }

  public static Duration ofMillis(long millis) {
    long seconds = millis / MILLISECONDS_PER_SECOND;
    long nanosAdjustment = (millis % MILLISECONDS_PER_SECOND) * NANOS_PER_MILLISECOND;
    return ofSeconds(seconds, nanosAdjustment);
  }

  public static Duration ofSeconds(long seconds, long nanoAdjustment) {
    long durationSeconds = ExactMath.addExact(seconds, nanoAdjustment / NANOS_PER_SECOND);
    int durationNanosOfSecond = (int) (nanoAdjustment % NANOS_PER_SECOND);
    if (durationNanosOfSecond < 0) {
      durationSeconds = ExactMath.subtractExact(durationSeconds, 1);
      durationNanosOfSecond += NANOS_PER_SECOND;
    }
    return new Duration(durationSeconds, durationNanosOfSecond);
  }

  public static Duration between(Instant t1, Instant t2) {
    // Checked arithmetic is not required because values are guaranteed within range due to
    // Instant.MIN and Instant.MAX.
    long t1Seconds = t1.getEpochSecond();
    long t2Seconds = t2.getEpochSecond();
    long seconds = t2Seconds - t1Seconds;

    int t1NanosOfSecond = t1.getNano();
    int t2NanosOfSecond = t2.getNano();
    int nanosOfSecond = t2NanosOfSecond - t1NanosOfSecond;
    if (nanosOfSecond < 0) {
      nanosOfSecond += NANOS_PER_SECOND;
      seconds--;
    }

    return new Duration(seconds, nanosOfSecond);
  }

  private Duration(long seconds, int nanosOfSecond) {
    this.seconds = seconds;
    this.nanosOfSecond = checkPositiveSubSecondNanos(nanosOfSecond);
  }

  public Duration minus(Duration toSubtract) {
    long seconds = ExactMath.subtractExact(this.seconds, toSubtract.seconds);
    int nanosOfSecond = this.nanosOfSecond - toSubtract.nanosOfSecond;
    if (nanosOfSecond < 0) {
      nanosOfSecond += NANOS_PER_SECOND;
      seconds = ExactMath.subtractExact(seconds, 1);
    }
    return new Duration(seconds, nanosOfSecond);
  }

  public Duration plus(Duration toAdd) {
    long seconds = ExactMath.addExact(this.seconds, toAdd.seconds);
    int nanosOfSecond = this.nanosOfSecond + toAdd.nanosOfSecond;
    if (nanosOfSecond >= NANOS_PER_SECOND) {
      nanosOfSecond -= NANOS_PER_SECOND;
      seconds = ExactMath.addExact(seconds, 1);
    }
    return new Duration(seconds, nanosOfSecond);
  }

  public Duration dividedBy(long divisor) {
    BigDecimal seconds =
        BigDecimal.valueOf(this.seconds)
            .add(BigDecimal.valueOf(nanosOfSecond, NANO_DECIMAL_DIGITS));
    seconds = seconds.divide(BigDecimal.valueOf(divisor), RoundingMode.DOWN);
    return ofBigDecimal(seconds);
  }

  private static Duration ofBigDecimal(BigDecimal secondsAsDecimal) {
    BigInteger nanos = secondsAsDecimal.movePointRight(NANO_DECIMAL_DIGITS).toBigIntegerExact();
    BigInteger[] divRem = nanos.divideAndRemainder(BIGINT_NANOS_PER_SECOND);
    BigInteger secondsAsInteger = divRem[0];
    if (secondsAsInteger.bitLength() > Long.SIZE - 1) {
      throw new ArithmeticException("Exceeds capacity of Duration: " + nanos);
    }
    BigInteger nanosAsInteger = divRem[1];
    return ofSeconds(secondsAsInteger.longValue(), nanosAsInteger.intValue());
  }

  public long toMillis() {
    // This appears to match OpenJDK's Duration.toMillis() behavior before version 9. Also Android's
    // own version, and threetenbp at the time of writing.
    long secondsMillis = ExactMath.multiplyExact(seconds, MILLISECONDS_PER_SECOND);
    return ExactMath.addExact(secondsMillis, this.nanosOfSecond / NANOS_PER_MILLISECOND);
  }

  public long toNanos() {
    // This appears to match OpenJDK's Duration.toNanos() behavior before version 9. Also Android's
    // own version, and threetenbp at the time of writing.
    long secondsNanos = ExactMath.multiplyExact(seconds, NANOS_PER_SECOND);
    return ExactMath.addExact(secondsNanos, nanosOfSecond);
  }

  public long getSeconds() {
    return this.seconds;
  }

  public int getNano() {
    return nanosOfSecond;
  }

  @Override
  public int compareTo(Duration other) {
    if (this.seconds < other.seconds) {
      return -1;
    } else if (this.seconds > other.seconds) {
      return 1;
    } else {
      return Long.compare(this.nanosOfSecond, other.nanosOfSecond);
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
    Duration duration = (Duration) o;
    return this.seconds == duration.seconds && this.nanosOfSecond == duration.nanosOfSecond;
  }

  @Override
  public int hashCode() {
    return Objects.hash(seconds, nanosOfSecond);
  }

  @Override
  public String toString() {
    // TODO ISO format would be nicer.
    return "Duration{seconds=" + seconds + ", nanosOfSecond=" + nanosOfSecond + "}";
  }

  /**
   * Converts a {@link java.time.Duration} to a {@link Duration}.
   *
   * <p>Interoperability with {@code java.time} classes for platforms that support it.
   */
  @SuppressWarnings("AndroidJdkLibsChecker")
  @RequiresApi(Build.VERSION_CODES.O)
  public static Duration ofJavaTime(java.time.Duration duration) {
    return Duration.ofNanos(duration.toNanos());
  }

  /**
   * Converts a {@link Duration} to a {@link java.time.Duration}.
   *
   * <p>Interoperability with {@code java.time} classes for platforms that support it.
   */
  @SuppressWarnings("AndroidJdkLibsChecker")
  @RequiresApi(Build.VERSION_CODES.O)
  public java.time.Duration toJavaTime() {
    return java.time.Duration.ofNanos(toNanos());
  }
}
