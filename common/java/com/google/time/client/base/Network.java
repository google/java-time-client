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
package com.google.time.client.base;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * An abstraction over networking behavior to aid with testing and to enable deployers of
 * network-based time sync clients to affect things like which network interface is used.
 */
public interface Network {

  InetAddress[] getAllByName(String hostString) throws UnknownHostException;

  UdpSocket createUdpSocket() throws IOException;

  /** A partial interface over {@link java.net.DatagramSocket} to make it easier to test with. */
  interface UdpSocket extends AutoCloseable {

    SocketAddress getLocalSocketAddress();

    void setSoTimeout(Duration timeout) throws SocketException;

    void send(DatagramPacket packet) throws IOException;

    void receive(DatagramPacket packet) throws IOException;

    void close();

    boolean isClosed();
  }
}
