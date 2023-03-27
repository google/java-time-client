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
import com.google.time.client.base.Logger;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.annotations.NonFinalForTesting;
import com.google.time.client.base.annotations.VisibleForTesting;
import com.google.time.client.base.impl.Objects;
import com.google.time.client.base.impl.PlatformRandom;
import com.google.time.client.sntp.InvalidNtpResponseException;
import com.google.time.client.sntp.NtpServerNotReachableException;
import com.google.time.client.sntp.SntpResult;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Random;

/** Reusable, thread-safe, SNTP client logic. */
@NonFinalForTesting
public class SntpClientEngine {

  private final Logger logger;
  private final SntpConnector sntpConnector;
  private final Random random;
  // Default to true, as this is the safest state.
  private boolean clientDataMinimizationEnabled = true;

  public SntpClientEngine(Logger logger, SntpConnector sntpConnector) {
    this(logger, sntpConnector, PlatformRandom.getDefaultRandom());
  }

  public SntpClientEngine(Logger logger, SntpConnector sntpConnector, Random random) {
    this.logger = Objects.requireNonNull(logger, "logger");
    this.sntpConnector = Objects.requireNonNull(sntpConnector, "sntpConnector");
    this.random = Objects.requireNonNull(random, "random");
  }

  /** Sets whether client data minimization is enabled. The default is enabled. */
  public void setClientDataMinimizationEnabled(boolean enabled) {
    clientDataMinimizationEnabled = enabled;
  }

  /**
   * Requests the current server instant using the associated {@link SntpConnector}.
   *
   * @param clientInstantSource the {@link InstantSource} to use to obtain client time
   * @return the result of a successful SNTP request
   * @throws NtpServerNotReachableException if no servers could be reached, or they returned invalid
   *     responses
   */
  public SntpResult requestInstant(InstantSource clientInstantSource)
      throws NtpServerNotReachableException {

    NtpServerNotReachableException overallException = new NtpServerNotReachableException("");
    SntpConnector.Session session = sntpConnector.createSession();
    NtpMessage request = createRequest(clientDataMinimizationEnabled, random, clientInstantSource);
    if (logger.isLoggingFine()) {
      logger.fine("requestInstant(): Sending request: " + request);
    }
    try {
      while (session.canTrySend()) {
        SntpSessionResult sessionResult;
        try {
          sessionResult = session.trySend(request);
          if (logger.isLoggingFine()) {
            logger.fine("requestInstant(): Response received sessionResult=" + sessionResult);
          }
        } catch (NtpServerNotReachableException e) {
          if (logger.isLoggingFine()) {
            logger.fine("requestInstant(): Server not reachable", e);
          }
          overallException.addSuppressed(e);
          // Try the next server.
          continue;
        }

        try {
          SntpResultImpl sntpResult = processResponse(clientInstantSource, sessionResult);
          if (logger.isLoggingFine()) {
            logger.fine(
                "requestInstant(): Response from address="
                    + sessionResult.serverSocketAddress
                    + " is valid: sntpResult="
                    + sntpResult);
          }
          return sntpResult;
        } catch (InvalidNtpResponseException e) {
          if (logger.isLoggingFine()) {
            logger.fine(
                "requestInstant(): Response from address="
                    + sessionResult.serverSocketAddress
                    + " is invalid.",
                e);
          }
          session.reportInvalidResponse(sessionResult, e);
          overallException.addSuppressed(e);
          // Repeat the loop if possible.
        }
      }
      // The loop terminates early if it is successful. Reaching here must mean failure.
      logger.fine(
          "requestInstant(): Failed to communicate with all session servers", overallException);
      throw overallException;
    } catch (Exception e) {
      if (logger.isLoggingFine()) {
        logger.fine("requestInstant() failed", e);
      }
      throw e;
    }
  }

  @VisibleForTesting
  static NtpMessage createRequest(
      boolean clientDataMinimization, Random random, InstantSource clientInstantSource) {
    NtpHeader.Builder headerBuilder = NtpHeader.Builder.createEmptyV3();
    headerBuilder.setMode(NtpHeader.NTP_MODE_CLIENT);

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
    headerBuilder.setTransmitTimestamp(requestTransmitTimestamp);
    return NtpMessage.create(headerBuilder.build());
  }

