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
import java.util.Vector;

/** Fake NTP server logic that behaves like a real one with configurable quirks. */
public final class FakeSntpServerEngine implements TestSntpServerEngine {

  public static final int QUIRK_MODE_NON_MATCHING_ORIGINATE_TIME = 0x1;
  public static final int QUIRK_MODE_DO_NOT_MATCH_REQUEST_PROTOCOL_VERSION = 0x2;
  public static final int QUIRK_MODE_ZERO_TRANSMIT_TIMESTAMP = 0x4;

  private final List<NtpMessage> requestsReceived = new ArrayList<>();
  private final List<NtpMessage> responsesSent = new ArrayList<>();
  private final List<Advanceable> advanceables = new ArrayList<>();
  private final FakeClocks.FakeInstantSource instantSource;

  private Vector<NtpMessage> responseTemplates = new Vector<>();
  /** The template used after {@link #responseTemplates} is exhausted. */
  private NtpMessage lastResponseTemplate = createDefaultResponseTemplate();

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

  public void setResponseTemplate(int startInc, int endExcl, NtpMessage responseTemplate) {
    if (startInc >= endExcl) {
      throw new IllegalArgumentException("Bad range=[" + startInc + ", " + endExcl + ")");
    }

    if (responseTemplates.size() < endExcl) {
      responseTemplates.setSize(endExcl);
    }

    for (int i = startInc; i < endExcl; i++) {
      responseTemplates.set(i, responseTemplate);
    }
  }

  public NtpMessage getLastResponseTemplate() {
    return lastResponseTemplate;
  }

  public void setLastResponseTemplate(NtpMessage lastResponseTemplate) {
    this.lastResponseTemplate = Objects.requireNonNull(lastResponseTemplate);
  }

  @Override
  public NtpMessage processRequest(NtpMessage request) {
    NtpMessage responseTemplate;
    int templateIndex = requestsReceived.size();
    if (templateIndex >= responseTemplates.size()) {
      responseTemplate = lastResponseTemplate;
    } else {
      responseTemplate = responseTemplates.get(templateIndex);
      if (responseTemplate == null) {
        throw new IllegalStateException(
            "No response template set for"
                + " templateIndex="
                + templateIndex
                + ", templates="
                + responseTemplates);
      }
    }

    requestsReceived.add(request);
    final NtpHeader requestHeader = request.getHeader();

    NtpHeader.Builder responseHeaderBuilder = responseTemplate.getHeader().toBuilder();
    if ((quirkMode & QUIRK_MODE_DO_NOT_MATCH_REQUEST_PROTOCOL_VERSION) == 0) {
      responseHeaderBuilder.setVersionNumber(requestHeader.getVersionNumber());
    }

    Instant receiveInstant = instantSource.instant();
    responseHeaderBuilder.setReceiveTimestamp(Timestamp64.fromInstant(receiveInstant));

    simulateElapsedTime(processingDuration);
    Timestamp64 transmitTimestamp;
    if ((quirkMode & QUIRK_MODE_ZERO_TRANSMIT_TIMESTAMP) == 0) {
      transmitTimestamp = Timestamp64.fromInstant(instantSource.getCurrentInstant());
    } else {
      transmitTimestamp = Timestamp64.ZERO;
    }
    responseHeaderBuilder.setTransmitTimestamp(transmitTimestamp);

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
    // Zero would indicate the server has never synchronized, which would be broken. This value
    // is arbitrary, just non-zero.
    Timestamp64 validReferenceTimestamp = Timestamp64.fromInstant(Instant.ofEpochMilli(1234));
    NtpHeader header =
        NtpHeader.Builder.createEmptyV3()
            .setStratum(1)
            .setReferenceIdentifierAsString("TEST")
            .setMode(NtpHeader.NTP_MODE_SERVER)
            .setPrecisionExponent(-10)
            .setPollIntervalExponent(3)
            .setReferenceTimestamp(validReferenceTimestamp)
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
