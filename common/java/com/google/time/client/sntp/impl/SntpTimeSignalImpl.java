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

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.annotations.VisibleForTesting;
import com.google.time.client.base.impl.Objects;
import com.google.time.client.sntp.InvalidNtpValueException;
import com.google.time.client.sntp.SntpTimeSignal;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/** The real implementation of {@link SntpTimeSignal}. */
final class SntpTimeSignalImpl extends SntpTimeSignal {

  private final InetSocketAddress serverSocketAddress;
  private final NtpMessage response;
  private final Duration totalTransactionDuration;
  private final Duration roundTripDuration;
  private final Instant responseInstant;
  private final Duration clientOffset;
  private final Ticks resultTicks;
  private final InstantSource instantSource;
  private final Instant resultInstant;

  SntpTimeSignalImpl(
      InetSocketAddress serverSocketAddress,
      NtpMessage response,
      Duration roundTripDuration,
      Duration totalTransactionDuration,
      Instant responseInstant,
      Duration clientOffset,
      Ticks resultTicks,
      InstantSource instantSource,
      Instant resultInstant) {
    this.serverSocketAddress = Objects.requireNonNull(serverSocketAddress, "serverSocketAddress");
    this.response = Objects.requireNonNull(response, "response");

    // Some other values derived from the request, response or captured during the interaction.
    this.clientOffset = Objects.requireNonNull(clientOffset, "clientOffset");
    this.totalTransactionDuration =
        Objects.requireNonNull(totalTransactionDuration, "totalTransactionDuration");
    this.roundTripDuration = Objects.requireNonNull(roundTripDuration, "roundTripDuration");
    this.responseInstant = Objects.requireNonNull(responseInstant, "responseInstant");

    // The calculated instant and the ticks according to the ticker when it applied.
    this.resultTicks = Objects.requireNonNull(resultTicks, "resultTicks");

    this.instantSource = Objects.requireNonNull(instantSource, "instantSource");
    this.resultInstant = Objects.requireNonNull(resultInstant, "resultInstant");
  }

  @Override
  public Duration getRootDelayDuration() {
    return response.getHeader().getRootDelayDuration();
  }

  @Override
  public Duration getPollInterval() throws InvalidNtpValueException {
    return response.getHeader().getPollIntervalAsDuration();
  }

  @Override
  public int getPrecisionExponent() {
    return response.getHeader().getPrecisionExponent();
  }

  @Override
  public Duration getRootDispersionDuration() {
    return response.getHeader().getRootDispersionDuration();
  }

  @Override
  public String getReferenceIdentifierAsString() {
    return response.getHeader().getReferenceIdentifierAsString();
  }

  @Override
  public byte[] getReferenceIdentifier() {
    return response.getHeader().getReferenceIdentifier();
  }

  /**
   * Returns the NTP "reference timestamp": the instant when the server last received an update from
   * its own source.
   *
   * <p>This is not exposed as a public API as that would require also exposing {@link Timestamp64}.
   * Instead, see the {@link #getReferenceTimestampAsInstant} methods.
   */
  @VisibleForTesting
  Timestamp64 getReferenceTimestamp() {
    return response.getHeader().getReferenceTimestamp();
  }

  @Override
  public Instant getReferenceTimestampAsInstant(Instant eraThreshold) {
    Timestamp64 referenceTimestamp = getReferenceTimestamp();

    // When the reference timestamp is after the era threshold instant, the reference timestamp is
    // assumed to be from the same era, otherwise it is assumed to be from the next era.
    // For example: if the threshold instant is 1/1/1970, which corresponds to x seconds in the NTP
    // era 0, that means timestamp values < x will be considered to be in era 1, i.e. after 2036,
    // while values >= x will be considered to be in era 0, between 1970 and 2036. In practical
    // terms, that means this means passing 1/1/1970 will give correct answers for what the server
    // thought the time was between 1/1/1970 and some time in year 2106 (i.e. 2036+70).
    Timestamp64 eraCutoverTimestamp = Timestamp64.fromInstant(eraThreshold);
    int eraThresholdEra = Timestamp64.ntpEra(eraThreshold);
    int referenceTimestampEra =
        referenceTimestamp.compareTo(eraCutoverTimestamp) >= 0
            ? eraThresholdEra
            : eraThresholdEra + 1;
    return referenceTimestamp.toInstant(referenceTimestampEra);
  }

  @Override
  public InetAddress getServerInetAddress() {
    return Objects.requireNonNull(serverSocketAddress.getAddress());
  }

  @Override
  public int getServerPort() {
    return serverSocketAddress.getPort();
  }

  @Override
  public int getResponseVersion() {
    return response.getHeader().getVersionNumber();
  }

  @Override
  public int getStratum() {
    return response.getHeader().getStratum();
  }

  @VisibleForTesting
  Instant getResponseInstant() {
    return responseInstant;
  }

  /**
   * Returns the client offset, i.e. the adjustment that was computed to be needed to the client
   * instant source at {@link #getResultTicks()}.
   */
  @Override
  public Duration getClientOffset() {
    return clientOffset;
  }

  /**
   * Returns the approximate duration it took from the NTP request leaving the client to the NTP
   * response being received as measured by the client.
   */
  @VisibleForTesting
  Duration getTotalTransactionDuration() {
    return totalTransactionDuration;
  }

  @Override
  public Duration getRoundTripDuration() {
    return roundTripDuration;
  }

  @Override
  public Ticks getResultTicks() {
    return resultTicks;
  }

  @Override
  public InstantSource getInstantSource() {
    return instantSource;
  }

  @Override
  public Instant getResultInstant() {
    return resultInstant;
  }

  @Override
  public String toString() {
    return "SntpTimeSignalImpl{"
        + "serverSocketAddress="
        + serverSocketAddress
        + ", response="
        + response
        + ", resultTicks="
        + resultTicks
        + ", resultInstant="
        + resultInstant
        + ", clientOffset="
        + clientOffset
        + ", totalTransactionDuration="
        + totalTransactionDuration
        + ", roundTripDuration="
        + roundTripDuration
        + ", responseInstant="
        + responseInstant
        + '}';
  }
}