  /**
   * Processes an SNTP response. Factored this way to enable easier unit testing around this step,
   * leaving the networking logic to be tested elsewhere.
   */
  @VisibleForTesting
  public static SntpResultImpl processResponse(
      InstantSource instantSource, SntpSessionResult sessionResult)
      throws InvalidNtpResponseException {

    NtpMessage request = sessionResult.request;
    InetSocketAddress serverSocketAddress = sessionResult.serverSocketAddress;
    NtpMessage response = sessionResult.response;
    NtpHeader responseHeader = response.getHeader();

    // Initial validation.
    validateServerResponse(request, response);

    Instant requestInstant = sessionResult.requestInstant;
    Ticks requestTimeTicks = sessionResult.requestTimeTicks;
    Ticks responseTimeTicks = sessionResult.responseTimeTicks;

    // T1 / [client]requestTimestamp
    final Timestamp64 requestTimestamp = Timestamp64.fromInstant(requestInstant);
    // T2 / [server]receiveTimestamp
    final Timestamp64 receiveTimestamp = responseHeader.getReceiveTimestamp();
    // T3 / [server]transmitTimestamp
    final Timestamp64 transmitTimestamp = responseHeader.getTransmitTimestamp();

    // totalTransactionDuration is the elapsed time between the request being sent and the
    // response being received. The spec obtains it using T4 - T1, but we use the
    // clientTicker to measure durations so use the associated ticks values.
    // totalTransactionDuration = T4 - T1
    //                          = [client]responseReceivedTicks - [client]requestSentTicks
    Duration totalTransactionDuration = requestTimeTicks.durationUntil(responseTimeTicks);

    // serverProcessingDuration is the elapsed time in the server.
    // serverProcessingDuration = T3 - T2
    //                          = [server]transmitTimestamp - [server]receiveTimestamp
    Duration serverProcessingDuration =
        Duration64.between(receiveTimestamp, transmitTimestamp).toDuration();

    // More validation
    if (serverProcessingDuration.compareTo(Duration.ZERO) < 0) {
      // Paranoia: serverProcessingDuration cannot be negative.
      throw new InvalidNtpResponseException(
          "serverProcessingDuration=" + serverProcessingDuration + " must not be negative");
    } else {
      if (serverProcessingDuration.compareTo(totalTransactionDuration) > 0) {
        // Paranoia: serverProcessingDuration cannot be greater than the elapsed time measured by
        // the client.
        throw new InvalidNtpResponseException(
            "serverProcessingDuration is too high."
                + " serverProcessingDuration="
                + serverProcessingDuration
                + ", totalTransactionDuration="
                + totalTransactionDuration);
      }
    }

    // Calculate the roundTripDuration, i.e. time it took for the client to get a response minus
    // the time spent in the server (as reported by the server).
    // According to RFC4330:
    // d is the roundtrip delay (the transit time of the request and response)
    // d = (T4 - T1) - (T3 - T2)
    //   = totalTransactionDuration - serverProcessingDuration
    Duration roundTripDuration = totalTransactionDuration.minus(serverProcessingDuration);

    // T4: [client]responseTimestamp = [client]requestTimestamp + totalTransactionDuration
    // i.e. it is established by dead reckoning. We do not consult the clientInstantSource again as
    // it can be adjusted and so cannot be trusted to measure elapsed time and could no longer be
    // consistent with requestTimestamp.
    final Instant responseInstant = requestInstant.plus(totalTransactionDuration);
    final Timestamp64 responseTimestamp = Timestamp64.fromInstant(responseInstant);

    Duration clientOffsetDuration =
        calculateClientOffset(
            requestTimestamp, receiveTimestamp, transmitTimestamp, responseTimestamp);

    Instant adjustedClientInstant = responseInstant.plus(clientOffsetDuration);

    return new SntpResultImpl(
        request,
        serverSocketAddress,
        response,
        roundTripDuration,
        totalTransactionDuration,
        responseInstant,
        clientOffsetDuration,
        responseTimeTicks,
        instantSource,
        adjustedClientInstant);
  }

