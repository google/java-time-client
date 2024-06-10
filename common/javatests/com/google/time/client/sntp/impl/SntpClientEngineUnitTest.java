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

import static com.google.common.truth.Truth.assertThat;
import static com.google.time.client.base.impl.DateTimeConstants.MILLISECONDS_PER_SECOND;
import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_MILLISECOND;
import static com.google.time.client.base.testing.Bytes.bytes;
import static com.google.time.client.base.testing.DateTimeUtils.utc;
import static com.google.time.client.sntp.impl.NtpHeader.NTP_MODE_SERVER;
import static org.junit.Assert.assertEquals;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.testing.FakeClocks;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link SntpClientEngine}. */
@RunWith(JUnit4.class)
public class SntpClientEngineUnitTest {

  // From tcpdump (admittedly, an NTPv4 packet):
  //
  // Server, Leap indicator:  (0), Stratum 2 (secondary reference), poll 6 (64s), precision -20
  // Root Delay: 0.005447, Root dispersion: 0.002716, Reference-ID: 221.253.71.41
  //   Reference Timestamp:
  //     d9ca9446.820a5000 / ERA0: 2015-10-15 21:08:22 UTC / ERA1: 2151-11-22 03:36:38 UTC
  //   Originator Timestamp:
  //     d9ca9451.938a3771 / ERA0: 2015-10-15 21:08:33 UTC / ERA1: 2151-11-22 03:36:49 UTC
  //   Receive Timestamp:
  //     d9ca9451.94bd3fff / ERA0: 2015-10-15 21:08:33 UTC / ERA1: 2151-11-22 03:36:49 UTC
  //   Transmit Timestamp:
  //     d9ca9451.94bd4001 / ERA0: 2015-10-15 21:08:33 UTC / ERA1: 2151-11-22 03:36:49 UTC
  //
  //     Originator - Receive Timestamp:  +0.004684958
  //     Originator - Transmit Timestamp: +0.004684958
  private static final NtpHeader LATE_ERA_RESPONSE_HEADER =
      NtpHeader.Builder.createEmptyV3()
          .setLeapIndicator(0)
          .setMode(NTP_MODE_SERVER)
          .setStratum(2)
          .setPollIntervalExponent(6)
          .setPrecisionExponent(-20)
          .setRootDelayDuration(Duration.ofNanos(5447000))
          .setRootDispersionDuration(Duration.ofNanos(2716000))
          .setReferenceIdentifier(bytes(221, 253, 71, 41))
          .setReferenceTimestamp(Timestamp64.fromString("d9ca9451.820a5000"))
          .setOriginateTimestamp(Timestamp64.fromString("d9ca9451.938a3771"))
          .setReceiveTimestamp(Timestamp64.fromString("d9ca9451.94bd3fff"))
          .setTransmitTimestamp(Timestamp64.fromString("d9ca9451.94bd4001"))
          .build();

  private static final Timestamp64 LATE_ERA_RECEIVE_TIMESTAMP =
      LATE_ERA_RESPONSE_HEADER.getReceiveTimestamp();
  private static final Timestamp64 LATE_ERA_TRANSMIT_TIMESTAMP =
      LATE_ERA_RESPONSE_HEADER.getTransmitTimestamp();

  /** This is the actual UTC time in the server if it is in ERA0 */
  private static final Instant LATE_ERA0_SERVER_INSTANT =
      calculateIdealServerTime(LATE_ERA_RECEIVE_TIMESTAMP, LATE_ERA_TRANSMIT_TIMESTAMP, 0);

  /**
   * This is the Unix epoch time matches the originator timestamp from {@link
   * #LATE_ERA_RESPONSE_HEADER} when interpreted as an ERA0 timestamp.
   */
  private static final Instant LATE_ERA0_REQUEST_INSTANT =
      LATE_ERA_RESPONSE_HEADER.getOriginateTimestamp().toInstant(0);

