/*
 * Copyright 2023 Google LLC
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
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.Supplier;
import com.google.time.client.base.impl.Objects;
import java.util.Random;

/** Creates new {@link NtpMessage} instances that contain SNTP requests. */
public class SntpRequestFactory implements Supplier<NtpMessage> {

  private final int clientReportedVersion;

  private final boolean clientDataMinimizationEnabled;

  private final InstantSource clientInstantSource;

  private final Random random;

  /**
   * Creates the factory.
   *
   * @param clientInstantSource the {@link InstantSource} used to populate client instant fields
   *     (usage depends on {@code clientDataMinimizationEnabled})
   * @param random the {@link Random} used to generate random data (usage depends on {@code
   *     clientDataMinimizationEnabled})
   * @param clientReportedVersion the NTP client version to report in the message
   * @param clientDataMinimizationEnabled minimize client information shared with the server as
   *     described in <a
   *     href="https://datatracker.ietf.org/doc/html/draft-ietf-ntp-data-minimization-04">NTP Client
   *     Data Minimization</a>
   */
  public SntpRequestFactory(
      InstantSource clientInstantSource,
      Random random,
      int clientReportedVersion,
      boolean clientDataMinimizationEnabled) {
    this.clientReportedVersion = clientReportedVersion;
    this.clientDataMinimizationEnabled = clientDataMinimizationEnabled;
    this.clientInstantSource = Objects.requireNonNull(clientInstantSource);
    this.random = Objects.requireNonNull(random);
  }

  @Override
  public NtpMessage get() {
    NtpHeader.Builder requestHeaderBuilder =
        NtpHeader.Builder.createEmpty()
            .setVersionNumber(clientReportedVersion)
            .setMode(NtpHeader.NTP_MODE_CLIENT);

    // Since it doesn't really matter what we send here (the server shouldn't use it for anything
    // except round-tripping), the transmit timestamp can be different from the value actually
    // used by the client.
    Timestamp64 requestTransmitTimestamp;
    if (clientDataMinimizationEnabled) {
      // As per: https://datatracker.ietf.org/doc/html/draft-ietf-ntp-data-minimization-04
      // Using an entirely random timestamp is better than revealing client clock data.
      long eraSeconds = random.nextInt() & 0xFFFF_FFFFL;
      requestTransmitTimestamp = Timestamp64.fromComponents(eraSeconds, random.nextInt());
    } else {
      Instant requestTransmitInstant = clientInstantSource.instant();
      requestTransmitTimestamp = Timestamp64.fromInstant(requestTransmitInstant);
      if (clientInstantSource.getPrecision() <= InstantSource.PRECISION_MILLIS) {
        // requestTransmitTimestamp is treated as a nonce, so randomize the sub-millis nanos to
        // ensure it is less predictable. This introduces an error, but only up to 1 millis, e.g.
        // requestTransmitTimestamp could now be in the future or in the past, but less than 1
        // millis.
        // The value is not used by the client again, and the server also shouldn't be using it for
        // anything that affects the response we get (besides replaying it back to the client).
        requestTransmitTimestamp = requestTransmitTimestamp.randomizeSubMillis(random);
      }
    }
    requestHeaderBuilder.setTransmitTimestamp(requestTransmitTimestamp);

    NtpHeader requestHeader = requestHeaderBuilder.build();
    NtpMessage.Builder requestBuilder = new NtpMessage.Builder().setHeader(requestHeader);
    return requestBuilder.build();
  }
}
