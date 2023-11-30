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
import com.google.time.client.base.Network;
import com.google.time.client.base.NetworkOperationResult;
import com.google.time.client.base.Supplier;
import com.google.time.client.base.Ticker;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.annotations.NonFinalForTesting;
import com.google.time.client.base.annotations.VisibleForTesting;
import com.google.time.client.base.impl.ClusteredServiceOperation;
import com.google.time.client.base.impl.ClusteredServiceOperation.ClusteredServiceResult;
import com.google.time.client.base.impl.Objects;
import com.google.time.client.sntp.BasicSntpClient.ClientConfig;
import com.google.time.client.sntp.NtpProtocolException;
import com.google.time.client.sntp.NtpServerNotReachableException;
import com.google.time.client.sntp.SntpQueryDebugInfo;
import com.google.time.client.sntp.SntpQueryResult;
import com.google.time.client.sntp.impl.SntpQueryOperation.FailureResult;
import com.google.time.client.sntp.impl.SntpQueryOperation.SuccessResult;
import java.net.UnknownHostException;

/** Internal, reusable SNTP client logic. {@link #executeQuery} is thread-safe. */
@NonFinalForTesting
public class SntpClientEngine {

  private final Logger logger;
  private final SntpServiceConnector sntpServiceConnector;

  public static SntpClientEngine create(
      Logger logger,
      InstantSource instantSource,
      ClientConfig clientConfig,
      Network network,
      Ticker ticker,
      Supplier<NtpMessage> requestSupplier) {

    SntpQueryOperation sntpQueryOperation =
        new SntpQueryOperation(
            logger, network, instantSource, ticker, clientConfig, requestSupplier);
    ClusteredServiceOperation<Void, SuccessResult, FailureResult> networkServiceConnector =
        new ClusteredServiceOperation<>(logger, ticker, network, sntpQueryOperation);
    SntpServiceConnectorImpl sntpServiceConnector =
        new SntpServiceConnectorImpl(clientConfig, networkServiceConnector);
    return new SntpClientEngine(logger, sntpServiceConnector);
  }

  @VisibleForTesting
  public SntpClientEngine(Logger logger, SntpServiceConnector sntpServiceConnector) {
    this.logger = Objects.requireNonNull(logger, "logger");
    this.sntpServiceConnector = Objects.requireNonNull(sntpServiceConnector, "sntpServerConnector");
  }

  /**
   * Queries the current server instant using the associated {@link SntpServiceConnector}.
   *
   * @param timeAllowed the time allowed or {@code null} for indefinite. The time allowed is
   *     considered a guide and may be exceeded
   * @return the result of a successful SNTP request or information about the failure
   */
  public SntpQueryResult executeQuery(Duration timeAllowed) throws UnknownHostException {
    ClusteredServiceResult<SuccessResult, FailureResult> clusteredServiceResult =
        sntpServiceConnector.executeQuery(timeAllowed);

    if (logger.isLoggingFine()) {
      logger.fine("executeQuery(): clusteredServiceResult=" + clusteredServiceResult);
    }

    // Every result, whatever the outcome, can have some information about servers that have been
    // tried. This is needed in all results so build the common aspects first.
    SntpQueryDebugInfo sntpQueryDebugInfo =
        new SntpQueryDebugInfo(clusteredServiceResult.getServiceAddresses());
    for (FailureResult failureResult : clusteredServiceResult.getFailureValues()) {
      sntpQueryDebugInfo.addSntpQueryOperationResults(
          NetworkOperationResult.failure(
              failureResult.serverSocketAddress,
              failureResult.failureIdentifier,
              failureResult.failureException));
    }

    if (clusteredServiceResult.isSuccess()) {
      sntpQueryDebugInfo.addSntpQueryOperationResults(
          NetworkOperationResult.success(
              clusteredServiceResult.getSuccessValue().serverSocketAddress));

      return processSuccessResult(sntpQueryDebugInfo, clusteredServiceResult);
    } else if (clusteredServiceResult.isTimeAllowedExceeded()) {
      return SntpQueryResult.timeAllowedExceeded(sntpQueryDebugInfo);
    } else if (clusteredServiceResult.isHalted()) {
      // Must be a failure: Halted means a cluster member responded in a way that suggests the whole
      // cluster is "bad" and there's some kind of protocol problem.
      return SntpQueryResult.protocolError(
          sntpQueryDebugInfo, clusteredServiceResult.getLastFailureValue().failureException);
    } else {
      // Must be a non-halting failure.
      NtpServerNotReachableException e =
          new NtpServerNotReachableException("IP addresses exhausted");
      for (FailureResult failureResult : clusteredServiceResult.getFailureValues()) {
        e.addSuppressed(failureResult.failureException);
      }
      return SntpQueryResult.retryLater(sntpQueryDebugInfo, e);
    }
  }

  private SntpQueryResult processSuccessResult(
      SntpQueryDebugInfo sntpQueryDebugInfo,
      ClusteredServiceResult<SuccessResult, FailureResult> clusteredServiceResult) {
    SuccessResult successResult = clusteredServiceResult.getSuccessValue();

    NtpMessage response = successResult.response;
    try {
      SntpTimeSignalImpl timeSignal = performNtpCalculations(successResult, response);
      return SntpQueryResult.success(sntpQueryDebugInfo, timeSignal);
    } catch (NtpProtocolException e) {
      if (logger.isLoggingFine()) {
        logger.fine("executeQuery(): Unable to process SNTP response", e);
      }

      return SntpQueryResult.protocolError(
          sntpQueryDebugInfo,
          new NtpServerNotReachableException("Failed to perform NTP calculation on result", e));
    }
  }

  /**
   * Processes an SNTP response. Factored this way to enable easier unit testing around this step,
   * leaving the networking logic to be tested elsewhere.
   */
  @VisibleForTesting
  public static SntpTimeSignalImpl performNtpCalculations(
      SuccessResult successResult, NtpMessage response) throws NtpProtocolException {

    final NtpHeader responseHeader = response.getHeader();

    final Instant requestInstant = successResult.requestInstant;
    final Ticks requestTimeTicks = successResult.requestTimeTicks;
    final Ticks responseTimeTicks = successResult.responseTimeTicks;

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
      throw new NtpProtocolException(
          "serverProcessingDuration=" + serverProcessingDuration + " must not be negative");
    } else {
      if (serverProcessingDuration.compareTo(totalTransactionDuration) > 0) {
        // Paranoia: serverProcessingDuration cannot be greater than the elapsed time measured by
        // the client.
        throw new NtpProtocolException(
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

    return new SntpTimeSignalImpl(
        successResult.serverSocketAddress,
        response,
        roundTripDuration,
        totalTransactionDuration,
        responseInstant,
        clientOffsetDuration,
        responseTimeTicks,
        successResult.instantSource,
        adjustedClientInstant);
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
