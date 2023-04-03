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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.google.time.client.base.Duration;
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.testing.FakeClocks;
import com.google.time.client.base.testing.PredictableRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SntpRequestFactoryTest {

  private static final int SNTP_CLIENT_VERSION = 3;

  private PredictableRandom random;
  private FakeClocks.FakeInstantSource instantSource;

  @Before
  public void setUp() {
    random = new PredictableRandom(1234);
    FakeClocks fakeClocks = new FakeClocks();
    instantSource = fakeClocks.getFakeInstantSource();
    instantSource.setEpochMillis(1_234_567_891L);
  }

  @Test
  public void get_clientDataMinimization() {
    boolean clientDataMinimization = true;
    SntpRequestFactory requestFactory =
        new SntpRequestFactory(instantSource, random, SNTP_CLIENT_VERSION, clientDataMinimization);
    NtpMessage requestMessage = requestFactory.get();
    assertDefaultRequestFields(requestMessage);

    // These are the important properties.
    assertEquals(NtpHeader.NTP_MODE_CLIENT, requestMessage.getHeader().getMode());
    assertEquals(SNTP_CLIENT_VERSION, requestMessage.getHeader().getVersionNumber());

    // Check for randomization of the transmit timestamp.
    Timestamp64 expectedTransmitTimestamp =
        Timestamp64.fromComponents(random.nextInt() & 0xFFFF_FFFFL, random.nextInt());
    assertEquals(expectedTransmitTimestamp, requestMessage.getHeader().getTransmitTimestamp());
  }

  @Test
  public void get_noClientDataMinimization_millisInstantSource() {
    instantSource.setPrecision(InstantSource.PRECISION_MILLIS);
    instantSource.setEpochMillis(1_234_567_891L);

    boolean clientDataMinimization = false;
    SntpRequestFactory requestFactory =
        new SntpRequestFactory(instantSource, random, SNTP_CLIENT_VERSION, clientDataMinimization);
    NtpMessage requestMessage = requestFactory.get();
    assertDefaultRequestFields(requestMessage);

    // These are the important properties.
    assertEquals(NtpHeader.NTP_MODE_CLIENT, requestMessage.getHeader().getMode());
    assertEquals(SNTP_CLIENT_VERSION, requestMessage.getHeader().getVersionNumber());

    // Check for randomization of millis-resolution transmit timestamp.
    Timestamp64 actualTime = Timestamp64.fromInstant(instantSource.instant());
    Timestamp64 expectedTransmitTimestamp = actualTime.randomizeSubMillis(random);
    assertNotEquals(actualTime, expectedTransmitTimestamp);
    assertEquals(expectedTransmitTimestamp, requestMessage.getHeader().getTransmitTimestamp());
  }

  @Test
  public void createSntpRequest_noClientDataMinimization_nanosInstantSource() {
    instantSource.setPrecision(InstantSource.PRECISION_NANOS);
    instantSource.setEpochNanos(1_234_567_891L);

    boolean clientDataMinimization = false;
    SntpRequestFactory requestFactory =
        new SntpRequestFactory(instantSource, random, SNTP_CLIENT_VERSION, clientDataMinimization);
    NtpMessage requestMessage = requestFactory.get();
    assertDefaultRequestFields(requestMessage);

    // These are the important properties.
    assertEquals(NtpHeader.NTP_MODE_CLIENT, requestMessage.getHeader().getMode());
    assertEquals(SNTP_CLIENT_VERSION, requestMessage.getHeader().getVersionNumber());

    // Check for no randomization of nano-resolution transmit timestamp.
    Timestamp64 actualTime = Timestamp64.fromInstant(instantSource.instant());
    assertEquals(requestMessage.getHeader().getTransmitTimestamp(), actualTime);
  }

  private static void assertDefaultRequestFields(NtpMessage ntpMessage) {
    NtpHeader ntpHeader = ntpMessage.getHeader();
    assertEquals(0, ntpHeader.getLeapIndicator());
    assertEquals(0, ntpHeader.getPrecisionExponent());
    assertEquals(Duration.ZERO, ntpHeader.getRootDelayDuration());
    assertEquals(Duration.ZERO, ntpHeader.getRootDispersionDuration());
    assertEquals("", ntpHeader.getReferenceIdentifierAsString());
    assertEquals(Timestamp64.ZERO, ntpHeader.getReferenceTimestamp());
    assertEquals(Timestamp64.ZERO, ntpHeader.getOriginateTimestamp());
    assertEquals(Timestamp64.ZERO, ntpHeader.getReceiveTimestamp());
  }
}
