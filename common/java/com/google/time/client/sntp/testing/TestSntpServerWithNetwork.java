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
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.Network;
import com.google.time.client.base.PlatformNetwork;
import com.google.time.client.base.ServerAddress;
import com.google.time.client.base.Ticker;
import com.google.time.client.base.impl.NoOpLogger;
import com.google.time.client.base.impl.Objects;
import com.google.time.client.base.testing.FakeClocks;
import com.google.time.client.sntp.BasicSntpClient;
import com.google.time.client.sntp.NoOpSntpNetworkListener;
import com.google.time.client.sntp.impl.SntpConnector;
import com.google.time.client.sntp.impl.SntpConnectorImpl;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;

/** A test NTP server with a network for SNTP clients. */
public final class TestSntpServerWithNetwork<E extends TestSntpServerEngine, N extends Network> {

  /**
   * Creates a test server consisting of a {@link FakeNetwork} with a {@link FakeSntpServerEngine}
   */
  public static TestSntpServerWithNetwork<FakeSntpServerEngine, FakeNetwork> createCompleteFake(
      FakeClocks clientClocks, FakeClocks serverClocks) {
    FakeSntpServerEngine fakeInMemoryNtpServer =
        new FakeSntpServerEngine(serverClocks.getFakeInstantSource());
    fakeInMemoryNtpServer.addAdvanceable(serverClocks);
    fakeInMemoryNtpServer.addAdvanceable(clientClocks);

    FakeNetwork fakeNetwork = new FakeNetwork(fakeInMemoryNtpServer);
    fakeNetwork.addServerIpAddress("defaultntpserver");
    fakeNetwork.addAdvanceable(serverClocks);
    fakeNetwork.addAdvanceable(clientClocks);
    return new TestSntpServerWithNetwork<>(
        fakeInMemoryNtpServer,
        fakeNetwork,
        () -> {},
        () -> fakeNetwork.getServerSocketAddress("defaultntpserver", 0));
  }

  /**
   * Creates a test server consisting of a real network with the supplied {@link
   * TestSntpServerEngine}.
   */
  public static <E extends TestSntpServerEngine>
      TestSntpServerWithNetwork<E, Network> wrapEngineWithRealNetwork(E engine) {
    Network network = PlatformNetwork.instance();
    SntpTestServer server = new SntpTestServer(engine);
    InetSocketAddress serverAddress = server.getInetSocketAddress();
    return new TestSntpServerWithNetwork<>(
        engine, network, () -> server.stop(), () -> serverAddress);
  }

  private final E serverEngine;
  private final N network;
  private final Runnable stopAction;
  private final Callable<InetSocketAddress> serverSocketAddressProvider;

  public TestSntpServerWithNetwork(
      E serverEngine,
      N network,
      Runnable stopAction,
      Callable<InetSocketAddress> serverSocketAddressProvider) {
    this.serverEngine = Objects.requireNonNull(serverEngine);
    this.network = Objects.requireNonNull(network);
    this.stopAction = Objects.requireNonNull(stopAction);
    this.serverSocketAddressProvider = Objects.requireNonNull(serverSocketAddressProvider);
  }

  public E getSntpServerEngine() {
    return serverEngine;
  }

  public N getNetwork() {
    return network;
  }

  /**
   * Returns a {@link ServerAddress} that can be used to communicate with the test server by name.
   */
  public ServerAddress getServerAddress() {
    InetSocketAddress serverSocketAddress = getServerSocketAddress();
    return new ServerAddress(serverSocketAddress.getHostName(), serverSocketAddress.getPort());
  }

  /** Returns a {@link InetSocketAddress} that can be used to communicate with the test server. */
  public InetSocketAddress getServerSocketAddress() {
    try {
      return serverSocketAddressProvider.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** Creates an {@link SntpConnector} that can be used to communicate with the test server. */
  public SntpConnector createConnector(InstantSource clientInstantSource, Ticker clientTicker) {
    BasicSntpClient.ClientConfig clientConfig =
        new BasicSntpClient.ClientConfig() {
          @Override
          public ServerAddress serverAddress() {
            return getServerAddress();
          }

          @Override
          public Duration responseTimeout() {
            return Duration.ofSeconds(5, 0);
          }
        };
    return new SntpConnectorImpl(
        NoOpLogger.instance(),
        network,
        clientInstantSource,
        clientTicker,
        new NoOpSntpNetworkListener(),
        clientConfig);
  }

  public void stop() {
    stopAction.run();
  }
}
