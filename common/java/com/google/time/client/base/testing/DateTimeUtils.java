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

import static com.google.time.client.base.impl.DateTimeConstants.MILLISECONDS_PER_SECOND;
import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_SECOND;

import com.google.time.client.base.Instant;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/** Testing support for date / time. */
public final class DateTimeUtils {

  private DateTimeUtils() {}

  /**
   * Constructs an Instant equivalent to the specified UTC time. Avoids the use of
   * java.time.LocalDateTime, which isn't available on all platforms supported by java-time-client.
   */
  public static Instant utc(
      int year, int monthOfYear, int day, int hour, int minute, int second, int nanosOfSecond) {

    // Do basic validation to catch obviously bad test data. Calendar with lenient mode off should
    // catch the rest.
    checkRange(year, 1900, 2100); // Arbitrary range
    checkRange(nanosOfSecond, 0, NANOS_PER_SECOND - 1);

    GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    calendar.setLenient(false);
    calendar.clear();

    int calendarMonth = monthOfYear - 1;
    calendar.set(year, calendarMonth, day, hour, minute, second);
    long epochMillis = calendar.getTimeInMillis();
    long instantSeconds = epochMillis / MILLISECONDS_PER_SECOND;
    return Instant.ofEpochSecond(instantSeconds, nanosOfSecond);
  }

  private static void checkRange(int value, int minInc, int maxInc) {
    if (value < minInc || value > maxInc) {
      throw new IllegalArgumentException(
          value + " is outside of the range [" + minInc + ", " + maxInc + "]");
    }
  }
}
