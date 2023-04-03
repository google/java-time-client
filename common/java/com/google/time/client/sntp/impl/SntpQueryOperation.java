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
import com.google.time.client.base.Supplier;
import com.google.time.client.base.Ticker;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.impl.ClusteredServiceOperation;
import com.google.time.client.base.impl.Objects;
import com.google.time.client.sntp.BasicSntpClient.ClientConfig;
import com.google.time.client.sntp.NtpProtocolException;
import com.google.time.client.sntp.NtpServerNotReachableException;
import com.google.time.client.sntp.impl.SntpQueryOperation.FailureResult;
import com.google.time.client.sntp.impl.SntpQueryOperation.SuccessResult;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

/**
 * The operation at the heart of SNTP: it sends an {@link NtpMessage} to a server in a UDP packet
 * and, when successful, returns the {@link NtpMessage} received via UDP in response. When
 * unsuccessful it returns an object describing the failure. This operation only partially validates
 * the response; a {@link SuccessResult} cannot currently be retried, so only cluster-level failures
 * are potentially let through. See {@link SntpClientEngine} for further validation and actual time
 * calculations. Some validation issues are bucketed as halting or non-halting arbitrarily because
 * it's not always clear whether a problem would be common across servers.
 */
public final class SntpQueryOperation
    implements ClusteredServiceOperation.ServiceOperation<Void, SuccessResult, FailureResult> {

  private final Logger logger;
  private final Network network;
  private final InstantSource clientInstantSource;
  private final Ticker clientTicker;
  private final ClientConfig clientConfig;
  private final Supplier<NtpMessage> requestFactory;

  public SntpQueryOperation(
      Logger logger,
      Network network,
      InstantSource clientInstantSource,
      Ticker clientTicker,
      ClientConfig clientConfig,
      Supplier<NtpMessage> requestFactory) {
    this.logger = Objects.requireNonNull(logger, "logger");
    this.network = Objects.requireNonNull(network, "network");
    this.clientInstantSource = Objects.requireNonNull(clientInstantSource, "clientInstantSource");
    this.clientTicker = Objects.requireNonNull(clientTicker, "clientTicker");
    this.clientConfig = Objects.requireNonNull(clientConfig, "clientConfig");
    this.requestFactory = Objects.requireNonNull(requestFactory, "requestFactory");
  }

  @Override
  public ServiceResult<SuccessResult, FailureResult> execute(
      String serverName, InetAddress inetAddress, Void unused, Duration timeAllowed) {
    int port = clientConfig.serverAddress().getPort();

    // Create a new request for each operation, avoiding sending the same information to each
    // server that could be used to identify the client.
    NtpMessage request = requestFactory.get();
    ServiceResult<SuccessResult, FailureResult> result;
    result =
        executeSntpQuery(request, inetAddress, port, clientConfig.responseTimeout(), timeAllowed);
    if (logger.isLoggingFine()) {
      logger.fine("SntpQueryOperation: Received result=" + result);
    }

    // The network query itself can fail.
    if (!result.isSuccess()) {
      return result;
    }

    // The network request succeeded, but now validate the response is usable.
    SuccessResult successValue = result.getSuccessValue();
    NtpHeader requestHeader = successValue.request.getHeader();
    NtpHeader responseHeader = successValue.response.getHeader();

    // RFC 4330 / 5.  SNTP Client Operations: Suggested check 3.
    // T1 / [server]originateTimestamp should be the same as [client]requestTimestamp, because the
    // server should echo it back to us.
    // Do validation according to RFC: if the response originateTimestamp != transmitTimestamp
    // then perhaps this response is an attack.
    final Timestamp64 requestTransmitTimestamp = requestHeader.getTransmitTimestamp();
    final Timestamp64 originateTimestamp = responseHeader.getOriginateTimestamp();
    if (!requestTransmitTimestamp.equals(originateTimestamp)) {
      // Service-level failure, expected to be shared by all servers in a cluster
      return createHaltingProtocolFailureResult(
          request,
          inetAddress,
          port,
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
      // Service-level failure, expected to be shared by all servers in a cluster
      return createHaltingProtocolFailureResult(
          request, inetAddress, port, "untrusted mode: " + responseHeader.getMode());
    }

    // RFC 4330 / 5.  SNTP Client Operations: Suggested check 4
    // https://www.rfc-editor.org/rfc/rfc4330#page-20
    //    In general, an SNTP client should stop sending to a particular server if that server
    //    returns a reply with a Stratum field of 0, regardless of kiss code, and an alternate
    //    server is available.  If no alternate server is available, the client should retransmit
    //    using an exponential-backoff algorithm described in the next section.

    // Handle kiss-o-death and other signalling responses without embedded time information.
    int stratum = responseHeader.getStratum();
    if (stratum == 0) {
      String kissCode = responseHeader.getReferenceIdentifierAsString();
      switch (kissCode) {
        case NtpHeader.KISS_CODE_ACST:
        case NtpHeader.KISS_CODE_AUTH:
        case NtpHeader.KISS_CODE_AUTO:
        case NtpHeader.KISS_CODE_BCST:
        case NtpHeader.KISS_CODE_CRYP:
        case NtpHeader.KISS_CODE_DENY:
        case NtpHeader.KISS_CODE_DROP:
        case NtpHeader.KISS_CODE_RSTR:
        case NtpHeader.KISS_CODE_MCST:
        case NtpHeader.KISS_CODE_NKEY:
        case NtpHeader.KISS_CODE_RATE:
        case NtpHeader.KISS_CODE_RMOT:
          {
            // Service-level failures, expected to be shared by all servers in a cluster
            return createHaltingProtocolFailureResult(
                request, inetAddress, port, "Server returned recognized KISS code=" + kissCode);
          }
        case NtpHeader.KISS_CODE_INIT:
        case NtpHeader.KISS_CODE_STEP:
          {
            return createNonHaltingProtocolFailureResult(
                request, inetAddress, port, "Server returned an ignorable KISS code=" + kissCode);
          }
        default:
          {
            return createHaltingProtocolFailureResult(
                request,
                inetAddress,
                port,
                "Stratum 0 response received with unknown KISS code=" + kissCode);
          }
      }
    }

    // RFC 4330 / 5.  SNTP Client Operations: Suggested check 4
    if (responseHeader.getTransmitTimestamp().equals(Timestamp64.ZERO)) {
      return createHaltingProtocolFailureResult(
          request, inetAddress, port, "Response has zero transmit timestamp");
    }

    // RFC 4330 / 5.  SNTP Client Operations: Suggested check 5 is not implemented

    // RFC 4330 / 4.  Message Format: On startup, servers set this field to 3 (clock not
    //   synchronized), and set this field to some other value when synchronized to the primary
    //   reference clock.
    // Checking this value is not suggested by the RFC, but it looks like it's an invalid /
    // unacceptable state.
    if (responseHeader.getLeapIndicator() == NtpHeader.NTP_LEAP_NOSYNC) {
      return createNonHaltingProtocolFailureResult(
          request, inetAddress, port, "Unsynchronized server");
    }

    // RFC 4330 / 5.  SNTP Client Operations: The table suggests the max is 15.
    if (stratum > NtpHeader.NTP_STRATUM_MAX) {
      return createNonHaltingProtocolFailureResult(
          request, inetAddress, port, "untrusted stratum: " + stratum);
    }

    if (responseHeader.getReferenceTimestamp().equals(Timestamp64.ZERO)) {
      return createNonHaltingProtocolFailureResult(
          request, inetAddress, port, "zero reference timestamp");
    }

    // The SNTP response passed all the validation checks.
    return result;
  }

  private static ServiceResult<SuccessResult, FailureResult> createNonHaltingFailureResult(
      NtpMessage request, InetSocketAddress serverSocketAddress, Exception exception) {
    FailureResult failureValue = new FailureResult(serverSocketAddress, request, exception);
    return ServiceResult.failure(serverSocketAddress.getAddress(), failureValue, false);
  }

  private static ServiceResult<SuccessResult, FailureResult> createNonHaltingProtocolFailureResult(
      NtpMessage request, InetAddress inetAddress, int port, String message) {
    Exception e = new NtpProtocolException(message);
    return createNonHaltingFailureResult(request, new InetSocketAddress(inetAddress, port), e);
  }

  private static ServiceResult<SuccessResult, FailureResult> createHaltingFailureResult(
      NtpMessage request, InetSocketAddress serverSocketAddress, Exception exception) {
    FailureResult failureValue = new FailureResult(serverSocketAddress, request, exception);
    return ServiceResult.failure(serverSocketAddress.getAddress(), failureValue, true);
  }

  private static ServiceResult<SuccessResult, FailureResult> createHaltingProtocolFailureResult(
      NtpMessage request, InetAddress inetAddress, int port, String message) {
    Exception e = new NtpProtocolException(message);
    return ServiceResult.failure(
        inetAddress,
        new FailureResult(new InetSocketAddress(inetAddress, port), request, e),
        /*halt=*/ true);
  }

  private ServiceResult<SuccessResult, FailureResult> executeSntpQuery(
      NtpMessage request,
      InetAddress serverInetAddress,
      int serverPort,
      Duration responseTimeout,
      Duration operationTimeoutOrNull) {

    // According to RFC4330:
    // T1 = [server]originateTimestamp = [client]requestTimestamp
    // T2 = [server]receiveTimestamp
    // T3 = [server]transmitTimestamp
    // T4 = [client]responseTimestamp

    final Instant requestInstant;
    final Ticks requestTimeTicks;
    final Ticks responseTimeTicks;

    DatagramPacket requestPacket = request.toDatagramPacket(serverInetAddress, serverPort);
    DatagramPacket responsePacket = createResponseDatagramPacket(requestPacket.getLength());

    // Work out the limiting time factor. An operation timeout will result in a timeout result,
    // where as a "normal" timeout is considered a failure.
    boolean usingOperationTimeout;
    Duration socketTimeout;
    if (operationTimeoutOrNull == null || responseTimeout.compareTo(operationTimeoutOrNull) <= 0) {
      usingOperationTimeout = false;
      socketTimeout = responseTimeout;
    } else {
      usingOperationTimeout = true;
      socketTimeout = operationTimeoutOrNull;
    }

    InetSocketAddress serverSocketAddress = new InetSocketAddress(serverInetAddress, serverPort);
    try (Network.UdpSocket socket = network.createUdpSocket()) {
      socket.setSoTimeout(socketTimeout);

      // The following block indicates the most time critical section. Be careful about adding
      // time-consuming logic there.
      {
        // Capture T1 / [client]requestTimestamp using both clocks. This is the only time that
        // clientInstantSource should be consulted. The clientInstantSource is assumed to be
        // mutable and can be changed by other processes, while the clientTicker cannot.
        requestInstant = clientInstantSource.instant();
        requestTimeTicks = clientTicker.ticks();

        // Send the request.
        try {
          socket.send(requestPacket);
        } catch (IOException e) {
          NtpServerNotReachableException e2 =
              new NtpServerNotReachableException(
                  "Unable to send NTP request to address="
                      + requestPacket.getAddress()
                      + ":"
                      + requestPacket.getPort(),
                  e);
          return createNonHaltingFailureResult(request, serverSocketAddress, e2);
        }

        // Read the response, or timeout.
        try {
          socket.receive(responsePacket);
        } catch (SocketTimeoutException e) {
          if (usingOperationTimeout) {
            return ServiceResult.timeAllowedExceeded(serverInetAddress);
          } else {
            Exception e2 =
                new NtpServerNotReachableException(
                    "Timeout waiting for response from "
                        + requestPacket.getAddress()
                        + ":"
                        + requestPacket.getPort(),
                    e);
            return createNonHaltingFailureResult(request, serverSocketAddress, e2);
          }
        } catch (IOException e) {
          Exception e2 = new NtpServerNotReachableException("Unable to receive NTP response", e);
          return createNonHaltingFailureResult(request, serverSocketAddress, e2);
        }

        // Capture T4 / [client]responseTimestamp according to the client's Ticker
        responseTimeTicks = clientTicker.ticks();
      }
    } catch (IOException e) {
      // These indicate an inability to create or configure the outgoing socket.
      Exception e2 = new NtpServerNotReachableException("Unable to create/configure UdpSocket", e);
      return createNonHaltingFailureResult(request, serverSocketAddress, e2);
    }

    // RFC 4330 / 5.  SNTP Client Operations: Suggested checks 1 & 2.
    // Validate the IP address / port of the response.
    if (!serverInetAddress.equals(responsePacket.getAddress())
        || !Objects.equals(responsePacket.getPort(), serverPort)) {
      // This is unexpected: Perhaps a packet arrived by chance...?
      NtpServerNotReachableException e =
          new NtpServerNotReachableException(
              "Response packet received from unexpected address: expected="
                  + serverInetAddress
                  + ":"
                  + serverPort
                  + ", actual="
                  + responsePacket.getAddress()
                  + ":"
                  + responsePacket.getPort());
      return createHaltingFailureResult(request, serverSocketAddress, e);
    }

    NtpMessage response = NtpMessage.fromDatagramPacket(responsePacket);
    SuccessResult successValue =
        new SuccessResult(
            serverSocketAddress,
            clientInstantSource,
            requestInstant,
            requestTimeTicks,
            responseTimeTicks,
            request,
            response);
    return ServiceResult.success(serverInetAddress, successValue);
  }

  /** Creates a {@link DatagramPacket} capable of holding the expected NTP response. */
  private static DatagramPacket createResponseDatagramPacket(int messageSizeBytes) {
    byte[] receiveBuffer = new byte[messageSizeBytes];
    return new DatagramPacket(receiveBuffer, receiveBuffer.length);
  }

  /**
   * A result struct for a successful SNTP interaction with an NTP server. It contains the request,
   * response and client-side information associated with a single round-trip to an NTP server.
   */
  public static final class SuccessResult {

    /** The address of the server that provided the result. */
    final InetSocketAddress serverSocketAddress;

    /** The InstantSource used to obtain Instants. */
    final InstantSource instantSource;

    /** T1 / [client]requestTimestamp according to the client's InstantSource. */
    final Instant requestInstant;

    /** T1 / [client]requestTimestamp according to the client's Ticker. */
    final Ticks requestTimeTicks;

    /** Capture T4 / [client]responseTimestamp according to the client's Ticker. */
    final Ticks responseTimeTicks;

    /** The message sent to the NTP server. */
    final NtpMessage request;

    /** The message received from the NTP server. */
    final NtpMessage response;

    SuccessResult(
        InetSocketAddress serverSocketAddress,
        InstantSource instantSource,
        Instant requestInstant,
        Ticks requestTimeTicks,
        Ticks responseTimeTicks,
        NtpMessage request,
        NtpMessage response) {
      this.serverSocketAddress = Objects.requireNonNull(serverSocketAddress);
      this.instantSource = Objects.requireNonNull(instantSource, "instantSource");
      this.requestInstant = Objects.requireNonNull(requestInstant, "requestInstant");
      this.requestTimeTicks = Objects.requireNonNull(requestTimeTicks, "requestTimeTicks");
      this.responseTimeTicks = Objects.requireNonNull(responseTimeTicks, "responseTimeTicks");
      this.request = Objects.requireNonNull(request, "request");
      this.response = Objects.requireNonNull(response, "response");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof SuccessResult)) {
        return false;
      }
      SuccessResult that = (SuccessResult) o;
      return serverSocketAddress.equals(that.serverSocketAddress)
          && instantSource.equals(that.instantSource)
          && requestInstant.equals(that.requestInstant)
          && requestTimeTicks.equals(that.requestTimeTicks)
          && responseTimeTicks.equals(that.responseTimeTicks)
          && request.equals(that.request)
          && response.equals(that.response);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(
          serverSocketAddress,
          instantSource,
          requestInstant,
          requestTimeTicks,
          responseTimeTicks,
          request,
          response);
    }

    @Override
    public String toString() {
      return "SuccessResult{"
          + "serverSocketAddress="
          + serverSocketAddress
          + ", requestInstant="
          + requestInstant
          + ", requestTimeTicks="
          + requestTimeTicks
          + ", responseTimeTicks="
          + responseTimeTicks
          + ", request="
          + request
          + ", response="
          + response
          + '}';
    }
  }

  /** A result struct for an unsuccessful SNTP interaction with an NTP server. */
  public static final class FailureResult {

    /** The address of the NTP server. */
    final InetSocketAddress serverSocketAddress;

    /** The message sent to the NTP server. */
    final NtpMessage request;

    /** An exception that describes the failure. */
    final Exception failureException;

    FailureResult(
        InetSocketAddress serverSocketAddress, NtpMessage request, Exception failureException) {
      this.serverSocketAddress = Objects.requireNonNull(serverSocketAddress, "serverSocketAddress");
      this.request = Objects.requireNonNull(request, "request");
      this.failureException = Objects.requireNonNull(failureException, "failureException");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof FailureResult)) {
        return false;
      }
      FailureResult that = (FailureResult) o;
      return serverSocketAddress.equals(that.serverSocketAddress)
          && request.equals(that.request)
          && failureException.equals(that.failureException);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(serverSocketAddress, request, failureException);
    }

    @Override
    public String toString() {
      return "FailureResult{"
          + "serverSocketAddress="
          + serverSocketAddress
          + ", request="
          + request
          + ", failureException="
          + failureException
          + '}';
    }
  }
}