  // A tweaked version of the ERA0 response to represent an ERA 1 response.
  //
  // Server, Leap indicator:  (0), Stratum 2 (secondary reference), poll 6 (64s), precision -20
  // Root Delay: 0.005447, Root dispersion: 0.002716, Reference-ID: 221.253.71.41
  //   Reference Timestamp:
  //     1db2d246.820a5000 / ERA0: 1915-10-16 21:08:22 UTC / ERA1: 2051-11-22 03:36:38 UTC
  //   Originator Timestamp:
  //     1db2d251.938a3771 / ERA0: 1915-10-16 21:08:33 UTC / ERA1: 2051-11-22 03:36:49 UTC
  //   Receive Timestamp:
  //     1db2d251.94bd3fff / ERA0: 1915-10-16 21:08:33 UTC / ERA1: 2051-11-22 03:36:49 UTC
  //   Transmit Timestamp:
  //     1db2d251.94bd4001 / ERA0: 1915-10-16 21:08:33 UTC / ERA1: 2051-11-22 03:36:49 UTC
  //
  //     Originator - Receive Timestamp:  +0.004684958
  //     Originator - Transmit Timestamp: +0.004684958
  private static final NtpHeader EARLY_ERA_RESPONSE_HEADER =
      NtpHeader.Builder.createEmptyV3()
          .setLeapIndicator(0)
          .setMode(NTP_MODE_SERVER)
          .setStratum(2)
          .setPollIntervalExponent(6)
          .setPrecisionExponent(-20)
          .setRootDelayDuration(Duration.ofNanos(5447000))
          .setRootDispersionDuration(Duration.ofNanos(2716000))
          .setReferenceIdentifier(bytes(221, 253, 71, 41))
          .setReferenceTimestamp(Timestamp64.fromString("1db2d246.820a5000"))
          .setOriginateTimestamp(Timestamp64.fromString("1db2d251.938a3771"))
          .setReceiveTimestamp(Timestamp64.fromString("1db2d251.94bd3fff"))
          .setTransmitTimestamp(Timestamp64.fromString("1db2d251.94bd4001"))
          .build();

  /** This is the actual UTC time in the server if it is in ERA0 */
  private static final Timestamp64 EARLY_ERA_RECEIVE_TIMESTAMP =
      EARLY_ERA_RESPONSE_HEADER.getReceiveTimestamp();

  private static final Timestamp64 EARLY_ERA_TRANSMIT_TIMESTAMP =
      EARLY_ERA_RESPONSE_HEADER.getTransmitTimestamp();

  /** This is the actual UTC time in the server if it is in ERA0 */
  private static final Instant EARLY_ERA1_SERVER_INSTANT =
      calculateIdealServerTime(EARLY_ERA_RECEIVE_TIMESTAMP, EARLY_ERA_TRANSMIT_TIMESTAMP, 1);

  /**
   * This is the Unix epoch time matches the originator timestamp from {@link
   * #EARLY_ERA_RESPONSE_HEADER} when interpreted as an ERA1 timestamp.
   */
  private static final Instant EARLY_ERA1_REQUEST_INSTANT =
      EARLY_ERA_RESPONSE_HEADER.getOriginateTimestamp().toInstant(1);

