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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.time.client.base.Duration;
import java.net.DatagramPacket;
import java.net.InetAddress;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NtpMessageTest {

  @Test
  public void datagramPacket() throws Exception {
    NtpHeader sampleHeader =
        NtpHeader.Builder.createEmptyV3()
            .setLeapIndicator(1)
            .setVersionNumber(2)
            .setMode(3)
            .setStratum(4)
            .setPollIntervalExponent(5)
            .setPrecisionExponent(-6)
            .setRootDelayDuration(Duration.ofSeconds(7, 0))
            .setRootDispersionDuration(Duration.ofSeconds(8, 0))
            .setReferenceIdentifierAsString("9ABC")
            .setReferenceTimestamp(Timestamp64.fromString("12345678.9ABCDEF1"))
            .setOriginateTimestamp(Timestamp64.fromString("23456789.ABCDEF12"))
            .setReceiveTimestamp(Timestamp64.fromString("3456789A.BCDEF123"))
            .setTransmitTimestamp(Timestamp64.fromString("456789AB.CDEF1234"))
            .build();
    NtpMessage sampleMessage = NtpMessage.create(sampleHeader);
    byte[] expectedMessageBytes = sampleMessage.toBytes();

    InetAddress address = InetAddress.getLoopbackAddress();
    Integer port = 1234;
    DatagramPacket datagramPacket =
        new DatagramPacket(expectedMessageBytes, expectedMessageBytes.length, address, port);
    NtpMessage actualMessage = NtpMessage.fromDatagramPacket(datagramPacket);
    NtpHeader actualHeader = actualMessage.getHeader();
    assertEquals(sampleHeader.getLeapIndicator(), actualHeader.getLeapIndicator());
    assertEquals(sampleHeader.getVersionNumber(), actualHeader.getVersionNumber());
    assertEquals(sampleHeader.getMode(), actualHeader.getMode());
    assertEquals(sampleHeader.getStratum(), actualHeader.getStratum());
    assertEquals(sampleHeader.getPollIntervalExponent(), actualHeader.getPollIntervalExponent());
    assertEquals(sampleHeader.getPrecisionExponent(), actualHeader.getPrecisionExponent());
    assertEquals(sampleHeader.getRootDelayDuration(), actualHeader.getRootDelayDuration());
    assertEquals(
        sampleHeader.getRootDispersionDuration(), actualHeader.getRootDispersionDuration());
    assertEquals(
        sampleHeader.getReferenceIdentifierAsString(),
        actualHeader.getReferenceIdentifierAsString());
    assertEquals(sampleHeader.getReferenceTimestamp(), actualHeader.getReferenceTimestamp());
    assertEquals(sampleHeader.getOriginateTimestamp(), actualHeader.getOriginateTimestamp());
    assertEquals(sampleHeader.getReceiveTimestamp(), actualHeader.getReceiveTimestamp());
    assertEquals(sampleHeader.getTransmitTimestamp(), actualHeader.getTransmitTimestamp());

    DatagramPacket actualDatagramPacket = sampleMessage.toDatagramPacket(address, port);
    assertEquals(address, actualDatagramPacket.getAddress());
    assertEquals(port.intValue(), actualDatagramPacket.getPort());
    assertArrayEquals(sampleMessage.toBytes(), actualDatagramPacket.getData());
  }
}
