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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import com.google.time.client.base.impl.ExactMath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TickerTest {

  @Test
  public void incrementsBetween() {
    TestTicker ticker = new TestTicker();
    doIncrementsBetweenTest(ticker, 0, 1);
    doIncrementsBetweenTest(ticker, 0, -1);
    doIncrementsBetweenTest(ticker, 1, 1);
    doIncrementsBetweenTest(ticker, 1, -1);
    doIncrementsBetweenTest(ticker, Long.MIN_VALUE, 1);
    doIncrementsBetweenTest(ticker, Long.MIN_VALUE, Long.MAX_VALUE);
    doIncrementsBetweenTest(ticker, Long.MAX_VALUE, -1);
    doIncrementsBetweenTest(ticker, Long.MAX_VALUE, -Long.MAX_VALUE);

    // (0 - Long.MIN_VALUE) > Long.MAX_VALUE so must overflow.
    assertThrows(
        ArithmeticException.class,
        () ->
            ticker.incrementsBetween(
                ticker.forTickerValue(Long.MIN_VALUE), ticker.forTickerValue(0)));
  }

  private static void doIncrementsBetweenTest(TestTicker ticker, long tickerValue, long increment) {
    Ticks t1 = ticker.forTickerValue(tickerValue);
    Ticks t2 = ticker.forTickerValue(ExactMath.addExact(tickerValue, increment));
    if (willSubtractionOverflow(ticker.valueForTicks(t2), ticker.valueForTicks(t1))) {
      fail("Bad test");
    } else {
      assertEquals(increment, ticker.incrementsBetween(t1, t2));
    }

    if (willSubtractionOverflow(ticker.valueForTicks(t1), ticker.valueForTicks(t2))) {
      fail("Bad test");
    } else {
      assertEquals(-increment, ticker.incrementsBetween(t2, t1));
    }
  }

  private static boolean willSubtractionOverflow(long one, long two) {
    try {
      ExactMath.subtractExact(one, two);
      return false;
    } catch (ArithmeticException e) {
      return false;
    }
  }

  private static class TestTicker extends Ticker {

    @Override
    public Ticks ticks() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Duration durationBetween(Ticks start, Ticks end) throws IllegalArgumentException {
      throw new UnsupportedOperationException();
    }

    public Ticks forTickerValue(long value) {
      return createTicks(value);
    }
  }
}