  /*
   * Sample output from an sntp command:
   *
   *   $ sntp -D 1000 216.239.35.12
   *   init_lib() done, ipv4_works ipv6_works
   *   sntp 4.2.8p15@1.3728-o Wed Sep 23 11:46:38 UTC 2020 (1)
   *   Initializing KOD DB...
   *   Starting to read KoD file /var/lib/sntp/kod...
   *   KoD DB /var/lib/sntp/kod empty.
   *   Can't open KOD db file /var/lib/sntp/kod for writing: Permission denied
   *   sntp auth_init: Couldn't open key file /etc/ntp.keys for reading!
   *   handle_lookup(216.239.35.12,0x2)
   *   move_fd: estimated max descriptors: 131072, initial socket boundary: 16
   *   blocking_getaddrinfo given node 216.239.35.12 serv 123 fam 0 flags 402
   *   216.239.35.12 []
   *   check_kod: checking <216.239.35.12>
   *   queue_xmt: xmt timer for 0 usec
   *   1 NTP and 0 name queries pending
   *   xmt_timer_cb: at .296433 -> 216.239.35.12
   *   generate_pkt: key_id -1, key pointer (nil)
   *   sntp sendpkt: Packet data:
   *   --------------------------------------------------------------------------------
   *     0: e3    1: 00    2: 08    3: 00    4: 00    5: 00    6: 00    7: 00
   *     8: 00    9: 00   10: 00   11: 00   12: 00   13: 00   14: 00   15: 00
   *    16: 00   17: 00   18: 00   19: 00   20: 00   21: 00   22: 00   23: 00
   *    24: 00   25: 00   26: 00   27: 00   28: 00   29: 00   30: 00   31: 00
   *    32: 00   33: 00   34: 00   35: 00   36: 00   37: 00   38: 00   39: 00
   *    40: e4   41: dc   42: 72   43: 0c   44: 4c   45: 00   46: 64   47: aa
   *   --------------------------------------------------------------------------------
   *   sntp sendpkt: Sending packet to 216.239.35.12:123 ...
   *   Packet sent.
   *   xmt: e4dc720c.296881 216.239.35.12 216.239.35.12
   *   sock_cb: event on sock4: read
   *   Received 48 bytes from 216.239.35.12:123:
   *   --------------------------------------------------------------------------------
   *     0: 24    1: 01    2: 08    3: ec    4: 00    5: 00    6: 00    7: 00
   *     8: 00    9: 00   10: 00   11: 07   12: 47   13: 4f   14: 4f   15: 47
   *    16: e4   17: dc   18: 72   19: 0c   20: 4d   21: 4f   22: c9   23: eb
   *    24: e4   25: dc   26: 72   27: 0c   28: 4c   29: 00   30: 64   31: aa
   *    32: e4   33: dc   34: 72   35: 0c   36: 4d   37: 4f   38: c9   39: ec
   *    40: e4   41: dc   42: 72   43: 0c   44: 4d   45: 4f   46: c9   47: ee
   *   --------------------------------------------------------------------------------
   *   sock_cb: 216.239.35.12 216.239.35.12:123
   *   sock_cb: process_pkt returned 48
   *   handle_pkt: 48 bytes from 216.239.35.12 216.239.35.12
   *   offset_calculation: LOGTOD(rpkt->precision): 0.000001
   *   sntp rootdelay: 0.000000
   *   sntp rootdisp: 0.000107
   *   sntp syncdist: 0.000561
   *   --------------------------------------------------------------------------------
   *     0: 24    1: 01    2: 08    3: ec    4: 00    5: 00    6: 00    7: 00
   *     8: 00    9: 00   10: 00   11: 07   12: 47   13: 4f   14: 4f   15: 47
   *    16: e4   17: dc   18: 72   19: 0c   20: 4d   21: 4f   22: c9   23: eb
   *    24: e4   25: dc   26: 72   27: 0c   28: 4c   29: 00   30: 64   31: aa
   *    32: e4   33: dc   34: 72   35: 0c   36: 4d   37: 4f   38: c9   39: ec
   *    40: e4   41: dc   42: 72   43: 0c   44: 4d   45: 4f   46: c9   47: ee
   *   --------------------------------------------------------------------------------
   *   sntp offset_calculation: rpkt->reftime:
   *   e4dc720c.4d4fc9eb  Fri, Sep  3 2021 11:06:04.301
   *   sntp offset_calculation: rpkt->org:
   *   e4dc720c.4c0064aa  Fri, Sep  3 2021 11:06:04.296
   *   sntp offset_calculation: rpkt->rec:
   *   e4dc720c.4d4fc9ec  Fri, Sep  3 2021 11:06:04.301
   *   sntp offset_calculation: rpkt->xmt:
   *   e4dc720c.4d4fc9ee  Fri, Sep  3 2021 11:06:04.301
   *   sntp offset_calculation:	rec - org t21: 0.005118
   *     xmt - dst t34: -0.006434	delta: 0.011552	offset: -0.000658
   *   2021-09-03 11:06:04.308433 (+0000) -0.000658 +/- 0.000561 216.239.35.12 s1 no-leap
   */
  @Test
  public void performNtpCalculations() throws Exception {
    FakeClocks fakeClocks = new FakeClocks();
    FakeClocks.FakeTicker ticker = fakeClocks.getFakeTicker();

    Timestamp64 requestTimestamp = Timestamp64.fromString("e4dc720c.4c0064aa");

    NtpHeader requestHeader =
        NtpHeader.Builder.createEmptyV3().setTransmitTimestamp(requestTimestamp).build();
    NtpMessage request = NtpMessage.create(requestHeader);

    Instant requestInstant = requestTimestamp.toInstant(0);
    Ticks requestTimeTicks = ticker.ticksForValue(296881000);
    Ticks responseTimeTicks = ticker.ticksForValue(308432730);

    NtpHeader responseHeader =
        NtpHeader.Builder.createEmptyV3()
            .setVersionNumber(4)
            .setMode(NTP_MODE_SERVER)
            .setLeapIndicator(0)
            .setStratum(1)
            .setReferenceIdentifierAsString("GOOG")
            .setPollIntervalExponent(8)
            .setPrecisionExponent(-20)
            .setRootDelay(bytes(0, 0, 0, 0))
            .setRootDispersion(bytes(0, 0, 0, 7))
            .setOriginateTimestamp(requestTimestamp)
            .setTransmitTimestamp(Timestamp64.fromString("e4dc720c.4d4fc9ee"))
            .setReceiveTimestamp(Timestamp64.fromString("e4dc720c.4d4fc9ec"))
            .setReferenceTimestamp(Timestamp64.fromString("e4dc720c.4d4fc9eb"))
            .build();
    NtpMessage response = NtpMessage.create(responseHeader);

    InetSocketAddress serverSocketAddress = createSocketAddress();
    SntpQueryOperation.SuccessResult sntpSuccessResult =
        new SntpQueryOperation.SuccessResult(
            serverSocketAddress,
            fakeClocks.getFakeInstantSource(),
            requestInstant,
            requestTimeTicks,
            responseTimeTicks,
            request,
            response);
    SntpTimeSignalImpl sntpTimeSignal =
        SntpClientEngine.performNtpCalculations(sntpSuccessResult, response);

    assertEquals(serverSocketAddress.getAddress(), sntpTimeSignal.getServerInetAddress());
    // offset_calculation: LOGTOD(rpkt->precision): 0.000001
    // sntp rootdelay: 0.000000
    // sntp rootdisp: 0.000107
    assertEquals(responseHeader.getPrecisionExponent(), sntpTimeSignal.getPrecisionExponent(), 0);
    assertEquals(responseHeader.getRootDelayDuration(), sntpTimeSignal.getRootDelayDuration());
    assertEquals(
        responseHeader.getRootDispersionDuration(), sntpTimeSignal.getRootDispersionDuration());

    // 2021-09-03 11:06:04.308433 (+0000) -0.000658 +/- 0.000561 216.239.35.12 s1 no-leap
    Instant expectedResponseTime =
        requestInstant.plus(requestTimeTicks.durationUntil(responseTimeTicks));
    assertEquals(expectedResponseTime, sntpTimeSignal.getResponseInstant());
    Duration expectedClockOffset = Duration.ofNanos(-658_135);
    assertEquals(expectedClockOffset, sntpTimeSignal.getClientOffset());

    // The actual result with the offset applied.
    assertEquals(responseTimeTicks, sntpTimeSignal.getResultTicks());
    assertEquals(expectedResponseTime.plus(expectedClockOffset), sntpTimeSignal.getResultInstant());
  }

