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

import com.google.time.client.base.Duration;
import com.google.time.client.base.impl.Objects;
import com.google.time.client.sntp.InvalidNtpValueException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * A strongly-typed facade around an NTPv3 UDP packet {@code byte[]} and associated information that
 * allows manipulation / retrieval of the contents. Only basic validation is performed when the
 * wrapper is created and individual accessor methods may throw exceptions if the content of the
 * {@code byte[]} content is found to be invalid at that point.
 *
 * <p>The convention used by this class is that invalid data when reading values (which may have
 * come from external sources) are treated as checked exceptions because they must be considered by
 * developers. When writing values, these are treated as runtime exceptions as they are most likely
 * caused by coding or config errors and should not be common.
 */
public final class NtpMessage {

  // @VisibleForTesting
  static final int LI_VN_MODE_OFFSET = 0;
  // @VisibleForTesting
  static final int STRATUM_OFFSET = 1;
  // @VisibleForTesting
  static final int POLL_OFFSET = 2;
  // @VisibleForTesting
  static final int PRECISION_OFFSET = 3;

  // @VisibleForTesting
  static final int ROOT_DELAY_OFFSET = 4;
  // @VisibleForTesting
  static final int ROOT_DISPERSION_OFFSET = 8;
  // @VisibleForTesting
  static final int REFERENCE_IDENTIFIER_OFFSET = 12;
  // @VisibleForTesting
  static final int REFERENCE_TIME_OFFSET = 16;
  // @VisibleForTesting
  static final int ORIGINATE_TIME_OFFSET = 24;
  // @VisibleForTesting
  static final int RECEIVE_TIME_OFFSET = 32;
  // @VisibleForTesting
  static final int TRANSMIT_TIME_OFFSET = 40;

  private static final int MINIMUM_NTP_PACKET_SIZE = 48;
  public static final int NTPV3_PACKET_SIZE = MINIMUM_NTP_PACKET_SIZE;
  public static final int NTP_MODE_CLIENT = 3;
  public static final int NTP_MODE_SERVER = 4;
  public static final int NTP_MODE_BROADCAST = 5;

  public static final int NTP_LEAP_NOSYNC = 3;
  public static final int NTP_STRATUM_DEATH = 0;
  public static final int NTP_STRATUM_MAX = 15;

  // RFC4330 (SNTPv4) / RFC5905 (NTPv4) say the valid range for poll interval is [4, 17], but
  // several NTP implementations appear to support down to 3. Google NTP returns zero. 17 appears to
  // be the high watermark arrived at in v4.
  private static final boolean STRICT_POLL_INTERVAL = false;
  private static final int MIN_POLL_INTERVAL_EXPONENT_VALUE = STRICT_POLL_INTERVAL ? 4 : 0;
  private static final int MAX_POLL_INTERVAL_EXPONENT_VALUE = 17;

  private static final int REFERENCE_IDENTIFIER_LENGTH = 4;

  /** Creates an empty {@link NtpMessage} for use with NTPv3. */
  public static NtpMessage createEmptyV3() {
    NtpMessage message = createEmpty(NTPV3_PACKET_SIZE);
    message.setVersionNumber(3);
    return message;
  }

  /**
   * Creates an {@link NtpMessage} from a datagram packet. Information about the origin of the
   * packet is also retrieved from the supplied packet.
   */
  public static NtpMessage fromDatagramPacket(DatagramPacket responsePacket) {
    byte[] bytes = responsePacket.getData();
    InetAddress originAddress =
        Objects.requireNonNull(responsePacket.getAddress(), "responsePacket.address");
    int originPort = responsePacket.getPort();
    return fromBytesCloned(bytes, originAddress, originPort);
  }

  /**
   * Creates an empty {@link NtpMessage} of the specified size.
   *
   * @param messageSizeBytes the size of the message to create
   * @throws IllegalArgumentException if messageSizeBytes is too small
   */
  private static NtpMessage createEmpty(int messageSizeBytes) {
    byte[] buffer = new byte[messageSizeBytes];
    return new NtpMessage(buffer, /*originAddress=*/ null, /*originPort=*/ null);
  }

  /**
   * Creates an {@link NtpMessage} from a raw {@code byte[]} by <em>cloning</em> the supplied array.
   *
   * @throws IllegalArgumentException if bytes is too small
   */
  private static NtpMessage fromBytesCloned(
      byte[] bytes, InetAddress originAddress, int originPort) {
    byte[] buffer = Arrays.copyOf(bytes, bytes.length);
    return new NtpMessage(buffer, originAddress, originPort);
  }

