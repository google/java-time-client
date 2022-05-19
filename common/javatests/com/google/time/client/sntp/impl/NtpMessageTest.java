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
import static com.google.time.client.sntp.impl.NtpDateTimeUtils.MAX_32BIT_SECONDS_VALUE;
import static com.google.time.client.sntp.impl.NtpMessage.LI_VN_MODE_OFFSET;
import static com.google.time.client.sntp.impl.NtpMessage.ORIGINATE_TIME_OFFSET;
import static com.google.time.client.sntp.impl.NtpMessage.POLL_OFFSET;
import static com.google.time.client.sntp.impl.NtpMessage.PRECISION_OFFSET;
import static com.google.time.client.sntp.impl.NtpMessage.RECEIVE_TIME_OFFSET;
import static com.google.time.client.sntp.impl.NtpMessage.REFERENCE_IDENTIFIER_OFFSET;
import static com.google.time.client.sntp.impl.NtpMessage.REFERENCE_TIME_OFFSET;
import static com.google.time.client.sntp.impl.NtpMessage.ROOT_DELAY_OFFSET;
import static com.google.time.client.sntp.impl.NtpMessage.ROOT_DISPERSION_OFFSET;
import static com.google.time.client.sntp.impl.NtpMessage.STRATUM_OFFSET;
import static com.google.time.client.sntp.impl.NtpMessage.TRANSMIT_TIME_OFFSET;
import static com.google.time.client.sntp.impl.NtpMessage.read32Unsigned;
import static com.google.time.client.sntp.impl.NtpMessage.readTimestamp64;
import static com.google.time.client.sntp.impl.NtpMessage.write32SignedFixedPointDuration;
import static com.google.time.client.sntp.impl.NtpMessage.write32Unsigned;
import static com.google.time.client.sntp.impl.NtpMessage.write32UnsignedFixedPointDuration;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.sntp.InvalidNtpValueException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import org.junit.Test;

public class NtpMessageTest {

  private static final int MIN_UNSIGNED_BYTE = 0;
  private static final int MIN_SIGNED_BYTE = -128;
  private static final int MAX_SIGNED_BYTE = 127;
  private static final int MAX_UNSIGNED_BYTE = 255;

  @Test
  public void test8Unsigned() {
    byte[] buffer = new byte[1];
    assertThrows(
        IllegalArgumentException.class,
        () -> NtpMessage.write8Unsigned(buffer, 0, MIN_UNSIGNED_BYTE - 1));
    roundTrip8Unsigned(buffer, 0, 0);
    assertArrayEquals(bytes(MIN_UNSIGNED_BYTE), buffer);

    roundTrip8Unsigned(buffer, 0, 1);
    assertArrayEquals(bytes(1), buffer);

    roundTrip8Unsigned(buffer, 0, MAX_SIGNED_BYTE);
    assertArrayEquals(bytes(MAX_SIGNED_BYTE), buffer);

    roundTrip8Unsigned(buffer, 0, MAX_UNSIGNED_BYTE);
    assertArrayEquals(bytes(MAX_UNSIGNED_BYTE), buffer);

    assertThrows(
        IllegalArgumentException.class,
        () -> NtpMessage.write8Unsigned(buffer, 0, MAX_UNSIGNED_BYTE + 1));
  }

  private static void roundTrip8Unsigned(byte[] buffer, int offset, int unsignedByte) {
    NtpMessage.write8Unsigned(buffer, offset, unsignedByte);
    assertEquals(unsignedByte, NtpMessage.read8Unsigned(buffer, offset));
  }

  @Test
  public void test8Signed() {
    byte[] buffer = new byte[1];
    roundTrip8Signed(buffer, 0, (byte) MIN_SIGNED_BYTE);
    assertArrayEquals(bytes(MIN_SIGNED_BYTE), buffer);

    roundTrip8Signed(buffer, 0, (byte) -127);
    roundTrip8Signed(buffer, 0, (byte) -1);
    roundTrip8Signed(buffer, 0, (byte) 0);
    roundTrip8Signed(buffer, 0, (byte) 1);

    roundTrip8Signed(buffer, 0, (byte) MAX_SIGNED_BYTE);
    assertArrayEquals(bytes(MAX_SIGNED_BYTE), buffer);
  }

  private static void roundTrip8Signed(byte[] buffer, int offset, byte signedByte) {
    NtpMessage.write8Signed(buffer, offset, signedByte);
    assertEquals(signedByte, NtpMessage.read8Signed(buffer, offset));
  }