  /**
   * Unit tests for the low-level offset calculations. More targeted / easier to write than the
   * end-to-end tests above that simulate the server. b/199481251.
   */
  @Test
  public void calculateClientOffset() {
    Instant era0Time1 = utc(2021, 10, 5, 2, 2, 2, 2);
    // Confirm what happens when the client and server are completely in sync.
    checkCalculateClientOffset(era0Time1, era0Time1);

    Instant era0Time2 = utc(2021, 10, 6, 1, 1, 1, 1);
    checkCalculateClientOffset(era0Time1, era0Time2);
    checkCalculateClientOffset(era0Time2, era0Time1);

    Instant era1Time1 = utc(2061, 10, 5, 2, 2, 2, 2);
    checkCalculateClientOffset(era1Time1, era1Time1);

    Instant era1Time2 = utc(2061, 10, 6, 1, 1, 1, 1);
    checkCalculateClientOffset(era1Time1, era1Time2);
    checkCalculateClientOffset(era1Time2, era1Time1);

    // Cross-era calcs (requires they are still within 68 years of each other).
    checkCalculateClientOffset(era0Time1, era1Time1);
    checkCalculateClientOffset(era1Time1, era0Time1);
  }

  private void checkCalculateClientOffset(Instant clientTime, Instant serverTime) {
    // The expected (ideal) offset is the difference between the client and server clocks. NTP
    // assumes delays are symmetric, i.e. that the server time is between server
    // receive/transmit time, client time is between request/response time, and send networking
    // delay == receive networking delay.
    Duration expectedOffset = Duration.between(clientTime, serverTime);

    // Try simulating various round trip delays, including zero.
    for (long totalElapsedTimeMillis : Arrays.asList(0, 20, 200, 2000, 20000)) {
      // Simulate that a 10% of the elapsed time is due to time spent in the server, the rest
      // is network / client processing time.
      long simulatedServerElapsedTimeMillis = totalElapsedTimeMillis / 10;
      long simulatedClientElapsedTimeMillis = totalElapsedTimeMillis;

      // Create some symmetrical timestamps.
      Timestamp64 clientRequestTimestamp =
          Timestamp64.fromInstant(
              adjustedInstant(clientTime, -(simulatedClientElapsedTimeMillis / 2)));
      Timestamp64 clientResponseTimestamp =
          Timestamp64.fromInstant(
              adjustedInstant(clientTime, simulatedClientElapsedTimeMillis / 2));
      Timestamp64 serverReceiveTimestamp =
          Timestamp64.fromInstant(
              adjustedInstant(serverTime, -(simulatedServerElapsedTimeMillis / 2)));
      Timestamp64 serverTransmitTimestamp =
          Timestamp64.fromInstant(
              adjustedInstant(serverTime, simulatedServerElapsedTimeMillis / 2));

      Duration actualOffset =
          SntpClientEngine.calculateClientOffset(
              clientRequestTimestamp, serverReceiveTimestamp,
              serverTransmitTimestamp, clientResponseTimestamp);

      // We allow up to 1ns variation because types used for NTP calcs are lossy and the types used
      // to calculate expectedOffset are not.
      assertAlmostEquals(expectedOffset, actualOffset);
    }
  }

