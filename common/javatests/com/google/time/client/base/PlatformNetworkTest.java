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

import static com.google.time.client.base.testing.Bytes.bytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.time.client.base.Network.UdpSocket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import org.junit.Test;

public class PlatformNetworkTest {

  @Test
  public void getAllByName() throws Exception {
    Network network = PlatformNetwork.instance();
    String hostString = "www.google.com";
    assertArrayEquals(InetAddress.getAllByName(hostString), network.getAllByName(hostString));
  }

  @Test
  public void udpSocketBehavior() throws Exception {
    Network network = PlatformNetwork.instance();
    UdpSocket wrappedSocket = network.createUdpSocket();
    wrappedSocket.setSoTimeout(Duration.ofSeconds(5, 0));

    DatagramSocket javaSocket = new DatagramSocket();
    javaSocket.setSoTimeout(5000);
    {
      byte[] sendBuf = bytes(1, 2, 3);
      SocketAddress javaSocketAddress = javaSocket.getLocalSocketAddress();
      DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, javaSocketAddress);
      wrappedSocket.send(sendPacket);

      byte[] receiveBuf = new byte[100];
      DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
      javaSocket.receive(receivePacket);
      assertEquals(3, receivePacket.getLength());
      byte[] receivedBytes = new byte[3];
      System.arraycopy(receiveBuf, 0, receivedBytes, 0, 3);
      assertArrayEquals(sendBuf, receivedBytes);
    }

    {
      byte[] sendBuf = bytes(3, 2, 1);
      SocketAddress wrappedSocketAddress = wrappedSocket.getLocalSocketAddress();
      DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, wrappedSocketAddress);
      javaSocket.send(sendPacket);

      byte[] receiveBuf = new byte[100];
      DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
      wrappedSocket.receive(receivePacket);
      assertEquals(3, receivePacket.getLength());
      byte[] receivedBytes = new byte[3];
      System.arraycopy(receiveBuf, 0, receivedBytes, 0, 3);
      assertArrayEquals(sendBuf, receivedBytes);
    }

    wrappedSocket.close();
    javaSocket.close();
  }
}
