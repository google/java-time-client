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
import com.google.time.client.base.impl.ClusteredServiceOperation.ClusteredServiceResult;
import com.google.time.client.sntp.impl.SntpQueryOperation.FailureResult;
import com.google.time.client.sntp.impl.SntpQueryOperation.SuccessResult;
import java.net.UnknownHostException;

/**
 * The interface for classes that handle routing requests to a clustered SNTP service.
 *
 * <p>This interface exists as an aid to testing: It hides the details of server addressing /
 * configuration so the caller doesn't have to provide a server address.
 */
public interface SntpServiceConnector {

  /** Sends an SNTP request to a clustered NTP server. */
  ClusteredServiceResult<SuccessResult, FailureResult> executeQuery(Duration timeAllowed)
      throws UnknownHostException;
}