  @Test
  public void calculateClientOffset_clientEra0_serverEra0() {
    Instant clientTime = LATE_ERA0_REQUEST_INSTANT;
    Timestamp64 clientRequestTimestamp = Timestamp64.fromInstant(clientTime);

    Timestamp64 receiveTimestamp = LATE_ERA_RECEIVE_TIMESTAMP;
    Timestamp64 transmitTimestamp = LATE_ERA_TRANSMIT_TIMESTAMP;
    Instant serverTime = LATE_ERA0_SERVER_INSTANT;

    Duration totalSessionResultDuration = Duration.ofSeconds(10, 0); // Shouldn't matter

    checkClientOffsetMath(
        receiveTimestamp,
        transmitTimestamp,
        clientTime,
        serverTime,
        clientRequestTimestamp,
        totalSessionResultDuration);
  }

  @Test
  public void calculateClientOffset_clientEra1_serverEra1() {
    Instant clientTime = EARLY_ERA1_REQUEST_INSTANT;
    Timestamp64 clientRequestTimestamp = Timestamp64.fromInstant(clientTime);

    Timestamp64 serverReceiveTimestamp = EARLY_ERA_RECEIVE_TIMESTAMP;
    Timestamp64 serverTransmitTimestamp = EARLY_ERA_TRANSMIT_TIMESTAMP;
    Instant serverTime = EARLY_ERA1_SERVER_INSTANT;

    Duration totalsessionResultDuration = Duration.ofSeconds(10, 0); // Shouldn't matter

    checkClientOffsetMath(
        serverReceiveTimestamp,
        serverTransmitTimestamp,
        clientTime,
        serverTime,
        clientRequestTimestamp,
        totalsessionResultDuration);
  }

