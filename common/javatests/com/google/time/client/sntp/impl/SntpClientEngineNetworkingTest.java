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
import static com.google.time.client.sntp.testing.FakeNetwork.FAILURE_MODE_RECEIVE;
import static com.google.time.client.sntp.testing.FakeNetwork.FAILURE_MODE_SEND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.base.NetworkOperationResult;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.impl.NoOpLogger;
import com.google.time.client.base.testing.FakeClocks;
import com.google.time.client.base.testing.FakeInstantSource;
import com.google.time.client.base.testing.FakeTicker;
import com.google.time.client.sntp.NtpProtocolException;
import com.google.time.client.sntp.NtpServerNotReachableException;
import com.google.time.client.sntp.SntpQueryDebugInfo;
import com.google.time.client.sntp.SntpQueryResult;
import com.google.time.client.sntp.SntpTimeSignal;
import com.google.time.client.sntp.testing.FakeNetwork;
import com.google.time.client.sntp.testing.FakeSntpServerEngine;
import com.google.time.client.sntp.testing.TestSntpServerWithNetwork;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link SntpClientEngine} networking. */
@RunWith(JUnit4.class)
public class SntpClientEngineNetworkingTest {

  @Test
  public void executeQuery_sendFailure() throws Exception {
    testNetworkingFailure(FAILURE_MODE_SEND);
  }

  @Test
  public void executeQuery_receiveFailure() throws Exception {
    testNetworkingFailure(FAILURE_MODE_RECEIVE);
  }

  private void testNetworkingFailure(int failureModeFlag) throws Exception {
    FakeClocks fakeClientClocks = new FakeClocks();
    FakeClocks fakeServerClocks = new FakeClocks();
    // Use a fake network that can simulate failures.
    TestSntpServerWithNetwork<FakeSntpServerEngine, FakeNetwork> testSntpServerWithNetwork =
        TestSntpServerWithNetwork.createCompleteFake(fakeClientClocks, fakeServerClocks);

    String serverHostName = testSntpServerWithNetwork.getServerAddress().getName();
    FakeNetwork fakeNetwork = testSntpServerWithNetwork.getNetwork();
    fakeNetwork.setFailureMode(failureModeFlag);
    // Give the server address a few IP addresses to try.
    for (int i = 0; i < 5; i++) {
      fakeNetwork.addServerIpAddress(serverHostName);
    }

    Duration timeAllowed = null;
    SntpClientEngine engine =
        new SntpClientEngine(
            NoOpLogger.instance(),
            testSntpServerWithNetwork.createConnector(
                fakeClientClocks.getFakeInstantSource(), fakeClientClocks.getFakeTicker()));
    SntpQueryResult sntpQueryResult = engine.executeQuery(timeAllowed);
    assertEquals(SntpQueryResult.TYPE_RETRY_LATER, sntpQueryResult.getType());
    assertNull(sntpQueryResult.getTimeSignal());
    assertEquals(NtpServerNotReachableException.class, sntpQueryResult.getException().getClass());

    // Each server IP should have been tried and will return the same result.
    SntpQueryDebugInfo queryDebugInfo = sntpQueryResult.getQueryDebugInfo();
    InetAddress[] serverIpAddresses = fakeNetwork.getAllByName(serverHostName);
    assertEquals(serverIpAddresses.length, queryDebugInfo.getSntpQueryAddresses().size());
    List<NetworkOperationResult> sntpQueryOperationResults =
        queryDebugInfo.getSntpQueryOperationResults();
    assertEquals(serverIpAddresses.length, sntpQueryOperationResults.size());
    for (int i = 0; i < sntpQueryOperationResults.size(); i++) {
      NetworkOperationResult networkOperationResult = sntpQueryOperationResults.get(i);
      assertEquals(serverIpAddresses[i], networkOperationResult.getSocketAddress().getAddress());
      assertEquals(
          testSntpServerWithNetwork.getServerSocketAddress().getPort(),
          networkOperationResult.getSocketAddress().getPort());
      assertEquals(NetworkOperationResult.TYPE_FAILURE, networkOperationResult.getType());
      assertEquals(
          NtpServerNotReachableException.class, networkOperationResult.getException().getClass());
    }

    testSntpServerWithNetwork.stop();
  }

