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
package com.google.time.client.sntp.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.time.client.base.Duration;
import com.google.time.client.base.ServerAddress;
import com.google.time.client.base.impl.NoOpLogger;
import com.google.time.client.base.testing.FakeClocks;
import com.google.time.client.sntp.BasicSntpClient;
import com.google.time.client.sntp.NtpServerNotReachableException;
import com.google.time.client.sntp.SntpNetworkListener;
import com.google.time.client.sntp.testing.FakeNetwork;
import com.google.time.client.sntp.testing.FakeSntpServerEngine;
import com.google.time.client.sntp.testing.TestSntpServerWithNetwork;
import java.net.InetSocketAddress;
import org.junit.Before;
import org.junit.Test;

public class SntpConnectorImplTest {

  private FakeClocks fakeClientClocks;
  private SntpNetworkListener mockListener;
  private BasicSntpClient.ClientConfig clientConfig;
  private SntpConnectorImpl connector;

  private TestSntpServerWithNetwork<FakeSntpServerEngine, FakeNetwork> testSntpServerWithNetwork;

  @Before
  public void setUp() throws Exception {
    fakeClientClocks = new FakeClocks();
    FakeClocks fakeServerClocks = new FakeClocks();

    mockListener = mock(SntpNetworkListener.class);

    testSntpServerWithNetwork =
        TestSntpServerWithNetwork.createCompleteFake(fakeClientClocks, fakeServerClocks);

    FakeNetwork network = testSntpServerWithNetwork.getNetwork();
    clientConfig = mock(BasicSntpClient.ClientConfig.class);
    when(clientConfig.responseTimeout()).thenReturn(Duration.ofSeconds(5, 0));

    connector =
        new SntpConnectorImpl(
            NoOpLogger.instance(),
            network,
            fakeClientClocks.getFakeInstantSource(),
            fakeClientClocks.getFakeTicker(),
            mockListener,
            clientConfig);
  }

  @Test
  public void allServersUnreachable() throws Exception {
    FakeNetwork network = testSntpServerWithNetwork.getNetwork();
    InetSocketAddress[] serverAddresses = {
      network.addServerIpAddress("ntpserver1"), network.addServerIpAddress("ntpserver1")
    };
    network.setFailureMode(FakeNetwork.FAILURE_MODE_SEND);

    when(clientConfig.serverAddress())
        .thenReturn(new ServerAddress("ntpserver1", serverAddresses[0].getPort()));

    NtpMessage request = NtpMessage.createEmptyV3();
    SntpConnector.Session session = connector.createSession();
    for (int i = 0; i < serverAddresses.length; i++) {
      assertTrue(session.canTrySend());
      assertThrows(NtpServerNotReachableException.class, () -> session.trySend(request));
      assertEquals(i + 1, testSntpServerWithNetwork.getNetwork().getUdpSocketsCreated().size());
      verify(mockListener)
          .failure(eq(serverAddresses[i].getAddress()), eq(serverAddresses[i].getPort()), any());
    }

    assertFalse(session.canTrySend());
    assertThrows(NtpServerNotReachableException.class, () -> session.trySend(request));
  }

  @Test
  public void onlyFirstServerUnreachable() throws Exception {
    FakeNetwork network = testSntpServerWithNetwork.getNetwork();
    InetSocketAddress[] serverAddresses = {
      network.addServerIpAddress("ntpserver1"), network.addServerIpAddress("ntpserver1")
    };
    network.setFailureMode(FakeNetwork.FAILURE_MODE_SEND);

    when(clientConfig.serverAddress())
        .thenReturn(new ServerAddress("ntpserver1", serverAddresses[0].getPort()));

    NtpMessage request = NtpMessage.createEmptyV3();
    SntpConnector.Session session = connector.createSession();

    assertTrue(session.canTrySend());
    assertThrows(NtpServerNotReachableException.class, () -> session.trySend(request));
    assertEquals(1, testSntpServerWithNetwork.getNetwork().getUdpSocketsCreated().size());
    verify(mockListener)
        .failure(eq(serverAddresses[0].getAddress()), eq(serverAddresses[0].getPort()), any());
    reset(mockListener);

    network.setFailureMode(FakeNetwork.FAILURE_MODE_NONE);
    assertTrue(session.canTrySend());

    SntpSessionResult result = session.trySend(request);
    assertEquals(2, testSntpServerWithNetwork.getNetwork().getUdpSocketsCreated().size());

    FakeSntpServerEngine sntpServerEngine = testSntpServerWithNetwork.getSntpServerEngine();
    NtpMessage lastRequestReceived = sntpServerEngine.getLastRequestReceived();
    assertArrayEquals(lastRequestReceived.toByteArray(), result.request.toByteArray());
    assertNotNull(result.requestTimeTicks);
    assertNotNull(result.requestInstant);
    assertArrayEquals(
        sntpServerEngine.getLastResponseSent().toByteArray(), result.response.toByteArray());
    assertNotNull(result.responseTimeTicks);

    verify(mockListener)
        .success(eq(serverAddresses[1].getAddress()), eq(serverAddresses[1].getPort()));
  }

  @Test
  public void serverLookupFailure() throws Exception {
    reset(clientConfig);
    ServerAddress badServerAddress = new ServerAddress("not_ntpserver1", 456);
    when(clientConfig.serverAddress()).thenReturn(badServerAddress);

    NtpMessage request = NtpMessage.createEmptyV3();
    SntpConnector.Session session = connector.createSession();

    assertTrue(session.canTrySend());
    assertThrows(NtpServerNotReachableException.class, () -> session.trySend(request));
    assertEquals(0, testSntpServerWithNetwork.getNetwork().getUdpSocketsCreated().size());
    verify(mockListener).serverLookupFailure(eq(badServerAddress.getName()), any());
  }
}
