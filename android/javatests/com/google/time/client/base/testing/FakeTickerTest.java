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

import static org.junit.Assert.assertEquals;

import com.google.time.client.base.AndroidTicker;
import com.google.time.client.base.Ticks;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeTickerTest {

  @Test
  public void implementsAndroidTicker() {
    FakeClocks fakeClocks = new FakeClocks();
    AndroidTicker fakeTicker = fakeClocks.getFakeTicker();
    {
      long elapsedRealtimeMillis = 1234;
      Ticks ticks = fakeTicker.ticksForElapsedRealtimeMillis(elapsedRealtimeMillis);
      assertEquals(elapsedRealtimeMillis, fakeTicker.elapsedRealtimeMillisForTicks(ticks));
    }
    {
      long elapsedRealtimeNanos = 1234;
      Ticks ticks = fakeTicker.ticksForElapsedRealtimeNanos(elapsedRealtimeNanos);
      assertEquals(elapsedRealtimeNanos, fakeTicker.elapsedRealtimeNanosForTicks(ticks));
    }
  }
}
