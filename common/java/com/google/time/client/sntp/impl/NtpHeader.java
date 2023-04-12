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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.time.client.base.Duration;
import com.google.time.client.base.annotations.VisibleForTesting;
import com.google.time.client.base.impl.Objects;
import com.google.time.client.sntp.InvalidNtpValueException;
import java.util.Arrays;

/**
 * An NTP header {@code byte[]} consisting of the fixed fields that are present in all NTP messages.
 * Only basic validation is performed when the object is created from a {@code byte[]} and so
 * individual accessor methods may throw exceptions if the content is found to be invalid at that
 * point.
 *
 * <p>{@link NtpHeader} is immutable; a {@link Builder} exists to create or modify headers.
 *
 * <p>The convention used by this class / the builder for invalid data:
 *
 * <ul>
 *   <li>When reading invalid values (which may have come from external sources), these are treated
 *       as checked exceptions because they must be considered by developers.
 *   <li>When writing values, these are treated as runtime exceptions as they are most likely caused
 *       by coding or config errors.
 * </ul>
 */
public final class NtpHeader {

  @VisibleForTesting static final int LI_VN_MODE_OFFSET = 0;
  @VisibleForTesting static final int STRATUM_OFFSET = 1;
  @VisibleForTesting static final int POLL_OFFSET = 2;
  @VisibleForTesting static final int PRECISION_OFFSET = 3;

  @VisibleForTesting static final int ROOT_DELAY_OFFSET = 4;
  @VisibleForTesting static final int ROOT_DISPERSION_OFFSET = 8;
  @VisibleForTesting static final int REFERENCE_IDENTIFIER_OFFSET = 12;
  @VisibleForTesting static final int REFERENCE_TIME_OFFSET = 16;
  @VisibleForTesting static final int ORIGINATE_TIME_OFFSET = 24;
  @VisibleForTesting static final int RECEIVE_TIME_OFFSET = 32;
  @VisibleForTesting static final int TRANSMIT_TIME_OFFSET = 40;

  @VisibleForTesting static final int FIXED_FIELDS_SIZE = 48;
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

  // Anycast server, i.e. not SNTP server
  static final String KISS_CODE_ACST = "ACST";
  // Server auth failed
  static final String KISS_CODE_AUTH = "AUTH";
  // Autokey sequence failed
  static final String KISS_CODE_AUTO = "AUTO";
  // The association belongs to a broadcast server
  static final String KISS_CODE_BCST = "BCST";
  // Cryptographic authentication or identification failed
  static final String KISS_CODE_CRYP = "CRYP";
  // Access denied by remote server
  static final String KISS_CODE_DENY = "DENY";
  // Lost peer in symmetric mode
  static final String KISS_CODE_DROP = "DROP";
  // Access denied due to local policy
  static final String KISS_CODE_RSTR = "RSTR";
  // The association belongs to a manycast server
  static final String KISS_CODE_MCST = "MCST";
  // No key found.  Either the key was never installed or is not trusted
  static final String KISS_CODE_NKEY = "NKEY";
  // Rate exceeded.  The server has temporarily denied access because the client exceeded the rate
  // threshold
  static final String KISS_CODE_RATE = "RATE";
  // Somebody is tinkering with the association from a remote host running ntpdc.  Not to worry
  // unless some rascal has stolen your keys
  static final String KISS_CODE_RMOT = "RMOT";
  // The association has not yet synchronized for the first time
  static final String KISS_CODE_INIT = "INIT";
  // A step change in system time has occurred, but the association has not yet resynchronized
  static final String KISS_CODE_STEP = "STEP";

  /**
   * Creates an {@link NtpHeader} from a {@code byte[]} by <em>cloning</em> a section of the
   * supplied array.
   *
   * @param bytes the bytes to read from
   * @param offset the bytes to start reading from
   * @throws IllegalArgumentException if bytes + offset doesn't provide enough bytes
   */
  public static NtpHeader fromBytes(byte[] bytes, int offset) {
    if (bytes.length - FIXED_FIELDS_SIZE < offset) {
      throw new IllegalArgumentException(
          "Not enough bytes remaining with length=" + bytes.length + " and offset=" + offset);
    }
    byte[] clonedBytes = new byte[FIXED_FIELDS_SIZE];
    System.arraycopy(bytes, offset, clonedBytes, 0, FIXED_FIELDS_SIZE);
    return new NtpHeader(clonedBytes);
  }

