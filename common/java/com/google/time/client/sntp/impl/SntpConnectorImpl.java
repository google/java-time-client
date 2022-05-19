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
import com.google.time.client.base.ServerAddress;
import com.google.time.client.base.Ticker;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.impl.Objects;
import com.google.time.client.sntp.BasicSntpClient;
import com.google.time.client.sntp.NtpServerNotReachableException;
import com.google.time.client.sntp.SntpNetworkListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * An {@link SntpConnector} that creates {@link Session}s that work through a list of IP addresses
 * for a single {@link ServerAddress}.
 */
public class SntpConnectorImpl implements SntpConnector {

  private final Logger logger;
  private final Network network;
  private final InstantSource clientInstantSource;
  private final Ticker clientTicker;
  private final SntpNetworkListener listener;
  private final BasicSntpClient.ClientConfig clientConfig;

  /** Creates an instance. */
  public SntpConnectorImpl(
      Logger logger,
      Network network,
      InstantSource clientInstantSource,
      Ticker clientTicker,
      SntpNetworkListener listener,
      BasicSntpClient.ClientConfig clientConfig) {
    this.logger = Objects.requireNonNull(logger, "logger");
    this.network = Objects.requireNonNull(network, "network");
    this.clientInstantSource = Objects.requireNonNull(clientInstantSource, "clientInstantSource");
    this.clientTicker = Objects.requireNonNull(clientTicker, "clientTicker");
    this.listener = Objects.requireNonNull(listener, "listener");
    ;
    this.clientConfig = Objects.requireNonNull(clientConfig, "clientConfig");
  }

  @Override
  public Session createSession() {
    return new SessionImpl();
  }

  private class SessionImpl implements Session {
    private int nextServerIndex;
    private InetAddress[] serverInetAddresses;

    private SessionImpl() {}

    @Override
    public SntpSessionResult trySend(NtpMessage request) throws NtpServerNotReachableException {
      // The first time send() is called, resolve the server IP addresses.
      if (serverInetAddresses == null) {
        ServerAddress serverAddress = clientConfig.serverAddress();
        lookupServerAddresses(serverAddress.getName());
        if (logger.isLoggingFine()) {
          logger.fine("requestTime(): Servers found: " + Arrays.toString(serverInetAddresses));
        }
      }

      if (nextServerIndex >= serverInetAddresses.length) {
        // This implies a coding error: hasNext() checks before calls to send() should prevent this
        // call.
        throw new NtpServerNotReachableException(
            "send() calls exceeded server count=" + nextServerIndex);
      }

      InetAddress serverInetAddress = serverInetAddresses[nextServerIndex];
      int port = clientConfig.serverAddress().getPort();
      try {
        SntpSessionResult sntpSessionResult =
            sendInternal(request, serverInetAddress, port, clientConfig.responseTimeout());
        listener.success(serverInetAddress, port);
        return sntpSessionResult;
      } catch (RuntimeException | NtpServerNotReachableException e) {
        listener.failure(serverInetAddress, port, e);
        logger.fine(
            "requestTime(): Failed to communicate with address["
                + nextServerIndex
                + "/"
                + serverInetAddresses.length
                + "="
                + serverInetAddress,
            e);
        throw e;
      } finally {
        nextServerIndex++;
      }
    }

    private SntpSessionResult sendInternal(
        NtpMessage request, InetAddress serverInetAddress, int serverPort, Duration responseTimeout)
        throws NtpServerNotReachableException {
      DatagramPacket requestPacket = request.toDatagramPacket(serverInetAddress, serverPort);
      DatagramPacket responsePacket = createResponseDatagramPacket(requestPacket.getLength());
      try (Network.UdpSocket socket = network.createUdpSocket()) {
        socket.setSoTimeout(responseTimeout);

        // According to RFC4330:
        // T1 = [server]originateTimestamp = [client]requestTimestamp
        // T2 = [server]receiveTimestamp
        // T3 = [server]transmitTimestamp
        // T4 = [client]responseTimestamp

        // The following block indicates the most time critical section. Be careful about adding
        // time-consuming logic there.
        final Instant requestInstant;
        final Ticks requestTimeTicks;
        final Ticks responseTimeTicks;
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
            throw new NtpServerNotReachableException(
                "Unable to send NTP request to address="
                    + requestPacket.getAddress()
                    + ":"
                    + requestPacket.getPort(),
                e);
          }

          // Read the response, or timeout.
          try {
            socket.receive(responsePacket);
          } catch (SocketTimeoutException e) {
            throw new NtpServerNotReachableException(
                "Timeout waiting for response from "
                    + requestPacket.getAddress()
                    + ":"
                    + requestPacket.getPort());
          } catch (IOException e) {
            throw new NtpServerNotReachableException("Unable to receive NTP response", e);
          }

          // Capture T4 / [client]responseTimestamp according to the client's Ticker
          responseTimeTicks = clientTicker.ticks();
        }

        // RFC 4330 / 5.  SNTP Client Operations: Suggested checks 1 & 2.
        // Validate the IP address / port of the response.
        if (!serverInetAddress.equals(responsePacket.getAddress())
            || !Objects.equals(responsePacket.getPort(), serverPort)) {
          // This is unexpected: Perhaps a packet arrived by chance...?
          throw new NtpServerNotReachableException(
              "Response packet received from unexpected address: expected="
                  + serverInetAddress
                  + ":"
                  + serverPort
                  + ", actual="
                  + responsePacket.getAddress()
                  + ":"
                  + responsePacket.getPort());
        }

        // Extract the results
        NtpMessage response = NtpMessage.fromDatagramPacket(responsePacket);

        return new SntpSessionResult(
            requestInstant, requestTimeTicks, responseTimeTicks, request, response);
      } catch (SocketException e) {
        throw new NtpServerNotReachableException("Socket", e);
      }
    }

    @Override
    public boolean canSend() {
      return serverInetAddresses == null || nextServerIndex < serverInetAddresses.length;
    }

    @Override
    public void reportInvalidResponse(SntpSessionResult sessionResult) {
      // No-op - no history is kept currently.
    }

    private void lookupServerAddresses(String serverName) throws NtpServerNotReachableException {
      try {
        serverInetAddresses = network.getAllByName(serverName);
      } catch (UnknownHostException e) {
        serverInetAddresses = new InetAddress[0];
        listener.serverLookupFailure(serverName, e);
        logger.warning("requestTime(): NTP server lookup failed", e);
        throw new NtpServerNotReachableException(serverName, e);
      }
    }
  }

  /** Creates a {@link DatagramPacket} capable of holding the expected NTP response. */
  private static DatagramPacket createResponseDatagramPacket(int messageSizeBytes) {
    byte[] receiveBuffer = new byte[messageSizeBytes];
    return new DatagramPacket(receiveBuffer, receiveBuffer.length);
  }
}
