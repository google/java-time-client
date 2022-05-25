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

import com.google.time.client.base.Duration;
import com.google.time.client.base.Logger;
import com.google.time.client.base.Network;
import com.google.time.client.base.PlatformNetwork;
import com.google.time.client.base.ServerAddress;
import com.google.time.client.base.impl.SystemStreamLogger;
import com.google.time.client.sntp.BasicSntpClient;
import com.google.time.client.sntp.BasicSntpClient.ClientConfig;
import com.google.time.client.sntp.NtpServerNotReachableException;
import com.google.time.client.sntp.SntpResult;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
      List<SntpResult> results = new ArrayList<>();
      for (InetSocketAddress expandedAddress : expandedAddresses) {
        ClientConfig clientConfig = createIpAddressConfig(expandedAddress, serverAddress.getPort());

        BasicSntpClient client =
            new BasicSntpClient.Builder().setClientConfig(clientConfig).setLogger(logger).build();
        try {
          SntpResult sntpResult = client.requestInstant();

          if (logger.isLoggingFine()) {
            logger.fine("SntpResult:" + sntpResult);
          }
          results.add(sntpResult);
        } catch (NtpServerNotReachableException e) {
          logger.warning("Server not reachable at " + expandedAddress);
        }
      }

      System.out.println(serverAddress + ":");
      printHeader();
      for (SntpResult result : results) {
        printResult(result);
      }
    }
  }

  private static final String TABLE_TEMPLATE =
      "|%20s|%20s|%4s|%15s|%8s|%7s|%14s|%14s|%13s|%14s|%14s|%30s|\n";

  private static void printHeader() {
    System.out.printf(
        TABLE_TEMPLATE,
        "Address",
        "Client offset",
        "Ref",
        "Ref bytes",
        "Versions",
        "Stratum",
        "Root delay",
        "Root disp",
        "Poll Interval",
        "Precision exp.",
        "Round trip",
        "Reference time");
  }

  private static void printResult(SntpResult result) {
    System.out.printf(
        TABLE_TEMPLATE,
        result.getServerInetAddress().getHostAddress(),
        result.getClientOffset(),
        result.getReferenceIdentifierAsString(),
        bytesToString(result.getReferenceIdentifier()),
        result.getRequestVersion() + "/" + result.getResponseVersion(),
        result.getStratum(),
        result.getRootDelayDuration(),
        result.getRootDispersionDuration(),
        swallowException(result::getPollInterval),
        result.getPrecisionExponent(),
        result.getRoundTripDuration(),
        result.getReferenceTimestampAsInstant());
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