  /** The NTP header bytes. */
  private final byte[] bytes;

  /**
   * Creates an {@link NtpHeader} by <em>wrapping</em> the supplied array, i.e. without copying the
   * array.
   *
   * @param bytes the byte array to wrap containing the fixed fields
   * @throws IllegalArgumentException if bytes is the wrong size
   */
  private NtpHeader(byte[] bytes) {
    if (bytes.length != FIXED_FIELDS_SIZE) {
      throw new IllegalArgumentException(
          "NTP header byte[] incorrect size:"
              + " length="
              + bytes.length
              + ", expected="
              + FIXED_FIELDS_SIZE);
    }

    this.bytes = Objects.requireNonNull(bytes, "bytes");
  }

  /** Returns the leap indicator. */
  public int getLeapIndicator() {
    int liVnMode = SerializationUtils.read8Unsigned(bytes, LI_VN_MODE_OFFSET);
    return (liVnMode >> 6) & 0b00000011;
  }

  /** Returns the protocol version number. */
  public int getVersionNumber() {
    int liVnMode = SerializationUtils.read8Unsigned(bytes, LI_VN_MODE_OFFSET);
    return (liVnMode >> 3) & 0b00000111;
  }

  /** Returns the mode. */
  public int getMode() {
    int liVnMode = SerializationUtils.read8Unsigned(bytes, LI_VN_MODE_OFFSET);
    return liVnMode & 0b00000111;
  }

  /** Returns the stratum. */
  public int getStratum() {
    return SerializationUtils.read8Unsigned(bytes, STRATUM_OFFSET);
  }

  /**
   * Returns the poll interval as a duration by calculating 2-to-the-power of the stored poll
   * interval exponent value.
   *
   * @throws InvalidNtpValueException if the value read is outside the supported range (0-17)
   */
  public Duration getPollIntervalAsDuration() throws InvalidNtpValueException {
    int exponent = getPollIntervalExponent();
    return SerializationUtils.pow2ToDuration(exponent);
  }

  /**
   * Returns the poll interval exponent.
   *
   * @throws InvalidNtpValueException if the value read is outside the supported range (0-17)
   */
  public int getPollIntervalExponent() throws InvalidNtpValueException {
    return SerializationUtils.checkReadValueInRange(
        MIN_POLL_INTERVAL_EXPONENT_VALUE,
        MAX_POLL_INTERVAL_EXPONENT_VALUE,
        SerializationUtils.read8Unsigned(bytes, POLL_OFFSET));
  }

  /** Returns the precision exponent value. */
  public int getPrecisionExponent() {
    return SerializationUtils.read8Signed(bytes, PRECISION_OFFSET);
  }

  /** Returns 2-to-the-power of the precision exponent value. */
  public double getPrecision() {
    return Math.pow(2, getPrecisionExponent());
  }

  /** Returns the root delay value. The conversion to Duration can be lossy. */
  public Duration getRootDelayDuration() {
    return SerializationUtils.read32SignedFixedPointDuration(bytes, ROOT_DELAY_OFFSET);
  }

  /** Returns the root dispersion value. The conversion to Duration can be lossy. */
  public Duration getRootDispersionDuration() {
    return SerializationUtils.read32UnsignedFixedPointDuration(bytes, ROOT_DISPERSION_OFFSET);
  }

