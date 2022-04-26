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

import static com.google.common.truth.Truth.assertThat;
import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_MILLISECOND;
import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_SECOND;
import static com.google.time.client.base.testing.TestEnvironmentUtils.getJavaVersion;
import static com.google.time.client.base.testing.TestEnvironmentUtils.isThisAndroid;
import static com.google.time.client.base.testing.TestEnvironmentUtils.isThisJavaSe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class DurationTest {

  @Test
  public void zero() {
    Duration constantZero = Duration.ZERO;
    assertEquals(0, constantZero.getSeconds());
    assertEquals(0, constantZero.getNano());
    assertThrows(ArithmeticException.class, () -> constantZero.dividedBy(0));
    assertEquals(constantZero, constantZero.plus(constantZero));
    assertEquals(constantZero, constantZero.minus(constantZero));
    assertEquals(0, constantZero.toMillis());
    assertNotNull(constantZero.toString());

    Duration otherZero = Duration.ofNanos(0);
    assertEqualityMethods(constantZero, otherZero);
  }

  @Test
  public void allowedRange() {
    Duration maxDuration = Duration.ofSeconds(Long.MAX_VALUE, NANOS_PER_SECOND - 1);
    assertThrows(
        ArithmeticException.class, () -> Duration.ofSeconds(Long.MAX_VALUE, NANOS_PER_SECOND));

    Duration minDuration = Duration.ofSeconds(Long.MIN_VALUE, 0);
    assertThrows(ArithmeticException.class, () -> Duration.ofSeconds(Long.MIN_VALUE, -1));
  }

  @Test
  public void positiveOneSecond() {
    Duration posOneSecond = Duration.ofSeconds(1, 0);
    assertEquals(1, posOneSecond.getSeconds());
    assertEquals(0, posOneSecond.getNano());
    assertThrows(ArithmeticException.class, () -> posOneSecond.dividedBy(0));
    assertEquals(posOneSecond, posOneSecond.plus(Duration.ZERO));
    assertEquals(Duration.ofSeconds(2, 0), posOneSecond.plus(posOneSecond));
    assertEquals(posOneSecond, posOneSecond.minus(Duration.ZERO));
    assertEquals(Duration.ZERO, posOneSecond.minus(posOneSecond));
    assertEquals(1000, posOneSecond.toMillis());
    assertNotNull(posOneSecond.toString());

    Duration otherPosOneSecond = Duration.ofNanos(1000000000);
    assertEqualityMethods(posOneSecond, otherPosOneSecond);
  }

  @Test
  public void negativeOneSecond() {
    Duration negOneSecond = Duration.ofSeconds(-1, 0);
    assertEquals(-1, negOneSecond.getSeconds());
    assertEquals(0, negOneSecond.getNano());
    assertThrows(ArithmeticException.class, () -> negOneSecond.dividedBy(0));
    assertEquals(negOneSecond, negOneSecond.plus(Duration.ZERO));
    assertEquals(Duration.ofSeconds(-2, 0), negOneSecond.plus(negOneSecond));
    assertEquals(negOneSecond, negOneSecond.minus(Duration.ZERO));
    assertEquals(Duration.ZERO, negOneSecond.minus(negOneSecond));
    assertEquals(-1000, negOneSecond.toMillis());
    assertNotNull(negOneSecond.toString());

    Duration otherNegOneSecond = Duration.ofNanos(-1000000000);
    assertEqualityMethods(negOneSecond, otherNegOneSecond);
  }

  @Test
  public void positiveOneNano() {
    Duration posOneNano = Duration.ofSeconds(0, 1);
    assertEquals(0, posOneNano.getSeconds());
    assertEquals(1, posOneNano.getNano());
    assertThrows(ArithmeticException.class, () -> posOneNano.dividedBy(0));
    assertEquals(posOneNano, posOneNano.plus(Duration.ZERO));
    assertEquals(Duration.ofSeconds(0, 2), posOneNano.plus(posOneNano));
    assertEquals(posOneNano, posOneNano.minus(Duration.ZERO));
    assertEquals(Duration.ZERO, posOneNano.minus(posOneNano));
    assertEquals(0, posOneNano.toMillis());
    assertNotNull(posOneNano.toString());

    Duration otherPosOneNano = Duration.ofNanos(1);
    assertEqualityMethods(posOneNano, otherPosOneNano);
  }

  @Test
  public void negativeOneNano() {
    Duration negOneNano = Duration.ofSeconds(0, -1);
    assertEquals(-1, negOneNano.getSeconds());
    assertEquals(999999999L, negOneNano.getNano());
    assertThrows(ArithmeticException.class, () -> negOneNano.dividedBy(0));
    assertEquals(negOneNano, negOneNano.plus(Duration.ZERO));
    assertEquals(Duration.ofSeconds(0, -2), negOneNano.plus(negOneNano));
    assertEquals(negOneNano, negOneNano.minus(Duration.ZERO));
    assertEquals(Duration.ZERO, negOneNano.minus(negOneNano));

    if (isThisAndroid() || (isThisJavaSe() && getJavaVersion() < 9)) {
      assertEquals(-1, negOneNano.toMillis());
    } else {
      assertEquals(0, negOneNano.toMillis());
    }
    assertNotNull(negOneNano.toString());

    Duration otherNegOneNano = Duration.ofNanos(-1);
    assertEqualityMethods(negOneNano, otherNegOneNano);
  }

  @Test
  public void positive999999999Nano() {
    Duration pos999999999Nano = Duration.ofSeconds(0, 999999999);
    assertEquals(0, pos999999999Nano.getSeconds());
    assertEquals(999999999, pos999999999Nano.getNano());
    assertThrows(ArithmeticException.class, () -> pos999999999Nano.dividedBy(0));
    assertEquals(pos999999999Nano, pos999999999Nano.plus(Duration.ZERO));
    assertEquals(Duration.ofSeconds(1, 999999998), pos999999999Nano.plus(pos999999999Nano));
    assertEquals(pos999999999Nano, pos999999999Nano.minus(Duration.ZERO));
    assertEquals(Duration.ZERO, pos999999999Nano.minus(pos999999999Nano));
    assertEquals(999, pos999999999Nano.toMillis());
    assertNotNull(pos999999999Nano.toString());

    Duration otherPos999999999Nano = Duration.ofNanos(999999999);
    assertEqualityMethods(pos999999999Nano, otherPos999999999Nano);
  }

  @Test
  public void negative999999999Nano() {
    Duration neg999999999Nano = Duration.ofSeconds(0, -999999999);
    assertEquals(-1, neg999999999Nano.getSeconds());
    assertEquals(1, neg999999999Nano.getNano());
    assertThrows(ArithmeticException.class, () -> neg999999999Nano.dividedBy(0));
    assertEquals(neg999999999Nano, neg999999999Nano.plus(Duration.ZERO));
    assertEquals(Duration.ofSeconds(-2, 2), neg999999999Nano.plus(neg999999999Nano));
    assertEquals(neg999999999Nano, neg999999999Nano.minus(Duration.ZERO));
    assertEquals(Duration.ZERO, neg999999999Nano.minus(neg999999999Nano));

    if (isThisAndroid() || (isThisJavaSe() && getJavaVersion() < 9)) {
      assertEquals(-1000, neg999999999Nano.toMillis());
    } else {
      assertEquals(-999L, neg999999999Nano.toMillis());
    }
    assertNotNull(neg999999999Nano.toString());

    Duration otherNeg999999999Nano = Duration.ofNanos(-999999999);
    assertEqualityMethods(neg999999999Nano, otherNeg999999999Nano);
  }

  // Covers ofNanos(), getSeconds(), getNanos(), toMillis(), toNanos().
  @Test
  public void ofNanos() {
    {
      Duration duration = Duration.ofNanos(0);
      assertEquals(0, duration.getSeconds());
      assertEquals(0, duration.getNano());
      assertEquals(0, duration.toMillis());
      assertEquals(0, duration.toNanos());
    }

    {
      Duration duration = Duration.ofNanos(1);
      assertEquals(0, duration.getSeconds());
      assertEquals(1, duration.getNano());
      assertEquals(0, duration.toMillis());
      assertEquals(1, duration.toNanos());
    }

    {
      Duration duration = Duration.ofNanos(-1);
      assertEquals(-1, duration.getSeconds());
      assertEquals(999999999, duration.getNano());
      if (isThisAndroid() || (isThisJavaSe() && getJavaVersion() < 9)) {
        assertEquals(-1, duration.toMillis());
      } else {
        assertEquals(0, duration.toMillis());
      }
      assertEquals(-1L, duration.toNanos());
    }

    {
      int nanos = -(NANOS_PER_MILLISECOND - 1);
      Duration duration = Duration.ofNanos(nanos);
      // -1 on OpenJDK 8, 0 on OpenJDK 9+, Android variant returns -1.
      assertThat(duration.toMillis()).isAnyOf(0L, -1L);
      assertEquals(-1, duration.getSeconds());
      assertEquals(NANOS_PER_SECOND + nanos, duration.getNano());
      if (isThisAndroid() || (isThisJavaSe() && getJavaVersion() < 9)) {
        assertEquals(-1, duration.toMillis());
      } else {
        assertEquals(0, duration.toMillis());
      }
      assertEquals(nanos, duration.toNanos());
    }

    {
      int nanos = NANOS_PER_MILLISECOND - 1;
      Duration duration = Duration.ofNanos(nanos);
      assertEquals(0, duration.toMillis());
      assertEquals(0, duration.getSeconds());
      assertEquals(nanos, duration.getNano());
      assertEquals(0, duration.toMillis());
      assertEquals(nanos, duration.toNanos());
    }

    {
      long nanos = Long.MIN_VALUE;
      Duration duration = Duration.ofNanos(nanos);
      assertEquals((nanos / NANOS_PER_SECOND) - 1, duration.getSeconds());
      assertEquals(NANOS_PER_SECOND + (nanos % NANOS_PER_SECOND), duration.getNano());
      if (isThisAndroid() || (isThisJavaSe() && getJavaVersion() < 9)) {
        assertEquals(nanos / NANOS_PER_MILLISECOND - 1, duration.toMillis());
      } else {
        assertEquals(nanos / NANOS_PER_MILLISECOND, duration.toMillis());
      }
      if (isThisJavaSe() && getJavaVersion() < 9) {
        assertThrows(ArithmeticException.class, duration::toNanos);
      } else {
        assertEquals(nanos, duration.toNanos());
      }
    }

    {
      long nanos = Long.MAX_VALUE;
      Duration duration = Duration.ofNanos(nanos);
      assertEquals(nanos / NANOS_PER_SECOND, duration.getSeconds());
      assertEquals(nanos % NANOS_PER_SECOND, duration.getNano());
      assertEquals(nanos / NANOS_PER_MILLISECOND, duration.toMillis());
      assertEquals(nanos, duration.toNanos());
    }
  }

  @Test
  public void between() {
    Instant zero = Instant.ofEpochMilli(0);
    assertEquals(Duration.ZERO, Duration.between(zero, zero));

    Instant pos1 = Instant.ofEpochMilli(1);
    Instant neg1 = Instant.ofEpochMilli(-1);
    assertEquals(Duration.ZERO, Duration.between(pos1, pos1));
    assertEquals(Duration.ZERO, Duration.between(neg1, neg1));
    assertEquals(Duration.ofSeconds(0, 1000000), Duration.between(zero, pos1));
    assertEquals(Duration.ofSeconds(-1, 999000000), Duration.between(pos1, zero));
    assertEquals(Duration.ofSeconds(-1, 998000000), Duration.between(pos1, neg1));
    assertEquals(Duration.ofSeconds(0, 2000000), Duration.between(neg1, pos1));
    assertEquals(Duration.ofSeconds(0, 1000000), Duration.between(neg1, zero));
    assertEquals(Duration.ofSeconds(-1, 999000000), Duration.between(zero, neg1));

    Instant pos2 = Instant.ofEpochSecond(0, 1);
    Instant neg2 = Instant.ofEpochSecond(0, -1);
    assertEquals(Duration.ZERO, Duration.between(pos2, pos2));
    assertEquals(Duration.ZERO, Duration.between(neg2, neg2));
    assertEquals(Duration.ofSeconds(0, 1), Duration.between(zero, pos2));
    assertEquals(Duration.ofSeconds(-1, 999999999), Duration.between(pos2, zero));
    assertEquals(Duration.ofSeconds(-1, 999999998), Duration.between(pos2, neg2));
    assertEquals(Duration.ofSeconds(0, 2), Duration.between(neg2, pos2));
    assertEquals(Duration.ofSeconds(0, 1), Duration.between(neg2, zero));
    assertEquals(Duration.ofSeconds(-1, 999999999), Duration.between(zero, neg2));

    Instant pos3 = Instant.ofEpochSecond(1, 0);
    Instant neg3 = Instant.ofEpochSecond(-1, 0);
    assertEquals(Duration.ZERO, Duration.between(pos3, pos3));
    assertEquals(Duration.ZERO, Duration.between(neg3, neg3));
    assertEquals(Duration.ofSeconds(1, 0), Duration.between(zero, pos3));
    assertEquals(Duration.ofSeconds(-1, 0), Duration.between(pos3, zero));
    assertEquals(Duration.ofSeconds(-2, 0), Duration.between(pos3, neg3));
    assertEquals(Duration.ofSeconds(2, 0), Duration.between(neg3, pos3));
    assertEquals(Duration.ofSeconds(1, 0), Duration.between(neg3, zero));
    assertEquals(Duration.ofSeconds(-1, 0), Duration.between(zero, neg3));

    Instant max = Instant.MAX;
    Instant min = Instant.MIN;
    assertEquals(Duration.ZERO, Duration.between(max, max));
    assertEquals(Duration.ZERO, Duration.between(min, min));
    assertEquals(
        Duration.ofSeconds(max.getEpochSecond(), max.getNano()), Duration.between(zero, max));
    assertEquals(Duration.ofSeconds(-max.getEpochSecond() - 1, 1), Duration.between(max, zero));
    assertEquals(
        Duration.ofSeconds(-max.getEpochSecond() + min.getEpochSecond() - 1, 1),
        Duration.between(max, min));
    assertEquals(
        Duration.ofSeconds(max.getEpochSecond() - min.getEpochSecond(), 999999999),
        Duration.between(min, max));
    assertEquals(Duration.ofSeconds(-min.getEpochSecond(), 0), Duration.between(min, zero));
    assertEquals(Duration.ofSeconds(min.getEpochSecond(), 0), Duration.between(zero, min));
  }

  @Test
  public void minus() {
    Duration zero = Duration.ZERO;
    assertEquals(zero, zero.minus(zero));

    Duration pos1 = Duration.ofSeconds(0, 1);
    Duration neg1 = Duration.ofSeconds(-1, NANOS_PER_SECOND - 1);
    assertEquals(zero, pos1.minus(pos1));
    assertEquals(zero, neg1.minus(neg1));
    assertEquals(pos1, pos1.minus(zero));
    assertEquals(neg1, zero.minus(pos1));
    assertEquals(Duration.ofSeconds(-1, 999999998), neg1.minus(pos1));
    assertEquals(Duration.ofSeconds(0, 2), pos1.minus(neg1));
    assertEquals(pos1, zero.minus(neg1));
    assertEquals(neg1, neg1.minus(zero));

    Duration pos2 = Duration.ofSeconds(1, 0);
    Duration neg2 = Duration.ofSeconds(-1, 0);
    assertEquals(Duration.ZERO, pos2.minus(pos2));
    assertEquals(Duration.ZERO, neg2.minus(neg2));
    assertEquals(pos2, pos2.minus(zero));
    assertEquals(neg2, zero.minus(pos2));
    assertEquals(Duration.ofSeconds(-2, 0), neg2.minus(pos2));
    assertEquals(Duration.ofSeconds(2, 0), pos2.minus(neg2));
    assertEquals(pos2, zero.minus(neg2));
    assertEquals(neg2, neg2.minus(zero));

    Duration max = Duration.ofSeconds(Long.MAX_VALUE, NANOS_PER_SECOND - 1);
    Duration min = Duration.ofSeconds(Long.MIN_VALUE, 0);
    assertEquals(Duration.ZERO, max.minus(max));
    assertEquals(Duration.ZERO, min.minus(min));
    assertEquals(max, max.minus(zero));
    assertEquals(Duration.ofSeconds(-max.getSeconds() - 1, 1), zero.minus(max));
    assertThrows(ArithmeticException.class, () -> min.minus(max));
    assertThrows(ArithmeticException.class, () -> max.minus(min));
    assertThrows(ArithmeticException.class, () -> zero.minus(min));
    assertEquals(min, min.minus(zero));
  }

  @Test
  public void plus() {
    Duration zero = Duration.ZERO;
    assertEquals(zero, zero.plus(zero));

    Duration pos1 = Duration.ofSeconds(0, 1);
    Duration neg1 = Duration.ofSeconds(-1, NANOS_PER_SECOND - 1);
    assertEquals(Duration.ofSeconds(0, 2), pos1.plus(pos1));
    assertEquals(Duration.ofSeconds(-1, 999999998), neg1.plus(neg1));
    assertEquals(pos1, pos1.plus(zero));
    assertEquals(pos1, zero.plus(pos1));
    assertEquals(zero, neg1.plus(pos1));
    assertEquals(zero, pos1.plus(neg1));
    assertEquals(neg1, zero.plus(neg1));
    assertEquals(neg1, neg1.plus(zero));

    Duration pos2 = Duration.ofSeconds(1, 0);
    Duration neg2 = Duration.ofSeconds(-1, 0);
    assertEquals(Duration.ofSeconds(2, 0), pos2.plus(pos2));
    assertEquals(Duration.ofSeconds(-2, 0), neg2.plus(neg2));
    assertEquals(pos2, pos2.plus(zero));
    assertEquals(pos2, zero.plus(pos2));
    assertEquals(zero, neg2.plus(pos2));
    assertEquals(zero, pos2.plus(neg2));
    assertEquals(neg2, zero.plus(neg2));
    assertEquals(neg2, neg2.plus(zero));

    Duration max = Duration.ofSeconds(Long.MAX_VALUE, NANOS_PER_SECOND - 1);
    Duration min = Duration.ofSeconds(Long.MIN_VALUE, 0);
    assertThrows(ArithmeticException.class, () -> max.plus(max));
    assertThrows(ArithmeticException.class, () -> min.plus(min));
    assertEquals(max, max.plus(zero));
    assertEquals(max, zero.plus(max));
    assertEquals(Duration.ofSeconds(-1, 999999999), min.plus(max));
    assertEquals(Duration.ofSeconds(-1, 999999999), max.plus(min));
    assertEquals(min, zero.plus(min));
    assertEquals(min, min.plus(zero));
  }

  @Test
  public void dividedBy() {
    {
      Duration zero = Duration.ZERO;
      assertThrows(ArithmeticException.class, () -> zero.dividedBy(0));
      assertEquals(zero, zero.dividedBy(1));
      assertEquals(zero, zero.dividedBy(-1));
      assertEquals(zero, zero.dividedBy(2));
      assertEquals(zero, zero.dividedBy(-2));
      assertEquals(zero, zero.dividedBy(Long.MAX_VALUE));
      assertEquals(zero, zero.dividedBy(Long.MIN_VALUE));
    }

    {
      Duration duration = Duration.ofSeconds(1, 0);
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
      assertEquals(duration, duration.dividedBy(1));
      assertEquals(Duration.ofSeconds(-1, 0), duration.dividedBy(-1));
      assertEquals(Duration.ofSeconds(0, 500000000), duration.dividedBy(2));
      assertEquals(Duration.ofSeconds(-1, 500000000), duration.dividedBy(-2));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MAX_VALUE));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MIN_VALUE));
    }

    {
      Duration duration = Duration.ofSeconds(-1, 0);
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
      assertEquals(duration, duration.dividedBy(1));
      assertEquals(Duration.ofSeconds(1, 0), duration.dividedBy(-1));
      assertEquals(Duration.ofSeconds(-1, 500000000), duration.dividedBy(2));
      assertEquals(Duration.ofSeconds(0, 500000000), duration.dividedBy(-2));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MAX_VALUE));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MIN_VALUE));
    }

    {
      Duration duration = Duration.ofSeconds(2, 0);
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
      assertEquals(duration, duration.dividedBy(1));
      assertEquals(Duration.ofSeconds(-2, 0), duration.dividedBy(-1));
      assertEquals(Duration.ofSeconds(1, 0), duration.dividedBy(2));
      assertEquals(Duration.ofSeconds(-1, 0), duration.dividedBy(-2));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MAX_VALUE));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MIN_VALUE));
    }

    {
      Duration duration = Duration.ofSeconds(-2, 0);
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
      assertEquals(duration, duration.dividedBy(1));
      assertEquals(Duration.ofSeconds(2, 0), duration.dividedBy(-1));
      assertEquals(Duration.ofSeconds(-1, 0), duration.dividedBy(2));
      assertEquals(Duration.ofSeconds(1, 0), duration.dividedBy(-2));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MAX_VALUE));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MIN_VALUE));
    }

    {
      Duration duration = Duration.ofSeconds(0, 1);
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
      assertEquals(duration, duration.dividedBy(1));
      assertEquals(Duration.ofSeconds(-1, 999999999), duration.dividedBy(-1));
      assertEquals(Duration.ZERO, duration.dividedBy(2));
      assertEquals(Duration.ZERO, duration.dividedBy(-2));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MAX_VALUE));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MIN_VALUE));
    }

    {
      Duration duration = Duration.ofSeconds(-1, 999999999);
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
      assertEquals(duration, duration.dividedBy(1));
      assertEquals(Duration.ofSeconds(0, 1), duration.dividedBy(-1));
      assertEquals(Duration.ZERO, duration.dividedBy(2));
      assertEquals(Duration.ZERO, duration.dividedBy(-2));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MAX_VALUE));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MIN_VALUE));
    }

    {
      Duration duration = Duration.ofSeconds(0, 2);
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
      assertEquals(duration, duration.dividedBy(1));
      assertEquals(Duration.ofSeconds(-1, 999999998), duration.dividedBy(-1));
      assertEquals(Duration.ofSeconds(0, 1), duration.dividedBy(2));
      assertEquals(Duration.ofSeconds(-1, 999999999), duration.dividedBy(-2));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MAX_VALUE));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MIN_VALUE));
    }

    {
      Duration duration = Duration.ofSeconds(-1, 999999998);
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
      assertEquals(duration, duration.dividedBy(1));
      assertEquals(Duration.ofSeconds(0, 2), duration.dividedBy(-1));
      assertEquals(Duration.ofSeconds(-1, 999999999), duration.dividedBy(2));
      assertEquals(Duration.ofSeconds(0, 1), duration.dividedBy(-2));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MAX_VALUE));
      assertEquals(Duration.ZERO, duration.dividedBy(Long.MIN_VALUE));
    }

    {
      Duration duration = Duration.ofSeconds(Long.MAX_VALUE, 999999999);
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
      assertEquals(duration, duration.dividedBy(1));
      assertEquals(Duration.ofSeconds(Long.MIN_VALUE, 1), duration.dividedBy(-1));
      assertEquals(Duration.ofSeconds(Long.MAX_VALUE / 2, 999999999), duration.dividedBy(2));
      assertEquals(Duration.ofSeconds(Long.MIN_VALUE / 2, 1), duration.dividedBy(-2));
      assertEquals(Duration.ofSeconds(1, 0), duration.dividedBy(Long.MAX_VALUE - 1));
      assertEquals(Duration.ofSeconds(-1, 0), duration.dividedBy(Long.MIN_VALUE + 1));
      assertEquals(Duration.ofSeconds(1, 0), duration.dividedBy(Long.MAX_VALUE));
      assertEquals(Duration.ofSeconds(-1, 1), duration.dividedBy(Long.MIN_VALUE));
    }

    {
      Duration duration = Duration.ofSeconds(Long.MAX_VALUE, 2);
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
      assertEquals(duration, duration.dividedBy(1));
      assertEquals(Duration.ofSeconds(Long.MIN_VALUE, 999999998), duration.dividedBy(-1));
      assertEquals(Duration.ofSeconds(Long.MAX_VALUE / 2, 500000001), duration.dividedBy(2));
      assertEquals(Duration.ofSeconds(Long.MIN_VALUE / 2, 499999999), duration.dividedBy(-2));
      assertEquals(Duration.ofSeconds(1, 0), duration.dividedBy(Long.MAX_VALUE - 1));
      assertEquals(Duration.ofSeconds(-1, 0), duration.dividedBy(Long.MIN_VALUE + 1));
      assertEquals(Duration.ofSeconds(1, 0), duration.dividedBy(Long.MAX_VALUE));
      assertEquals(Duration.ofSeconds(-1, 1), duration.dividedBy(Long.MIN_VALUE));
    }

    {
      Duration duration = Duration.ofSeconds(Long.MIN_VALUE, 0);
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
      assertEquals(duration, duration.dividedBy(1));
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(-1));
      assertEquals(Duration.ofSeconds(Long.MIN_VALUE / 2, 0), duration.dividedBy(2));
      assertEquals(Duration.ofSeconds((Long.MAX_VALUE / 2) + 1, 0), duration.dividedBy(-2));
      assertEquals(Duration.ofSeconds(-1, 0), duration.dividedBy(Long.MAX_VALUE - 1));
      assertEquals(Duration.ofSeconds(1, 0), duration.dividedBy(Long.MIN_VALUE + 1));
      assertEquals(Duration.ofSeconds(-1, 0), duration.dividedBy(Long.MAX_VALUE));
      assertEquals(Duration.ofSeconds(1, 0), duration.dividedBy(Long.MIN_VALUE));
    }

    {
      Duration duration = Duration.ofSeconds(Long.MIN_VALUE, 2);
      assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
      assertEquals(duration, duration.dividedBy(1));
      assertEquals(Duration.ofSeconds(Long.MAX_VALUE, 999999998), duration.dividedBy(-1));
      assertEquals(Duration.ofSeconds(Long.MIN_VALUE / 2, 1), duration.dividedBy(2));
      assertEquals(Duration.ofSeconds((Long.MAX_VALUE / 2), 999999999), duration.dividedBy(-2));
      assertEquals(Duration.ofSeconds(-1, 0), duration.dividedBy(Long.MAX_VALUE - 1));
      assertEquals(Duration.ofSeconds(1, 0), duration.dividedBy(Long.MIN_VALUE + 1));
      assertEquals(Duration.ofSeconds(-1, 0), duration.dividedBy(Long.MAX_VALUE));
      assertEquals(Duration.ofSeconds(0, 999999999), duration.dividedBy(Long.MIN_VALUE));
    }
  }

  // Covers equals(Object), compareTo(Duration) & hashCode() (for equality only).
  @Test
  public void equals() {
    assertEqualityMethods(Duration.ofNanos(0), Duration.ZERO);
    doEqualityTest(1);
    doEqualityTest(-1);
    doEqualityTest(NANOS_PER_SECOND);
    doEqualityTest(-NANOS_PER_SECOND);
    doEqualityTest(NANOS_PER_SECOND + 1);
    doEqualityTest(-NANOS_PER_SECOND - 1);
    doEqualityTest(Long.MIN_VALUE);
    doEqualityTest(Long.MAX_VALUE);

    doEqualityTest(Long.MIN_VALUE, 0);
    doEqualityTest(Long.MAX_VALUE, 999999999);
    doEqualityTest(Long.MIN_VALUE, 1);
    doEqualityTest(Long.MAX_VALUE, 999999998);
    doEqualityTest(Long.MIN_VALUE, 999999999);
    doEqualityTest(Long.MAX_VALUE, 0);
  }

  private static void doEqualityTest(long nanos) {
    assertEqualityMethods(Duration.ofNanos(nanos), Duration.ofNanos(nanos));
  }

  private static void doEqualityTest(long seconds, int nanosOfSecond) {
    assertEqualityMethods(
        Duration.ofSeconds(seconds, nanosOfSecond), Duration.ofSeconds(seconds, nanosOfSecond));
  }

  // See also equals() test.
  @Test
  public void compareTo() {
    doSimpleComparisonTestNanos(0);
    doSimpleComparisonTestNanos(1);
    doSimpleComparisonTestNanos(-1);
    doSimpleComparisonTestNanos(NANOS_PER_SECOND);
    doSimpleComparisonTestNanos(-NANOS_PER_SECOND);
    doSimpleComparisonTestNanos(NANOS_PER_SECOND + 1);
    doSimpleComparisonTestNanos(-NANOS_PER_SECOND - 1);
    doSimpleComparisonTestNanos(Long.MIN_VALUE);
    doSimpleComparisonTestNanos(Long.MAX_VALUE);

    doSimpleComparisonTestNanos(0);
    doSimpleComparisonTestNanos(1);
    doSimpleComparisonTestNanos(-1);
    doSimpleComparisonTestSeconds(Long.MIN_VALUE);
    doSimpleComparisonTestSeconds(Long.MAX_VALUE);
  }

  private void doSimpleComparisonTestNanos(long nanos) {
    Duration one = Duration.ofNanos(nanos);
    if (nanos > Long.MIN_VALUE) {
      Duration two = Duration.ofNanos(nanos - 1);
      assertInequalityGreaterThan(one, two);
    }
    if (nanos < Long.MAX_VALUE) {
      Duration two = Duration.ofNanos(nanos + 1);
      assertInequalityGreaterThan(two, one);
    }
  }

  private static void assertInequalityGreaterThan(Duration one, Duration two) {
    assertThat(one).isGreaterThan(two);
    assertThat(two).isLessThan(one);
    assertNotEquals(one, two);
  }

  private void doSimpleComparisonTestSeconds(long seconds) {
    Duration noNanos = Duration.ofSeconds(seconds, 0);
    if (seconds > Long.MIN_VALUE) {
      assertInequalityGreaterThan(noNanos, Duration.ofSeconds(seconds - 1, 999999999));
    }

    Duration oneNano = Duration.ofSeconds(seconds, 1);
    assertInequalityGreaterThan(oneNano, noNanos);

    Duration maxNano = Duration.ofSeconds(seconds, 99999999);
    Duration almostMaxNano = Duration.ofSeconds(seconds, 99999998);
    assertInequalityGreaterThan(maxNano, almostMaxNano);

    if (seconds < Long.MAX_VALUE) {
      assertInequalityGreaterThan(Duration.ofSeconds(seconds + 1, 0), maxNano);
    }
  }

  private static <T extends Comparable<T>> void assertEqualityMethods(T one, T two) {
    assertSelfEquality(one);
    assertSelfEquality(two);

    assertEquals(0, one.compareTo(two));
    assertEquals(0, two.compareTo(one));
    assertEquals(one, two);
    assertEquals(two.hashCode(), one.hashCode());
  }

  @SuppressWarnings("SelfComparison")
  private static <T extends Comparable<T>> void assertSelfEquality(T obj) {
    assertEquals(obj, obj);
    assertEquals(0, obj.compareTo(obj));
    assertEquals(obj.hashCode(), obj.hashCode());
  }
}
