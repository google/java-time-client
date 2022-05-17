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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.time.client.base.Instant;
import org.junit.Test;

public class DateTimeUtilsTest {

  @Test
  public void utc() {
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1899, 1, 1, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(2101, 1, 1, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 0, 1, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 13, 1, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 1, 0, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 1, 32, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 1, 1, -1, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 1, 1, 24, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 1, 1, 0, -1, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 1, 1, 0, 60, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 1, 1, 0, 0, -1, 0));
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 1, 1, 0, 0, 60, 0));
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 1, 1, 0, 0, 60, -1));
    assertThrows(
        IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 1, 1, 0, 0, 60, 1000000000));

    // Gregorian calendar validation
    assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.utc(1970, 2, 31, 0, 0, 0, 0));

    // Arbitrary date / times.
    assertEquals(Instant.ofEpochSecond(-2208988800L, 0), DateTimeUtils.utc(1900, 1, 1, 0, 0, 0, 0));
    assertEquals(Instant.ofEpochSecond(0, 0), DateTimeUtils.utc(1970, 1, 1, 0, 0, 0, 0));
    assertEquals(Instant.ofEpochSecond(0, 1), DateTimeUtils.utc(1970, 1, 1, 0, 0, 0, 1));
    assertEquals(
        Instant.ofEpochSecond(0, 1000000), DateTimeUtils.utc(1970, 1, 1, 0, 0, 0, 1000000));
    assertEquals(
        Instant.ofEpochSecond(0, 999999999), DateTimeUtils.utc(1970, 1, 1, 0, 0, 0, 999999999));
  }
}
