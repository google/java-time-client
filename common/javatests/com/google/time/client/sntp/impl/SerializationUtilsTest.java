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
import static com.google.time.client.sntp.impl.SerializationUtils.check16Unsigned;
import static com.google.time.client.sntp.impl.SerializationUtils.write32SignedFixedPointDuration;
import static com.google.time.client.sntp.impl.SerializationUtils.write32Unsigned;
import static com.google.time.client.sntp.impl.SerializationUtils.write32UnsignedFixedPointDuration;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.sntp.InvalidNtpValueException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SerializationUtilsTest {

  private static final int MIN_UNSIGNED_BYTE = 0;
  private static final int MIN_SIGNED_BYTE = -128;
  private static final int MAX_SIGNED_BYTE = 127;
  private static final int MAX_UNSIGNED_BYTE = 255;

  @Test
  public void testWriteBytes() {
    byte[] bytes = {1, 2, 3, 4, 5};
    byte[] buffer = new byte[6];
    SerializationUtils.writeBytes(buffer, bytes, 4, 1);

    byte[] expectedBuffer = {0, 1, 2, 3, 4, 0};
    assertArrayEquals(expectedBuffer, buffer);
  }

  @Test
  public void test8Unsigned() {
    byte[] buffer = new byte[1];
    assertThrows(
        IllegalArgumentException.class,
        () -> SerializationUtils.write8Unsigned(buffer, 0, MIN_UNSIGNED_BYTE - 1));
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
        () -> SerializationUtils.write8Unsigned(buffer, 0, MAX_UNSIGNED_BYTE + 1));
  }

  private static void roundTrip8Unsigned(byte[] buffer, int offset, int unsignedByte) {
    SerializationUtils.write8Unsigned(buffer, offset, unsignedByte);
    assertEquals(unsignedByte, SerializationUtils.read8Unsigned(buffer, offset));
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
    SerializationUtils.write8Signed(buffer, offset, signedByte);
    assertEquals(signedByte, SerializationUtils.read8Signed(buffer, offset));
  }

  @Test
  public void pow2ToDuration() throws Exception {
    assertThrows(InvalidNtpValueException.class, () -> SerializationUtils.pow2ToDuration(-1));
    assertEquals(Duration.ofSeconds(1, 0), SerializationUtils.pow2ToDuration(0));
    assertEquals(Duration.ofSeconds(2, 0), SerializationUtils.pow2ToDuration(1));
    assertEquals(
        Duration.ofSeconds((long) Math.pow(2, 31), 0), SerializationUtils.pow2ToDuration(31));
    assertEquals(
        Duration.ofSeconds((long) Math.pow(2, 62), 0), SerializationUtils.pow2ToDuration(62));
    assertThrows(InvalidNtpValueException.class, () -> SerializationUtils.pow2ToDuration(63));
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
        IllegalArgumentException.class,
        () -> SerializationUtils.writeAscii(buffer, 0, length4, "12345"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SerializationUtils.writeAscii(buffer, 0, length4, "\uffff"));

    final int length3 = 3;
    assertThrows(
        IllegalArgumentException.class,
        () -> SerializationUtils.writeAscii(buffer, 0, length3, "1234"));
  }

  private static void roundTripAscii(byte[] buffer, int offset, int length, String value) {
    SerializationUtils.writeAscii(buffer, offset, length, value);
    assertEquals(value, SerializationUtils.readAscii(buffer, offset, length));
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

    roundTrip32Unsigned(buffer, 0, 0xFFFFFFFFL);
    assertArrayEquals(
        bytes(MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE), buffer);

    assertThrows(IllegalArgumentException.class, () -> write32Unsigned(buffer, 0, 0x0100000000L));
  }

  private static void roundTrip32Unsigned(byte[] buffer, int offset, long unsignedInt) {
    write32Unsigned(buffer, offset, unsignedInt);
    assertEquals(unsignedInt, SerializationUtils.read32Unsigned(buffer, offset));
  }

  @Test
  public void test16Unsigned() {
    byte[] buffer = new byte[2];
    assertThrows(
        IllegalArgumentException.class, () -> SerializationUtils.write16Unsigned(buffer, 0, -1));

    roundTrip16Unsigned(buffer, 0, 0);
    assertArrayEquals(bytes(0, 0), buffer);

    roundTrip16Unsigned(buffer, 0, 1);
    assertArrayEquals(bytes(0, 1), buffer);

    roundTrip16Unsigned(buffer, 0, 0xFFFF);
    assertArrayEquals(bytes(MAX_UNSIGNED_BYTE, MAX_UNSIGNED_BYTE), buffer);

    assertThrows(
        IllegalArgumentException.class,
        () -> SerializationUtils.write16Unsigned(buffer, 0, 0x010000));
  }

  private static void roundTrip16Unsigned(byte[] buffer, int offset, int unsignedInt) {
    SerializationUtils.write16Unsigned(buffer, offset, unsignedInt);
    assertEquals(unsignedInt, SerializationUtils.read16Unsigned(buffer, offset));
  }

  @Test
  public void testCheck16Unsigned() {
    assertThrows(IllegalArgumentException.class, () -> check16Unsigned(Integer.MIN_VALUE));
    assertThrows(IllegalArgumentException.class, () -> check16Unsigned(-1));
    check16Unsigned(0);
    check16Unsigned(1);
    check16Unsigned(0xFFFF);
    assertThrows(IllegalArgumentException.class, () -> check16Unsigned(0x10000));
    assertThrows(IllegalArgumentException.class, () -> check16Unsigned(Integer.MAX_VALUE));
  }

  @Test
  public void testWrite16UnsignedList() {
    byte[] buffer = new byte[8];
    SerializationUtils.write16UnsignedList(buffer, 0, Arrays.asList(1, 2, 3, 4));

    byte[] expectedBuffer = {0, 1, 0, 2, 0, 3, 0, 4};
    assertArrayEquals(expectedBuffer, buffer);
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
    SerializationUtils.write32Signed(buffer, offset, signedInt);
    assertEquals(signedInt, SerializationUtils.read32Signed(buffer, offset));
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
            SerializationUtils.write32UnsignedFixedPointDuration(
                buffer, 0, Duration.ofSeconds(minSeconds - 1, 0)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SerializationUtils.write32UnsignedFixedPointDuration(
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
    SerializationUtils.write32UnsignedFixedPointDuration(buffer, offset, duration);
    Duration valueRead = SerializationUtils.read32UnsignedFixedPointDuration(buffer, offset);
    assert32DurationsEqualsWithDelta(duration, valueRead);
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
    Duration valueRead = SerializationUtils.read32SignedFixedPointDuration(buffer, offset);
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

  private static byte[] appendArrays(byte[] one, byte[] two) {
    byte[] out = new byte[one.length + two.length];
    System.arraycopy(one, 0, out, 0, one.length);
    System.arraycopy(two, 0, out, one.length, two.length);
    return out;
  }

  private static void roundTripTimestamp64(byte[] buffer, int offset, Timestamp64 timestamp64) {
    SerializationUtils.writeTimestamp64(buffer, offset, timestamp64);
    Timestamp64 valueRead = SerializationUtils.readTimestamp64(buffer, offset);
    assertEquals(timestamp64, valueRead);
  }

  @Test
  public void testStreamReadBytesExact() throws Exception {
    byte[] buffer = bytes(1, 2, 3, 4);
    assertThrows(
        EOFException.class, () -> SerializationUtils.readStreamBytesExact(byteStream(), 5));
    assertArrayEquals(buffer, SerializationUtils.readStreamBytesExact(byteStream(buffer), 4));
    assertArrayEquals(
        bytes(1, 2, 3), SerializationUtils.readStreamBytesExact(byteStream(buffer), 3));
    assertArrayEquals(bytes(1), SerializationUtils.readStreamBytesExact(byteStream(buffer), 1));
    assertArrayEquals(bytes(), SerializationUtils.readStreamBytesExact(byteStream(buffer), 0));
  }

  @Test
  public void testStream16Unsigned() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    SerializationUtils.write16Unsigned(baos, 0xFFFF);
    SerializationUtils.write16Unsigned(baos, 0x0001);

    assertArrayEquals(bytes(0xFF, 0xFF, 0x00, 0x01), baos.toByteArray());

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    assertEquals(0xFFFF, SerializationUtils.read16Unsigned(bais));
    assertEquals(0x0001, SerializationUtils.read16Unsigned(bais));
  }

  @Test
  public void testReadFully() throws Exception {
    // Create an unusually sized (but > 1024 bytes) array with some non-zero values within
    byte[] bytes = new byte[3333];
    byte val = 0;
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = val++;
    }

    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    byte[] readValues = SerializationUtils.readFully(bais);

    assertNotSame(readValues, bytes);
    assertArrayEquals(bytes, readValues);
  }

  private ByteArrayInputStream byteStream(int... ints) {
    byte[] bytes = bytes(ints);
    return new ByteArrayInputStream(bytes);
  }

  private ByteArrayInputStream byteStream(byte[] bytes) {
    return new ByteArrayInputStream(bytes);
  }
}
