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

package com.google.time.client.base.impl;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Network;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Provides access to a generally suitable default {@link Network} instance. This implementation
 * uses stock Java APIs with no special behavior.
 */
public final class PlatformNetwork {

  private static final Network INSTANCE = new NetworkImpl();

  public static Network instance() {
    return INSTANCE;
  }

  private PlatformNetwork() {}

  private static class NetworkImpl implements Network {
    @Override
    public InetAddress[] getAllByName(String hostString) throws UnknownHostException {
      return InetAddress.getAllByName(hostString);
    }

    @Override
    public UdpSocket createUdpSocket() throws SocketException {
      return new DefaultUdpSocket();
    }
  }

  private static class DefaultUdpSocket implements Network.UdpSocket {

    private final DatagramSocket delegate;

    DefaultUdpSocket() throws SocketException {
      this(new DatagramSocket());
    }

    DefaultUdpSocket(DatagramSocket delegate) {
      this.delegate = delegate;
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
      return delegate.getLocalSocketAddress();
    }

    @Override
    public void setSoTimeout(Duration timeout) throws SocketException {
      long timeoutMillis = timeout.toMillis();
      if (timeoutMillis < 0 || timeoutMillis > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Invalid timeout: " + timeout);
      }
      delegate.setSoTimeout((int) timeoutMillis);
    }

    @Override
    public void send(DatagramPacket packet) throws IOException {
      delegate.send(packet);
    }

    @Override
    public void receive(DatagramPacket packet) throws IOException {
      delegate.receive(packet);
    }

    @Override
    public void close() {
      delegate.close();
    }

    @Override
    public boolean isClosed() {
      return delegate.isClosed();
    }
  }
}
