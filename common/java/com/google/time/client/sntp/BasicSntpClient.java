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
import com.google.time.client.base.InstantSource;
import com.google.time.client.base.Logger;
import com.google.time.client.base.Network;
import com.google.time.client.base.PlatformInstantSource;
import com.google.time.client.base.PlatformNetwork;
import com.google.time.client.base.PlatformTicker;
import com.google.time.client.base.ServerAddress;
import com.google.time.client.base.Supplier;
import com.google.time.client.base.Ticker;
import com.google.time.client.base.annotations.VisibleForTesting;
import com.google.time.client.base.impl.Objects;
import com.google.time.client.base.impl.PlatformRandom;
import com.google.time.client.base.impl.SystemStreamLogger;
import com.google.time.client.sntp.impl.NtpMessage;
import com.google.time.client.sntp.impl.SntpClientEngine;
import com.google.time.client.sntp.impl.SntpRequestFactory;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * A simple implementation of {@link SntpClient} that is configured with a single server name. The
 * server's name is resolved each time {@link #executeQuery}, is called and if the server name
 * resolves to multiple IP addresses, each is tried sequentially until one request succeeds, the
 * time allowed is exceeded, or the IP addresses are exhausted.
 *
 * <p>The {@link Builder} class is used to configure and construct the object.
 */
public final class BasicSntpClient implements SntpClient {

  private final SntpClientEngine engine;

  @VisibleForTesting
  BasicSntpClient(SntpClientEngine engine) {
    this.engine = Objects.requireNonNull(engine, "engine");
  }

  @Override
  public SntpQueryResult executeQuery(Duration timeAllowed) throws UnknownHostException {
    return engine.executeQuery(timeAllowed);
  }

  /** SNTP client configuration */
  public interface ClientConfig {
    /** The address of the NTP server to resolve / use. */
    ServerAddress serverAddress();

    /** The timeout to use while listening for a response from the NTP server. */
    Duration responseTimeout();
  }

  /**
   * The class used to construct {@link BasicSntpClient} instances.
   *
   * <p>The following properties are defaulted:
   *
   * <ul>
   *   <li>logger - {@link SystemStreamLogger}
   *   <li>clientInstantSource - {@link PlatformInstantSource}
   *   <li>clientTicker - {@link PlatformTicker}
   *   <li>network - {@link PlatformNetwork}
   *   <li>random - {@link PlatformRandom}
   * </ul>
   *
   * <p>Other properties must be set explicitly.
   */
  public static final class Builder {

    // Properties with defaults.
    private Logger logger = new SystemStreamLogger();
    private InstantSource clientInstantSource = PlatformInstantSource.instance();
    private Ticker clientTicker = PlatformTicker.instance();
    private Network network = PlatformNetwork.instance();
    private boolean clientDataMinimizationEnabled = true;
    private Random random = PlatformRandom.getDefaultRandom();

    // Properties without defaults.
    private ClientConfig clientConfig;

    /** Sets the {@link Logger logger} to use. */
    public Builder setLogger(Logger logger) {
      this.logger = Objects.requireNonNull(logger);
      return this;
    }

    /** Sets the {@link InstantSource client instant source} to use. */
    public Builder setClientInstantSource(InstantSource clientInstantSource) {
      this.clientInstantSource = Objects.requireNonNull(clientInstantSource);
      return this;
    }

    /** Sets the {@link Ticker client ticker} to use. */
    public Builder setClientTicker(Ticker clientTicker) {
      this.clientTicker = Objects.requireNonNull(clientTicker);
      return this;
    }

    /** Sets the {@link Network network} to use. */
    public Builder setNetwork(Network network) {
      this.network = Objects.requireNonNull(network);
      return this;
    }

    /**
     * Sets whether to use client data minimization suggested in <a
     * href="https://datatracker.ietf.org/doc/html/draft-ietf-ntp-data-minimization-04"
     * >draft-ietf-ntp-data-minimization-04</a>. The default is enabled.
     */
    public Builder setClientDataMinimizationEnabled(boolean enabled) {
      this.clientDataMinimizationEnabled = enabled;
      return this;
    }

    /** Sets the {@link Random} to use. Its exact use depends on the protocol / other settings. */
    public Builder setRandom(Random random) {
      this.random = Objects.requireNonNull(random);
      return this;
    }

    /** Sets the {@link ClientConfig config} to use. */
    public Builder setClientConfig(ClientConfig clientConfig) {
      this.clientConfig = Objects.requireNonNull(clientConfig);
      return this;
    }

    /** Creates a new {@link BasicSntpClient} instance using the builder's properties. */
    public BasicSntpClient build() {
      // Check non-defaulted properties.
      Objects.requireNonNull(clientConfig, "config");

      final int clientReportedVersion = 3;
      Supplier<NtpMessage> requestSupplier =
          new SntpRequestFactory(
              clientInstantSource, random, clientReportedVersion, clientDataMinimizationEnabled);

      SntpClientEngine engine =
          SntpClientEngine.create(
              logger, clientInstantSource, clientConfig, network, clientTicker, requestSupplier);
      return new BasicSntpClient(engine);
    }
  }
}
