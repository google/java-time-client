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

import static org.junit.Assert.fail;

import com.google.time.client.base.impl.Objects;
import com.google.time.client.sntp.impl.NtpMessage;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

/** Exposes a {@link TestSntpServerEngine} on a real network socket. */
final class SntpTestServer {

  private final Object lock = new Object();
  private final DatagramSocket socket;
  private final Thread mListeningThread;
  private volatile boolean running;

  public SntpTestServer(TestSntpServerEngine engine) {
    Objects.requireNonNull(engine);
    socket = makeServerSocket();
    running = true;

    mListeningThread =
        new Thread() {
          public void run() {
            try {
              while (running) {
                byte[] buffer = new byte[512];
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                try {
                  socket.receive(requestPacket);
                } catch (IOException e) {
                  fail("datagram receive error: " + e);
                  break;
                }

                NtpMessage request = NtpMessage.fromDatagramPacket(requestPacket);
                synchronized (lock) {
                  NtpMessage response = engine.processRequest(request);
                  byte[] responseBytes = response.toBytes();
                  DatagramPacket responsePacket =
                      new DatagramPacket(
                          responseBytes,
                          0,
                          responseBytes.length,
                          requestPacket.getAddress(),
                          requestPacket.getPort());
                  try {
                    socket.send(responsePacket);
                  } catch (IOException e) {
                    fail("datagram send error: " + e);
                    break;
                  }
                }
              }
            } finally {
              socket.close();
            }
          }
        };
    mListeningThread.start();
  }

  public void stop() {
    running = false;
    mListeningThread.interrupt();
  }

  private DatagramSocket makeServerSocket() {
    DatagramSocket socket;
    try {
      socket = new DatagramSocket(0, InetAddress.getLoopbackAddress());
    } catch (SocketException e) {
      fail("Failed to create test server socket: " + e);
      return null;
    }
    return socket;
  }

  public InetSocketAddress getInetSocketAddress() {
    return (InetSocketAddress) socket.getLocalSocketAddress();
  }
}