  @Test
  public void executeQuery_networkTimeout() throws Exception {
    FakeClocks fakeClientClocks = new FakeClocks();
    FakeClocks fakeServerClocks = new FakeClocks();
    // This uses a fake network with a fake server engine set to introduce delays.
    TestSntpServerWithNetwork<FakeSntpServerEngine, FakeNetwork> testSntpServerWithNetwork =
        TestSntpServerWithNetwork.createCompleteFake(fakeClientClocks, fakeServerClocks);

    String serverHostName = testSntpServerWithNetwork.getServerAddress().getName();
    FakeNetwork fakeNetwork = testSntpServerWithNetwork.getNetwork();

    SntpServiceConnectorImpl connector =
        testSntpServerWithNetwork.createConnector(
            fakeClientClocks.getFakeInstantSource(), fakeClientClocks.getFakeTicker());
    SntpClientEngine engine = new SntpClientEngine(NoOpLogger.instance(), connector);

    // Set the network to introduce delays so the SNTP query should exceed the timeout 2x.
    fakeNetwork.setNetworkPropagationTimeSend(connector.getClientConfig().responseTimeout());
    fakeNetwork.setNetworkPropagationTimeReceive(connector.getClientConfig().responseTimeout());

    Duration timeAllowed = null;
    SntpQueryResult sntpQueryResult = engine.executeQuery(timeAllowed);
    assertEquals(SntpQueryResult.TYPE_RETRY_LATER, sntpQueryResult.getType());
    assertNull(sntpQueryResult.getTimeSignal());
    assertEquals(NtpServerNotReachableException.class, sntpQueryResult.getException().getClass());

    // Check the SntpQueryDebugInfo.
    SntpQueryDebugInfo queryDebugInfo = sntpQueryResult.getQueryDebugInfo();
    InetAddress[] serverIpAddresses = fakeNetwork.getAllByName(serverHostName);
    assertEquals(1, queryDebugInfo.getSntpQueryAddresses().size());
    List<NetworkOperationResult> sntpQueryOperationResults =
        queryDebugInfo.getSntpQueryOperationResults();
    NetworkOperationResult networkOperationResult = sntpQueryOperationResults.get(0);
    assertEquals(serverIpAddresses[0], networkOperationResult.getSocketAddress().getAddress());
    assertEquals(
        testSntpServerWithNetwork.getServerSocketAddress().getPort(),
        networkOperationResult.getSocketAddress().getPort());
    assertEquals(NetworkOperationResult.TYPE_FAILURE, networkOperationResult.getType());
    assertEquals(
        NtpServerNotReachableException.class, networkOperationResult.getException().getClass());

    testSntpServerWithNetwork.stop();
  }

