/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.google.time.client.base.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Network;
import com.google.time.client.base.impl.ClusteredServiceOperation.ServiceOperation.ServiceResult;
import com.google.time.client.base.testing.FakeClocks;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ClusteredServiceOperationTest {

  private FakeClocks.FakeTicker fakeTicker;
  private FakeNetwork fakeNetwork;
  private TestServiceOperation serviceOperation;
  private ClusteredServiceOperation<Parameter, Success, Failure> clusteredOperation;

  @Before
  public void setUp() throws Exception {
    FakeClocks fakeClocks = new FakeClocks();
    fakeTicker = fakeClocks.getFakeTicker();
    fakeNetwork = new FakeNetwork(fakeTicker);
    serviceOperation = new TestServiceOperation();
    clusteredOperation = new ClusteredServiceOperation<>(fakeTicker, fakeNetwork, serviceOperation);
  }

  @Test
  public void serverLookupFailure() throws Exception {
    // Deliberately do not set up any DNS entries so lookup will fail.

    Parameter parameter = new Parameter();
    assertThrows(
        UnknownHostException.class,
        () -> clusteredOperation.execute("server1", parameter, /*timeAllowed=*/ null));
  }

  @Test
  public void allNetworkOperationsFail() throws Exception {
    InetAddress[] inetAddresses = {
      createInetAddress(1, 1, 1, 1), createInetAddress(2, 2, 2, 2), createInetAddress(3, 3, 3, 3)
    };
    fakeNetwork.registerIpAddresses("server1", inetAddresses);

    // All operations fail without halting.
    serviceOperation.setOperationResultFunction(alwaysFails());

    Parameter parameter = new Parameter();
    ClusteredServiceOperation.ClusteredServiceResult<Success, Failure> clusteredServiceResult =
        clusteredOperation.execute("server1", parameter, /*timeAllowed=*/ null);
    assertFalse(clusteredServiceResult.isSuccess());
    assertTrue(clusteredServiceResult.isFailure());
    assertFalse(clusteredServiceResult.isHalted());
    assertFalse(clusteredServiceResult.isTimeAllowedExceeded());
    List<Failure> expectedFailureValues =
        Arrays.asList(
            new Failure(inetAddresses[0], parameter),
            new Failure(inetAddresses[1], parameter),
            new Failure(inetAddresses[2], parameter));
    assertEquals(expectedFailureValues, clusteredServiceResult.getFailureValues());
    assertEquals(expectedFailureValues.get(2), clusteredServiceResult.getLastFailureValue());
    assertNull(clusteredServiceResult.getSuccessValue());
  }

  @Test
  public void firstNetworkOperationSucceeds() throws Exception {
    InetAddress[] inetAddresses = {
      createInetAddress(1, 1, 1, 1), createInetAddress(2, 2, 2, 2), createInetAddress(3, 3, 3, 3)
    };
    fakeNetwork.registerIpAddresses("server1", inetAddresses);

    serviceOperation.setOperationResultFunction(alwaysSucceeds());

    Parameter parameter = new Parameter();
    ClusteredServiceOperation.ClusteredServiceResult<Success, Failure> clusteredServiceResult =
        clusteredOperation.execute("server1", parameter, /*timeAllowed=*/ null);
    assertTrue(clusteredServiceResult.isSuccess());
    assertFalse(clusteredServiceResult.isFailure());
    assertTrue(clusteredServiceResult.isHalted());
    assertFalse(clusteredServiceResult.isTimeAllowedExceeded());
    List<Failure> expectedFailureValues = Collections.emptyList();
    assertEquals(expectedFailureValues, clusteredServiceResult.getFailureValues());
    assertNull(clusteredServiceResult.getLastFailureValue());
    Success expectedSuccessValue = new Success(inetAddresses[0], parameter);
    assertEquals(expectedSuccessValue, clusteredServiceResult.getSuccessValue());
  }

  @Test
  public void firstNetworkOperationFails_halting() throws Exception {
    InetAddress[] inetAddresses = {
      createInetAddress(1, 1, 1, 1), createInetAddress(2, 2, 2, 2), createInetAddress(3, 3, 3, 3)
    };
    fakeNetwork.registerIpAddresses("server1", inetAddresses);

    InetAddress errorAddress = inetAddresses[0];
    boolean errorIsHalting = true;
    serviceOperation.setOperationResultFunction(failsIfAddressEquals(errorAddress, errorIsHalting));

    Parameter parameter = new Parameter();
    ClusteredServiceOperation.ClusteredServiceResult<Success, Failure> clusteredServiceResult =
        clusteredOperation.execute("server1", parameter, /*timeAllowed=*/ null);
    assertFalse(clusteredServiceResult.isSuccess());
    assertTrue(clusteredServiceResult.isFailure());
    assertTrue(clusteredServiceResult.isHalted());
    assertFalse(clusteredServiceResult.isTimeAllowedExceeded());
    List<Failure> expectedFailureValues =
        Collections.singletonList(new Failure(inetAddresses[0], parameter));
    assertEquals(expectedFailureValues, clusteredServiceResult.getFailureValues());
    assertEquals(expectedFailureValues.get(0), clusteredServiceResult.getLastFailureValue());
    assertNull(clusteredServiceResult.getSuccessValue());
  }

  @Test
  public void firstNetworkOperationFails_nonHalting() throws Exception {
    InetAddress[] inetAddresses = {
      createInetAddress(1, 1, 1, 1), createInetAddress(2, 2, 2, 2), createInetAddress(3, 3, 3, 3)
    };
    fakeNetwork.registerIpAddresses("server1", inetAddresses);

    InetAddress errorAddress = inetAddresses[0];
    boolean errorIsHalting = false;
    serviceOperation.setOperationResultFunction(failsIfAddressEquals(errorAddress, errorIsHalting));

    Parameter parameter = new Parameter();
    ClusteredServiceOperation.ClusteredServiceResult<Success, Failure> clusteredServiceResult =
        clusteredOperation.execute("server1", parameter, /*timeAllowed=*/ null);
    assertTrue(clusteredServiceResult.isSuccess());
    assertFalse(clusteredServiceResult.isFailure());
    assertTrue(clusteredServiceResult.isHalted());
    assertFalse(clusteredServiceResult.isTimeAllowedExceeded());
    List<Failure> expectedFailureValues =
        Collections.singletonList(new Failure(inetAddresses[0], parameter));
    assertEquals(expectedFailureValues, clusteredServiceResult.getFailureValues());
    assertEquals(expectedFailureValues.get(0), clusteredServiceResult.getLastFailureValue());
    Success expectedSuccessValue = new Success(inetAddresses[1], parameter);
    assertEquals(expectedSuccessValue, clusteredServiceResult.getSuccessValue());
  }

  @Test
  public void timeout_initialTimeZeroOrNegative() throws Exception {
    Parameter parameter = new Parameter();

    Duration zeroTimeAllowed = Duration.ZERO;
    assertThrows(
        IllegalArgumentException.class,
        () -> clusteredOperation.execute("server1", parameter, zeroTimeAllowed));

    Duration negativeTimeAllowed = Duration.ofSeconds(-1, 0);
    assertThrows(
        IllegalArgumentException.class,
        () -> clusteredOperation.execute("server1", parameter, negativeTimeAllowed));
  }

  @Test
  public void timeout_hostLookupTimeAllowedExceeded() throws Exception {
    InetAddress[] inetAddresses = {
      createInetAddress(1, 1, 1, 1), createInetAddress(2, 2, 2, 2), createInetAddress(3, 3, 3, 3)
    };
    fakeNetwork.registerIpAddresses("server1", inetAddresses);
    fakeNetwork.setFakeDnsLookupTimeTaken(Duration.ofSeconds(10, 0));

    Duration timeAllowed = Duration.ofSeconds(5, 0);
    Parameter parameter = new Parameter();
    ClusteredServiceOperation.ClusteredServiceResult<Success, Failure> clusteredServiceResult =
        clusteredOperation.execute("server1", parameter, timeAllowed);
    assertFalse(clusteredServiceResult.isSuccess());
    assertFalse(clusteredServiceResult.isFailure());
    assertFalse(clusteredServiceResult.isHalted());
    assertTrue(clusteredServiceResult.isTimeAllowedExceeded());
    List<Failure> expectedFailureValues = Collections.emptyList();
    assertEquals(expectedFailureValues, clusteredServiceResult.getFailureValues());
  }

  @Test
  public void timeout_timeAllowedExceeded() throws Exception {
    InetAddress[] inetAddresses = {
      createInetAddress(1, 1, 1, 1), createInetAddress(2, 2, 2, 2), createInetAddress(3, 3, 3, 3)
    };
    fakeNetwork.registerIpAddresses("server1", inetAddresses);

    Duration serviceTimeTaken = Duration.ofSeconds(3, 0);
    boolean operationReportsTimeAllowedExceeded = false;
    serviceOperation.setOperationResultFunction(
        alwaysFailsAndTakesTime(fakeTicker, serviceTimeTaken, operationReportsTimeAllowedExceeded));

    // With 5 seconds allowed, 2x 3s operation should make the time allowed run out before the third
    // IP address is tried.
    Duration timeAllowed = Duration.ofSeconds(5, 0);
    Parameter parameter = new Parameter();
    ClusteredServiceOperation.ClusteredServiceResult<Success, Failure> clusteredServiceResult =
        clusteredOperation.execute("server1", parameter, timeAllowed);
    assertFalse(clusteredServiceResult.isSuccess());
    assertFalse(clusteredServiceResult.isFailure());
    assertFalse(clusteredServiceResult.isHalted());
    assertTrue(clusteredServiceResult.isTimeAllowedExceeded());
    List<Failure> expectedFailureValues =
        Arrays.asList(
            new Failure(inetAddresses[0], parameter), new Failure(inetAddresses[1], parameter));
    assertEquals(expectedFailureValues, clusteredServiceResult.getFailureValues());
  }

  @Test
  public void timeout_timeAllowedExceeded_operationReported() throws Exception {
    InetAddress[] inetAddresses = {
      createInetAddress(1, 1, 1, 1), createInetAddress(2, 2, 2, 2), createInetAddress(3, 3, 3, 3)
    };
    fakeNetwork.registerIpAddresses("server1", inetAddresses);

    Duration serviceTimeTaken = Duration.ofSeconds(3, 0);
    boolean operationReportsTimeAllowedExceeded = true;
    serviceOperation.setOperationResultFunction(
        alwaysFailsAndTakesTime(fakeTicker, serviceTimeTaken, operationReportsTimeAllowedExceeded));

    // With 5 seconds allowed, 2x 3s operation should make the time allowed run out in the middle of
    // the second operation.
    Duration timeAllowed = Duration.ofSeconds(5, 0);
    Parameter parameter = new Parameter();
    ClusteredServiceOperation.ClusteredServiceResult<Success, Failure> clusteredServiceResult =
        clusteredOperation.execute("server1", parameter, timeAllowed);
    assertFalse(clusteredServiceResult.isSuccess());
    assertFalse(clusteredServiceResult.isFailure());
    assertFalse(clusteredServiceResult.isHalted());
    assertTrue(clusteredServiceResult.isTimeAllowedExceeded());
    List<Failure> expectedFailureValues = Arrays.asList(new Failure(inetAddresses[0], parameter));
    assertEquals(expectedFailureValues, clusteredServiceResult.getFailureValues());
  }

  @Test
  public void timeout_timeAllowedExceeded_operationReportedIncorrectly() throws Exception {
    InetAddress[] inetAddresses = {
      createInetAddress(1, 1, 1, 1), createInetAddress(2, 2, 2, 2), createInetAddress(3, 3, 3, 3)
    };
    fakeNetwork.registerIpAddresses("server1", inetAddresses);

    Duration serviceTimeTaken = Duration.ofSeconds(3, 0);
    serviceOperation.setOperationResultFunction(
        takesTimeAndLiesAboutTimeout(fakeTicker, serviceTimeTaken));

    // The first operation will report a timeout, but will only have taken 3 seconds.
    Duration timeAllowed = Duration.ofSeconds(5, 0);
    Parameter parameter = new Parameter();
    assertThrows(
        IllegalStateException.class,
        () -> clusteredOperation.execute("server1", parameter, timeAllowed));
  }

  private static InetAddress createInetAddress(int... addressByteInts) throws UnknownHostException {
    byte[] addressBytes = new byte[addressByteInts.length];
    for (int i = 0; i < addressBytes.length; i++) {
      addressBytes[i] = (byte) addressByteInts[i];
    }
    return InetAddress.getByAddress(addressBytes);
  }

  private static class Parameter {}

  private static final class Success extends Result {
    Success(InetAddress inetAddress, Parameter parameter) {
      super(inetAddress, parameter);
    }
  }

  private static final class Failure extends Result {
    Failure(InetAddress inetAddress, Parameter parameter) {
      super(inetAddress, parameter);
    }
  }

  private abstract static class Result {
    private final InetAddress inetAddress;
    private final Parameter parameter;

    Result(InetAddress inetAddress, Parameter parameter) {
      this.inetAddress = Objects.requireNonNull(inetAddress);
      this.parameter = Objects.requireNonNull(parameter);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Result result = (Result) o;
      return inetAddress.equals(result.inetAddress) && parameter.equals(result.parameter);
    }

    @Override
    public int hashCode() {
      return Objects.hash(inetAddress, parameter);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName()
          + "{"
          + "inetAddress="
          + inetAddress
          + ", parameter="
          + parameter
          + '}';
    }
  }

  private static TestServiceOperation.Function alwaysSucceeds() {
    return (ipAddress, param, timeAllowed) ->
        ServiceResult.success(ipAddress, new Success(ipAddress, param));
  }

  private static TestServiceOperation.Function failsIfAddressEquals(
      InetAddress failingAddress, boolean halt) {
    return (ipAddress, param, timeAllowed) -> {
      if (ipAddress.equals(failingAddress)) {
        return ServiceResult.failure(ipAddress, new Failure(ipAddress, param), halt);
      } else {
        return ServiceResult.success(ipAddress, new Success(ipAddress, param));
      }
    };
  }

  private static TestServiceOperation.Function alwaysFails() {
    return (ipAddress, param, timeAllowed) ->
        ServiceResult.failure(ipAddress, new Failure(ipAddress, param), /*halt=*/ false);
  }

  private static TestServiceOperation.Function alwaysFailsAndTakesTime(
      FakeClocks.FakeTicker ticker,
      Duration serviceTimeTaken,
      boolean operationReportsTimeAllowedExceeded) {
    return (ipAddress, param, timeAllowed) -> {
      if (operationReportsTimeAllowedExceeded && timeAllowed.compareTo(serviceTimeTaken) <= 0) {
        ticker.advance(timeAllowed);
        return ServiceResult.timeAllowedExceeded(ipAddress);
      } else {
        ticker.advance(serviceTimeTaken);
        return ServiceResult.failure(ipAddress, new Failure(ipAddress, param), /*halt=*/ false);
      }
    };
  }

  private static TestServiceOperation.Function takesTimeAndLiesAboutTimeout(
      FakeClocks.FakeTicker ticker, Duration serviceTimeTaken) {
    return (ipAddress, param, timeAllowed) -> {
      ticker.advance(serviceTimeTaken);
      return ServiceResult.timeAllowedExceeded(ipAddress);
    };
  }

  private static class FakeNetwork implements Network {

    private Map<String, InetAddress[]> fakeDns = new HashMap<>();
    private Duration fakeDnsLookupTimeTaken = Duration.ZERO;
    private FakeClocks.FakeTicker fakeTicker;

    public FakeNetwork(FakeClocks.FakeTicker fakeTicker) {
      this.fakeTicker = Objects.requireNonNull(fakeTicker);
    }

    public void setFakeDnsLookupTimeTaken(Duration duration) {
      fakeDnsLookupTimeTaken = Objects.requireNonNull(duration);
    }

    public void registerIpAddresses(String hostString, InetAddress... addresses) {
      fakeDns.put(hostString, addresses);
    }

    @Override
    public InetAddress[] getAllByName(String hostString) throws UnknownHostException {
      InetAddress[] result = fakeDns.get(hostString);
      if (result == null) {
        throw new UnknownHostException("Fake DNS lookup failure");
      }
      fakeTicker.advance(fakeDnsLookupTimeTaken);
      return result;
    }

    @Override
    public UdpSocket createUdpSocket() throws IOException {
      throw new UnsupportedOperationException("Not needed for tests");
    }
  }

  private static class TestServiceOperation
      implements ClusteredServiceOperation.ServiceOperation<Parameter, Success, Failure> {

    private interface Function {
      ServiceResult<Success, Failure> execute(
          InetAddress ipAddress, Parameter parameter, Duration timeAllowed);
    }

    private Function resultFunction;

    public void setOperationResultFunction(Function resultFunction) {
      this.resultFunction = resultFunction;
    }

    @Override
    public ServiceResult<Success, Failure> execute(
        String serverName, InetAddress inetAddress, Parameter parameter, Duration timeAllowed) {
      return resultFunction.execute(inetAddress, parameter, timeAllowed);
    }
  }
}
