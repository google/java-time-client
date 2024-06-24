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

import com.google.time.client.base.Duration;
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.Ticker;

/**
 * A source of fake {@link Ticker} and {@link InstantSource} objects that can be made to advance
 * with each other automatically each time they are accessed via methods on this class. The
 * individual clocks can be advanced independently via methods on each.
 */
public final class FakeClocks implements Advanceable {

  private final FakeInstantSource fakeInstantSource = new FakeInstantSource(this);
  private final FakeTicker fakeTicker = new FakeTicker(this);

  Duration autoAdvanceDuration = Duration.ZERO;

  /** Clock is automatically advanced <em>before</em> the time is read. */
  public void setAutoAdvanceNanos(long autoAdvanceNanos) {
    this.autoAdvanceDuration = Duration.ofNanos(autoAdvanceNanos);
  }

  /** Clock is automatically advanced <em>before</em> the time is read. */
  public void setAutoAdvanceDuration(Duration autoAdvanceDuration) {
    this.autoAdvanceDuration = autoAdvanceDuration;
  }

  /** Returns the {@link FakeInstantSource}. */
  public FakeInstantSource getFakeInstantSource() {
    return fakeInstantSource;
  }

  /** Returns the {@link FakeTicker}. */
  public FakeTicker getFakeTicker() {
    return fakeTicker;
  }

  @Override
  public void advance(Duration duration) {
    fakeInstantSource.advance(duration);
    fakeTicker.advance(duration);
  }
}