  @Test
  public void pow2ToDuration() throws Exception {
    assertThrows(InvalidNtpValueException.class, () -> NtpMessage.pow2ToDuration(-1));
    assertEquals(Duration.ofSeconds(1, 0), NtpMessage.pow2ToDuration(0));
    assertEquals(Duration.ofSeconds(2, 0), NtpMessage.pow2ToDuration(1));
    assertEquals(Duration.ofSeconds((long) Math.pow(2, 31), 0), NtpMessage.pow2ToDuration(31));
    assertEquals(Duration.ofSeconds((long) Math.pow(2, 62), 0), NtpMessage.pow2ToDuration(62));
    assertThrows(InvalidNtpValueException.class, () -> NtpMessage.pow2ToDuration(63));
  }

  @Test
  public void ascii() {
    byte[] buffer = new byte[4];
    final int length4 = 4;
    roundTripAscii(buffer, 0, length4, "");
    assertArrayEquals(bytes(0, 0, 0, 0), buffer);

    roundTripAscii(buffer, 0, length4, "1");
    assertArrayEquals(bytes('1', 0, 0, 0), buffer);

    roundTripAscii(buffer, 0, length4, "12");
    assertArrayEquals(bytes('1', '2', 0, 0), buffer);

    roundTripAscii(buffer, 0, length4, "123");
    assertArrayEquals(bytes('1', '2', '3', 0), buffer);

    roundTripAscii(buffer, 0, length4, "ABCD");
    assertArrayEquals(bytes('A', 'B', 'C', 'D'), buffer);

    assertThrows(
        IllegalArgumentException.class, () -> NtpMessage.writeAscii(buffer, 0, length4, "12345"));
    assertThrows(
        IllegalArgumentException.class, () -> NtpMessage.writeAscii(buffer, 0, length4, "\uffff"));

    final int length3 = 3;
    assertThrows(
        IllegalArgumentException.class, () -> NtpMessage.writeAscii(buffer, 0, length3, "1234"));
  }

  private static void roundTripAscii(byte[] buffer, int offset, int length, String value) {
    NtpMessage.writeAscii(buffer, offset, length, value);
    assertEquals(value, NtpMessage.readAscii(buffer, offset, length));
  }

  @Test
  public void test32Unsigned() {
    byte[] buffer = new byte[4];
    assertThrows(IllegalArgumentException.class, () -> write32Unsigned(buffer, 0, -1));

    roundTrip32Unsigned(buffer, 0, 0);
    assertArrayEquals(bytes(0, 0, 0, 0), buffer);

    roundTrip32Unsigned(buffer, 0, 1);
    assertArrayEquals(bytes(0, 0, 0, 1), buffer);

    roundTrip32Unsigned(buffer, 0, Integer.MAX_VALUE);
    assertArrayEquals(bytes(0x7F, MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE), buffer);

    roundTrip32Unsigned(buffer, 0, Integer.MAX_VALUE);
    assertArrayEquals(bytes(0x7F, MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE), buffer);

    roundTrip32Unsigned(buffer, 0, 0xFFFFFFFFL);
    assertArrayEquals(
        bytes(MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE), buffer);

    assertThrows(IllegalArgumentException.class, () -> write32Unsigned(buffer, 0, 0x0100000000L));
  }

  private static void roundTrip32Unsigned(byte[] buffer, int offset, long unsignedInt) {
    write32Unsigned(buffer, offset, unsignedInt);
    assertEquals(unsignedInt, NtpMessage.read32Unsigned(buffer, offset));
  }

  @Test
  public void test32Signed() {
    byte[] buffer = new byte[4];
    roundTrip32Signed(buffer, 0, Integer.MIN_VALUE);
    assertArrayEquals(bytes(0x80, 0, 0, 0), buffer);

    roundTrip32Signed(buffer, 0, -1);
    assertArrayEquals(
        bytes(MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE), buffer);

    roundTrip32Signed(buffer, 0, 0);
    assertArrayEquals(bytes(0, 0, 0, 0), buffer);

    roundTrip32Signed(buffer, 0, 1);
    assertArrayEquals(bytes(0, 0, 0, 0x01), buffer);

    roundTrip32Signed(buffer, 0, Integer.MAX_VALUE);
    assertArrayEquals(bytes(0x7F, MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE), buffer);
  }

  private static void roundTrip32Signed(byte[] buffer, int offset, int signedInt) {
    NtpMessage.write32Signed(buffer, offset, signedInt);
    assertEquals(signedInt, NtpMessage.read32Signed(buffer, offset));
  }

  @Test
  public void test32UnsignedFixedPointDuration() {
    byte[] buffer = new byte[4];

    // Confirm a selection of nano values can be stored / retrieved with acceptable accuracy.
    final boolean exhaustive = false;
    for (int i = 0; i < NANOS_PER_SECOND; i++) {
      roundTrip32UnsignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(1, i));
      if (!exhaustive) {
        i += 999_999;
      }
    }

    // Explore integer seconds storage.
    int minSeconds = Character.MIN_VALUE;
    int maxSeconds = Character.MAX_VALUE;

