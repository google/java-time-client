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
import static com.google.time.client.base.impl.DateTimeConstants.MILLISECONDS_PER_SECOND;
import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_MILLISECOND;
import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_SECOND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.google.time.client.base.testing.TestEnvironmentUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class InstantTest {

  @Test
  public void allowedRange() {
    // Values obtained by probing Java SE version.
    assertEquals(31556889864403199L, Instant.MAX.getEpochSecond());
    assertEquals(999999999, Instant.MAX.getNano());
    assertEquals(-31557014167219200L, Instant.MIN.getEpochSecond());
    assertEquals(0, Instant.MIN.getNano());

    assertThrows(
        DateTimeException.class,
        () -> Instant.ofEpochSecond(Instant.MAX.getEpochSecond() + 1, Instant.MAX.getNano()));
    assertThrows(
        DateTimeException.class,
        () -> Instant.ofEpochSecond(Instant.MAX.getEpochSecond(), Instant.MAX.getNano() + 1));

    assertThrows(
        DateTimeException.class,
        () -> Instant.ofEpochSecond(Instant.MIN.getEpochSecond() - 1, Instant.MIN.getNano()));
    assertThrows(
        DateTimeException.class,
        () -> Instant.ofEpochSecond(Instant.MIN.getEpochSecond(), Instant.MIN.getNano() - 1));
  }

  @Test
  public void positiveOneSecond() {
    Instant posOneSecond = Instant.ofEpochSecond(1, 0);
    assertEquals(1, posOneSecond.getEpochSecond());
    assertEquals(0, posOneSecond.getNano());
    assertEquals(posOneSecond, posOneSecond.plus(Duration.ZERO));
    assertEquals(Instant.ofEpochSecond(2, 0), posOneSecond.plus(Duration.ofSeconds(1, 0)));
    assertEquals(1000, posOneSecond.toEpochMilli());
    assertNotNull(posOneSecond.toString());

    Instant otherPosOneSecond = Instant.ofEpochMilli(1000);
    assertEqualityMethods(posOneSecond, otherPosOneSecond);
  }

  @Test
  public void negativeOneSecond() {
    Instant negOneSecond = Instant.ofEpochSecond(-1, 0);
    assertEquals(-1, negOneSecond.getEpochSecond());
    assertEquals(0, negOneSecond.getNano());
    assertEquals(negOneSecond, negOneSecond.plus(Duration.ZERO));
    assertEquals(Instant.ofEpochSecond(-2, 0), negOneSecond.plus(Duration.ofSeconds(-1, 0)));
    assertEquals(-1000, negOneSecond.toEpochMilli());
    assertNotNull(negOneSecond.toString());

    Instant otherNegOneSecond = Instant.ofEpochMilli(-1000);
    assertEqualityMethods(negOneSecond, otherNegOneSecond);
  }

  @Test
  public void positiveOneNano() {
    Instant posOneNano = Instant.ofEpochSecond(0, 1);
    assertEquals(0, posOneNano.getEpochSecond());
    assertEquals(1, posOneNano.getNano());
    assertEquals(posOneNano, posOneNano.plus(Duration.ZERO));
    assertEquals(Instant.ofEpochSecond(0, 2), posOneNano.plus(Duration.ofSeconds(0, 1)));
    assertEquals(0, posOneNano.toEpochMilli());
    assertNotNull(posOneNano.toString());

    Instant otherPosOneNano = Instant.ofEpochSecond(0, 1);
    assertEqualityMethods(posOneNano, otherPosOneNano);
  }

  @Test
  public void negativeOneNano() {
    Instant negOneNano = Instant.ofEpochSecond(0, -1);
    assertEquals(-1, negOneNano.getEpochSecond());
    assertEquals(999999999L, negOneNano.getNano());
    assertEquals(negOneNano, negOneNano.plus(Duration.ZERO));
    assertEquals(Instant.ofEpochSecond(0, -2), negOneNano.plus(Duration.ofSeconds(0, -1)));
    assertEquals(-1, negOneNano.toEpochMilli());
    assertNotNull(negOneNano.toString());

    Instant otherNegOneNano = Instant.ofEpochSecond(0, -1);
    assertEqualityMethods(negOneNano, otherNegOneNano);
  }

  @Test
  public void positive999999999Nano() {
    Instant pos999999999Nano = Instant.ofEpochSecond(0, 999999999);
    assertEquals(0, pos999999999Nano.getEpochSecond());
    assertEquals(999999999, pos999999999Nano.getNano());
    assertEquals(pos999999999Nano, pos999999999Nano.plus(Duration.ZERO));
    assertEquals(
        Instant.ofEpochSecond(1, 999999998),
        pos999999999Nano.plus(Duration.ofSeconds(0, 999999999)));
    assertEquals(999, pos999999999Nano.toEpochMilli());
    assertNotNull(pos999999999Nano.toString());

    Instant otherPos999999999Nano = Instant.ofEpochSecond(0, 999999999);
    assertEqualityMethods(pos999999999Nano, otherPos999999999Nano);
  }

  @Test
  public void negative999999999Nano() {
    Instant neg999999999Nano = Instant.ofEpochSecond(0, -999999999);
    assertEquals(-1, neg999999999Nano.getEpochSecond());
    assertEquals(1, neg999999999Nano.getNano());
    assertEquals(neg999999999Nano, neg999999999Nano.plus(Duration.ZERO));
    assertEquals(
        Instant.ofEpochSecond(-2, 2), neg999999999Nano.plus(Duration.ofSeconds(0, -999999999)));

    // This fails on JDK 9+
    assertEquals(-1000, neg999999999Nano.toEpochMilli());
    assertNotNull(neg999999999Nano.toString());

    Instant otherNeg999999999Nano = Instant.ofEpochSecond(0, -999999999);
    assertEqualityMethods(neg999999999Nano, otherNeg999999999Nano);
  }

  // Covers ofEpochMilli(), getEpochSecond(), getNanos(), toEpochMillis().
  @Test
  public void ofEpochMillis() {
    {
      Instant instant = Instant.ofEpochMilli(0);
      assertEquals(0, instant.getEpochSecond());
      assertEquals(0, instant.getNano());
      assertEquals(0, instant.toEpochMilli());
    }

    {
      int epochMillis = 1;
      Instant instant = Instant.ofEpochMilli(epochMillis);
      assertEquals(0, instant.getEpochSecond());
      assertEquals(NANOS_PER_MILLISECOND, instant.getNano());
      assertEquals(epochMillis, instant.toEpochMilli());
    }

    {
      int epochMillis = -1;
      Instant instant = Instant.ofEpochMilli(epochMillis);
      assertEquals(-1, instant.getEpochSecond());
      assertEquals(999000000, instant.getNano());
      assertEquals(epochMillis, instant.toEpochMilli());
    }

    {
      long epochMillis = Long.MIN_VALUE;
      Instant instant = Instant.ofEpochMilli(epochMillis);
      assertEquals((epochMillis / MILLISECONDS_PER_SECOND) - 1, instant.getEpochSecond());
      assertEquals(
          NANOS_PER_SECOND + ((epochMillis % MILLISECONDS_PER_SECOND) * NANOS_PER_MILLISECOND),
          instant.getNano());
      assertEquals(epochMillis, instant.toEpochMilli());
    }

    {
      long epochMillis = Long.MAX_VALUE;
      Instant instant = Instant.ofEpochMilli(epochMillis);
      assertEquals(epochMillis / MILLISECONDS_PER_SECOND, instant.getEpochSecond());
      assertEquals(
          (epochMillis % MILLISECONDS_PER_SECOND) * NANOS_PER_MILLISECOND, instant.getNano());
      assertEquals(epochMillis, instant.toEpochMilli());
    }
  }

  // Covers ofEpochSecond(), getEpochSecond(), getNanos(), toEpochMillis().
  @Test
  public void ofEpochSecond() {
    doOfEpochSecondTest(0);
    doOfEpochSecondTest(1);
    doOfEpochSecondTest(-1);
    doOfEpochSecondTest(Instant.MAX.getEpochSecond());
    doOfEpochSecondTest(Instant.MIN.getEpochSecond());
    doOfEpochSecondTest(Long.MAX_VALUE);
    doOfEpochSecondTest(Long.MIN_VALUE);
  }

  private static void doOfEpochSecondTest(long epochSeconds) {
    if (epochSeconds >= Instant.MIN.getEpochSecond()
        && epochSeconds <= Instant.MAX.getEpochSecond()) {
      {
        Instant instant = Instant.ofEpochSecond(epochSeconds, 0);
        assertEquals(epochSeconds, instant.getEpochSecond());
        assertEquals(0, instant.getNano());
        if (isOutsideEpochMillisLongRange(epochSeconds)) {
          assertThrows(ArithmeticException.class, instant::toEpochMilli);
        } else {
          assertEquals(epochSeconds * MILLISECONDS_PER_SECOND, instant.toEpochMilli());
        }
      }

      {
        Instant instant = Instant.ofEpochSecond(epochSeconds, 999999999);
        assertEquals(epochSeconds, instant.getEpochSecond());
        assertEquals(999999999, instant.getNano());
        if (isOutsideEpochMillisLongRange(epochSeconds)) {
          assertThrows(ArithmeticException.class, instant::toEpochMilli);
        } else {
          assertEquals(epochSeconds * MILLISECONDS_PER_SECOND + 999, instant.toEpochMilli());
        }
      }
    } else {
      assertThrows(DateTimeException.class, () -> Instant.ofEpochSecond(epochSeconds, 0));
      assertThrows(DateTimeException.class, () -> Instant.ofEpochSecond(epochSeconds, 999999999));
    }

    if (epochSeconds >= Instant.MIN.getEpochSecond()
        && epochSeconds < Instant.MAX.getEpochSecond()) {
      long expectedEpochSeconds = epochSeconds + 1;
      Instant newInstant = Instant.ofEpochSecond(epochSeconds, NANOS_PER_SECOND);
      assertEquals(Instant.ofEpochSecond(expectedEpochSeconds, 0), newInstant);
      if (isOutsideEpochMillisLongRange(epochSeconds)) {
        assertThrows(ArithmeticException.class, newInstant::toEpochMilli);
      } else {
        assertEquals(expectedEpochSeconds * MILLISECONDS_PER_SECOND, newInstant.toEpochMilli());
      }
    } else if (epochSeconds == Long.MAX_VALUE) {
      assertThrows(
          ArithmeticException.class, () -> Instant.ofEpochSecond(epochSeconds, NANOS_PER_SECOND));
    } else {
      assertThrows(
          DateTimeException.class, () -> Instant.ofEpochSecond(epochSeconds, NANOS_PER_SECOND));
    }

    if (epochSeconds > Instant.MIN.getEpochSecond()
        && epochSeconds <= Instant.MAX.getEpochSecond()) {
      long expectedEpochSeconds = epochSeconds - 1;
      Instant newInstant = Instant.ofEpochSecond(epochSeconds, -1);
      assertEquals(Instant.ofEpochSecond(expectedEpochSeconds, 999999999), newInstant);
      if (epochSeconds > Long.MAX_VALUE / MILLISECONDS_PER_SECOND) {
        assertThrows(ArithmeticException.class, newInstant::toEpochMilli);
      } else {
        assertEquals(
            expectedEpochSeconds * MILLISECONDS_PER_SECOND + 999, newInstant.toEpochMilli());
      }
    } else if (epochSeconds == Long.MIN_VALUE) {
      assertThrows(ArithmeticException.class, () -> Instant.ofEpochSecond(epochSeconds, -1));
    } else {
      assertThrows(DateTimeException.class, () -> Instant.ofEpochSecond(epochSeconds, -1));
    }
  }

  private static boolean isOutsideEpochMillisLongRange(long epochSeconds) {
    return epochSeconds < Long.MIN_VALUE / MILLISECONDS_PER_SECOND
        || epochSeconds > Long.MAX_VALUE / MILLISECONDS_PER_SECOND;
  }

  @Test
  public void plus() {
    doPlusTests(Instant.ofEpochMilli(0));
    doPlusTests(Instant.ofEpochMilli(1));
    doPlusTests(Instant.ofEpochMilli(-1));
    doPlusTests(Instant.ofEpochMilli(-1000));
    doPlusTests(Instant.ofEpochMilli(1000));
    doPlusTests(Instant.ofEpochMilli(Long.MAX_VALUE));
    doPlusTests(Instant.ofEpochMilli(Long.MIN_VALUE));

    doPlusTests(Instant.ofEpochSecond(Instant.MAX.getEpochSecond(), Instant.MAX.getNano() - 1));
    doPlusTests(Instant.ofEpochSecond(Instant.MIN.getEpochSecond(), Instant.MIN.getNano() + 1));
    doPlusTests(Instant.MAX);
    doPlusTests(Instant.MIN);
  }

  private static void doPlusTests(Instant instant) {
    assertEquals(instant, instant.plus(Duration.ofNanos(0)));

    {
      Duration adjustment = Duration.ofNanos(1);
      if (instant.equals(Instant.MAX)) {
        assertThrows(DateTimeException.class, () -> instant.plus(adjustment));
      } else {
        Instant result = instant.plus(adjustment);
        if (instant.getNano() == 999999999) {
          assertEquals(instant.getEpochSecond() + 1, result.getEpochSecond());
          assertEquals(0, result.getNano());
        } else {
          assertEquals(instant.getEpochSecond(), result.getEpochSecond());
          assertEquals(instant.getNano() + 1, result.getNano());
        }
      }
    }

    {
      Duration adjustment = Duration.ofNanos(-1);
      // A quirk of the representation of negative numbers is that a negative duration, even
      // something small like -1 nanos, involves a Duration with a negative number of seconds
      // (e.g. -1 nanos == "-1s + 999999999ns"), which can cause overflow. It is therefore not
      // possible to add a negative nanos value to and instant with Instant.MIN.getEpochSeconds()
      // seconds, however small.
      if (instant.getEpochSecond() == Instant.MIN.getEpochSecond()) {
        assertThrows(DateTimeException.class, () -> instant.plus(adjustment));
      } else {
        Instant result = instant.plus(Duration.ofNanos(-1));
        if (instant.getNano() == 0) {
          assertEquals(instant.getEpochSecond() - 1, result.getEpochSecond());
          assertEquals(999999999, result.getNano());
        } else {
          assertEquals(instant.getEpochSecond(), result.getEpochSecond());
          assertEquals(instant.getNano() - 1, result.getNano());
        }
      }
    }
  }

  // Covers equals(Object), compareTo(Instant) & hashCode() (for equality only).
  @Test
  public void equals() {
    doEqualityTest(0);
    doEqualityTest(1);
    doEqualityTest(-1);
    doEqualityTest(NANOS_PER_SECOND);
    doEqualityTest(-NANOS_PER_SECOND);
    doEqualityTest(NANOS_PER_SECOND + 1);
    doEqualityTest(-NANOS_PER_SECOND - 1);
    doEqualityTest(Long.MIN_VALUE);
    doEqualityTest(Long.MAX_VALUE);

    doEqualityTest(Instant.MIN.getEpochSecond(), 0);
    doEqualityTest(Instant.MAX.getEpochSecond(), 999999999);
    doEqualityTest(Instant.MIN.getEpochSecond(), 1);
    doEqualityTest(Instant.MAX.getEpochSecond(), 999999998);
    doEqualityTest(Instant.MIN.getEpochSecond(), 999999999);
    doEqualityTest(Instant.MAX.getEpochSecond(), 0);
  }

  private static void doEqualityTest(long epochMillis) {
    assertEqualityMethods(Instant.ofEpochMilli(epochMillis), Instant.ofEpochMilli(epochMillis));
  }

  private static void doEqualityTest(long seconds, int nanoAdjustment) {
    assertEqualityMethods(
        Instant.ofEpochSecond(seconds, nanoAdjustment),
        Instant.ofEpochSecond(seconds, nanoAdjustment));
  }

  // See also equals() test.
  @Test
  public void compareTo() {
    doSimpleComparisonTestEpochMillis(0);
    doSimpleComparisonTestEpochMillis(1);
    doSimpleComparisonTestEpochMillis(-1);
    doSimpleComparisonTestEpochMillis(NANOS_PER_SECOND);
    doSimpleComparisonTestEpochMillis(-NANOS_PER_SECOND);
    doSimpleComparisonTestEpochMillis(NANOS_PER_SECOND + 1);
    doSimpleComparisonTestEpochMillis(-NANOS_PER_SECOND - 1);
    doSimpleComparisonTestEpochMillis(Long.MIN_VALUE);
    doSimpleComparisonTestEpochMillis(Long.MAX_VALUE);

    doSimpleComparisonTestEpochMillis(0);
    doSimpleComparisonTestEpochMillis(1);
    doSimpleComparisonTestEpochMillis(-1);
    doSimpleComparisonTestSeconds(Instant.MIN.getEpochSecond());
    doSimpleComparisonTestSeconds(Instant.MAX.getEpochSecond());
  }

  private void doSimpleComparisonTestEpochMillis(long epochMillis) {
    Instant one = Instant.ofEpochMilli(epochMillis);
    if (epochMillis > Long.MIN_VALUE) {
      Instant two = Instant.ofEpochMilli(epochMillis - 1);
      assertInequalityGreaterThan(one, two);
    }
    if (epochMillis < Long.MAX_VALUE) {
      Instant two = Instant.ofEpochMilli(epochMillis + 1);
      assertInequalityGreaterThan(two, one);
    }
  }

  private static void assertInequalityGreaterThan(Instant one, Instant two) {
    assertThat(one).isGreaterThan(two);
    assertThat(two).isLessThan(one);
    assertNotEquals(one, two);
  }

  private void doSimpleComparisonTestSeconds(long seconds) {
    Instant noNanos = Instant.ofEpochSecond(seconds, 0);
    if (seconds > Instant.MIN.getEpochSecond()) {
      assertInequalityGreaterThan(noNanos, Instant.ofEpochSecond(seconds - 1, 999999999));
    }

    Instant oneNano = Instant.ofEpochSecond(seconds, 1);
    assertInequalityGreaterThan(oneNano, noNanos);

    Instant maxNano = Instant.ofEpochSecond(seconds, 99999999);
    Instant almostMaxNano = Instant.ofEpochSecond(seconds, 99999998);
    assertInequalityGreaterThan(maxNano, almostMaxNano);

    if (seconds < Instant.MAX.getEpochSecond()) {
      assertInequalityGreaterThan(Instant.ofEpochSecond(seconds + 1, 0), maxNano);
    }
  }

  @Test
  public void javaTimeInterop() {
    long epochMilli = 1234567890L;
    // Avoid linkage errors.
    if (TestEnvironmentUtils.isThisJavaSe()
        || TestEnvironmentUtils.getAndroidApiLevel() >= 26
        || TestEnvironmentUtils.isThisRobolectric()) {
      assertEquals(
          java.time.Instant.ofEpochMilli(epochMilli),
          Instant.ofEpochMilli(epochMilli).toJavaTime());
      assertEquals(
          Instant.ofEpochMilli(epochMilli),
          Instant.ofJavaTime(java.time.Instant.ofEpochMilli(epochMilli)));
    } else {
      assertThrows(NoClassDefFoundError.class, () -> Instant.ofEpochMilli(epochMilli).toJavaTime());
      assertThrows(
          NoClassDefFoundError.class,
          () -> Instant.ofJavaTime(java.time.Instant.ofEpochMilli(epochMilli)));
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
