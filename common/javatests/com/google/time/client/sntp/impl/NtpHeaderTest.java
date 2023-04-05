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

import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_SECOND;
import static com.google.time.client.base.testing.Bytes.bytes;
import static com.google.time.client.sntp.impl.NtpHeader.LI_VN_MODE_OFFSET;
import static com.google.time.client.sntp.impl.NtpHeader.ORIGINATE_TIME_OFFSET;
import static com.google.time.client.sntp.impl.NtpHeader.POLL_OFFSET;
import static com.google.time.client.sntp.impl.NtpHeader.PRECISION_OFFSET;
import static com.google.time.client.sntp.impl.NtpHeader.RECEIVE_TIME_OFFSET;
import static com.google.time.client.sntp.impl.NtpHeader.REFERENCE_IDENTIFIER_OFFSET;
import static com.google.time.client.sntp.impl.NtpHeader.REFERENCE_TIME_OFFSET;
import static com.google.time.client.sntp.impl.NtpHeader.ROOT_DELAY_OFFSET;
import static com.google.time.client.sntp.impl.NtpHeader.ROOT_DISPERSION_OFFSET;
import static com.google.time.client.sntp.impl.NtpHeader.STRATUM_OFFSET;
import static com.google.time.client.sntp.impl.NtpHeader.TRANSMIT_TIME_OFFSET;
import static com.google.time.client.sntp.impl.SerializationUtils.read32Unsigned;
import static com.google.time.client.sntp.impl.SerializationUtils.readTimestamp64;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.time.client.base.Duration;
import com.google.time.client.sntp.InvalidNtpValueException;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NtpHeaderTest {

  @Test
  public void leapIndicator() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    assertThrows(IllegalArgumentException.class, () -> headerBuilder.setLeapIndicator(-1));
    assertThrows(IllegalArgumentException.class, () -> headerBuilder.setLeapIndicator(4));
    headerBuilder.setLeapIndicator(1);
    assertEquals(1, headerBuilder.build().getLeapIndicator());
    headerBuilder.setLeapIndicator(2);
    assertEquals(2, headerBuilder.build().getLeapIndicator());

    // Check binary representation.
    assertEquals((byte) 0b10011000, builderToBytes(headerBuilder)[LI_VN_MODE_OFFSET]);
  }

  @Test
  public void version() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    assertThrows(IllegalArgumentException.class, () -> headerBuilder.setVersionNumber(-1));
    assertThrows(IllegalArgumentException.class, () -> headerBuilder.setVersionNumber(8));
    assertEquals(3, headerBuilder.build().getVersionNumber());
    headerBuilder.setVersionNumber(7);
    assertEquals(7, headerBuilder.build().getVersionNumber());

    // Check binary representation.
    assertEquals((byte) 0b00111000, builderToBytes(headerBuilder)[LI_VN_MODE_OFFSET]);
  }

  @Test
  public void mode() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    assertThrows(IllegalArgumentException.class, () -> headerBuilder.setMode(-1));
    assertThrows(IllegalArgumentException.class, () -> headerBuilder.setMode(8));
    headerBuilder.setMode(7);
    assertEquals(7, headerBuilder.build().getMode());

    // Check binary representation.
    assertEquals((byte) 0b00011111, builderToBytes(headerBuilder)[LI_VN_MODE_OFFSET]);
  }

  @Test
  public void stratum() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    assertThrows(IllegalArgumentException.class, () -> headerBuilder.setStratum(-1));
    assertThrows(IllegalArgumentException.class, () -> headerBuilder.setStratum(256));
    headerBuilder.setStratum(7);
    assertEquals(7, headerBuilder.build().getStratum());
    headerBuilder.setStratum(255);
    assertEquals(255, headerBuilder.build().getStratum());

    // Check binary representation.
    assertEquals((byte) 255, builderToBytes(headerBuilder)[STRATUM_OFFSET]);
  }

  @Test
  public void pollInterval() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    assertThrows(IllegalArgumentException.class, () -> headerBuilder.setPollIntervalExponent(-1));
    assertThrows(IllegalArgumentException.class, () -> headerBuilder.setPollIntervalExponent(18));

    headerBuilder.setPollIntervalExponent(7);
    {
      NtpHeader header = headerBuilder.build();
      assertEquals(7, header.getPollIntervalExponent());
      assertEquals(
          Duration.ofSeconds((long) Math.pow(2, 7), 0), header.getPollIntervalAsDuration());
    }

    headerBuilder.setPollIntervalExponent(17);
    {
      NtpHeader header = headerBuilder.build();
      assertEquals(17, header.getPollIntervalExponent());
      assertEquals(
          Duration.ofSeconds((long) Math.pow(2, 17), 0), header.getPollIntervalAsDuration());
    }

    // Check binary representation.
    assertEquals((byte) 17, builderToBytes(headerBuilder)[POLL_OFFSET]);

    // Check handling of bad binary representation.
    {
      byte[] bytes = new byte[NtpHeader.FIXED_FIELDS_SIZE];
      bytes[POLL_OFFSET] = (byte) 18;
      NtpHeader badValueheader = NtpHeader.fromBytes(bytes, 0);
      assertThrows(InvalidNtpValueException.class, badValueheader::getPollIntervalExponent);
      assertThrows(InvalidNtpValueException.class, badValueheader::getPollIntervalAsDuration);
    }
  }

  @Test
  public void precision() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    assertThrows(IllegalArgumentException.class, () -> headerBuilder.setPrecisionExponent(0));
    assertThrows(IllegalArgumentException.class, () -> headerBuilder.setPrecisionExponent(-129));

    headerBuilder.setPrecisionExponent(-7);
    {
      NtpHeader header = headerBuilder.build();
      assertEquals(-7, header.getPrecisionExponent());
      assertEquals(Math.pow(2, -7), header.getPrecision(), 0.0);
    }

    headerBuilder.setPrecisionExponent(-17);
    {
      NtpHeader header = headerBuilder.build();
      assertEquals(-17, header.getPrecisionExponent());
      assertEquals(Math.pow(2, -17), header.getPrecision(), 0.0);
    }

    // Check binary representation.
    assertEquals((byte) -17, builderToBytes(headerBuilder)[PRECISION_OFFSET]);
  }

  @Test
  public void rootDelay() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    Duration duration = Duration.ofSeconds(123, 456789012);
    headerBuilder.setRootDelayDuration(duration);
    assert32DurationsEqualsWithDelta(duration, headerBuilder.build().getRootDelayDuration());

    // Check binary representation.
    long expectedBinaryRepresentation = 123 << 16 | ((456789012L << 16) / NANOS_PER_SECOND);
    assertEquals(
        expectedBinaryRepresentation,
        read32Unsigned(builderToBytes(headerBuilder), ROOT_DELAY_OFFSET));
  }

  @Test
  public void rootDispersion() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    Duration duration = Duration.ofSeconds(123, 456789012);
    headerBuilder.setRootDispersionDuration(duration);
    assert32DurationsEqualsWithDelta(duration, headerBuilder.build().getRootDispersionDuration());

    // Check binary representation.
    long expectedBinaryRepresentation = 123 << 16 | ((456789012L << 16) / NANOS_PER_SECOND);
    assertEquals(
        expectedBinaryRepresentation,
        read32Unsigned(builderToBytes(headerBuilder), ROOT_DISPERSION_OFFSET));
  }

  @Test
  public void referenceIdentifier() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    checkReferenceIdentifierRoundTrip(headerBuilder, "", bytes(0, 0, 0, 0));
    checkReferenceIdentifierRoundTrip(headerBuilder, " ", bytes(' ', 0, 0, 0));
    checkReferenceIdentifierRoundTrip(headerBuilder, "AbCd", bytes('A', 'b', 'C', 'd'));
    checkReferenceIdentifierRoundTrip(headerBuilder, "AbC", bytes('A', 'b', 'C', 0));

    assertThrows(
        IllegalArgumentException.class,
        () -> headerBuilder.setReferenceIdentifierAsString("12345"));
  }

  private static void checkReferenceIdentifierRoundTrip(
      NtpHeader.Builder headerBuilder, String stringForm, byte[] expectedBytes) {
    headerBuilder.setReferenceIdentifierAsString(stringForm);

    NtpHeader header = headerBuilder.build();
    assertEquals(stringForm, header.getReferenceIdentifierAsString());
    assertArrayEquals(expectedBytes, header.getReferenceIdentifier());

    // Check binary representation.
    assertArrayEquals(
        expectedBytes,
        Arrays.copyOfRange(
            builderToBytes(headerBuilder),
            REFERENCE_IDENTIFIER_OFFSET,
            REFERENCE_IDENTIFIER_OFFSET + 4));
  }

  @Test
  public void referenceTimestamp() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    Timestamp64 timestamp64 = Timestamp64.fromString("12345678.9ABCDEF1");
    headerBuilder.setReferenceTimestamp(timestamp64);
    assertEquals(timestamp64, headerBuilder.build().getReferenceTimestamp());

    // Check correct offset is used. The binary representation for timestamps is tested elsewhere.
    assertEquals(
        timestamp64, readTimestamp64(builderToBytes(headerBuilder), REFERENCE_TIME_OFFSET));
  }

  @Test
  public void originateTimestamp() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    Timestamp64 timestamp64 = Timestamp64.fromString("12345678.9ABCDEF1");
    headerBuilder.setOriginateTimestamp(timestamp64);
    assertEquals(timestamp64, headerBuilder.build().getOriginateTimestamp());

    // Check correct offset is used. The binary representation for timestamps is tested elsewhere.
    assertEquals(
        timestamp64, readTimestamp64(builderToBytes(headerBuilder), ORIGINATE_TIME_OFFSET));
  }

  @Test
  public void receiveTimestamp() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    Timestamp64 timestamp64 = Timestamp64.fromString("12345678.9ABCDEF1");
    headerBuilder.setReceiveTimestamp(timestamp64);
    assertEquals(timestamp64, headerBuilder.build().getReceiveTimestamp());

    // Check correct offset is used. The binary representation for timestamps is tested elsewhere.
    assertEquals(timestamp64, readTimestamp64(builderToBytes(headerBuilder), RECEIVE_TIME_OFFSET));
  }

  @Test
  public void transmitTimestamp() throws Exception {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    Timestamp64 timestamp64 = Timestamp64.fromString("12345678.9ABCDEF1");
    headerBuilder.setTransmitTimestamp(timestamp64);
    assertEquals(timestamp64, headerBuilder.build().getTransmitTimestamp());

    // Check correct offset is used. The binary representation for timestamps is tested elsewhere.
    assertEquals(timestamp64, readTimestamp64(builderToBytes(headerBuilder), TRANSMIT_TIME_OFFSET));
  }

  private static byte[] builderToBytes(NtpHeader.Builder headerBuilder) {
    return headerBuilder.build().toBytes();
  }

  private static void assert32DurationsEqualsWithDelta(Duration duration, Duration valueRead) {
    // Sub-second fractions are stored with fixed-point binary, which is lossy due to truncation:
    // the value will always be rounded down if it cannot be represented exactly.
    assertEquals(duration.getSeconds(), valueRead.getSeconds());
    int delta = duration.getNano() - valueRead.getNano();

    // TODO Work out the correct value.
    int allowedTruncationError = 20000;
    assertTrue(
        "Expected=" + duration.getNano() + ", actual=" + valueRead.getNano(),
        delta >= 0 && delta <= allowedTruncationError);
  }
}
