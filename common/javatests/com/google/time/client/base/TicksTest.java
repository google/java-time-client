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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.time.client.base.testing.FakeClocks;
import com.google.time.client.base.testing.MoreAsserts;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TicksTest {

  private static final Ticker TICKER_1 = new FakeClocks().getFakeTicker();
  private static final Ticker TICKER_2 = new FakeClocks().getFakeTicker();

  @Test
  public void equalsAndHashcode() {
    Ticks ticks1_1 = Ticks.fromTickerValue(TICKER_1, 1234L);
    Ticks ticks1_2 = Ticks.fromTickerValue(TICKER_1, 1234L);
    MoreAsserts.assertComparisonMethods(ticks1_1, ticks1_2);

    Ticks ticks2_1 = Ticks.fromTickerValue(TICKER_2, 1234L);
    assertNotEquals(ticks1_1, ticks2_1);

    Ticks ticks1_3 = Ticks.fromTickerValue(TICKER_2, 4321L);
    assertNotEquals(ticks1_1, ticks1_3);
  }

  private void assertEqualsAndHashcodeMatch(Ticks one, Ticks two) {
    assertEquals(one, two);
    assertEquals(two, one);
    assertEquals(one.hashCode(), two.hashCode());
  }

  @Test
  public void durationUntil() {
    Ticks floor = Ticks.fromTickerValue(TICKER_1, Long.MIN_VALUE);
    Ticks zero = Ticks.fromTickerValue(TICKER_1, 0);
    Ticks ceil = Ticks.fromTickerValue(TICKER_1, Long.MAX_VALUE);

    assertThrows(ArithmeticException.class, () -> floor.durationUntil(ceil));
    assertThrows(ArithmeticException.class, () -> ceil.durationUntil(floor));
    assertThrows(ArithmeticException.class, () -> floor.durationUntil(zero));
    assertEquals(Duration.ofNanos(Long.MIN_VALUE), zero.durationUntil(floor));

    assertEquals(Duration.ofNanos(Long.MAX_VALUE), zero.durationUntil(ceil));
    assertEquals(Duration.ofNanos(-Long.MAX_VALUE), ceil.durationUntil(zero));

    assertEquals(Duration.ofNanos(0), zero.durationUntil(zero));
    assertEquals(Duration.ofNanos(0), floor.durationUntil(floor));
    assertEquals(Duration.ofNanos(0), ceil.durationUntil(ceil));

    Ticks differentClock = Ticks.fromTickerValue(TICKER_2, 0);
    assertThrows(IllegalArgumentException.class, () -> differentClock.durationUntil(zero));

    Ticks a = Ticks.fromTickerValue(TICKER_1, 1000);
    Ticks b = Ticks.fromTickerValue(TICKER_1, 1001);

    assertEquals(Duration.ofNanos(1), a.durationUntil(b));
    assertEquals(Duration.ofNanos(-1), b.durationUntil(a));
  }

  @Test
  public void comparable() {
    Ticks ticks1_1 = Ticks.fromTickerValue(TICKER_1, 1L);
    Ticks ticks1_2 = Ticks.fromTickerValue(TICKER_1, 2L);
    assertTrue(ticks1_1.compareTo(ticks1_2) < 0);
    assertTrue(ticks1_2.compareTo(ticks1_1) > 0);

    Ticks ticks2_1 = Ticks.fromTickerValue(TICKER_2, 1L);
    assertThrows(ClassCastException.class, () -> ticks2_1.compareTo(ticks1_1));
    assertThrows(ClassCastException.class, () -> ticks1_1.compareTo(ticks2_1));
  }
}
