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
import com.google.time.client.sntp.impl.NtpMessage;
import com.google.time.client.sntp.impl.Timestamp64;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/** Fake NTP server logic that behaves like a real one with configurable quirks. */
public class FakeSntpServerEngine implements TestSntpServerEngine {

  public static final int QUIRK_MODE_NON_MATCHING_ORIGINATE_TIME = 0x1;
  public static final int QUIRK_MODE_DO_NOT_MATCH_REQUEST_PROTOCOL_VERSION = 0x2;

  private final List<NtpMessage> requestsReceived = new ArrayList<>();
  private final List<NtpMessage> responsesSent = new ArrayList<>();
  private final List<Advanceable> advanceables = new ArrayList<>();
  private final NtpMessage responseTemplate = createDefaultResponseTemplate();
  private final FakeClocks.FakeInstantSource instantSource;

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

  @Override
  public NtpMessage processRequest(NtpMessage request) {
    requestsReceived.add(request);

    NtpMessage response = createResponse(responseTemplate, null, 0);
    if ((quirkMode & QUIRK_MODE_DO_NOT_MATCH_REQUEST_PROTOCOL_VERSION) == 0) {
      response.setVersionNumber(request.getVersionNumber());
    }

    Instant receiveInstant = instantSource.instant();
    response.setReceiveTimestamp(Timestamp64.fromInstant(receiveInstant));

    simulateElapsedTime(processingDuration);
    response.setTransmitTimestamp(Timestamp64.fromInstant(instantSource.getCurrentInstant()));

    Timestamp64 originateTimestamp;
    if ((quirkMode & QUIRK_MODE_NON_MATCHING_ORIGINATE_TIME) == 0) {
      originateTimestamp = request.getTransmitTimestamp();
    } else {
      originateTimestamp =
          request.getTransmitTimestamp().randomizeSubMillis(PlatformRandom.getDefaultRandom());
    }
    response.setOriginateTimestamp(originateTimestamp);

    responsesSent.add(response);

    return response;
  }

  @Override
  public int numRequestsReceived() {
    return requestsReceived.size();
  }

  private static NtpMessage createDefaultResponseTemplate() {
    NtpMessage responseTemplate = NtpMessage.createEmptyV3();
    responseTemplate.setReferenceIdentifierAsString("TEST");
    responseTemplate.setMode(NtpMessage.NTP_MODE_SERVER);
    responseTemplate.setPrecisionExponent(-10);
    responseTemplate.setPollIntervalExponent(3);
    return responseTemplate;
  }

  private static NtpMessage createResponse(
      NtpMessage responseTemplate, InetAddress serverAddress, int serverPort) {
    return NtpMessage.fromBytesForTests(responseTemplate.toByteArray(), serverAddress, serverPort);
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