  @VisibleForTesting
  static void validateServerResponse(NtpMessage request, NtpMessage response)
      throws InvalidNtpResponseException {

    NtpHeader requestHeader = request.getHeader();
    NtpHeader responseHeader = response.getHeader();

    // RFC 4330 / 5.  SNTP Client Operations: Suggested check 3.
    // T1 / [server]originateTimestamp should be the same as [client]requestTimestamp, because the
    // server should echo it back to us.
    // Do validation according to RFC: if the response originateTimestamp != transmitTimestamp
    // then perhaps this response is an attack.
    final Timestamp64 requestTransmitTimestamp = requestHeader.getTransmitTimestamp();
    final Timestamp64 originateTimestamp = responseHeader.getOriginateTimestamp();
    if (!requestTransmitTimestamp.equals(originateTimestamp)) {
      throw new InvalidNtpResponseException(
          "Received originateTimestamp="
              + originateTimestamp
              + " but expected "
              + requestTransmitTimestamp);
    }

    // RFC 4330 / 5.  SNTP Client Operations: Suggested check 4 includes LI == 0 as invalid.
    // But, that's also suggested as a valid value in the table above, so the check is not
    // implemented.

    // RFC 4330 / 5.  SNTP Client Operations: Suggested check 4. We do not support multicast here.
    // RFC 4330 / 4.  Message Format: In unicast and manycast modes, [...] the server sets it to 4
    //   (server) in the reply.
    if (responseHeader.getMode() != NtpHeader.NTP_MODE_SERVER) {
      throw new InvalidNtpResponseException("untrusted mode: " + responseHeader.getMode());
    }
    int stratum = responseHeader.getStratum();
    // RFC 4330 / 5.  SNTP Client Operations: Suggested check 4
    if ((stratum == NtpHeader.NTP_STRATUM_DEATH)) {
      // RFC 4330 / 6. SNTP Server Operations: If the server is not
      //   synchronized, the Stratum field is set to zero, and the Reference
      //   Identifier field is set to an ASCII error identifier...
      throw new InvalidNtpResponseException(
          "untrusted stratum: "
              + stratum
              + ", reference id="
              + responseHeader.getReferenceIdentifierAsString()
              + "("
              + Arrays.toString(responseHeader.getReferenceIdentifier())
              + ")");
    }
    // RFC 4330 / 5.  SNTP Client Operations: Suggested check 4
    if (responseHeader.getTransmitTimestamp().equals(Timestamp64.ZERO)) {
      throw new InvalidNtpResponseException("zero transmit timestamp");
    }

    // RFC 4330 / 5.  SNTP Client Operations: Suggested check 5 are not implemented

    // RFC 4330 / 4.  Message Format: On startup, servers set this field to 3 (clock not
    //   synchronized), and set this field to some other value when synchronized to the primary
    //   reference clock.
    // Checking this value is not suggested by the RFC, but it looks like it's an invalid /
    // unacceptable state.
    if (responseHeader.getLeapIndicator() == NtpHeader.NTP_LEAP_NOSYNC) {
      throw new InvalidNtpResponseException("Unsynchronized server");
    }

    // RFC 4330 / 5.  SNTP Client Operations: The table suggests the max is 15.
    if (stratum > NtpHeader.NTP_STRATUM_MAX) {
      throw new InvalidNtpResponseException("untrusted stratum: " + stratum);
    }

    if (responseHeader.getReferenceTimestamp().equals(Timestamp64.ZERO)) {
      throw new InvalidNtpResponseException("zero reference timestamp");
    }
  }

  /**
   * Calculates the adjustment needed for the client's instant source. This method is broken out
   * because this part of the NTP calculation is easy to get wrong and relies on binary
   * overflow/underflow behavior implemented by {@link Timestamp64}.
   *
   * @param requestTimestamp T1: [client]requestTimestamp
   * @param receiveTimestamp T2: [server]receiveTimestamp
   * @param transmitTimestamp T3: [server]transmitTimestamp
   * @param responseTimestamp T4: [client]responseTimestamp
   */
  @VisibleForTesting
  public static Duration calculateClientOffset(
      Timestamp64 requestTimestamp,
      Timestamp64 receiveTimestamp,
      Timestamp64 transmitTimestamp,
      Timestamp64 responseTimestamp) {
    // According to RFC4330:
    // t is the system clock (client instant source) offset (the adjustment we are trying to find)
    // t = ((T2 - T1) + (T3 - T4)) / 2
    //
    // Which is:
    // t = (([server]receiveTimestamp - [client]requestTimestamp)
    //       + ([server]transmitTimestamp - [client]responseTimestamp)) / 2
    //
    // Proof, given:
    // [server]receiveTimestamp = [client]requestTimestamp + transit + skew
    // [client]responseTimestamp = [server]transmitTimestamp + transit - skew
    // ... then:
    // t = (([client]requestTimestamp + transit + skew - [client]requestTimestamp) +
    //       ([server]transmitTimestamp - ([server]transmitTimestamp + transit - skew))) / 2
    //
    // ... now cancel [client]requestTimestamp
    // t = ((transit + skew) + ([server]transmitTimestamp - [server]transmitTimestamp - transit +
    // skew)) / 2
    //
    // ... now cancel [server]transmitTimestamp
    // t = (transit + skew - transit + skew) / 2
    // t = (2 * skew) / 2
    // t = skew
    //

    Duration clientOffsetDuration =
        Duration64.between(requestTimestamp, receiveTimestamp)
            .plus(Duration64.between(responseTimestamp, transmitTimestamp))
            .dividedBy(2);
    return clientOffsetDuration;
  }
}
