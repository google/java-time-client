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

package com.google.time.client.sntp.testing;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.base.impl.Objects;
import com.google.time.client.base.impl.PlatformRandom;
import com.google.time.client.base.testing.Advanceable;
import com.google.time.client.base.testing.FakeClocks;
import com.google.time.client.sntp.impl.NtpHeader;
import com.google.time.client.sntp.impl.NtpMessage;
import com.google.time.client.sntp.impl.Timestamp64;
import java.util.ArrayList;
import java.util.List;

/** Fake NTP server logic that behaves like a real one with configurable quirks. */
public final class FakeSntpServerEngine implements TestSntpServerEngine {

  public static final int QUIRK_MODE_NON_MATCHING_ORIGINATE_TIME = 0x1;
  public static final int QUIRK_MODE_DO_NOT_MATCH_REQUEST_PROTOCOL_VERSION = 0x2;

  private final List<NtpMessage> requestsReceived = new ArrayList<>();
  private final List<NtpMessage> responsesSent = new ArrayList<>();
  private final List<Advanceable> advanceables = new ArrayList<>();
  private final FakeClocks.FakeInstantSource instantSource;

  private NtpMessage responseTemplate = createDefaultResponseTemplate();
  private Duration processingDuration = Duration.ZERO;
  private int quirkMode;

  public FakeSntpServerEngine(FakeClocks.FakeInstantSource instantSource) {
    this.instantSource = Objects.requireNonNull(instantSource);
  }

  public void addAdvanceable(Advanceable advanceable) {
    advanceables.add(advanceable);
  }

  public void setQuirkMode(int flags) {
    quirkMode = flags;
  }

  public void setSimulatedProcessingDuration(Duration processingDuration) {
    this.processingDuration = Objects.requireNonNull(processingDuration);
  }

  public NtpMessage getResponseTemplate() {
    return responseTemplate;
  }

  public void setResponseTemplate(NtpMessage responseTemplate) {
    this.responseTemplate = Objects.requireNonNull(responseTemplate);
  }

  @Override
  public NtpMessage processRequest(NtpMessage request) {
    requestsReceived.add(request);
    final NtpHeader requestHeader = request.getHeader();

    NtpHeader.Builder responseHeaderBuilder = responseTemplate.getHeader().toBuilder();
    if ((quirkMode & QUIRK_MODE_DO_NOT_MATCH_REQUEST_PROTOCOL_VERSION) == 0) {
      responseHeaderBuilder.setVersionNumber(requestHeader.getVersionNumber());
    }

    Instant receiveInstant = instantSource.instant();
    responseHeaderBuilder.setReceiveTimestamp(Timestamp64.fromInstant(receiveInstant));

    simulateElapsedTime(processingDuration);
    responseHeaderBuilder.setTransmitTimestamp(
        Timestamp64.fromInstant(instantSource.getCurrentInstant()));

    Timestamp64 originateTimestamp;
    if ((quirkMode & QUIRK_MODE_NON_MATCHING_ORIGINATE_TIME) == 0) {
      originateTimestamp = requestHeader.getTransmitTimestamp();
    } else {
      originateTimestamp =
          requestHeader
              .getTransmitTimestamp()
              .randomizeSubMillis(PlatformRandom.getDefaultRandom());
    }
    responseHeaderBuilder.setOriginateTimestamp(originateTimestamp);

    NtpMessage response =
        responseTemplate.toBuilder().setHeader(responseHeaderBuilder.build()).build();
    responsesSent.add(response);
    return response;
  }

  @Override
  public int numRequestsReceived() {
    return requestsReceived.size();
  }

  private static NtpMessage createDefaultResponseTemplate() {
    NtpHeader header =
        NtpHeader.Builder.createEmptyV3()
            .setReferenceIdentifierAsString("TEST")
            .setMode(NtpHeader.NTP_MODE_SERVER)
            .setPrecisionExponent(-10)
            .setPollIntervalExponent(3)
            .build();
    return NtpMessage.create(header);
  }

  private void simulateElapsedTime(Duration elapsedTime) {
    for (Advanceable advanceable : advanceables) {
      advanceable.advance(elapsedTime);
    }
  }

  public NtpMessage getLastRequestReceived() {
    return requestsReceived.get(requestsReceived.size() - 1);
  }

  public NtpMessage getLastResponseSent() {
    return responsesSent.get(responsesSent.size() - 1);
  }
}
