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

import static com.google.time.client.base.testing.Bytes.bytes;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Network;
import com.google.time.client.base.ServerAddress;
import com.google.time.client.base.testing.Advanceable;
import com.google.time.client.sntp.impl.NtpMessage;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A fake, in-memory, single-threaded implementation of {@link Network} where all UDP sockets
 * sending to known IP addresses lead to an NTP server engine.
 */
public class FakeNetwork implements Network {

  public static final int FAILURE_MODE_RECEIVE_TIMEOUT = 0x1;

  public static final int FAILURE_MODE_RECEIVE = 0x2;

  public static final int FAILURE_MODE_SEND = 0x4;

  public static final int FAILURE_MODE_NONE = 0;

  private final TestSntpServerEngine serverEngine;
  private final Map<String, List<InetSocketAddress>> serverSocketAddresses = new HashMap<>();
  private final Map<String, List<InetAddress>> fakeDns = new HashMap<>();
  private final Set<InetAddress> knownAddresses = new HashSet<>();
  private final List<FakeUdpSocket> socketsCreated = new ArrayList<>();
  private final List<Advanceable> advanceables = new ArrayList<>();

  private Duration networkPropagationTimeSend = Duration.ZERO;
  private Duration networkPropagationTimeReceive = Duration.ZERO;

  private int failureMode;

  public FakeNetwork(TestSntpServerEngine serverEngine) {
    this.serverEngine = serverEngine;
  }

  public void addAdvanceable(Advanceable advanceable) {
    advanceables.add(advanceable);
  }

  public void setNetworkPropagationTimeSend(Duration networkPropagationTimeSend) {
    this.networkPropagationTimeSend = networkPropagationTimeSend;
  }

  public void setNetworkPropagationTimeReceive(Duration networkPropagationTimeReceive) {
    this.networkPropagationTimeReceive = networkPropagationTimeReceive;
  }

  public InetSocketAddress addServerIpAddress(String serverName) {
    InetAddress ipAddress = createAddress(serverName, knownAddresses.size());
    knownAddresses.add(ipAddress);

    List<InetSocketAddress> socketAddresses = serverSocketAddresses.get(serverName);
    List<InetAddress> ipAddresses = fakeDns.get(serverName);
    if (socketAddresses == null) {
      socketAddresses = new ArrayList<>();
      serverSocketAddresses.put(serverName, socketAddresses);
      ipAddresses = new ArrayList<>();
      fakeDns.put(serverName, ipAddresses);
    }
    InetSocketAddress inetSocketAddress = new InetSocketAddress(ipAddress, 123);
    socketAddresses.add(inetSocketAddress);
    ipAddresses.add(ipAddress);
    return inetSocketAddress;
  }

  @Override
  public InetAddress[] getAllByName(String hostString) throws UnknownHostException {
    List<InetAddress> inetAddresses = fakeDns.get(hostString);
    if (inetAddresses == null) {
      throw new UnknownHostException("hostString=" + hostString);
    }
    return inetAddresses.toArray(new InetAddress[0]);
  }

  @Override
  public UdpSocket createUdpSocket() throws SocketException {
    FakeUdpSocket socket = new FakeUdpSocket();
    socketsCreated.add(socket);
    return socket;
  }

  public void setFailureMode(int failureMode) {
    this.failureMode = failureMode;
  }

  public List<FakeUdpSocket> getUdpSocketsCreated() {
    return socketsCreated;
  }

  public InetSocketAddress getServerSocketAddress(String serverName, int i) {
    return serverSocketAddresses.get(serverName).get(i);
  }

  public ServerAddress getServerAddress(String serverName, int i) {
    InetSocketAddress inetSocketAddress = serverSocketAddresses.get(serverName).get(i);
    return new ServerAddress(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
  }

  public class FakeUdpSocket implements UdpSocket {

    private InetSocketAddress localSocketAddress =
        new InetSocketAddress(Inet4Address.getLoopbackAddress(), 11111);

    private Duration soTimeout;

    private List<DatagramPacket> packetsSent = new ArrayList<>();

    private List<DatagramPacket> packetsReceived = new ArrayList<>();

    private boolean isClosed;

    private NtpMessage nextResponseMessage;

    @Override
    public SocketAddress getLocalSocketAddress() {
      return localSocketAddress;
    }

    @Override
    public void setSoTimeout(Duration timeout) throws SocketException {
      checkSocketOpen();
      soTimeout = timeout;
    }

    @Override
    public void send(DatagramPacket packet) throws IOException {
      checkSocketOpen();
      if ((failureMode & FAILURE_MODE_SEND) != 0) {
        throw new SocketTimeoutException();
      }

      packetsSent.add(packet);
      if (!knownAddresses.contains(packet.getAddress())) {
        // Packets sent to unknown IP addresses are dropped.
        return;
      }

      // Simulate the packet being travelling over the network and being received by the server.
      simulateElapsedTime(FakeNetwork.this.networkPropagationTimeSend);

      NtpMessage requestMessage = NtpMessage.fromDatagramPacket(packet);
      nextResponseMessage = serverEngine.processRequest(requestMessage);
      nextResponseMessage.setInetAddress(packet.getAddress());
      nextResponseMessage.setPort(packet.getPort());
    }

    @Override
    public void receive(DatagramPacket packet) throws IOException {
      checkSocketOpen();

      if ((failureMode & FAILURE_MODE_RECEIVE_TIMEOUT) != 0) {
        throw new SocketTimeoutException();
      } else if ((failureMode & FAILURE_MODE_RECEIVE) != 0) {
        throw new IOException();
      }
      packetsReceived.add(packet);

      simulateElapsedTime(FakeNetwork.this.networkPropagationTimeReceive);

      byte[] bytes = nextResponseMessage.toByteArray();
      packet.setData(bytes);
      packet.setAddress(nextResponseMessage.getInetAddress());
      packet.setPort(nextResponseMessage.getPort());
    }

    @Override
    public void close() {
      isClosed = true;
    }

    @Override
    public boolean isClosed() {
      return isClosed;
    }

    private void checkSocketOpen() throws SocketException {
      if (isClosed) {
        throw new SocketException("closed");
      }
    }
  }

  private void simulateElapsedTime(Duration elapsedTime) {
    for (Advanceable advanceable : advanceables) {
      advanceable.advance(elapsedTime);
    }
  }

  private static InetAddress createAddress(String name, int n) {
    try {
      return Inet4Address.getByAddress(name, bytes(192, 168, 0, n));
    } catch (UnknownHostException e) {
      throw new RuntimeException("Unable to create address", e);
    }
  }
}