  /**
   * Creates an {@link NtpMessage} from a raw {@code byte[]} by <em>wrapping</em> the supplied
   * array.
   *
   * @throws IllegalArgumentException if bytes is too small
   */
  // @VisibleForTesting
  public static NtpMessage fromBytesForTests(
      byte[] bytes, InetAddress originAddress, Integer originPort) {
    return new NtpMessage(bytes, originAddress, originPort);
  }

  /** The NTP packet bytes. */
  private final byte[] buffer;

  /** The origin of the NTP packet. Usually set for response messages. */
  private InetAddress inetAddress;

  /** The origin of the NTP packet. Usually set for response messages. */
  private Integer port;

  /**
   * Creates an {@link NtpMessage} by <em>wrapping</em> the supplied array, i.e. without copying the
   * array.
   *
   * @param bytes the byte array to wrap
   * @param originAddress the origin IP address of the message, or null
   * @param originAddress the origin port of the message, or null
   * @throws IllegalArgumentException if bytes is too small
   */
  private NtpMessage(byte[] bytes, InetAddress originAddress, Integer originPort) {
    if (bytes.length < MINIMUM_NTP_PACKET_SIZE) {
      throw new IllegalArgumentException("byte[] too small for NTP packet: length=" + bytes.length);
    }

    buffer = Objects.requireNonNull(bytes, "bytes");
    inetAddress = originAddress;
    port = originPort;
  }

  /** Sets the origin IP address of the message, or {@code null} when not applicable / available. */
  public void setInetAddress(InetAddress inetAddress) {
    this.inetAddress = inetAddress;
  }

  /**
   * Returns the origin IP address of the message, or {@code null} when not applicable / available.
   */
  public InetAddress getInetAddress() {
    return inetAddress;
  }

  /** Sets the origin port of the message, or {@code null} if not applicable / available. */
  public void setPort(int port) {
    this.port = port;
  }

  /** Returns the origin port of the message, or {@code null} if not applicable / available. */
  public Integer getPort() {
    return port;
  }

  /** Returns the leap indicator. */
  public int getLeapIndicator() {
    int liVnMode = read8Unsigned(buffer, LI_VN_MODE_OFFSET);
    return (liVnMode >> 6) & 0b00000011;
  }

  /** Sets the leap indicator. */
  public void setLeapIndicator(int leapIndicator) {
    checkArgument(leapIndicator >= 0 && leapIndicator <= 3);
    buffer[LI_VN_MODE_OFFSET] =
        (byte) ((buffer[LI_VN_MODE_OFFSET] & 0b00111111) | (leapIndicator << 6));
  }

  /** Returns the protocol version number. */
  public int getVersionNumber() {
    int liVnMode = read8Unsigned(buffer, LI_VN_MODE_OFFSET);
    return (liVnMode >> 3) & 0b00000111;
  }

  /** Sets the protocol version number. */
  public void setVersionNumber(int versionNumber) {
    checkArgument(versionNumber >= 0 && versionNumber <= 7);
    buffer[LI_VN_MODE_OFFSET] =
        (byte) ((buffer[LI_VN_MODE_OFFSET] & 0b11000111) | (versionNumber << 3));
  }

  /** Returns the mode. */
  public int getMode() {
    int liVnMode = read8Unsigned(buffer, LI_VN_MODE_OFFSET);
    return liVnMode & 0b00000111;
  }

  /** Sets the mode. */
  public void setMode(int mode) {
    checkArgument(mode >= 0 && mode <= 7);
    buffer[LI_VN_MODE_OFFSET] = (byte) ((buffer[LI_VN_MODE_OFFSET] & 0b11111000) | mode);
  }

  /** Returns the stratum. */
  public int getStratum() {
    return read8Unsigned(buffer, STRATUM_OFFSET);
  }

  /** Sets the stratum. */
  public void setStratum(int stratum) {
    checkArgument(stratum >= 0 && stratum <= 255);
    buffer[STRATUM_OFFSET] = (byte) stratum;
  }

  /**
   * Returns the poll interval as a duration by calculating 2-to-the-power of the stored poll
   * interval exponent value.
   *
   * @throws InvalidNtpValueException if the value read is outside the supported range (0-17)
   */
  public Duration getPollIntervalAsDuration() throws InvalidNtpValueException {
    int exponent = getPollIntervalExponent();
    return pow2ToDuration(exponent);
  }

