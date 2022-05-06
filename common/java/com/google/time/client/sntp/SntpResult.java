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

package com.google.time.client.sntp;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.Ticks;
import com.google.time.client.sntp.impl.Timestamp64;
import java.net.InetAddress;

/**
 * The interface for a value object containing information related to a successful SNTP
 * client/server interaction. This includes the calculated current instant and the ticks when it
 * applied, via {@link #getResultInstant()} and {@link #getResultTicks()} respectively, and
 * request/response metadata such as latency and server information that could influence accuracy.
 */
public abstract class SntpResult {

  /**
   * The default instant that can be used as the threshold for interpreting NTP timestamps that
   * don't have era context. This value is the mid-point in the NTP epoch and corresponds to
   * "1968-01-20T03:14:07Z".
   */
  public static final Instant DEFAULT_ERA_THRESHOLD =
      Timestamp64.fromComponents(0x7FFF_FFFFL, 0).toInstant(0);

  /** Returns the root delay value. The conversion to Duration can be lossy. */
  public abstract Duration getRootDelayDuration();

  /** Returns the NTP response root dispersion. */
  public abstract Duration getRootDispersionDuration();

  /**
   * Returns the poll interval as a duration by calculating 2-to-the-power of the stored poll
   * interval exponent value.
   *
   * @throws InvalidNtpValueException if the value read is outside the supported range (0-17)
   */
  public abstract Duration getPollInterval() throws InvalidNtpValueException;

  /** Returns the precision exponent value. */
  public abstract int getPrecisionExponent();

  /**
   * Returns the NTP response 4 character reference identifier as ASCII.
   *
   * <p>For strata 0, this field is used to hold the "KISS" code (NTPv4). For strata 1, this field
   * is used to hold an ASCII string. For strata 2-15, this field is used to hold values like IPv4
   * addresses.
   *
   * @see #getReferenceIdentifier()
   */
  public abstract String getReferenceIdentifierAsString();

  /**
   * Returns the NTP response 4 byte reference identifier as raw bytes.
   *
   * <p>For strata 0, this field is used to hold the "KISS" code (NTPv4). For strata 1, this field
   * is used to hold an ASCII string. For strata 2-15, this field is used to hold values like IPv4
   * addresses.
   *
   * @see #getReferenceIdentifierAsString()
   */
  public abstract byte[] getReferenceIdentifier();

  /**
   * Returns the NTP "reference timestamp": the instant when the server last received an update from
   * its own source as an {@link Instant}. This method uses {@link #DEFAULT_ERA_THRESHOLD} to
   * determine the NTP era.
   */
  public Instant getReferenceTimestampAsInstant() {
    return getReferenceTimestampAsInstant(DEFAULT_ERA_THRESHOLD);
  }

  /**
   * Returns the NTP "reference timestamp": the instant when the server last received an update from
   * its own source as an {@link Instant}, This method uses the specified threshold to determine the
   * NTP era.
   */
  public abstract Instant getReferenceTimestampAsInstant(Instant eraThreshold);

  /** The {@link InetAddress} that was used to communicate with the server. */
  public abstract InetAddress getServerInetAddress();

  /** The UDP port that was used to communicate with the server. */
  public abstract int getServerPort();

  /**
   * The version reported by the client in the NTP request.
   *
   * <p>Note: NTP servers typically respond with the version of the request.
   */
  public abstract int getRequestVersion();

  /**
   * The version reported by the server in the NTP response.
   *
   * <p>Note: NTP servers typically respond with the version of the request.
   */
  public abstract int getResponseVersion();

  /** The stratum reported by the server in the NTP response. */
  public abstract int getStratum();

  /**
   * Returns the approximate duration that the NTP request and NTP response were in transit (i.e.
   * due to network latency) as computed by the client by taking the total end-to-end duration and
   * subtracting the processing duration reported by the server.
   */
  public abstract Duration getRoundTripDuration();

  /**
   * Returns the client offset, i.e. the adjustment that was needed to the client instant source at
   * {@link #getResultTicks()}.
   */
  public abstract Duration getClientOffset();

  /**
   * Returns the ticks according to the client ticker associated with the calculated {@link
   * #getResultInstant()}.
   */
  public abstract Ticks getResultTicks();

  /**
   * Returns the {@link InstantSource} used by the client and associated with {@link
   * #getResultInstant()}.
   */
  public abstract InstantSource getInstantSource();

  /** Returns the computed instant at {@link #getResultTicks()} according to the NTP algorithm. */
  public abstract Instant getResultInstant();
}
