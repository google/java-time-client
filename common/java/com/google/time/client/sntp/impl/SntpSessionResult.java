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

package com.google.time.client.sntp.impl;

import com.google.time.client.base.Instant;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.annotations.VisibleForTesting;
import com.google.time.client.base.impl.Objects;

/**
 * A result struct for an SNTP interaction with an NTP server. It contains the request, response and
 * client-side information associated with a single round-trip to an NTP server.
 */
@VisibleForTesting
final class SntpSessionResult {

  // T1 / [client]requestTimestamp according to the client's InstantSource.
  final Instant requestInstant;

  // T1 / [client]requestTimestamp according to the client's Ticker.
  final Ticks requestTimeTicks;

  // Capture T4 / [client]responseTimestamp according to the client's Ticker
  final Ticks responseTimeTicks;

  /** The message sent to the NTP server. */
  final NtpMessage request;

  /** The message received from the NTP server. */
  final NtpMessage response;

  SntpSessionResult(
      Instant requestInstant,
      Ticks requestTimeTicks,
      Ticks responseTimeTicks,
      NtpMessage request,
      NtpMessage response) {
    this.requestInstant = Objects.requireNonNull(requestInstant, "requestInstant");
    this.requestTimeTicks = Objects.requireNonNull(requestTimeTicks, "requestTimeTicks");
    this.responseTimeTicks = Objects.requireNonNull(responseTimeTicks, "responseTimeTicks");
    this.request = Objects.requireNonNull(request, "request");
    this.response = Objects.requireNonNull(response, "response");
  }

  @Override
  public String toString() {
    return "SntpSessionResult{"
        + "requestInstant="
        + requestInstant
        + ", requestTimeTicks="
        + requestTimeTicks
        + ", responseTimeTicks="
        + responseTimeTicks
        + ", request="
        + request
        + ", response="
        + response
        + '}';
  }
}