    assertThrows(
        IllegalArgumentException.class,
        () ->
            NtpMessage.write32UnsignedFixedPointDuration(
                buffer, 0, Duration.ofSeconds(minSeconds - 1, 0)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            NtpMessage.write32UnsignedFixedPointDuration(
                buffer, 0, Duration.ofSeconds(maxSeconds + 1, 0)));

    roundTrip32UnsignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(minSeconds, 0));
    assertArrayEquals(bytes(0, 0, 0, 0), buffer);
    roundTrip32UnsignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(1, 0));
    assertArrayEquals(bytes(0, 1, 0, 0), buffer);
    roundTrip32UnsignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(maxSeconds, 0));
    assertArrayEquals(bytes(MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE, 0, 0), buffer);

    // Explore fractional seconds storage.

    // minRepresentableNanos is the smallest nanos value representable as a fraction of a second (as
    // 0x00, 0x01). Any value lower than this and above zero will be truncated to zero.
    int minRepresentable16BitNanos = (int) ((0x0001L * NANOS_PER_SECOND) >>> 16) + 1;
    write32UnsignedFixedPointDuration(
        buffer, 0, Duration.ofSeconds(minSeconds, minRepresentable16BitNanos - 1));
    assertArrayEquals(bytes(0, 0, 0, 0), buffer);
    write32UnsignedFixedPointDuration(
        buffer, 0, Duration.ofSeconds(minSeconds, minRepresentable16BitNanos));
    assertArrayEquals(bytes(0, 0, 0, 0x01), buffer);
    write32UnsignedFixedPointDuration(
        buffer, 0, Duration.ofSeconds(minSeconds, minRepresentable16BitNanos + 1));
    assertArrayEquals(bytes(0, 0, 0, 0x01), buffer);
    write32UnsignedFixedPointDuration(
        buffer, 0, Duration.ofSeconds(maxSeconds, minRepresentable16BitNanos - 1));
    assertArrayEquals(bytes(0xFF, 0xFF, 0, 0), buffer);
    write32UnsignedFixedPointDuration(
        buffer, 0, Duration.ofSeconds(maxSeconds, minRepresentable16BitNanos));
    assertArrayEquals(bytes(0xFF, 0xFF, 0, 0x01), buffer);
    write32UnsignedFixedPointDuration(
        buffer, 0, Duration.ofSeconds(maxSeconds, minRepresentable16BitNanos + 1));
    assertArrayEquals(bytes(0xFF, 0xFF, 0, 0x01), buffer);

    // maxRepresentableNanos is the largest nanos value representable as a fraction of a second (as
    // 0xFF, 0xFF). Any value higher than this and below NANOS_PER_SECOND will be truncated to this
    // value.
    int maxRepresentable16BitNanos = (int) ((0xFFFFL * NANOS_PER_SECOND) >>> 16) + 1;
    roundTrip32UnsignedFixedPointDurationWithDelta(
        buffer, 0, Duration.ofSeconds(minSeconds, maxRepresentable16BitNanos));
    assertArrayEquals(bytes(0, 0, 0xFF, 0xFF), buffer);
    roundTrip32UnsignedFixedPointDurationWithDelta(
        buffer, 0, Duration.ofSeconds(minSeconds, maxRepresentable16BitNanos + 1));
    assertArrayEquals(bytes(0, 0, 0xFF, 0xFF), buffer);
    roundTrip32UnsignedFixedPointDurationWithDelta(
        buffer, 0, Duration.ofSeconds(minSeconds, NANOS_PER_SECOND - 1));
    assertArrayEquals(bytes(0, 0, 0xFF, 0xFF), buffer);
    roundTrip32UnsignedFixedPointDurationWithDelta(
        buffer, 0, Duration.ofSeconds(maxSeconds, maxRepresentable16BitNanos));
    assertArrayEquals(bytes(0xFF, 0xFF, 0xFF, 0xFF), buffer);
    roundTrip32UnsignedFixedPointDurationWithDelta(
        buffer, 0, Duration.ofSeconds(maxSeconds, maxRepresentable16BitNanos + 1));
    assertArrayEquals(bytes(0xFF, 0xFF, 0xFF, 0xFF), buffer);
    roundTrip32UnsignedFixedPointDurationWithDelta(
        buffer, 0, Duration.ofSeconds(maxSeconds, NANOS_PER_SECOND - 1));
    assertArrayEquals(bytes(0xFF, 0xFF, 0xFF, 0xFF), buffer);

    // Easy cases: 1/(2^n) representable exactly in both decimal and binary nanos..
    roundTrip32UnsignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(1, 500_000_000));
    assertArrayEquals(bytes(0, 0x01, 0x80, 0), buffer);
    roundTrip32UnsignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(1, 3_906_250));
    assertArrayEquals(bytes(0, 0x01, 0x01, 0), buffer);
    roundTrip32UnsignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(1, 1_953_125));
    assertArrayEquals(bytes(0, 0x01, 0, 0x80), buffer);
  }

  private static void roundTrip32UnsignedFixedPointDurationWithDelta(
      byte[] buffer, int offset, Duration duration) {
    NtpMessage.write32UnsignedFixedPointDuration(buffer, offset, duration);
    Duration valueRead = NtpMessage.read32UnsignedFixedPointDuration(buffer, offset);
    assert32DurationsEqualsWithDelta(duration, valueRead);
  }

  private static void assert32DurationsEqualsWithDelta(Duration duration, Duration valueRead) {
    // Sub-second fractions are stored with fixed-point binary, which is lossy due to truncation:
    // the value will always be rounded down if it cannot be represented exactly.
    assertEquals(duration.getSeconds(), valueRead.getSeconds());
    long delta = duration.getNano() - valueRead.getNano();

    // TODO Work out the correct value.
    long allowedTruncationError = 20000;
    assertTrue(
        "Expected=" + duration.getNano() + ", actual=" + valueRead.getNano(),
        delta >= 0 && delta <= allowedTruncationError);
  }

  @Test
  public void test32SignedFixedPointDuration() {
    byte[] buffer = new byte[4];

    // Confirm a selection of nano values can be stored / retrieved with acceptable accuracy.
    final boolean exhaustive = false;
    for (int i = 0; i < NANOS_PER_SECOND; i++) {
      roundTrip32SignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(1, i));
      if (!exhaustive) {
        i += 999_999;
      }
    }

    // Explore integer seconds storage.
    int minSeconds = Short.MIN_VALUE;
    int maxSeconds = Short.MAX_VALUE;

    assertThrows(
        IllegalArgumentException.class,
        () -> write32SignedFixedPointDuration(buffer, 0, Duration.ofSeconds(minSeconds - 1, 0)));
    assertThrows(
        IllegalArgumentException.class,
        () -> write32SignedFixedPointDuration(buffer, 0, Duration.ofSeconds(maxSeconds + 1, 0)));

    roundTrip32SignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(minSeconds, 0));
    assertArrayEquals(bytes(MIN_SIGNED_BYTE, 0, 0, 0), buffer);
    roundTrip32SignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(1, 0));
    assertArrayEquals(bytes(0, 1, 0, 0), buffer);
    roundTrip32SignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(maxSeconds, 0));
    assertArrayEquals(bytes(MAX_SIGNED_BYTE, 0xFF, 0, 0), buffer);

    // Explore fractional seconds storage.

    // minRepresentableNanos is the smallest nanos value representable as a fraction of a second (as
    // 0x00, 0x01). Any value lower than this and above zero will be truncated to zero.
    int minRepresentable16BitNanos = (int) ((0x0001L * NANOS_PER_SECOND) >>> 16) + 1;
    write32SignedFixedPointDuration(
        buffer, 0, Duration.ofSeconds(minSeconds, minRepresentable16BitNanos - 1));
    assertArrayEquals(bytes(MIN_SIGNED_BYTE, 0, 0, 0), buffer);
    write32SignedFixedPointDuration(
        buffer, 0, Duration.ofSeconds(minSeconds, minRepresentable16BitNanos));
    assertArrayEquals(bytes(MIN_SIGNED_BYTE, 0, 0, 0x01), buffer);
    write32SignedFixedPointDuration(
        buffer, 0, Duration.ofSeconds(minSeconds, minRepresentable16BitNanos + 1));
    assertArrayEquals(bytes(MIN_SIGNED_BYTE, 0, 0, 0x01), buffer);
    write32SignedFixedPointDuration(
        buffer, 0, Duration.ofSeconds(maxSeconds, minRepresentable16BitNanos - 1));
    assertArrayEquals(bytes(MAX_SIGNED_BYTE, 0xFF, 0, 0), buffer);
    write32SignedFixedPointDuration(
        buffer, 0, Duration.ofSeconds(maxSeconds, minRepresentable16BitNanos));
    assertArrayEquals(bytes(MAX_SIGNED_BYTE, 0xFF, 0, 0x01), buffer);
    write32SignedFixedPointDuration(
        buffer, 0, Duration.ofSeconds(maxSeconds, minRepresentable16BitNanos + 1));
    assertArrayEquals(bytes(MAX_SIGNED_BYTE, 0xFF, 0, 0x01), buffer);

    // maxRepresentableNanos is the largest nanos value representable as a fraction of a second (as
    // 0xFF, 0xFF). Any value higher than this and below NANOS_PER_SECOND will be truncated to this
    // value.
    int maxRepresentable16BitNanos = (int) ((0xFFFFL * NANOS_PER_SECOND) >>> 16) + 1;
    roundTrip32SignedFixedPointDurationWithDelta(
        buffer, 0, Duration.ofSeconds(minSeconds, maxRepresentable16BitNanos));
    assertArrayEquals(bytes(MIN_SIGNED_BYTE, 0, 0xFF, 0xFF), buffer);
    roundTrip32SignedFixedPointDurationWithDelta(
        buffer, 0, Duration.ofSeconds(minSeconds, maxRepresentable16BitNanos + 1));
    assertArrayEquals(bytes(MIN_SIGNED_BYTE, 0, 0xFF, 0xFF), buffer);
    roundTrip32SignedFixedPointDurationWithDelta(
        buffer, 0, Duration.ofSeconds(minSeconds, NANOS_PER_SECOND - 1));
    assertArrayEquals(bytes(MIN_SIGNED_BYTE, 0, 0xFF, 0xFF), buffer);
    roundTrip32SignedFixedPointDurationWithDelta(
        buffer, 0, Duration.ofSeconds(maxSeconds, maxRepresentable16BitNanos));
    assertArrayEquals(bytes(MAX_SIGNED_BYTE, 0xFF, 0xFF, 0xFF), buffer);
    roundTrip32SignedFixedPointDurationWithDelta(
        buffer, 0, Duration.ofSeconds(maxSeconds, maxRepresentable16BitNanos + 1));
    assertArrayEquals(bytes(MAX_SIGNED_BYTE, 0xFF, 0xFF, 0xFF), buffer);
    roundTrip32SignedFixedPointDurationWithDelta(
        buffer, 0, Duration.ofSeconds(maxSeconds, NANOS_PER_SECOND - 1));
    assertArrayEquals(bytes(MAX_SIGNED_BYTE, 0xFF, 0xFF, 0xFF), buffer);

    // Easy cases: 1/(2^n) representable exactly in both decimal and binary nanos..
    roundTrip32SignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(1, 500_000_000));
    assertArrayEquals(bytes(0, 0x01, 0x80, 0), buffer);
    roundTrip32SignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(1, 3_906_250));
    assertArrayEquals(bytes(0, 0x01, 0x01, 0), buffer);
    roundTrip32SignedFixedPointDurationWithDelta(buffer, 0, Duration.ofSeconds(1, 1_953_125));
    assertArrayEquals(bytes(0, 0x01, 0, 0x80), buffer);
  }

  private static void roundTrip32SignedFixedPointDurationWithDelta(
      byte[] buffer, int offset, Duration duration) {
    write32SignedFixedPointDuration(buffer, offset, duration);
    Duration valueRead = NtpMessage.read32SignedFixedPointDuration(buffer, offset);
    assert32DurationsEqualsWithDelta(duration, valueRead);
  }

  @Test
  public void testTimestamp64() {
    byte[] buffer = new byte[8];

    // Confirm nanosecond storage / precision.
    final boolean exhaustive = false;
    for (int i = 0; i < NANOS_PER_SECOND; i++) {
      roundTripTimestamp64(buffer, 0, Timestamp64.fromInstant(Instant.ofEpochSecond(0, i)));
      if (!exhaustive) {
        i += 999_999;
      }
    }

    // Check fraction storage.
    {
      int ntpEraSeconds = 1;
      byte[] ntpEraSecondsBytes = bytes(0, 0, 0, 1);

      // Easy cases: 1/(2^n) representable exactly in both decimal and binary nanos.
      roundTripTimestamp64(buffer, 0, Timestamp64.fromComponents(ntpEraSeconds, 0x80000000));
      assertArrayEquals(appendArrays(ntpEraSecondsBytes, bytes(0x80, 0, 0, 0)), buffer);

      roundTripTimestamp64(buffer, 0, Timestamp64.fromComponents(ntpEraSeconds, 0x01000000));
      assertArrayEquals(appendArrays(ntpEraSecondsBytes, bytes(0x01, 0, 0, 0)), buffer);

      roundTripTimestamp64(buffer, 0, Timestamp64.fromComponents(ntpEraSeconds, 0x00800000));
      assertArrayEquals(appendArrays(ntpEraSecondsBytes, bytes(0, 0x80, 0, 0)), buffer);

      roundTripTimestamp64(buffer, 0, Timestamp64.fromComponents(ntpEraSeconds, 0x00000001));
      assertArrayEquals(appendArrays(ntpEraSecondsBytes, bytes(0, 0, 0, 1)), buffer);

      roundTripTimestamp64(buffer, 0, Timestamp64.fromComponents(ntpEraSeconds, 0xFFFFFFFF));
      assertArrayEquals(appendArrays(ntpEraSecondsBytes, bytes(0xFF, 0xFF, 0xFF, 0xFF)), buffer);
    }

    // Check seconds storage.
    {
      long minNtpEraSeconds = 0;
      long maxNtpEraSeconds = MAX_32BIT_SECONDS_VALUE;
      int fraction = 1;
      byte[] fractionBytes = bytes(0, 0, 0, 1);

      roundTripTimestamp64(buffer, 0, Timestamp64.fromComponents(minNtpEraSeconds, fraction));
      assertArrayEquals(appendArrays(bytes(0, 0, 0, 0), fractionBytes), buffer);

      roundTripTimestamp64(buffer, 0, Timestamp64.fromComponents(maxNtpEraSeconds, 0));
    }

    // Semi-special value: Can be interpreted in some places as "invalid".
    roundTripTimestamp64(buffer, 0, Timestamp64.fromComponents(0, 0));
    assertArrayEquals(appendArrays(bytes(0, 0, 0, 0), bytes(0, 0, 0, 0)), buffer);
  }

  @Test
  public void createEmptyV3() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    assertNull(message.getInetAddress());
    assertNull(message.getPort());
    assertEquals(0, message.getLeapIndicator());
    assertEquals(3, message.getVersionNumber());
    assertEquals(0, message.getMode());
    assertEquals(0, message.getStratum());
    assertEquals(0, message.getPollIntervalExponent());
    assertEquals(Duration.ofSeconds(1, 0), message.getPollIntervalAsDuration());
    assertEquals(0, message.getPrecisionExponent());
    assertEquals(1.0, message.getPrecision(), 0.0);
    assertEquals(Duration.ofSeconds(0, 0), message.getRootDelayDuration());
    assertEquals(Duration.ofSeconds(0, 0), message.getRootDispersionDuration());
    assertArrayEquals(bytes(0, 0, 0, 0), message.getReferenceIdentifier());
    assertEquals("", message.getReferenceIdentifierAsString());
    assertEquals(Timestamp64.ZERO, message.getReferenceTimestamp());
    assertEquals(Timestamp64.ZERO, message.getOriginateTimestamp());
    assertEquals(Timestamp64.ZERO, message.getReceiveTimestamp());
    assertEquals(Timestamp64.ZERO, message.getTransmitTimestamp());

    byte[] bytes = message.toByteArray();

    assertNotSame(bytes, message.toByteArray());
    assertArrayEquals(bytes, message.toByteArray());

    byte[] bytesCopy = Arrays.copyOf(bytes, bytes.length);
    // Clear the only value we expect to be set by default, and check the array we received earlier
    // hasn't changed (confirming a clone).
    message.setVersionNumber(0);
    assertArrayEquals(bytes, bytesCopy);

    // The message content should now be all zeros.
    assertArrayEquals(new byte[bytes.length], message.toByteArray());
  }

  @Test
  public void datagramPacket() throws Exception {
    NtpMessage sampleMessage = NtpMessage.createEmptyV3();
    sampleMessage.setLeapIndicator(1);
    sampleMessage.setVersionNumber(2);
    sampleMessage.setMode(3);
    sampleMessage.setStratum(4);
    sampleMessage.setPollIntervalExponent(5);
    sampleMessage.setPrecisionExponent(-6);
    sampleMessage.setRootDelayDuration(Duration.ofSeconds(7, 0));
    sampleMessage.setRootDispersionDuration(Duration.ofSeconds(8, 0));
    sampleMessage.setReferenceIdentifierAsString("9ABC");
    sampleMessage.setReferenceTimestamp(Timestamp64.fromString("12345678.9ABCDEF1"));
    sampleMessage.setOriginateTimestamp(Timestamp64.fromString("23456789.ABCDEF12"));
    sampleMessage.setReceiveTimestamp(Timestamp64.fromString("3456789A.BCDEF123"));
    sampleMessage.setTransmitTimestamp(Timestamp64.fromString("456789AB.CDEF1234"));
    byte[] expectedMessageBytes = sampleMessage.toByteArray();

    InetAddress address = InetAddress.getLoopbackAddress();
    Integer port = 1234;
    DatagramPacket datagramPacket =
        new DatagramPacket(expectedMessageBytes, expectedMessageBytes.length, address, port);
    NtpMessage actualMessage = NtpMessage.fromDatagramPacket(datagramPacket);
    assertEquals(address, actualMessage.getInetAddress());
    assertEquals(port, actualMessage.getPort());
    assertEquals(sampleMessage.getLeapIndicator(), actualMessage.getLeapIndicator());
    assertEquals(sampleMessage.getVersionNumber(), actualMessage.getVersionNumber());
    assertEquals(sampleMessage.getMode(), actualMessage.getMode());
    assertEquals(sampleMessage.getStratum(), actualMessage.getStratum());
    assertEquals(sampleMessage.getPollIntervalExponent(), actualMessage.getPollIntervalExponent());
    assertEquals(sampleMessage.getPrecisionExponent(), actualMessage.getPrecisionExponent());
    assertEquals(sampleMessage.getRootDelayDuration(), actualMessage.getRootDelayDuration());
    assertEquals(
        sampleMessage.getRootDispersionDuration(), actualMessage.getRootDispersionDuration());
    assertEquals(
        sampleMessage.getReferenceIdentifierAsString(),
        actualMessage.getReferenceIdentifierAsString());
    assertEquals(sampleMessage.getReferenceTimestamp(), actualMessage.getReferenceTimestamp());
    assertEquals(sampleMessage.getOriginateTimestamp(), actualMessage.getOriginateTimestamp());
    assertEquals(sampleMessage.getReceiveTimestamp(), actualMessage.getReceiveTimestamp());
    assertEquals(sampleMessage.getTransmitTimestamp(), actualMessage.getTransmitTimestamp());

    DatagramPacket actualDatagramPacket = sampleMessage.toDatagramPacket(address, port);
    assertEquals(address, actualDatagramPacket.getAddress());
    assertEquals(port.intValue(), actualDatagramPacket.getPort());
    assertArrayEquals(sampleMessage.toByteArray(), actualDatagramPacket.getData());
  }

  @Test
  public void leapIndicator() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    assertThrows(IllegalArgumentException.class, () -> message.setLeapIndicator(-1));
    assertThrows(IllegalArgumentException.class, () -> message.setLeapIndicator(4));
    message.setLeapIndicator(1);
    assertEquals(1, message.getLeapIndicator());
    message.setLeapIndicator(2);
    assertEquals(2, message.getLeapIndicator());

    // Check binary representation.
    assertEquals((byte) 0b10011000, message.toByteArray()[LI_VN_MODE_OFFSET]);
  }

  @Test
  public void version() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    assertThrows(IllegalArgumentException.class, () -> message.setVersionNumber(-1));
    assertThrows(IllegalArgumentException.class, () -> message.setVersionNumber(8));
    assertEquals(3, message.getVersionNumber());
    message.setVersionNumber(7);
    assertEquals(7, message.getVersionNumber());

    // Check binary representation.
    assertEquals((byte) 0b00111000, message.toByteArray()[LI_VN_MODE_OFFSET]);
  }

  @Test
  public void mode() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    assertThrows(IllegalArgumentException.class, () -> message.setMode(-1));
    assertThrows(IllegalArgumentException.class, () -> message.setMode(8));
    message.setMode(7);
    assertEquals(7, message.getMode());

    // Check binary representation.
    assertEquals((byte) 0b00011111, message.toByteArray()[LI_VN_MODE_OFFSET]);
  }

  @Test
  public void stratum() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    assertThrows(IllegalArgumentException.class, () -> message.setStratum(-1));
    assertThrows(IllegalArgumentException.class, () -> message.setStratum(256));
    message.setStratum(7);
    assertEquals(7, message.getStratum());
    message.setStratum(255);
    assertEquals(255, message.getStratum());

    // Check binary representation.
    assertEquals((byte) 255, message.toByteArray()[STRATUM_OFFSET]);
  }

  @Test
  public void pollInterval() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    assertThrows(IllegalArgumentException.class, () -> message.setPollIntervalExponent(-1));
    assertThrows(IllegalArgumentException.class, () -> message.setPollIntervalExponent(18));
    message.setPollIntervalExponent(7);
    assertEquals(7, message.getPollIntervalExponent());
    assertEquals(Duration.ofSeconds((long) Math.pow(2, 7), 0), message.getPollIntervalAsDuration());
    message.setPollIntervalExponent(17);
    assertEquals(17, message.getPollIntervalExponent());
    assertEquals(
        Duration.ofSeconds((long) Math.pow(2, 17), 0), message.getPollIntervalAsDuration());

    // Check binary representation.
    assertEquals((byte) 17, message.toByteArray()[POLL_OFFSET]);

    // Check handling of bad binary representation.
    {
      byte[] bytes = new byte[NtpMessage.NTPV3_PACKET_SIZE];
      bytes[POLL_OFFSET] = (byte) 18;
      NtpMessage badValueMessage = NtpMessage.fromBytesForTests(bytes, null, null);
      assertThrows(InvalidNtpValueException.class, badValueMessage::getPollIntervalExponent);
      assertThrows(InvalidNtpValueException.class, badValueMessage::getPollIntervalAsDuration);
    }
  }

  @Test
  public void precision() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    assertThrows(IllegalArgumentException.class, () -> message.setPrecisionExponent(0));
    assertThrows(IllegalArgumentException.class, () -> message.setPrecisionExponent(-129));
    message.setPrecisionExponent(-7);
    assertEquals(-7, message.getPrecisionExponent());
    assertEquals(Math.pow(2, -7), message.getPrecision(), 0.0);
    message.setPrecisionExponent(-17);
    assertEquals(-17, message.getPrecisionExponent());
    assertEquals(Math.pow(2, -17), message.getPrecision(), 0.0);

    // Check binary representation.
    assertEquals((byte) -17, message.toByteArray()[PRECISION_OFFSET]);
  }

  @Test
  public void rootDelay() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    Duration duration = Duration.ofSeconds(123, 456789012);
    message.setRootDelayDuration(duration);
    assert32DurationsEqualsWithDelta(duration, message.getRootDelayDuration());

    // Check binary representation.
    long expectedBinaryRepresentation = 123 << 16 | ((456789012L << 16) / NANOS_PER_SECOND);
    assertEquals(
        expectedBinaryRepresentation, read32Unsigned(message.toByteArray(), ROOT_DELAY_OFFSET));
  }

  @Test
  public void rootDispersion() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    Duration duration = Duration.ofSeconds(123, 456789012);
    message.setRootDispersionDuration(duration);
    assert32DurationsEqualsWithDelta(duration, message.getRootDispersionDuration());

    // Check binary representation.
    long expectedBinaryRepresentation = 123 << 16 | ((456789012L << 16) / NANOS_PER_SECOND);
    assertEquals(
        expectedBinaryRepresentation,
        read32Unsigned(message.toByteArray(), ROOT_DISPERSION_OFFSET));
  }

  @Test
  public void referenceIdentifier() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    checkReferenceIdentifierRoundTrip(message, "", bytes(0, 0, 0, 0));
    checkReferenceIdentifierRoundTrip(message, " ", bytes(' ', 0, 0, 0));
    checkReferenceIdentifierRoundTrip(message, "AbCd", bytes('A', 'b', 'C', 'd'));
    checkReferenceIdentifierRoundTrip(message, "AbC", bytes('A', 'b', 'C', 0));

    assertThrows(
        IllegalArgumentException.class, () -> message.setReferenceIdentifierAsString("12345"));
  }

  private static void checkReferenceIdentifierRoundTrip(
      NtpMessage message, String stringForm, byte[] expectedBytes) {
    message.setReferenceIdentifierAsString(stringForm);
    assertEquals(stringForm, message.getReferenceIdentifierAsString());
    assertArrayEquals(expectedBytes, message.getReferenceIdentifier());

    // Check binary representation.
    assertArrayEquals(
        expectedBytes,
        Arrays.copyOfRange(
            message.toByteArray(), REFERENCE_IDENTIFIER_OFFSET, REFERENCE_IDENTIFIER_OFFSET + 4));
  }

  @Test
  public void referenceTimestamp() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    Timestamp64 timestamp64 = Timestamp64.fromString("12345678.9ABCDEF1");
    message.setReferenceTimestamp(timestamp64);
    assertEquals(timestamp64, message.getReferenceTimestamp());

    // Check correct offset is used. The binary representation for timestamps is tested elsewhere.
    assertEquals(timestamp64, readTimestamp64(message.toByteArray(), REFERENCE_TIME_OFFSET));
  }

  @Test
  public void originateTimestamp() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    Timestamp64 timestamp64 = Timestamp64.fromString("12345678.9ABCDEF1");
    message.setOriginateTimestamp(timestamp64);
    assertEquals(timestamp64, message.getOriginateTimestamp());

    // Check correct offset is used. The binary representation for timestamps is tested elsewhere.
    assertEquals(timestamp64, readTimestamp64(message.toByteArray(), ORIGINATE_TIME_OFFSET));
  }

  @Test
  public void receiveTimestamp() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    Timestamp64 timestamp64 = Timestamp64.fromString("12345678.9ABCDEF1");
    message.setReceiveTimestamp(timestamp64);
    assertEquals(timestamp64, message.getReceiveTimestamp());

    // Check correct offset is used. The binary representation for timestamps is tested elsewhere.
    assertEquals(timestamp64, readTimestamp64(message.toByteArray(), RECEIVE_TIME_OFFSET));
  }

  @Test
  public void transmitTimestamp() throws Exception {
    NtpMessage message = NtpMessage.createEmptyV3();
    Timestamp64 timestamp64 = Timestamp64.fromString("12345678.9ABCDEF1");
    message.setTransmitTimestamp(timestamp64);
    assertEquals(timestamp64, message.getTransmitTimestamp());

    // Check correct offset is used. The binary representation for timestamps is tested elsewhere.
    assertEquals(timestamp64, readTimestamp64(message.toByteArray(), TRANSMIT_TIME_OFFSET));
  }

  private static byte[] appendArrays(byte[] one, byte[] two) {
    byte[] out = new byte[one.length + two.length];
    System.arraycopy(one, 0, out, 0, one.length);
    System.arraycopy(two, 0, out, one.length, two.length);
    return out;
  }

  private static void roundTripTimestamp64(byte[] buffer, int offset, Timestamp64 timestamp64) {
    NtpMessage.writeTimestamp64(buffer, offset, timestamp64);
    Timestamp64 valueRead = NtpMessage.readTimestamp64(buffer, offset);
    assertEquals(timestamp64, valueRead);
  }
}
