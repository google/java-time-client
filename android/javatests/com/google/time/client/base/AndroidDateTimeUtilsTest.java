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

import org.junit.Test;

public class AndroidDateTimeUtilsTest {

  @Test
  public void checkPositiveSubSecondNanos() {
    assertEquals(0, AndroidDateTimeUtils.checkPositiveSubSecondNanos(0));
    assertEquals(1, AndroidDateTimeUtils.checkPositiveSubSecondNanos(1));
    assertEquals(500_000_000, AndroidDateTimeUtils.checkPositiveSubSecondNanos(500_000_000));
    assertEquals(999_999_999, AndroidDateTimeUtils.checkPositiveSubSecondNanos(999_999_999));

    assertThrows(
        DateTimeException.class,
        () -> AndroidDateTimeUtils.checkPositiveSubSecondNanos(Integer.MIN_VALUE));
    assertThrows(
        DateTimeException.class, () -> AndroidDateTimeUtils.checkPositiveSubSecondNanos(-1));
    assertThrows(
        DateTimeException.class,
        () -> AndroidDateTimeUtils.checkPositiveSubSecondNanos(1_000_000_000));
    assertThrows(
        DateTimeException.class,
        () -> AndroidDateTimeUtils.checkPositiveSubSecondNanos(Integer.MAX_VALUE));
  }
}