  @Test
  public void executeQuery_nonMatchingOriginateTime() throws Exception {
    FakeClocks fakeClientClocks = new FakeClocks();
    FakeClocks fakeServerClocks = new FakeClocks();
    // This uses a fake network with a fake server engine set to behave strangely.
    TestSntpServerWithNetwork<FakeSntpServerEngine, FakeNetwork> testSntpServerWithNetwork =
        TestSntpServerWithNetwork.createCompleteFake(fakeClientClocks, fakeServerClocks);

    String serverHostName = testSntpServerWithNetwork.getServerAddress().getName();
    FakeNetwork fakeNetwork = testSntpServerWithNetwork.getNetwork();
    // Give the server address a few IP addresses to try.
    for (int i = 0; i < 5; i++) {
      fakeNetwork.addServerIpAddress(serverHostName);
    }

    // Set the server to behave badly.
    testSntpServerWithNetwork
        .getSntpServerEngine()
        .setQuirkMode(FakeSntpServerEngine.QUIRK_MODE_NON_MATCHING_ORIGINATE_TIME);

    SntpClientEngine engine =
        new SntpClientEngine(
            NoOpLogger.instance(),
            testSntpServerWithNetwork.createConnector(
                fakeClientClocks.getFakeInstantSource(), fakeClientClocks.getFakeTicker()));

    Duration timeAllowed = null;
    SntpQueryResult sntpQueryResult = engine.executeQuery(timeAllowed);
    assertEquals(SntpQueryResult.TYPE_PROTOCOL_ERROR, sntpQueryResult.getType());

    // One server IP should have been tried: if one server in the cluster is not implementing the
    // protocol properly, we assume all will be similarly configured / incorrect.
    SntpQueryDebugInfo queryDebugInfo = sntpQueryResult.getQueryDebugInfo();
    InetAddress[] serverIpAddresses = fakeNetwork.getAllByName(serverHostName);
    assertEquals(serverIpAddresses.length, queryDebugInfo.getSntpQueryAddresses().size());
    List<NetworkOperationResult> sntpQueryOperationResults =
        queryDebugInfo.getSntpQueryOperationResults();
    assertEquals(1, sntpQueryOperationResults.size());
    NetworkOperationResult networkOperationResult = sntpQueryOperationResults.get(0);
    assertEquals(serverIpAddresses[0], networkOperationResult.getSocketAddress().getAddress());
    assertEquals(
        testSntpServerWithNetwork.getServerSocketAddress().getPort(),
        networkOperationResult.getSocketAddress().getPort());
    assertEquals(NetworkOperationResult.TYPE_FAILURE, networkOperationResult.getType());
    assertEquals(NtpProtocolException.class, networkOperationResult.getException().getClass());

    testSntpServerWithNetwork.stop();
  }

  @Test
  public void executeQuery_timeAllowedExceeded() throws Exception {
    FakeClocks fakeClientClocks = new FakeClocks();
    FakeClocks fakeServerClocks = new FakeClocks();
    // This uses a fake network with a fake server engine set to introduce delays.
    TestSntpServerWithNetwork<FakeSntpServerEngine, FakeNetwork> testSntpServerWithNetwork =
        TestSntpServerWithNetwork.createCompleteFake(fakeClientClocks, fakeServerClocks);

    String serverHostName = testSntpServerWithNetwork.getServerAddress().getName();
    FakeNetwork fakeNetwork = testSntpServerWithNetwork.getNetwork();
    // Give the server address a few IP addresses to try.
    for (int i = 0; i < 5; i++) {
      fakeNetwork.addServerIpAddress(serverHostName);
    }

    // Set the network to introduce delays and each server to send a "single server" failure so we
    // should exceed the timeout.
    fakeNetwork.setNetworkPropagationTimeSend(Duration.ofSeconds(1, 0));
    fakeNetwork.setNetworkPropagationTimeReceive(Duration.ofSeconds(1, 0));
    FakeSntpServerEngine sntpServerEngine = testSntpServerWithNetwork.getSntpServerEngine();
    // Set the server to indicate it is still syncing time.
    sntpServerEngine.setLastResponseTemplate(
        createBadResponseTemplate(sntpServerEngine.getLastResponseTemplate()));

    SntpClientEngine engine =
        new SntpClientEngine(
            NoOpLogger.instance(),
            testSntpServerWithNetwork.createConnector(
                fakeClientClocks.getFakeInstantSource(), fakeClientClocks.getFakeTicker()));

    // Allow 5 seconds, which should be exceeded after a few (simulated) IP addresses are tried.
    // 2 server IPs should have been tried, and it will have exceeded the time allowed on the 3rd.
    Duration timeAllowed = Duration.ofSeconds(5, 0);
    SntpQueryResult sntpQueryResult = engine.executeQuery(timeAllowed);
    assertEquals(SntpQueryResult.TYPE_TIME_ALLOWED_EXCEEDED, sntpQueryResult.getType());
    assertNull(sntpQueryResult.getTimeSignal());
    assertNull(sntpQueryResult.getException());

    // Check the SntpQueryDebugInfo.
    SntpQueryDebugInfo queryDebugInfo = sntpQueryResult.getQueryDebugInfo();
    InetAddress[] serverIpAddresses = fakeNetwork.getAllByName(serverHostName);
    assertEquals(serverIpAddresses.length, queryDebugInfo.getSntpQueryAddresses().size());
    List<NetworkOperationResult> sntpQueryOperationResults =
        queryDebugInfo.getSntpQueryOperationResults();
    assertEquals(2, sntpQueryOperationResults.size());
    for (int i = 0; i < sntpQueryOperationResults.size(); i++) {
      NetworkOperationResult networkOperationResult = sntpQueryOperationResults.get(i);
      assertEquals(serverIpAddresses[i], networkOperationResult.getSocketAddress().getAddress());
      assertEquals(
          testSntpServerWithNetwork.getServerSocketAddress().getPort(),
          networkOperationResult.getSocketAddress().getPort());
      assertEquals(NetworkOperationResult.TYPE_FAILURE, networkOperationResult.getType());
      assertEquals(NtpProtocolException.class, networkOperationResult.getException().getClass());
    }

    testSntpServerWithNetwork.stop();
  }

