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

import com.google.time.client.base.impl.DateTimeConstants;

/** Utility date / time methods to support Android-specific implementations of class. */
class AndroidDateTimeUtils {

  private AndroidDateTimeUtils() {}

  /**
   * Checks that the supplied value falls in the range [0..999,999,999] and returns it.
   *
   * @throws DateTimeException if the value is outside of the allowed range
   */
  public static int checkPositiveSubSecondNanos(int nanosOfSecond) {
    if (nanosOfSecond < 0) {
      throw new DateTimeException("nanosOfSecond must not be negative, is " + nanosOfSecond);
    } else if (nanosOfSecond >= DateTimeConstants.NANOS_PER_SECOND) {
      throw new DateTimeException(
          "nanosOfSecond must not be > "
              + DateTimeConstants.NANOS_PER_SECOND
              + ", is "
              + nanosOfSecond);
    }
    return nanosOfSecond;
  }
}