  /** Returns the root dispersion value. */
  public byte[] getRootDispersion() {
    return readBytes(4, ROOT_DISPERSION_OFFSET);
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
    return SerializationUtils.readAscii(
        bytes, REFERENCE_IDENTIFIER_OFFSET, REFERENCE_IDENTIFIER_LENGTH);
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

  /** Returns the "reference timestamp". */
  public Timestamp64 getReferenceTimestamp() {
    return SerializationUtils.readTimestamp64(bytes, REFERENCE_TIME_OFFSET);
  }

  /** Returns the "originate timestamp". */
  public Timestamp64 getOriginateTimestamp() {
    return SerializationUtils.readTimestamp64(bytes, ORIGINATE_TIME_OFFSET);
  }

  /** Returns the "receive timestamp". */
  public Timestamp64 getReceiveTimestamp() {
    return SerializationUtils.readTimestamp64(bytes, RECEIVE_TIME_OFFSET);
  }

  /** Returns the "transmit timestamp". */
  public Timestamp64 getTransmitTimestamp() {
    return SerializationUtils.readTimestamp64(bytes, TRANSMIT_TIME_OFFSET);
  }

  /** Returns a copy of the packet data. */
  @VisibleForTesting
  public byte[] toBytes() {
    return bytes.clone();
  }

  /** Returns a {@link NtpMessage.Builder} initialized from this instance. */
  public Builder toBuilder() {
    return new Builder(bytes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NtpHeader ntpHeader = (NtpHeader) o;
    return Arrays.equals(bytes, ntpHeader.bytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }

  @Override
  public String toString() {
    String pollInterval;
    try {
      pollInterval = getPollIntervalAsDuration().toString();
    } catch (InvalidNtpValueException e) {
      pollInterval = "{Invalid}";
    }

    return "NtpHeader{"
        + "leapIndicator="
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
    System.arraycopy(this.bytes, offset, bytes, 0, byteCount);
    return bytes;
  }

  static void checkArgument(boolean condition) {
    if (!condition) throw new IllegalArgumentException();
  }

  /** A builder of {@link NtpHeader} instances. */
  public static final class Builder {

    /* @NonNull */ private final byte[] bytes;

    /** Creates an empty {@link NtpHeader.Builder} with the version number preset to 3. */
    public static Builder createEmptyV3() {
      Builder builder = new Builder().setVersionNumber(3);
      return builder;
    }

    /** Creates an empty {@link NtpHeader.Builder}. */
    public static Builder createEmpty() {
      return new Builder();
    }

    private Builder() {
      bytes = new byte[FIXED_FIELDS_SIZE];
    }

    private Builder(byte[] bytes) {
      this.bytes = bytes.clone();
    }

    /** Sets the leap indicator. */
    @CanIgnoreReturnValue
    public Builder setLeapIndicator(int leapIndicator) {
      checkArgument(leapIndicator >= 0 && leapIndicator <= 3);
      bytes[LI_VN_MODE_OFFSET] =
          (byte) ((bytes[LI_VN_MODE_OFFSET] & 0b00111111) | (leapIndicator << 6));
      return this;
    }

    /** Sets the protocol version number. */
    @CanIgnoreReturnValue
    public Builder setVersionNumber(int versionNumber) {
      checkArgument(versionNumber >= 0 && versionNumber <= 7);
      bytes[LI_VN_MODE_OFFSET] =
          (byte) ((bytes[LI_VN_MODE_OFFSET] & 0b11000111) | (versionNumber << 3));
      return this;
    }

    /** Sets the mode. */
    @CanIgnoreReturnValue
    public Builder setMode(int mode) {
      checkArgument(mode >= 0 && mode <= 7);
      bytes[LI_VN_MODE_OFFSET] = (byte) ((bytes[LI_VN_MODE_OFFSET] & 0b11111000) | mode);
      return this;
    }

    /** Sets the stratum. */
    @CanIgnoreReturnValue
    public Builder setStratum(int stratum) {
      checkArgument(stratum >= 0 && stratum <= 255);
      bytes[STRATUM_OFFSET] = (byte) stratum;
      return this;
    }

    /** Sets the poll interval exponent. */
    @CanIgnoreReturnValue
    public Builder setPollIntervalExponent(int exponent) {
      checkArgument(
          MIN_POLL_INTERVAL_EXPONENT_VALUE <= exponent
              && exponent <= MAX_POLL_INTERVAL_EXPONENT_VALUE);
      SerializationUtils.write8Unsigned(bytes, POLL_OFFSET, exponent);
      return this;
    }

    /** Sets the precision exponent value. */
    @CanIgnoreReturnValue
    public Builder setPrecisionExponent(int precisionExponent) {
      if (precisionExponent >= 0 || precisionExponent < Byte.MIN_VALUE) {
        throw new IllegalArgumentException(Integer.toString(precisionExponent));
      }
      SerializationUtils.write8Signed(bytes, PRECISION_OFFSET, (byte) precisionExponent);
      return this;
    }

    /** Sets the root delay value as a Duration. The conversion from Duration can be lossy. */
    @CanIgnoreReturnValue
    public Builder setRootDelayDuration(Duration rootDelay) {
      SerializationUtils.write32SignedFixedPointDuration(bytes, ROOT_DELAY_OFFSET, rootDelay);
      return this;
    }

    /** Sets the root delay value as raw bytes. */
    @CanIgnoreReturnValue
    public Builder setRootDelay(byte[] bytes) {
      if (bytes.length != 4) {
        throw new IllegalArgumentException();
      }
      SerializationUtils.writeBytes(this.bytes, bytes, 4, ROOT_DELAY_OFFSET);
      return this;
    }

    /** Sets the root dispersion value. The conversion from Duration can be lossy. */
    @CanIgnoreReturnValue
    public Builder setRootDispersionDuration(Duration rootDispersion) {
      SerializationUtils.write32UnsignedFixedPointDuration(
          bytes, ROOT_DISPERSION_OFFSET, rootDispersion);
      return this;
    }

    /** Sets the root dispersion value. */
    @CanIgnoreReturnValue
    public Builder setRootDispersion(byte[] bytes) {
      if (bytes.length != 4) {
        throw new IllegalArgumentException();
      }
      SerializationUtils.writeBytes(this.bytes, bytes, 4, ROOT_DISPERSION_OFFSET);
      return this;
    }

    /**
     * Sets the 4 character reference identifier as ASCII. Bytes outside the ASCII range for
     * printable characters will cause an {@link IllegalArgumentException}.
     *
     * <p>For strata 0, this field is used to hold the "KISS" code (NTPv4). For strata 1, this field
     * is used to hold an ASCII string. For strata 2-15, this field is used to hold values like IPv4
     * addresses.
     *
     * @see #setReferenceIdentifier(byte[])
     */
    @CanIgnoreReturnValue
    public Builder setReferenceIdentifierAsString(String referenceIdentifier) {
      SerializationUtils.writeAscii(
          bytes, REFERENCE_IDENTIFIER_OFFSET, REFERENCE_IDENTIFIER_LENGTH, referenceIdentifier);
      return this;
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
    @CanIgnoreReturnValue
    public Builder setReferenceIdentifier(byte[] referenceIdentifierBytes) {
      if (referenceIdentifierBytes.length != REFERENCE_IDENTIFIER_LENGTH) {
        throw new IllegalArgumentException(
            "Argument must be 4 bytes exactly: " + Arrays.toString(referenceIdentifierBytes));
      }
      SerializationUtils.writeBytes(
          bytes,
          referenceIdentifierBytes,
          REFERENCE_IDENTIFIER_LENGTH,
          REFERENCE_IDENTIFIER_OFFSET);
      return this;
    }

    /** Sets the "reference timestamp". */
    @CanIgnoreReturnValue
    public Builder setReferenceTimestamp(Timestamp64 referenceTimestamp) {
      SerializationUtils.writeTimestamp64(bytes, REFERENCE_TIME_OFFSET, referenceTimestamp);
      return this;
    }

    /** Sets the "originate timestamp". */
    @CanIgnoreReturnValue
    public Builder setOriginateTimestamp(Timestamp64 originateTimestamp) {
      SerializationUtils.writeTimestamp64(bytes, ORIGINATE_TIME_OFFSET, originateTimestamp);
      return this;
    }

    /** Sets the "receive timestamp". */
    @CanIgnoreReturnValue
    public Builder setReceiveTimestamp(Timestamp64 receiveTimestamp) {
      SerializationUtils.writeTimestamp64(bytes, RECEIVE_TIME_OFFSET, receiveTimestamp);
      return this;
    }

    /** Sets the "transmit timestamp". */
    @CanIgnoreReturnValue
    public Builder setTransmitTimestamp(Timestamp64 transmitTimestamp) {
      SerializationUtils.writeTimestamp64(bytes, TRANSMIT_TIME_OFFSET, transmitTimestamp);
      return this;
    }

    /** Builds an {@link NtpHeader}. */
    public NtpHeader build() {
      return NtpHeader.fromBytes(bytes, 0);
    }
  }
}