  /**
   * An end-to-end test calling {@link SntpClientEngine#executeQuery} with a tightly controlled,
   * idealized and faked, test server + network.
   */
  @Test
  public void executeQuery_successDetails() throws Exception {
    FakeClocks fakeClientClocks = new FakeClocks();
    Instant clientStartInstant = Instant.ofEpochMilli(1234L);
    fakeClientClocks.getFakeInstantSource().setInstant(clientStartInstant);
    FakeTicker clientTicker = fakeClientClocks.getFakeTicker();
    clientTicker.setTicksValue(99999999);
    Ticks clientStartTicks = clientTicker.getCurrentTicks();

    FakeClocks fakeServerClocks = new FakeClocks();
    Instant serverStartInstant = Instant.ofEpochMilli(12345678L);
    fakeServerClocks.getFakeInstantSource().setInstant(serverStartInstant);

    TestSntpServerWithNetwork<FakeSntpServerEngine, FakeNetwork> testSntpServerWithNetwork =
        TestSntpServerWithNetwork.createCompleteFake(fakeClientClocks, fakeServerClocks);
    FakeNetwork fakeNetwork = testSntpServerWithNetwork.getNetwork();

    // Important note: Propagation delays have to be measurable in millis, because the
    // InstantSources in use for this test are millisecond-based. The same delay is used for send
    // and receive delays because symmetry is what SNTP assumes (necessarily). With asymmetric
    // propagation delays errors will be visible.
    Duration networkPropagationDelay = Duration.ofSeconds(0, 100 * NANOS_PER_MILLISECOND);
    fakeNetwork.setNetworkPropagationTimeSend(networkPropagationDelay);
    fakeNetwork.setNetworkPropagationTimeReceive(networkPropagationDelay);

    FakeSntpServerEngine fakeSntpServerEngine = testSntpServerWithNetwork.getSntpServerEngine();
    Duration processingDuration = Duration.ofSeconds(0, 10 * NANOS_PER_MILLISECOND);
    fakeSntpServerEngine.setSimulatedProcessingDuration(processingDuration);

    NtpMessage responseTemplate = fakeSntpServerEngine.getLastResponseTemplate();
    NtpHeader responseHeaderTemplate =
        responseTemplate.getHeader().toBuilder()
            .setStratum(1)
            .setLeapIndicator(0)
            .setPollIntervalExponent(6)
            .setPrecisionExponent(-20)
            .setReferenceIdentifierAsString("GOOG")
            // Choose an arbitrary time for the reference timestamp (which is not used for any
            // calculations).
            .setReferenceTimestamp(
                Timestamp64.fromInstant(serverStartInstant.plus(millisDuration(50000))))
            .build();
    responseTemplate = responseTemplate.toBuilder().setHeader(responseHeaderTemplate).build();
    fakeSntpServerEngine.setLastResponseTemplate(responseTemplate);

    FakeInstantSource clientInstantSource = fakeClientClocks.getFakeInstantSource();
    SntpServiceConnector sntpServiceConnector =
        testSntpServerWithNetwork.createConnector(clientInstantSource, clientTicker);
    SntpClientEngine engine = new SntpClientEngine(NoOpLogger.instance(), sntpServiceConnector);

    Duration timeAllowed = null;
    SntpQueryResult result = engine.executeQuery(timeAllowed);
    assertEquals(
        "Unexpected result type: " + result, SntpQueryResult.TYPE_SUCCESS, result.getType());

    SntpTimeSignal sntpTimeSignal = result.getTimeSignal();
    NtpMessage serverResponse =
        testSntpServerWithNetwork.getSntpServerEngine().getLastResponseSent();
    assertSame(clientInstantSource, sntpTimeSignal.getInstantSource());
    assertEquals(
        serverResponse.getHeader().getVersionNumber(), sntpTimeSignal.getResponseVersion());

    // The result ticks should be when the response was received, which is the start time plus 2
    // network legs + server processing time.
    Duration expectedRoundTripDuration = networkPropagationDelay.plus(networkPropagationDelay);
    // Round trip time is just the network time (both legs).
    assertEquals(expectedRoundTripDuration, sntpTimeSignal.getRoundTripDuration());
    Duration totalExpectedSessionResultDuration =
        expectedRoundTripDuration.plus(processingDuration);

    // This is ultimately what we're trying to find...
    assertAlmostEquals(
        Duration.between(clientStartInstant, serverStartInstant), sntpTimeSignal.getClientOffset());
    assertEquals(
        totalExpectedSessionResultDuration,
        clientStartTicks.durationUntil(sntpTimeSignal.getResultTicks()));
    assertAlmostEquals(
        serverStartInstant.plus(totalExpectedSessionResultDuration),
        sntpTimeSignal.getResultInstant());

    // Check various metadata fields.
    NtpHeader serverResponseHeader = serverResponse.getHeader();
    assertEquals(
        serverResponseHeader.getPrecisionExponent(), sntpTimeSignal.getPrecisionExponent());
    assertEquals(
        serverResponseHeader.getPollIntervalAsDuration(), sntpTimeSignal.getPollInterval());
    assertEquals(
        serverResponseHeader.getReferenceIdentifierAsString(),
        sntpTimeSignal.getReferenceIdentifierAsString());
    assertEquals(
        serverResponseHeader.getReferenceTimestamp().toInstant(0),
        sntpTimeSignal.getReferenceTimestampAsInstant());
    assertEquals(
        serverResponseHeader.getRootDelayDuration(), sntpTimeSignal.getRootDelayDuration());
    assertEquals(
        serverResponseHeader.getRootDispersionDuration(),
        sntpTimeSignal.getRootDispersionDuration());
    InetSocketAddress serverSocketAddress = testSntpServerWithNetwork.getServerSocketAddress();
    assertEquals(serverSocketAddress.getAddress(), sntpTimeSignal.getServerInetAddress());
    assertEquals(serverSocketAddress.getPort(), sntpTimeSignal.getServerPort());

    List<FakeNetwork.FakeUdpSocket> udpSocketsCreated = fakeNetwork.getUdpSocketsCreated();
    assertEquals(1, udpSocketsCreated.size());
    FakeNetwork.FakeUdpSocket fakeUdpSocket = udpSocketsCreated.get(0);
    assertTrue(fakeUdpSocket.isClosed());
  }