  @Test
  public void calculateClientOffset_clientEra0_serverEra1() {
    Instant clientTime = LATE_ERA0_REQUEST_INSTANT;
    Timestamp64 clientRequestTimestamp = Timestamp64.fromInstant(clientTime);

    Timestamp64 serverReceiveTimestamp = EARLY_ERA_RECEIVE_TIMESTAMP;
    Timestamp64 serverTransmitTimestamp = EARLY_ERA_TRANSMIT_TIMESTAMP;
    Instant serverTime = EARLY_ERA1_SERVER_INSTANT;

    Duration totalsessionResultDuration = Duration.ofSeconds(10, 0); // Shouldn't matter

    checkClientOffsetMath(
        serverReceiveTimestamp,
        serverTransmitTimestamp,
        clientTime,
        serverTime,
        clientRequestTimestamp,
        totalsessionResultDuration);
  }

  @Test
  public void calculateClientOffset_clientEra1_serverEra0() {
    Instant clientTime = EARLY_ERA1_REQUEST_INSTANT;
    Timestamp64 clientRequestTimestamp = Timestamp64.fromInstant(clientTime);

    Timestamp64 serverReceiveTimestamp = LATE_ERA_RECEIVE_TIMESTAMP;
    Timestamp64 serverTransmitTimestamp = LATE_ERA_TRANSMIT_TIMESTAMP;
    Instant serverTime = LATE_ERA0_SERVER_INSTANT;

    Duration totalsessionResultDuration = Duration.ofSeconds(10, 0); // Shouldn't matter

    checkClientOffsetMath(
        serverReceiveTimestamp,
        serverTransmitTimestamp,
        clientTime,
        serverTime,
        clientRequestTimestamp,
        totalsessionResultDuration);
  }

  private static void checkClientOffsetMath(
      Timestamp64 serverReceiveTimestamp,
      Timestamp64 serverTransmitTimestamp,
      Instant clientTime,
      Instant serverTime,
      Timestamp64 clientRequestTimestamp,
      Duration totalsessionResultDuration) {
    final Instant clientResponseTime = clientTime.plus(totalsessionResultDuration);
    final Timestamp64 clientResponseTimestamp = Timestamp64.fromInstant(clientResponseTime);

    Duration clientOffsetDuration =
        SntpClientEngine.calculateClientOffset(
            clientRequestTimestamp, serverReceiveTimestamp,
            serverTransmitTimestamp, clientResponseTimestamp);

    // The offset NTP calculates should be the difference between the server and client times
    // plus half of the sessionResult time.
    //
    // The expectedOffset is calculated using Duration throughout whereas the real calculation uses
    // Duration64 for some of it, so the calculation can produce an answer that is up to 1ns
    // different.
    Duration expectedOffset =
        Duration.between(clientTime, serverTime).minus(totalsessionResultDuration.dividedBy(2));
    assertAlmostEquals(expectedOffset, clientOffsetDuration);
  }

  /**
   * Generates the "real" server time assuming it is exactly between the receive and transmit
   * timestamp and in the NTP era specified.
   */
  private static Instant calculateIdealServerTime(
      Timestamp64 receiveTimestamp, Timestamp64 transmitTimestamp, int era) {
    Duration serverProcessingTime =
        Duration64.between(receiveTimestamp, transmitTimestamp).toDuration();
    return receiveTimestamp.toInstant(era).plus(serverProcessingTime.dividedBy(2));
  }

  private static void assertAlmostEquals(Duration expected, Duration actual) {
    long actualNanos = actual.toNanos();
    long expectedNanos = expected.toNanos();
    assertThat(actualNanos).isAnyOf(expectedNanos, expectedNanos - 1, expectedNanos + 1);
  }

  private static Instant adjustedInstant(Instant instant, long adjustmentMillis) {
    return instant.plus(millisDuration(adjustmentMillis));
  }

  /**
   * The java-time-client Duration doesn't support ofMillis() and we shouldn't add one just for
   * tests.
   */
  private static Duration millisDuration(long millis) {
    return Duration.ofSeconds(
        millis / MILLISECONDS_PER_SECOND,
        (millis % MILLISECONDS_PER_SECOND) * NANOS_PER_MILLISECOND);
  }

  private static InetSocketAddress createSocketAddress() throws UnknownHostException {
    return new InetSocketAddress(Inet4Address.getByAddress(bytes(216, 239, 35, 12)), 123);
  }
}
