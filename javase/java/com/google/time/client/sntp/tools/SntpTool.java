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
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.ServerAddress;
import com.google.time.client.base.Ticker;
import com.google.time.client.base.impl.PlatformTicker;
import com.google.time.client.base.impl.SystemStreamLogger;
import com.google.time.client.sntp.BasicSntpClient;
import com.google.time.client.sntp.BasicSntpClient.ClientConfig;
import com.google.time.client.sntp.SntpResult;

/** A simple command / demo of the {@link BasicSntpClient}. */
public class SntpTool {

  private SntpTool() {}

  /**
   * Executes the tool.
   *
   * <p>Args: {server name}[:port]
   */
  public static void main(String[] args) throws Exception {
    SystemStreamLogger logger = new SystemStreamLogger();
    logger.setLoggingFine(true);

    ServerAddress serverAddress = parseServerAddress(args[0]);
    ClientConfig clientConfig = createConfig(serverAddress);

    Ticker clientTicker = PlatformTicker.instance();
    BasicSntpClient client =
        new BasicSntpClient.Builder()
            .setConfig(clientConfig)
            .setClientTicker(clientTicker)
            .setLogger(logger)
            .build();
    SntpResult sntpResult = client.requestInstant();

    if (logger.isLoggingFine()) {
      logger.fine("SntpResult:" + sntpResult);
    }

    System.out.println("Client ticker: " + clientTicker);
    InstantSource clientInstantSource = client.getClientInstantSource();
    System.out.println("Client instant source: " + clientInstantSource);

    System.out.println("Server instant: " + sntpResult.getResultInstant());
    System.out.println("At ticks: " + sntpResult.getResultTicks());
    System.out.println("Current ticks: " + clientTicker.ticks());
    System.out.println("Offset between client and server: " + sntpResult.getClientOffset());
    System.out.println("Client instant now: " + clientInstantSource.instant());
  }

  /** Returns the address for an NTP server. */
  static ServerAddress parseServerAddress(String s) {
    String serverName;
    int port = 123;
    if (s.contains(":")) {
      String[] parts = s.split(":");
      serverName = parts[0];
      port = Integer.parseInt(parts[1]);
    } else {
      serverName = s;
    }
    return new ServerAddress(serverName, port);
  }

  static ClientConfig createConfig(ServerAddress serverAddress) {
    return new ClientConfig() {
      @Override
      public ServerAddress serverAddress() {
        return serverAddress;
      }

      @Override
      public Duration responseTimeout() {
        return Duration.ofSeconds(5, 0);
      }
    };
  }
}