  /**
   * Returns the poll interval exponent.
   *
   * @throws InvalidNtpValueException if the value read is outside the supported range (0-17)
   */
  public int getPollIntervalExponent() throws InvalidNtpValueException {
    return checkReadValueInRange(
        MIN_POLL_INTERVAL_EXPONENT_VALUE,
        MAX_POLL_INTERVAL_EXPONENT_VALUE,
        read8Unsigned(buffer, POLL_OFFSET));
  }

  /** Sets the poll interval exponent. */
  public void setPollIntervalExponent(int exponent) {
    checkArgument(
        MIN_POLL_INTERVAL_EXPONENT_VALUE <= exponent
            && exponent <= MAX_POLL_INTERVAL_EXPONENT_VALUE);
    write8Unsigned(buffer, POLL_OFFSET, exponent);
  }

  /** Returns the precision exponent value. */
  public int getPrecisionExponent() {
    return read8Signed(buffer, PRECISION_OFFSET);
  }

  /** Sets the precision exponent value. */
  public void setPrecisionExponent(int precisionExponent) {
    if (precisionExponent >= 0 || precisionExponent < Byte.MIN_VALUE) {
      throw new IllegalArgumentException(Integer.toString(precisionExponent));
    }
    write8Signed(buffer, PRECISION_OFFSET, (byte) precisionExponent);
  }

  /** Returns 2-to-the-power of the precision exponent value. */
  public double getPrecision() {
    return Math.pow(2, getPrecisionExponent());
  }

  /** Returns the root delay value. The conversion to Duration can be lossy. */
  public Duration getRootDelayDuration() {
    return read32SignedFixedPointDuration(buffer, ROOT_DELAY_OFFSET);
  }

  /** Returns the root delay value as raw bytes. */
  public byte[] getRootDelay() {
    return readBytes(4, ROOT_DELAY_OFFSET);
  }

  /** Sets the root delay value as a Duration. The conversion from Duration can be lossy. */
  public void setRootDelayDuration(Duration rootDelay) {
    write32SignedFixedPointDuration(buffer, ROOT_DELAY_OFFSET, rootDelay);
  }

  /** Sets the root delay value as raw bytes. */
  public void setRootDelay(byte[] bytes) {
    if (bytes.length != 4) {
      throw new IllegalArgumentException();
    }
    writeBytes(bytes, 4, ROOT_DELAY_OFFSET);
  }

  /** Returns the root dispersion value. The conversion to Duration can be lossy. */
  public Duration getRootDispersionDuration() {
    return read32UnsignedFixedPointDuration(buffer, ROOT_DISPERSION_OFFSET);
  }

  /** Returns the root dispersion value. */
  public byte[] getRootDispersion() {
    return readBytes(4, ROOT_DISPERSION_OFFSET);
  }

  /** Sets the root dispersion value. The conversion from Duration can be lossy. */
  public void setRootDispersionDuration(Duration rootDispersion) {
    write32UnsignedFixedPointDuration(buffer, ROOT_DISPERSION_OFFSET, rootDispersion);
  }

  /** Sets the root dispersion value. */
  public void setRootDispersion(byte[] bytes) {
    if (bytes.length != 4) {
      throw new IllegalArgumentException();
    }
    writeBytes(bytes, 4, ROOT_DISPERSION_OFFSET);
  }

  /**
   * Returns the 4 byte reference identifier as ASCII. Bytes outside the ASCII range for printable
   * characters will be converted into character code U+FFFD.
   *
   * <p>For strata 0, this field is used to hold the "KISS" code (NTPv4). For strata 1, this field
   * is used to hold an ASCII string. For strata 2-15, this field is used to hold values like IPv4
   * addresses.
   *
   * @see #getReferenceIdentifier()
   */
  public String getReferenceIdentifierAsString() {
    return readAscii(buffer, REFERENCE_IDENTIFIER_OFFSET, REFERENCE_IDENTIFIER_LENGTH);
  }

  /**
   * Returns the 4 byte reference identifier as raw bytes.
   *
   * <p>For strata 0, this field is used to hold the "KISS" code (NTPv4). For strata 1, this field
   * is used to hold an ASCII string. For strata 2-15, this field is used to hold values like IPv4
   * addresses.
   *
   * @see #getReferenceIdentifierAsString()
   */
  public byte[] getReferenceIdentifier() {
    return readBytes(4, REFERENCE_IDENTIFIER_OFFSET);
  }

