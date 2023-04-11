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
import com.google.time.client.base.impl.ClusteredServiceOperation;
import com.google.time.client.base.impl.ClusteredServiceOperation.ClusteredServiceResult;
import com.google.time.client.base.impl.Objects;
import com.google.time.client.sntp.BasicSntpClient.ClientConfig;
import com.google.time.client.sntp.impl.SntpQueryOperation.FailureResult;
import com.google.time.client.sntp.impl.SntpQueryOperation.SuccessResult;
import java.net.UnknownHostException;

/**
 * The real {@link SntpServiceConnector} implementation.
 *
 * <p>An adapter from {@link SntpServiceConnector} to {@link ClusteredServiceOperation}.
 */
public final class SntpServiceConnectorImpl implements SntpServiceConnector {

  private final ClientConfig clientConfig;
  private final ClusteredServiceOperation<Void, SuccessResult, FailureResult>
      clusteredServiceOperation;

  public SntpServiceConnectorImpl(
      ClientConfig clientConfig,
      ClusteredServiceOperation<Void, SuccessResult, FailureResult> clusteredServiceOperation) {
    this.clientConfig = Objects.requireNonNull(clientConfig);
    this.clusteredServiceOperation = Objects.requireNonNull(clusteredServiceOperation);
  }

  public ClientConfig getClientConfig() {
    return clientConfig;
  }

  @Override
  public ClusteredServiceResult<SuccessResult, FailureResult> executeQuery(Duration timeAllowed)
      throws UnknownHostException {
    Void parameter = null;
    return clusteredServiceOperation.execute(
        clientConfig.serverAddress().getName(), parameter, timeAllowed);
  }
}
