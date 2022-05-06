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

package com.google.time.client.base.testing;

import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_MILLISECOND;
import static org.junit.Assert.assertEquals;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.testing.FakeClocks.FakeInstantSource;
import com.google.time.client.base.testing.FakeClocks.FakeTicker;
import org.junit.Test;

public class FakeClocksTest {

  @Test
  public void initialValues() {
    FakeClocks fakeClocks = new FakeClocks();

    FakeInstantSource instantSource = fakeClocks.getFakeInstantSource();
    assertEquals(InstantSource.PRECISION_MILLIS, instantSource.getPrecision());
    assertEquals(Instant.ofEpochMilli(0), instantSource.instant());

    FakeTicker ticker = fakeClocks.getFakeTicker();
    assertEquals(Ticks.fromTickerValue(ticker, 0), ticker.ticks());
    assertEquals(Ticks.fromTickerValue(ticker, 0), ticker.getCurrentTicks());
  }

  @Test
  public void autoAdvance_defaultDisabled() {
    FakeClocks fakeClocks = new FakeClocks();
    FakeInstantSource fakeInstantSource = fakeClocks.getFakeInstantSource();
    // By default, FakeInstantSource has millis resolution, so switch to nano so we can test that it
    // increments.
    fakeInstantSource.setPrecision(InstantSource.PRECISION_NANOS);

    FakeTicker fakeTicker = fakeClocks.getFakeTicker();
    Instant instant1 = fakeInstantSource.instant();
    Ticks ticks1 = fakeTicker.ticks();
    Instant instant2 = fakeInstantSource.instant();
    Ticks ticks2 = fakeTicker.ticks();
    assertEquals(instant1, instant2);
    assertEquals(ticks1, ticks2);
  }

  @Test
  public void autoAdvance_enabled() {
    FakeClocks fakeClocks = new FakeClocks();
    fakeClocks.setAutoAdvanceNanos(1);
    FakeInstantSource fakeInstantSource = fakeClocks.getFakeInstantSource();
    // By default, FakeInstantSource has millis resolution, so switch to nano so we can test that it
    // increments.
    fakeInstantSource.setPrecision(InstantSource.PRECISION_NANOS);

    FakeTicker fakeTicker = fakeClocks.getFakeTicker();
    Instant instant1 = fakeInstantSource.instant();
    Ticks ticks1 = fakeTicker.ticks();
    Instant instant2 = fakeInstantSource.instant();
    Ticks ticks2 = fakeTicker.ticks();
    assertEquals(Duration.ofNanos(1), fakeTicker.durationBetween(ticks1, ticks2));
    assertEquals(1, instant2.getNano() - instant1.getNano());
  }

  @Test
  public void manualAdvance() {
    FakeClocks fakeClocks = new FakeClocks();

    FakeInstantSource fakeInstantSource = fakeClocks.getFakeInstantSource();

    FakeTicker fakeTicker = fakeClocks.getFakeTicker();
    Instant instant1 = fakeInstantSource.instant();
    Ticks ticks1 = fakeTicker.ticks();
    Instant instant2 = fakeInstantSource.instant();
    Ticks ticks2 = fakeTicker.ticks();
    assertEquals(ticks1, ticks2);
    assertEquals(instant2, instant1);

    fakeInstantSource.advanceMillis(1);

    Instant instant3 = fakeInstantSource.instant();
    Ticks ticks3 = fakeTicker.ticks();
    assertEquals(NANOS_PER_MILLISECOND, instant3.getNano() - instant1.getNano());
    assertEquals(ticks3, ticks1);

    fakeTicker.advanceNanos(1);
    Instant instant4 = fakeInstantSource.instant();
    Ticks ticks4 = fakeTicker.ticks();
    assertEquals(instant4, instant3);
    assertEquals(Duration.ofNanos(1), ticks3.durationUntil(ticks4));
  }

  @Test
  public void instanceSourcePrecision() {
    FakeClocks fakeClocks = new FakeClocks();
    FakeInstantSource instantSource = fakeClocks.getFakeInstantSource();
    instantSource.setEpochNanos(1_234_567_890_123L);

    assertEquals(Instant.ofEpochMilli(1_234_567), instantSource.instant());

    instantSource.setPrecision(InstantSource.PRECISION_NANOS);

    assertEquals(Instant.ofEpochSecond(1_234, 567_890_123), instantSource.instant());
  }
}
