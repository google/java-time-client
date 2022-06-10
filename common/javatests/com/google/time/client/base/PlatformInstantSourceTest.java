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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PlatformInstantSourceTest {

  @Test
  public void precision() {
    InstantSource instantSource = PlatformInstantSource.instance();
    assertEquals(InstantSource.PRECISION_MILLIS, instantSource.getPrecision());
    assertEquals(0, instantSource.instant().getNano() % NANOS_PER_MILLISECOND);
    assertEquals(0, instantSource.instant().getNano() % NANOS_PER_MILLISECOND);
    assertEquals(0, instantSource.instant().getNano() % NANOS_PER_MILLISECOND);
  }

  @Test
  public void instant() {
    long before = System.currentTimeMillis();
    Instant instant = PlatformInstantSource.instance().instant();
    long after = System.currentTimeMillis();

    long instantEpochMillis = instant.toEpochMilli();
    assertTrue(instantEpochMillis >= before && instantEpochMillis <= after);
  }
}
