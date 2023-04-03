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

import static com.google.time.client.base.testing.Bytes.bytes;
import static com.google.time.client.base.testing.DateTimeUtils.utc;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.testing.FakeClocks;
import com.google.time.client.sntp.SntpTimeSignal;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SntpTimeSignalImplTest {

  @Test
  public void basicBehavior() throws Exception {
    NtpHeader.Builder responseHeaderBuilder = NtpHeader.Builder.createEmptyV3();
    // This instant value has been chosen because it can be represented as an NTP type exactly.
    Instant referenceTimestampAsInstant = utc(2020, 1, 2, 3, 4, 5, 500_000_000);
    Timestamp64 referenceTimestamp = Timestamp64.fromInstant(referenceTimestampAsInstant);
    responseHeaderBuilder.setReferenceTimestamp(referenceTimestamp);

    Duration roundtripDuration = Duration.ofSeconds(1, 0);
    Duration totalTransactionDuration = Duration.ofSeconds(2, 0);
    Instant responseInstant = Instant.ofEpochMilli(1234);
    Duration clientOffset = Duration.ofSeconds(3, 0);
    FakeClocks fakeClocks = new FakeClocks();
    Ticks resultTicks = fakeClocks.getFakeTicker().ticks();
    InstantSource instantSource = fakeClocks.getFakeInstantSource();
    Instant resultInstant = instantSource.instant();
    InetSocketAddress serverSocketAddress = new InetSocketAddress(createInetAddress("test"), 11111);
    NtpMessage response = NtpMessage.create(responseHeaderBuilder.build());
    SntpTimeSignalImpl result =
        new SntpTimeSignalImpl(
            serverSocketAddress,
            response,
            roundtripDuration,
            totalTransactionDuration,
            responseInstant,
            clientOffset,
            resultTicks,
            instantSource,
            resultInstant);

    assertEquals(serverSocketAddress.getAddress(), result.getServerInetAddress());
    assertEquals(serverSocketAddress.getPort(), result.getServerPort());

    // Test accessors that don't just delegate through to the NTP response.
    assertEquals(roundtripDuration, result.getRoundTripDuration());
    assertEquals(totalTransactionDuration, result.getTotalTransactionDuration());

    // Reference timestamp has some special behavior.
    assertEquals(referenceTimestamp, result.getReferenceTimestamp());
    assertEquals(referenceTimestamp.toInstant(0), result.getReferenceTimestampAsInstant());
    assertEquals(
        referenceTimestamp.toInstant(0),
        result.getReferenceTimestampAsInstant(SntpTimeSignal.DEFAULT_ERA_THRESHOLD));

    // When the reference timestamp < threshold, the timestamp is interpreted as being from the next
    // NTP era.
    Instant alternativeThreshold = utc(2021, 1, 2, 3, 4, 5, 500_000_000);
    assertEquals(
        referenceTimestamp.toInstant(1),
        result.getReferenceTimestampAsInstant(alternativeThreshold));

    assertEquals(clientOffset, result.getClientOffset());
    assertEquals(resultTicks, result.getResultTicks());
    assertSame(instantSource, result.getInstantSource());
    assertEquals(resultInstant, result.getResultInstant());

    NtpHeader responseHeader = response.getHeader();
    assertEquals(responseHeader.getVersionNumber(), result.getResponseVersion());
    assertEquals(responseHeader.getStratum(), result.getStratum());
  }

  private static InetAddress createInetAddress(String name) {
    try {
      return Inet4Address.getByAddress(name, bytes(192, 168, 0, 0));
    } catch (UnknownHostException e) {
      throw new RuntimeException("Unable to create address", e);
    }
  }
}
