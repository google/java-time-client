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

import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_MILLISECOND;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PlatformTickerTest {

  @Test
  public void ticksAndDurationBetween() throws Exception {
    Ticker ticker = PlatformTicker.instance();

    long before1 = System.nanoTime();
    Ticks tick1 = ticker.ticks();
    long after1 = System.nanoTime();

    Thread.sleep(250);

    long before2 = System.nanoTime();
    Ticks tick2 = ticker.ticks();
    long after2 = System.nanoTime();

    Duration maxDuration = Duration.ofNanos(after2 - before1);
    Duration minDuration = Duration.ofNanos(before2 - after1);
    Duration durationBetweenTicks = tick1.durationUntil(tick2);

    assertTrue(durationBetweenTicks.compareTo(minDuration) >= 0);
    assertTrue(durationBetweenTicks.compareTo(maxDuration) <= 0);
  }

  @Test
  public void ticksPrecision() {
    Ticker ticker = PlatformTicker.instance();

    // Try to prove that the ticker is precise below millis.
    Ticks ticks1 = ticker.ticks();
    Ticks ticks2 = ticker.ticks();
    Ticks ticks3 = ticker.ticks();
    assertTrue(
        durationUntilHasNonZeroNanos(ticks1, ticks2)
            || durationUntilHasNonZeroNanos(ticks1, ticks3));
  }

  private static boolean durationUntilHasNonZeroNanos(Ticks ticks1, Ticks ticks2) {
    return ticks1.durationUntil(ticks2).getNano() % NANOS_PER_MILLISECOND != 0;
  }
}