  @Test
  public void executeQuery_successAfterFailure() throws Exception {
    FakeClocks fakeClientClocks = new FakeClocks();
    FakeClocks fakeServerClocks = new FakeClocks();
    fakeServerClocks.getFakeInstantSource().setEpochMillis(111111111L);

    TestSntpServerWithNetwork<FakeSntpServerEngine, FakeNetwork> testSntpServerWithNetwork =
        TestSntpServerWithNetwork.createCompleteFake(fakeClientClocks, fakeServerClocks);

    // Set the fake server to return 2 bad responses. Good responses will be returned after that.
    FakeSntpServerEngine fakeServerEngine = testSntpServerWithNetwork.getSntpServerEngine();
    NtpMessage badResponseTemplate =
        createBadResponseTemplate(fakeServerEngine.getLastResponseTemplate());
    fakeServerEngine.setResponseTemplate(0, 2, badResponseTemplate);

    String serverHostName = testSntpServerWithNetwork.getServerAddress().getName();
    FakeNetwork fakeNetwork = testSntpServerWithNetwork.getNetwork();
    // Set the network to introduce delays and each server to fail so we should exceed the timeout.
    fakeNetwork.setNetworkPropagationTimeSend(Duration.ofSeconds(1, 0));
    fakeNetwork.setNetworkPropagationTimeReceive(Duration.ofSeconds(1, 0));
    // Give the server address a few IP addresses to try.
    for (int i = 0; i < 5; i++) {
      fakeNetwork.addServerIpAddress(serverHostName);
    }

    SntpClientEngine engine =
        new SntpClientEngine(
            NoOpLogger.instance(),
            testSntpServerWithNetwork.createConnector(
                fakeClientClocks.getFakeInstantSource(), fakeClientClocks.getFakeTicker()));

    Duration timeAllowed = null;
    SntpQueryResult sntpQueryResult = engine.executeQuery(timeAllowed);
    assertEquals(SntpQueryResult.TYPE_SUCCESS, sntpQueryResult.getType());
    assertNull(sntpQueryResult.getException());

    // Check the SntpQueryDebugInfo.
    SntpQueryDebugInfo queryDebugInfo = sntpQueryResult.getQueryDebugInfo();
    InetAddress[] serverIpAddresses = fakeNetwork.getAllByName(serverHostName);
    assertEquals(serverIpAddresses.length, queryDebugInfo.getSntpQueryAddresses().size());
    List<NetworkOperationResult> sntpQueryOperationResults =
        queryDebugInfo.getSntpQueryOperationResults();
    assertEquals(3, sntpQueryOperationResults.size());
    for (int i = 0; i < 2; i++) {
      NetworkOperationResult networkOperationResult = sntpQueryOperationResults.get(i);
      assertEquals(serverIpAddresses[i], networkOperationResult.getSocketAddress().getAddress());
      assertEquals(
          testSntpServerWithNetwork.getServerSocketAddress().getPort(),
          networkOperationResult.getSocketAddress().getPort());
      assertEquals(NetworkOperationResult.TYPE_FAILURE, networkOperationResult.getType());
      assertEquals(NtpProtocolException.class, networkOperationResult.getException().getClass());
    }
    NetworkOperationResult networkOperationResult = sntpQueryOperationResults.get(2);
    assertEquals(serverIpAddresses[2], networkOperationResult.getSocketAddress().getAddress());
    assertEquals(
        testSntpServerWithNetwork.getServerSocketAddress().getPort(),
        networkOperationResult.getSocketAddress().getPort());
    assertEquals(NetworkOperationResult.TYPE_SUCCESS, networkOperationResult.getType());
    assertNull(networkOperationResult.getException());

    // Check the actual time signal.
    SntpTimeSignal actualTimeSignal = sntpQueryResult.getTimeSignal();
    // Conduct cursory checks on the resulting time signal. Unit tests cover the NTP calculations in
    // more detail.
    assertEquals(serverIpAddresses[2], actualTimeSignal.getServerInetAddress());
    assertEquals(
        testSntpServerWithNetwork.getServerSocketAddress().getPort(),
        actualTimeSignal.getServerPort());
    assertEquals(Duration.ofSeconds(2, 0), actualTimeSignal.getRoundTripDuration());

    testSntpServerWithNetwork.stop();
  }

  private static NtpMessage createBadResponseTemplate(NtpMessage responseTemplate) {
    NtpHeader responseHeaderTemplate =
        responseTemplate.getHeader().toBuilder()
            .setLeapIndicator(NtpHeader.NTP_LEAP_NOSYNC)
            .build();
    return responseTemplate.toBuilder().setHeader(responseHeaderTemplate).build();
  }

  private static void assertAlmostEquals(Instant expected, Instant actual) {
    assertEquals(expected.getEpochSecond(), actual.getEpochSecond());
    int expectedNanos = expected.getNano();
    assertThat(actual.getNano()).isAnyOf(expectedNanos, expectedNanos - 1, expectedNanos + 1);
  }

  private static void assertAlmostEquals(Duration expected, Duration actual) {
    long actualNanos = actual.toNanos();
    long expectedNanos = expected.toNanos();
    assertThat(actualNanos).isAnyOf(expectedNanos, expectedNanos - 1, expectedNanos + 1);
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
}
