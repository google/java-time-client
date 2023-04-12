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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.time.client.base.Duration;
import com.google.time.client.sntp.InvalidNtpValueException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** A collection of utility methods for reading and writing binary values in different encodings. */
public class SerializationUtils {

  /**
   * Writes the specified number of bytes from {@code bytes} starting at offset 0, into {@code
   * buffer} starting at {@code offset}. Equivalent to {@code System.arraycopy(bytes, 0, buffer,
   * offset, byteCount)};
   */
  static void writeBytes(byte[] buffer, byte[] bytes, int byteCount, int offset) {
    System.arraycopy(bytes, 0, buffer, offset, byteCount);
  }

  static byte read8Signed(byte[] buffer, int offset) {
    return buffer[offset];
  }

  static void write8Signed(byte[] buffer, int offset, byte value) {
    buffer[offset] = value;
  }

  static int read8Unsigned(byte[] buffer, int offset) {
    return buffer[offset] & 0xFF;
  }

  static void write8Unsigned(byte[] buffer, int offset, int value) {
    if (value < 0 || value > 255) {
      throw new IllegalArgumentException("value=" + value);
    }
    buffer[offset] = (byte) value;
  }

  /**
   * Reads up to {@code maxLength} buffer from {@code buffer} as ASCII values. '\0' is treated as a
   * terminator, byte values &lt; 32 or &gt; 126 are replaced by unicode value U+FFFD, other buffer
   * are interpreted as char values, i.e. mapped to UTF-8 code units.
   *
   * @param buffer the buffer to read from
   * @param offset the index to start reading from
   * @param maxLength the maximum characters to read
   * @return the String value read
   */
  static String readAscii(byte[] buffer, int offset, int maxLength) {
    StringBuilder sb = new StringBuilder(maxLength);
    for (int i = offset; i < offset + maxLength; i++) {
      char c = (char) (buffer[i] & 0xFF);
      if (c == '\0') {
        break;
      } else if (c < 32 || c > 126) {
        sb.append('�');
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * Writes characters from {@code value} into buffer as ASCII values by directly mapping the char
   * code units to ASCII. {@code value} can be shorter than {@code length}, in which case zeros will
   * be written for the missing characters. If {@code value} is longer than {@code length} or chars
   * from it fall outside the UTF-8 code range 32-126 then an {@link IllegalArgumentException} will
   * be thrown.
   *
   * @param buffer the buffer to write to
   * @param offset the index to start writing to
   * @param length the number of buffer to write
   * @param value the source of characters
   */
  static void writeAscii(byte[] buffer, int offset, int length, String value) {
    if (value.length() > length) {
      throw new IllegalArgumentException("String too long:" + Integer.toString(length));
    }

    for (int i = 0; i < length; i++) {
      char c;
      if (i < value.length()) {
        c = value.charAt(i);
        if (c < 32 || c > 126) {
          throw new IllegalArgumentException("Invalid char: " + value);
        }
      } else {
        c = 0;
      }
      write8Unsigned(buffer, offset++, c);
    }
  }

  /** Reads an unsigned 32-bit big endian number from the specified offset in the buffer. */
  static long read32Unsigned(byte[] buffer, int offset) {
    // Widen the int to a long, keep only the bottom 32 bits.
    return read32Signed(buffer, offset) & 0xFFFF_FFFFL;
  }

  /** Writes an unsigned 32-bit big endian number to the specified offset in the buffer. */
  static void write32Unsigned(byte[] buffer, int offset, long value) {
    if (value < 0 || value > 0xFFFF_FFFFL) {
      throw new IllegalArgumentException(Long.toString(value));
    }

    buffer[offset++] = (byte) (value >> 24);
    buffer[offset++] = (byte) (value >> 16);
    buffer[offset++] = (byte) (value >> 8);
    buffer[offset++] = (byte) (value);
  }

  /** Writes an unsigned 16-bit big endian number to the specified offset in the buffer. */
  public static void write16Unsigned(byte[] buffer, int offset, int value) {
    check16Unsigned(value);
    buffer[offset++] = (byte) (value >> 8);
    buffer[offset++] = (byte) (value);
  }

  /**
   * Throws an {@link IllegalArgumentException} if {@code value} is outside the supported range for
   * an unsigned 16-bit value.
   */
  @CanIgnoreReturnValue
  public static int check16Unsigned(int value) {
    if (value < 0 || value > 0xFFFF) {
      throw new IllegalArgumentException(
          "Value out of range 0 to 0xFFFF," + " value=" + Integer.toHexString(value));
    }
    return value;
  }

  /**
   * Reads two bytes from {@code buffer} at {@code offset} in network byte order interpreting them
   * as an unsigned 16-bit value.
   */
  public static int read16Unsigned(byte[] buffer, int offset) {
    int i0 = buffer[offset] & 0xFF;
    int i1 = buffer[offset + 1] & 0xFF;
    return (i0 << 8) | i1;
  }

  public static void write16UnsignedList(byte[] buffer, int offset, List<Integer> values) {
    for (Integer value : values) {
      write16Unsigned(buffer, offset, value);
      offset += 2;
    }
  }

  /** Reads a signed 32-bit big endian number from the specified offset in the buffer. */
  static int read32Signed(byte[] buffer, int offset) {
    int i0 = buffer[offset] & 0xFF;
    int i1 = buffer[offset + 1] & 0xFF;
    int i2 = buffer[offset + 2] & 0xFF;
    int i3 = buffer[offset + 3] & 0xFF;

    return (i0 << 24) | (i1 << 16) | (i2 << 8) | i3;
  }

  /** Writes a signed 32-bit big endian number to the specified offset in the buffer. */
  static void write32Signed(byte[] buffer, int offset, int value) {
    buffer[offset++] = (byte) (value >>> 24);
    buffer[offset++] = (byte) (value >>> 16);
    buffer[offset++] = (byte) (value >>> 8);
    buffer[offset++] = (byte) (value);
  }

  /**
   * Reads a duration that has been encoded as an unsigned 32-bit NTP duration value. The conversion
   * to {@link Duration} is lossy.
   */
  static Duration read32UnsignedFixedPointDuration(byte[] buffer, int offset) {
    long value = read32Unsigned(buffer, offset);

    //                     1                   2                   3
    // 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    // |         Seconds              |  Seconds Fraction (0-padded) +
    // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

    long seconds = value >> 16;
    long fraction = value & 0xFFFF;
    return createDuration(seconds, fraction);
  }

  /**
   * Writes a duration encoded as an unsigned 32-bit NTP duration value. The conversion from {@link
   * Duration} is lossy. If the duration is outside the range representable as an unsigned 16-bit
   * number of seconds then {@link IllegalArgumentException} will be thrown.
   */
  static void write32UnsignedFixedPointDuration(byte[] buffer, int offset, Duration duration) {
    long seconds = duration.getSeconds();
    if (seconds < 0 || seconds > Character.MAX_VALUE) {
      throw new IllegalArgumentException(duration.toString());
    }

    int nanos = duration.getNano();
    int fraction = calculate16Fraction(nanos);
    int value = ((int) (seconds << 16)) | fraction;
    write32Signed(buffer, offset, value);
  }

  /**
   * Reads a duration that has been encoded as a signed 32-bit NTP duration value. The conversion to
   * {@link Duration} is lossy.
   */
  static Duration read32SignedFixedPointDuration(byte[] buffer, int offset) {
    int value = read32Signed(buffer, offset);

    //                     1                   2                   3
    // 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    // |         Seconds              |  Seconds Fraction (0-padded) +
    // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

    // The following line widens to a long, with sign-extend, then performs an arithmetic shift,
    // thus retaining the sign.
    long seconds = value >> 16;
    long fraction = value & 0xFFFF;
    return createDuration(seconds, fraction);
  }

  private static Duration createDuration(long seconds, long fraction) {
    long nanos = (fraction * NANOS_PER_SECOND) >>> 16;
    return Duration.ofSeconds(seconds, nanos);
  }

  /**
   * Writes a duration encoded as a signed 32-bit NTP duration value. The conversion from {@link
   * Duration} is lossy. If the duration is outside the range representable as an signed 16-bit
   * number of seconds then {@link IllegalArgumentException} will be thrown.
   */
  static void write32SignedFixedPointDuration(byte[] buffer, int offset, Duration duration) {
    long seconds = duration.getSeconds();
    if (seconds < Short.MIN_VALUE || seconds > Short.MAX_VALUE) {
      throw new IllegalArgumentException(duration.toString());
    }

    int nanos = duration.getNano();
    int fraction = calculate16Fraction(nanos);
    int value = ((int) (seconds << 16)) | fraction;
    write32Signed(buffer, offset, value);
  }

  private static int calculate16Fraction(int nanos) {
    // nanos is assumed to be < NANOS_PER_SECOND since it should come from a Duration.
    long value = nanos;
    return (int) (((value << 16L) / NANOS_PER_SECOND) & 0xFFFF);
  }

  /** Reads the NTP timestamp at the specified offset in the buffer. */
  static Timestamp64 readTimestamp64(byte[] buffer, int offset) {
    long seconds = read32Unsigned(buffer, offset);
    int fraction = read32Signed(buffer, offset + 4);
    return Timestamp64.fromComponents(seconds, fraction);
  }

  /** Writes {@code timestamp} as an NTP timestamp at the specified offset in the buffer. */
  static void writeTimestamp64(byte[] buffer, int offset, Timestamp64 timestamp) {
    long seconds = timestamp.getEraSeconds();
    int fraction = timestamp.getFractionBits();

    // Write seconds in big endian format (this will throw if it is outside the supported range).
    write32Unsigned(buffer, offset, seconds);
    offset += 4;

    // write fraction in big endian format
    write32Signed(buffer, offset, fraction);
  }

  static Duration pow2ToDuration(int exponent) throws InvalidNtpValueException {
    checkReadValueInRange(0, 62, exponent);
    long seconds = 1L << exponent;
    return Duration.ofSeconds(seconds, 0);
  }

  @CanIgnoreReturnValue
  static int checkReadValueInRange(int minValueInc, int maxValueInc, int value)
      throws InvalidNtpValueException {
    if (value < minValueInc || value > maxValueInc) {
      throw new InvalidNtpValueException("Value out of range: " + value);
    }
    return value;
  }

  /**
   * Reads the specified number of bytes or throws {@link java.io.EOFException} if the stream is
   * exhausted. The stream is not closed.
   */
  public static byte[] readStreamBytesExact(ByteArrayInputStream byteStream, int count)
      throws EOFException {
    byte[] bytes = new byte[count];
    if (byteStream.read(bytes, 0, count) != count) {
      throw new EOFException();
    }
    return bytes;
  }

  /**
   * Writes the specified value as an unsigned 16-bit value in network byte order to the supplied
   * stream. Throws {@link IllegalArgumentException} if the value is outside the supported range.
   */
  public static void write16Unsigned(ByteArrayOutputStream byteStream, int value) {
    check16Unsigned(value);
    byteStream.write((byte) (value >> 8));
    byteStream.write((byte) value);
  }

  /**
   * Reads 2-bytes from the supplied stream as an unsigned 16-bit value encoded in network byte
   * order. Throws {@link EOFException} if 2 bytes cannot be read.
   */
  public static int read16Unsigned(ByteArrayInputStream byteStream) throws EOFException {
    int byte1 = byteStream.read();
    if (byte1 == -1) {
      throw new EOFException();
    }
    int byte2 = byteStream.read();
    if (byte2 == -1) {
      throw new EOFException();
    }
    return ((byte1 & 0xFF) << 8) | (byte2 & 0xFF);
  }

  /**
   * Reads all bytes from the supplied stream until the supplied stream is exhausted (returns -1),
   * returning them as a byte array.
   */
  public static byte[] readFully(InputStream inputStream) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int count;
    while ((count = inputStream.read(buffer)) != -1) {
      byteArrayOutputStream.write(buffer, 0, count);
    }
    return byteArrayOutputStream.toByteArray();
  }
}
