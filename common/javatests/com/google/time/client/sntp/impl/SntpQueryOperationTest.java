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

import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_MILLISECOND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.impl.ClusteredServiceOperation.ServiceOperation.ServiceResult;
import com.google.time.client.base.impl.NoOpLogger;
import com.google.time.client.base.testing.FakeClocks;
import com.google.time.client.base.testing.PredictableRandom;
import com.google.time.client.sntp.BasicSntpClient;
import com.google.time.client.sntp.NtpProtocolException;
import com.google.time.client.sntp.NtpServerNotReachableException;
import com.google.time.client.sntp.impl.SntpQueryOperation.FailureResult;
import com.google.time.client.sntp.impl.SntpQueryOperation.SuccessResult;
import com.google.time.client.sntp.testing.FakeNetwork;
import com.google.time.client.sntp.testing.FakeSntpServerEngine;
import com.google.time.client.sntp.testing.TestSntpServerWithNetwork;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SntpQueryOperationTest {

  private FakeClocks fakeClientClocks;
  private FakeClocks fakeServerClocks;
  private TestSntpServerWithNetwork<FakeSntpServerEngine, FakeNetwork> testSntpServerWithNetwork;
  private FakeNetwork fakeNetwork;
  private BasicSntpClient.ClientConfig clientConfig;
  private SntpRequestFactory requestFactory;

  private FakeClocks.FakeTicker fakeClientTicker;

  private FakeClocks.FakeInstantSource fakeClientInstantSource;

  @Before
  public void setUp() throws Exception {
    fakeClientClocks = new FakeClocks();
    fakeClientInstantSource = fakeClientClocks.getFakeInstantSource();
    fakeClientTicker = fakeClientClocks.getFakeTicker();

    Instant clientStartInstant = Instant.ofEpochMilli(1234L);
    fakeClientInstantSource.setInstant(clientStartInstant);
    fakeClientTicker.setTicksValue(99999999);
    fakeClientClocks.setAutoAdvanceDuration(Duration.ofMillis(10));

    fakeServerClocks = new FakeClocks();
    Instant serverStartInstant = Instant.ofEpochMilli(12345678L);
    fakeServerClocks.getFakeInstantSource().setInstant(serverStartInstant);

    testSntpServerWithNetwork =
        TestSntpServerWithNetwork.createCompleteFake(fakeClientClocks, fakeServerClocks);
    fakeNetwork = testSntpServerWithNetwork.getNetwork();
    clientConfig = testSntpServerWithNetwork.getClientConfig();

    requestFactory =
        new SntpRequestFactory(fakeClientInstantSource, new PredictableRandom(), 3, true);
  }

  @Test
  public void execute_networkIssues_failToSend() throws Exception {
    testNetworkIssues(
        FakeNetwork.FAILURE_MODE_SEND, FailureResult.FAILURE_IDENTIFIER_SOCKET_SEND_EXCEPTION);
  }

  @Test
  public void execute_networkIssues_failToReceive() throws Exception {
    testNetworkIssues(
        FakeNetwork.FAILURE_MODE_RECEIVE,
        FailureResult.FAILURE_IDENTIFIER_SOCKET_RECEIVE_EXCEPTION);
  }

  private void testNetworkIssues(int failureMode, int expectedFailureIdentifier) throws Exception {
    fakeNetwork.setFailureMode(failureMode);

    testExecuteReturnsFailure(
        expectedFailureIdentifier, NtpServerNotReachableException.class, false);
  }

  /**
   * An end-to-end test calling {@link SntpQueryOperation#execute} with a tightly controlled,
   * idealized and faked, test server + network.
   */
  @Test
  public void execute_success() throws Exception {
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

    NtpMessage cannedRequestMessage = requestFactory.get();
    SntpQueryOperation operation =
        new SntpQueryOperation(
            NoOpLogger.instance(),
            fakeNetwork,
            fakeClientInstantSource,
            fakeClientTicker,
            clientConfig,
            () -> cannedRequestMessage);

    Instant clientStartInstant = fakeClientInstantSource.getCurrentInstant();
    Ticks clientStartTicks = fakeClientTicker.getCurrentTicks();

    String unusedServerName = clientConfig.serverAddress().getName();
    InetAddress serverAddress = fakeNetwork.getAllByName(unusedServerName)[0];
    Void unusedParameter = null;
    Duration timeAllowed = null;
    ServiceResult<SuccessResult, FailureResult> result =
        operation.execute(unusedServerName, serverAddress, unusedParameter, timeAllowed);

    Ticks clientEndTicks = fakeClientTicker.getCurrentTicks();
    Instant clientEndInstant = fakeClientInstantSource.getCurrentInstant();

    assertTrue(result.isSuccess());
    assertFalse(result.isHaltingFailure());
    assertFalse(result.isTimeAllowedExceeded());
    assertNull(result.getFailureValue());
    SuccessResult successResult = result.getSuccessValue();
    NtpMessage serverResponse =
        testSntpServerWithNetwork.getSntpServerEngine().getLastResponseSent();
    assertEquals(cannedRequestMessage, successResult.request);
    assertBetweenInclusive(clientStartInstant, clientEndInstant, successResult.requestInstant);
    assertBetweenInclusive(clientStartTicks, clientEndTicks, successResult.requestTimeTicks);
    assertBetweenInclusive(clientStartTicks, clientEndTicks, successResult.responseTimeTicks);
    assertEquals(
        new InetSocketAddress(serverAddress, clientConfig.serverAddress().getPort()),
        successResult.serverSocketAddress);
    assertSame(fakeClientInstantSource, successResult.instantSource);
    assertEquals(serverResponse, successResult.response);

    assertNetworkSocketCreated(testSntpServerWithNetwork.getClientConfig().responseTimeout());
  }

  @Test
  public void execute_badServer_badOriginateTimestamp() throws Exception {
    FakeSntpServerEngine fakeSntpServerEngine = testSntpServerWithNetwork.getSntpServerEngine();
    fakeSntpServerEngine.setQuirkMode(FakeSntpServerEngine.QUIRK_MODE_NON_MATCHING_ORIGINATE_TIME);

    testExecuteReturnsFailure(
        FailureResult.FAILURE_IDENTIFIER_MISMATCHED_ORIGINATE_TIMESTAMP,
        NtpProtocolException.class,
        true);
  }

  @Test
  public void execute_badServer_unexpectedMode() throws Exception {
    FakeSntpServerEngine fakeSntpServerEngine = testSntpServerWithNetwork.getSntpServerEngine();
    NtpMessage responseTemplate = fakeSntpServerEngine.getLastResponseTemplate();
    NtpHeader responseTemplateHeader =
        responseTemplate.getHeader().toBuilder().setMode(NtpHeader.NTP_MODE_BROADCAST).build();
    fakeSntpServerEngine.setLastResponseTemplate(NtpMessage.create(responseTemplateHeader));

    testExecuteReturnsFailure(
        FailureResult.FAILURE_IDENTIFIER_BAD_SERVER_MODE, NtpProtocolException.class, true);
  }

  @Test
  public void execute_badServer_stratumZeroUnknownCode() throws Exception {
    testStratumZeroKissCode("JUNK", true, FailureResult.FAILURE_IDENTIFIER_UNKNOWN_KISS_CODE);
  }

  @Test
  public void execute_badServer_stratumZeroDeny() throws Exception {
    testStratumZeroKissCode(
        NtpHeader.KISS_CODE_DENY, true, FailureResult.FAILURE_IDENTIFIER_UNEXPECTED_KISS_CODE1);
  }

  @Test
  public void execute_badServer_stratumZeroInit() throws Exception {
    testStratumZeroKissCode(
        NtpHeader.KISS_CODE_INIT, false, FailureResult.FAILURE_IDENTIFIER_UNEXPECTED_KISS_CODE2);
  }

  private void testStratumZeroKissCode(
      String kissCode, boolean expectHaltingFailure, int expectedFailureIdentifier)
      throws Exception {
    FakeSntpServerEngine fakeSntpServerEngine = testSntpServerWithNetwork.getSntpServerEngine();
    NtpMessage responseTemplate = fakeSntpServerEngine.getLastResponseTemplate();
    NtpHeader responseTemplateHeader =
        responseTemplate.getHeader().toBuilder()
            .setStratum(0)
            .setReferenceIdentifierAsString(kissCode)
            .build();
    fakeSntpServerEngine.setLastResponseTemplate(NtpMessage.create(responseTemplateHeader));

    testExecuteReturnsFailure(
        expectedFailureIdentifier, NtpProtocolException.class, expectHaltingFailure);
  }

  private void testExecuteReturnsFailure(
      int expectedFailureIdentifier,
      Class<? extends Exception> expectedExceptionClass,
      boolean expectHaltingFailure)
      throws Exception {
    NtpMessage cannedRequestMessage = requestFactory.get();
    SntpQueryOperation operation =
        new SntpQueryOperation(
            NoOpLogger.instance(),
            fakeNetwork,
            fakeClientInstantSource,
            fakeClientTicker,
            clientConfig,
            () -> cannedRequestMessage);

    String unusedServerName = clientConfig.serverAddress().getName();
    InetAddress serverAddress = fakeNetwork.getAllByName(unusedServerName)[0];
    Void unusedParameter = null;
    Duration timeAllowed = null;
    ServiceResult<SuccessResult, FailureResult> result =
        operation.execute(unusedServerName, serverAddress, unusedParameter, timeAllowed);

    assertResultIsFailure(
        expectedFailureIdentifier,
        expectedExceptionClass,
        expectHaltingFailure,
        cannedRequestMessage,
        serverAddress,
        result);

    assertNetworkSocketCreated(testSntpServerWithNetwork.getClientConfig().responseTimeout());
  }

  private void assertResultIsFailure(
      int expectedFailureIdentifier,
      Class<? extends Exception> expectedExceptionClass,
      boolean expectHaltingFailure,
      NtpMessage expectedRequestMessage,
      InetAddress expectedServerAddress,
      ServiceResult<SuccessResult, FailureResult> result) {
    assertFalse(result.isSuccess());
    assertEquals(expectHaltingFailure, result.isHaltingFailure());
    assertFalse(result.isTimeAllowedExceeded());
    assertNull(result.getSuccessValue());
    FailureResult failureResult = result.getFailureValue();
    assertEquals(expectedRequestMessage, failureResult.request);
    assertEquals(
        new InetSocketAddress(expectedServerAddress, clientConfig.serverAddress().getPort()),
        failureResult.serverSocketAddress);
    assertEquals(expectedFailureIdentifier, failureResult.failureIdentifier);
    assertEquals(expectedExceptionClass, failureResult.failureException.getClass());
  }

  @Test
  public void execute_badServer_zeroTransmitTimestamp() throws Exception {
    FakeSntpServerEngine fakeSntpServerEngine = testSntpServerWithNetwork.getSntpServerEngine();
    fakeSntpServerEngine.setQuirkMode(FakeSntpServerEngine.QUIRK_MODE_ZERO_TRANSMIT_TIMESTAMP);

    testExecuteReturnsFailure(
        FailureResult.FAILURE_IDENTIFIER_ZERO_TRANSMIT_TIMESTAMP, NtpProtocolException.class, true);
  }

  @Test
  public void execute_badServer_noLeapSync() throws Exception {
    FakeSntpServerEngine fakeSntpServerEngine = testSntpServerWithNetwork.getSntpServerEngine();
    NtpMessage responseTemplate = fakeSntpServerEngine.getLastResponseTemplate();
    NtpHeader responseTemplateHeader =
        responseTemplate.getHeader().toBuilder()
            .setLeapIndicator(NtpHeader.NTP_LEAP_NOSYNC)
            .build();
    fakeSntpServerEngine.setLastResponseTemplate(NtpMessage.create(responseTemplateHeader));

    testExecuteReturnsFailure(
        FailureResult.FAILURE_IDENTIFIER_UNSYNCHRONIZED_SERVER, NtpProtocolException.class, false);
  }

  @Test
  public void execute_badServer_stratumTooHigh() throws Exception {
    FakeSntpServerEngine fakeSntpServerEngine = testSntpServerWithNetwork.getSntpServerEngine();
    NtpMessage responseTemplate = fakeSntpServerEngine.getLastResponseTemplate();
    NtpHeader responseTemplateHeader =
        responseTemplate.getHeader().toBuilder().setStratum(16).build();
    fakeSntpServerEngine.setLastResponseTemplate(NtpMessage.create(responseTemplateHeader));

    testExecuteReturnsFailure(
        FailureResult.FAILURE_IDENTIFIER_UNTRUSTED_STRATUM, NtpProtocolException.class, false);
  }

  @Test
  public void execute_badServer_referenceTimestampZero() throws Exception {
    FakeSntpServerEngine fakeSntpServerEngine = testSntpServerWithNetwork.getSntpServerEngine();
    NtpMessage responseTemplate = fakeSntpServerEngine.getLastResponseTemplate();
    NtpHeader responseTemplateHeader =
        responseTemplate.getHeader().toBuilder().setReferenceTimestamp(Timestamp64.ZERO).build();
    fakeSntpServerEngine.setLastResponseTemplate(NtpMessage.create(responseTemplateHeader));

    testExecuteReturnsFailure(
        FailureResult.FAILURE_IDENTIFIER_REFERENCE_TIMESTAMP_ZERO,
        NtpProtocolException.class,
        false);
  }

  @Test
  public void execute_timeAllowedExceeded_overResponseTimeout() throws Exception {
    // This test relies on the configured response timeout being exceeded.
    assertEquals(
        Duration.ofSeconds(5, 0), testSntpServerWithNetwork.getClientConfig().responseTimeout());
    Duration timeAllowed = Duration.ofSeconds(3, 0);
    fakeNetwork.setNetworkPropagationTimeSend(Duration.ofSeconds(10, 0));

    NtpMessage cannedRequestMessage = requestFactory.get();
    SntpQueryOperation operation =
        new SntpQueryOperation(
            NoOpLogger.instance(),
            fakeNetwork,
            fakeClientInstantSource,
            fakeClientTicker,
            clientConfig,
            () -> cannedRequestMessage);

    String unusedServerName = clientConfig.serverAddress().getName();
    InetAddress serverAddress = fakeNetwork.getAllByName(unusedServerName)[0];
    Void unusedParameter = null;
    ServiceResult<SuccessResult, FailureResult> result =
        operation.execute(unusedServerName, serverAddress, unusedParameter, timeAllowed);

    assertResultTimeAllowedExceeded(result);
    assertNetworkSocketCreated(timeAllowed);
  }

  @Test
  public void execute_timeAllowedExceeded_underResponseTimeout() throws Exception {
    // This test relies on the configured response timeout not being exceeded, but the time allowed
    // being exceeded.
    assertEquals(
        Duration.ofSeconds(5, 0), testSntpServerWithNetwork.getClientConfig().responseTimeout());
    Duration timeAllowed = Duration.ofSeconds(3, 0);
    fakeNetwork.setNetworkPropagationTimeSend(Duration.ofSeconds(4, 0));

    NtpMessage cannedRequestMessage = requestFactory.get();
    SntpQueryOperation operation =
        new SntpQueryOperation(
            NoOpLogger.instance(),
            fakeNetwork,
            fakeClientInstantSource,
            fakeClientTicker,
            clientConfig,
            () -> cannedRequestMessage);

    String unusedServerName = clientConfig.serverAddress().getName();
    InetAddress serverAddress = fakeNetwork.getAllByName(unusedServerName)[0];
    Void unusedParameter = null;
    ServiceResult<SuccessResult, FailureResult> result =
        operation.execute(unusedServerName, serverAddress, unusedParameter, timeAllowed);

    assertResultTimeAllowedExceeded(result);

    assertNetworkSocketCreated(timeAllowed);
  }

  private void assertResultTimeAllowedExceeded(ServiceResult<SuccessResult, FailureResult> result) {
    assertFalse(result.isSuccess());
    assertFalse(result.isHaltingFailure());
    assertTrue(result.isTimeAllowedExceeded());
    assertNull(result.getSuccessValue());
    assertNull(result.getFailureValue());
  }

  private void assertNetworkSocketCreated(Duration expectedSoTimeout) {
    List<FakeNetwork.FakeUdpSocket> udpSocketsCreated = fakeNetwork.getUdpSocketsCreated();
    assertEquals(1, udpSocketsCreated.size());
    FakeNetwork.FakeUdpSocket fakeUdpSocket = udpSocketsCreated.get(0);
    assertTrue(fakeUdpSocket.isClosed());

    assertEquals(expectedSoTimeout, fakeUdpSocket.getSoTimeout());
  }

  private <T extends Comparable<T>> void assertBetweenInclusive(
      T expectedStartInc, T expectedEndInc, T actual) {
    assertTrue(
        "[" + expectedStartInc + ", " + expectedEndInc + "]",
        expectedStartInc.compareTo(expectedEndInc) <= 0);
    assertTrue(
        "actual=" + actual + " expected between [" + expectedStartInc + ", " + expectedEndInc + "]",
        expectedStartInc.compareTo(actual) <= 0 && expectedEndInc.compareTo(actual) >= 0);
  }
}
