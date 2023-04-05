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

import com.google.time.client.base.Instant;
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.annotations.VisibleForTesting;
import com.google.time.client.base.impl.Objects;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;

/**
 * An NTP message. A minimal NTP message consists of the fixed fields ({@link NtpHeader}). In future
 * an NTP message may also contain extension fields for protocols like NTS.
 */
public final class NtpMessage {

  /** The NTP header / fixed fields. */
  private final NtpHeader header;

  /** Creates an {@link NtpMessage} that just consists of the supplied {@link NtpHeader}. */
  public static NtpMessage create(NtpHeader header) {
    return new NtpMessage(header);
  }

  /** Creates a basic SNTP request message. */
  public static NtpMessage createSntpRequest(
      int version,
      boolean clientDataMinimization,
      Random random,
      InstantSource clientInstantSource) {
    NtpHeader.Builder requestHeaderBuilder =
        NtpHeader.Builder.createEmpty()
            .setVersionNumber(version)
            .setMode(NtpHeader.NTP_MODE_CLIENT);

    // Since it doesn't really matter what we send here (the server shouldn't use it for anything
    // except round-tripping), the transmit timestamp can be different from the value actually
    // used by the client.
    Timestamp64 requestTransmitTimestamp;
    if (clientDataMinimization) {
      // As per: https://datatracker.ietf.org/doc/html/draft-ietf-ntp-data-minimization-04
      // Using an entirely random timestamp is better than revealing client clock data.
      long eraSeconds = random.nextInt() & 0xFFFF_FFFFL;
      requestTransmitTimestamp = Timestamp64.fromComponents(eraSeconds, random.nextInt());
    } else {
      Instant requestTransmitInstant = clientInstantSource.instant();
      requestTransmitTimestamp = Timestamp64.fromInstant(requestTransmitInstant);
      if (clientInstantSource.getPrecision() <= InstantSource.PRECISION_MILLIS) {
        // requestTransmitTimestamp is treated as a nonce, so randomize the sub-millis nanos to
        // ensure it is less predictable. This introduces an error, but only up to 1 millis, e.g.
        // requestTransmitTimestamp could now be in the future or in the past, but less than 1
        // millis.
        // The value is not used by the client again, and the server also shouldn't be using it for
        // anything that affects the response we get (besides replaying it back to the client).
        requestTransmitTimestamp = requestTransmitTimestamp.randomizeSubMillis(random);
      }
    }
    requestHeaderBuilder.setTransmitTimestamp(requestTransmitTimestamp);
    NtpHeader requestHeader = requestHeaderBuilder.build();
    Builder requestBuilder = new Builder().setHeader(requestHeader);
    return requestBuilder.build();
  }

  /**
   * Creates an {@link NtpHeader} from a datagram packet. Information about the origin of the
   * message is also retrieved from the supplied packet.
   */
  public static NtpMessage fromDatagramPacket(DatagramPacket responsePacket) {
    byte[] bytes = responsePacket.getData();
    NtpHeader ntpHeader = NtpHeader.fromBytes(bytes, 0);
    return new NtpMessage(ntpHeader);
  }

  /**
   * Creates an {@link NtpHeader} by <em>wrapping</em> the supplied array, i.e. without copying the
   * array.
   *
   * @param header the NTP header
   */
  private NtpMessage(NtpHeader header) {
    this.header = Objects.requireNonNull(header, "header");
  }

  /** Returns the mandatory, fixed fields associated with an NTP message. */
  public NtpHeader getHeader() {
    return header;
  }

  /** Returns a copy of the message data as a {@code byte[]}. */
  @VisibleForTesting
  public byte[] toBytes() {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    byte[] headerBytes = header.toBytes();
    byteStream.write(headerBytes, 0, headerBytes.length);
    return byteStream.toByteArray();
  }

  /** Returns a copy of the message data as a {@link DatagramPacket}. */
  public DatagramPacket toDatagramPacket(InetAddress inetAddress, int port) {
    byte[] bytes = toBytes();
    return new DatagramPacket(bytes, bytes.length, inetAddress, port);
  }

  /** Returns a {@link Builder} initialized from this message. */
  public Builder toBuilder() {
    return new Builder(header);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NtpMessage)) {
      return false;
    }
    NtpMessage that = (NtpMessage) o;
    return header.equals(that.header);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(header);
  }

  @Override
  public String toString() {
    return "NtpMessage{" + "header=" + header + '}';
  }

  /** A builder of {@link NtpMessage} instances. */
  public static final class Builder {

    private NtpHeader header;

    /** Create an empty instance. */
    public Builder() {}

    private Builder(NtpHeader header) {
      this.header = header;
    }

    /** Sets the fixed fields header, must not be {@code null}. */
    public Builder setHeader(NtpHeader header) {
      this.header = Objects.requireNonNull(header);
      return this;
    }

    /** Builds an {@link NtpMessage}. */
    public NtpMessage build() {
      return new NtpMessage(header);
    }
  }
}
