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

package com.google.time.client.sntp.tools;

import static com.google.time.client.sntp.SntpQueryResult.TYPE_RETRY_LATER;
import static com.google.time.client.sntp.SntpQueryResult.TYPE_SUCCESS;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Logger;
import com.google.time.client.base.Network;
import com.google.time.client.base.PlatformNetwork;
import com.google.time.client.base.ServerAddress;
import com.google.time.client.base.impl.Objects;
import com.google.time.client.base.impl.SystemStreamLogger;
import com.google.time.client.sntp.BasicSntpClient;
import com.google.time.client.sntp.BasicSntpClient.ClientConfig;
import com.google.time.client.sntp.SntpQueryResult;
import com.google.time.client.sntp.SntpTimeSignal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/** A simple command for reading the time from one or more NTP servers. */
public final class SntpComparisonTool {

  private SntpComparisonTool() {}

  /**
   * Executes the tool.
   *
   * <p>Args: {server name}[:port] [,{server name}[:port]]+
   */
  public static void main(String[] args) throws Exception {
    SystemStreamLogger logger = new SystemStreamLogger();
    logger.setLoggingFine(false);
    Network network = PlatformNetwork.instance();

    List<ServerAddress> serverAddresses = parseServerAddresses(args);
    for (ServerAddress serverAddress : serverAddresses) {
      List<InetSocketAddress> expandedAddresses = expandAddress(logger, network, serverAddress);
      List<SntpQueryResult> results = new ArrayList<>();
      for (InetSocketAddress expandedAddress : expandedAddresses) {
        ClientConfig clientConfig = createIpAddressConfig(expandedAddress, serverAddress.getPort());

        BasicSntpClient client =
            new BasicSntpClient.Builder().setClientConfig(clientConfig).setLogger(logger).build();
        try {
          SntpQueryResult sntpQueryResult = client.executeQuery(null);

          if (logger.isLoggingFine()) {
            logger.fine("SntpResult:" + sntpQueryResult);
          }
          results.add(sntpQueryResult);
        } catch (UnknownHostException e) {
          // Unexpected: the name should already have been resolved.
          logger.warning("Unexpected host lookup error", e);
        }
      }

      System.out.println(serverAddress + ":");
      printHeader();
      for (SntpQueryResult result : results) {
        printResult(result);
      }
    }
  }

  private static final String TABLE_TEMPLATE =
      "|%20s|%20s|%4s|%15s|%7s|%7s|%14s|%14s|%13s|%14s|%14s|%30s|\n";

  private static void printHeader() {
    System.out.printf(
        TABLE_TEMPLATE,
        "Address",
        "Client offset",
        "Ref",
        "Ref bytes",
        "Version",
        "Stratum",
        "Root delay",
        "Root disp",
        "Poll Interval",
        "Precision exp.",
        "Round trip",
        "Reference time");
  }

  private static void printResult(SntpQueryResult result) {
    switch (result.getType()) {
      case TYPE_SUCCESS:
        printTimeSignal(result);
        break;
      case TYPE_RETRY_LATER:
        Exception exception = result.getException();
        StringBuilder builder = new StringBuilder();
        builder.append(exception.getMessage());
        builder.append("[");
        for (Throwable suppressed : exception.getSuppressed()) {
          builder.append(suppressed.getMessage());
          builder.append(",");
        }
        builder.append("]");
        System.out.println("== RETRY LATER: " + builder + " ==");
        break;
      default:
        System.out.println("== Unknown type: " + result.getType() + "==");
        break;
    }
  }

  private static void printTimeSignal(SntpQueryResult result) {
    SntpTimeSignal sntpTimeSignal = result.getTimeSignal();
    System.out.printf(
        TABLE_TEMPLATE,
        sntpTimeSignal.getServerInetAddress().getHostAddress(),
        sntpTimeSignal.getClientOffset(),
        sntpTimeSignal.getReferenceIdentifierAsString(),
        bytesToString(sntpTimeSignal.getReferenceIdentifier()),
        sntpTimeSignal.getResponseVersion(),
        sntpTimeSignal.getStratum(),
        sntpTimeSignal.getRootDelayDuration(),
        sntpTimeSignal.getRootDispersionDuration(),
        swallowException(sntpTimeSignal::getPollInterval),
        sntpTimeSignal.getPrecisionExponent(),
        sntpTimeSignal.getRoundTripDuration(),
        sntpTimeSignal.getReferenceTimestampAsInstant());
  }

  private static String swallowException(Callable<?> function) {
    try {
      return Objects.toString(function.call());
    } catch (Exception e) {
      return "Err";
    }
  }

  private static String bytesToString(byte[] bytes) {
    StringBuilder buf = new StringBuilder(bytes.length * 4);
    for (byte b : bytes) {
      buf.append(b & 0xFF);
      buf.append(",");
    }
    buf.setLength(buf.length() - 1);
    return buf.toString();
  }

  @SuppressWarnings("MixedMutabilityReturnType")
  private static List<InetSocketAddress> expandAddress(
      Logger logger, Network network, ServerAddress serverAddress) {
    List<InetSocketAddress> expandedAddresses = new ArrayList<>();
    try {
      InetAddress[] serverIps = network.getAllByName(serverAddress.getName());
      for (InetAddress serverIp : serverIps) {
        expandedAddresses.add(new InetSocketAddress(serverIp, serverAddress.getPort()));
      }
      return expandedAddresses;
    } catch (UnknownHostException e) {
      logger.warning("Failed to lookup " + serverAddress + ": " + e.getMessage());
      return Collections.emptyList();
    }
  }

  private static List<ServerAddress> parseServerAddresses(String[] args) {
    List<ServerAddress> list = new ArrayList<>();
    for (String arg : args) {
      ServerAddress inetSocketAddress = SntpTool.parseServerAddress(arg);
      list.add(inetSocketAddress);
    }
    return list;
  }

  /** Creates a Config where the server address's name is already resolved to an IP. */
  static ClientConfig createIpAddressConfig(InetSocketAddress serverAddress, int port) {
    return new ClientConfig() {
      @Override
      public ServerAddress serverAddress() {
        return new ServerAddress(serverAddress.getAddress().getHostAddress(), port);
      }

      @Override
      public Duration responseTimeout() {
        return Duration.ofSeconds(5, 0);
      }
    };
  }
}