  /**
   * Sets the 4 character reference identifier as ASCII. Bytes outside the ASCII range for printable
   * characters will cause an {@link IllegalArgumentException}.
   *
   * <p>For strata 0, this field is used to hold the "KISS" code (NTPv4). For strata 1, this field
   * is used to hold an ASCII string. For strata 2-15, this field is used to hold values like IPv4
   * addresses.
   *
   * @see #setReferenceIdentifier(byte[])
   */
  public void setReferenceIdentifierAsString(String referenceIdentifier) {
    writeAscii(
        buffer, REFERENCE_IDENTIFIER_OFFSET, REFERENCE_IDENTIFIER_LENGTH, referenceIdentifier);
  }

  /**
   * Sets the 4 byte reference identifier as raw bytes.
   *
   * <p>For strata 0, this field is used to hold the "KISS" code (NTPv4). For strata 1, this field
   * is used to hold an ASCII string. For strata 2-15, this field is used to hold values like IPv4
   * addresses.
   *
   * @see #setReferenceIdentifierAsString(String)
   */
  public void setReferenceIdentifier(byte[] referenceIdentifierBytes) {
    if (referenceIdentifierBytes.length != REFERENCE_IDENTIFIER_LENGTH) {
      throw new IllegalArgumentException(
          "Argument must be 4 bytes exactly: " + Arrays.toString(referenceIdentifierBytes));
    }
    writeBytes(referenceIdentifierBytes, REFERENCE_IDENTIFIER_LENGTH, REFERENCE_IDENTIFIER_OFFSET);
  }

  /** Returns the "reference timestamp". */
  public Timestamp64 getReferenceTimestamp() {
    return readTimestamp64(buffer, REFERENCE_TIME_OFFSET);
  }

  /** Sets the "reference timestamp". */
  public void setReferenceTimestamp(Timestamp64 referenceTimestamp) {
    writeTimestamp64(buffer, REFERENCE_TIME_OFFSET, referenceTimestamp);
  }

  /** Returns the "originate timestamp". */
  public Timestamp64 getOriginateTimestamp() {
    return readTimestamp64(buffer, ORIGINATE_TIME_OFFSET);
  }

  /** Sets the "originate timestamp". */
  public void setOriginateTimestamp(Timestamp64 originateTimestamp) {
    writeTimestamp64(buffer, ORIGINATE_TIME_OFFSET, originateTimestamp);
  }

  /** Returns the "receive timestamp". */
  public Timestamp64 getReceiveTimestamp() {
    return readTimestamp64(buffer, RECEIVE_TIME_OFFSET);
  }

  /** Sets the "receive timestamp". */
  public void setReceiveTimestamp(Timestamp64 receiveTimestamp) {
    writeTimestamp64(buffer, RECEIVE_TIME_OFFSET, receiveTimestamp);
  }

  /** Returns the "transmit timestamp". */
  public Timestamp64 getTransmitTimestamp() {
    return readTimestamp64(buffer, TRANSMIT_TIME_OFFSET);
  }

  /** Sets the "transmit timestamp". */
  public void setTransmitTimestamp(Timestamp64 transmitTimestamp) {
    writeTimestamp64(buffer, TRANSMIT_TIME_OFFSET, transmitTimestamp);
  }

  /** Returns a copy of the packet data. */
  // @VisibleForTesting
  public byte[] toByteArray() {
    return readBytes(buffer.length, 0);
  }

  /** Returns a copy of the packet data as a {@link DatagramPacket}. */
  public DatagramPacket toDatagramPacket(InetAddress inetAddress, int port) {
    byte[] bytes = toByteArray();
    return new DatagramPacket(bytes, bytes.length, inetAddress, port);
  }

  @Override
  public String toString() {
    String pollInterval;
    try {
      pollInterval = getPollIntervalAsDuration().toString();
    } catch (InvalidNtpValueException e) {
      pollInterval = "{Invalid}";
    }

    return "NtpMessage{"
        + "address="
        + inetAddress
        + ", port="
        + port
        + ", leapIndicator="
        + getLeapIndicator()
        + ", versionNumber="
        + getVersionNumber()
        + ", mode="
        + getMode()
        + ", stratum="
        + getStratum()
        + ", pollInterval="
        + pollInterval
        + ", precisionExponent="
        + getPrecisionExponent()
        + ", rootDelay="
        + getRootDelayDuration()
        + ", rootDispersion="
        + getRootDispersionDuration()
        + ", referenceIdentifier='"
        + getReferenceIdentifierAsString()
        + '\''
        + ", referenceTimestamp="
        + getReferenceTimestamp()
        + ", originateTimestamp="
        + getOriginateTimestamp()
        + ", receiveTimestamp="
        + getReceiveTimestamp()
        + ", transmitTimestamp="
        + getTransmitTimestamp()
        + '}';
  }

  private byte[] readBytes(int byteCount, int offset) {
    byte[] bytes = new byte[byteCount];
    System.arraycopy(buffer, offset, bytes, 0, byteCount);
    return bytes;
  }

  private void writeBytes(byte[] bytes, int byteCount, int offset) {
    System.arraycopy(bytes, 0, buffer, offset, byteCount);
  }

  private static void checkArgument(boolean condition) {
    if (!condition) throw new IllegalArgumentException();
  }

  // @VisibleForTesting
  static byte read8Signed(byte[] buffer, int offset) {
    return buffer[offset];
  }

  // @VisibleForTesting
  static void write8Signed(byte[] buffer, int offset, byte value) {
    buffer[offset] = value;
  }

  // @VisibleForTesting
  static int read8Unsigned(byte[] buffer, int offset) {
    return buffer[offset] & 0xFF;
  }

  // @VisibleForTesting
  static void write8Unsigned(byte[] buffer, int offset, int value) {
    if (value < 0 || value > 255) {
      throw new IllegalArgumentException("value=" + value);
    }
    buffer[offset] = (byte) value;
  }

  private static int checkReadValueInRange(int minValueInc, int maxValueInc, int value)
      throws InvalidNtpValueException {
    if (value < minValueInc || value > maxValueInc) {
      throw new InvalidNtpValueException("Value out of range: " + value);
    }
    return value;
  }

  // @VisibleForTesting
  static Duration pow2ToDuration(int exponent) throws InvalidNtpValueException {
    checkReadValueInRange(0, 62, exponent);
    long seconds = 1L << exponent;
    return Duration.ofSeconds(seconds, 0);
  }

  /**
   * Reads up to {@code maxLength} bytes from {@code buffer} as ASCII values. '\0' is treated as a
   * terminator, byte values &lt; 32 or &gt; 126 are replaced by unicode value U+FFFD, other bytes
   * are interpreted as char values, i.e. mapped to UTF-8 code units.
   *
   * @param buffer the buffer to read from
   * @param offset the index to start reading from
   * @param maxLength the maximum characters to read
   * @return the String value read
   */
  // @VisibleForTesting
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
   * @param length the number of bytes to write
   * @param value the source of characters
   */
  // @VisibleForTesting
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
  // @VisibleForTesting
  static long read32Unsigned(byte[] buffer, int offset) {
    // Widen the int to a long, keep only the bottom 32 bits.
    return read32Signed(buffer, offset) & 0xFFFF_FFFFL;
  }

  /** Writes an unsigned 32-bit big endian number to the specified offset in the buffer. */
  // @VisibleForTesting
  static void write32Unsigned(byte[] buffer, int offset, long value) {
    if (value < 0 || value > 0xFFFF_FFFFL) {
      throw new IllegalArgumentException(Long.toString(value));
    }

    buffer[offset++] = (byte) (value >> 24);
    buffer[offset++] = (byte) (value >> 16);
    buffer[offset++] = (byte) (value >> 8);
    buffer[offset++] = (byte) (value);
  }

  /** Reads a signed 32-bit big endian number from the specified offset in the buffer. */
  // @VisibleForTesting
  static int read32Signed(byte[] buffer, int offset) {
    int i0 = buffer[offset] & 0xFF;
    int i1 = buffer[offset + 1] & 0xFF;
    int i2 = buffer[offset + 2] & 0xFF;
    int i3 = buffer[offset + 3] & 0xFF;

    return (i0 << 24) | (i1 << 16) | (i2 << 8) | i3;
  }

  /** Writes a signed 32-bit big endian number to the specified offset in the buffer. */
  // @VisibleForTesting
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
  // @VisibleForTesting
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
  // @VisibleForTesting
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
  // @VisibleForTesting
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
  // @VisibleForTesting
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
  // @VisibleForTesting
  static Timestamp64 readTimestamp64(byte[] buffer, int offset) {
    long seconds = read32Unsigned(buffer, offset);
    int fraction = read32Signed(buffer, offset + 4);
    return Timestamp64.fromComponents(seconds, fraction);
  }

  /** Writes {@code timestamp} as an NTP timestamp at the specified offset in the buffer. */
  // @VisibleForTesting
  static void writeTimestamp64(byte[] buffer, int offset, Timestamp64 timestamp) {
    long seconds = timestamp.getEraSeconds();
    int fraction = timestamp.getFractionBits();

    // Write seconds in big endian format (this will throw if it is outside the supported range).
    write32Unsigned(buffer, offset, seconds);
    offset += 4;

    // write fraction in big endian format
    write32Signed(buffer, offset, fraction);
  }
}
