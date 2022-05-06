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

import java.net.InetAddress;

/** An interface for listening to network operations associated with an SNTP request/response. */
public interface SntpNetworkListener {

  /**
   * Called when a server name could not be resolved.
   *
   * <p>Depending on the {@link SntpClient} implementation, this could happen once or several times
   * per call to {@link SntpClient#requestInstant()} and may not indicate a total failure.
   */
  void serverLookupFailure(String serverName, Throwable e);

  /**
   * Called when communication with the specified server succeeded.
   *
   * <p>Depending on the {@link SntpClient} implementation, this could happen once or several times
   * per call to {@link SntpClient#requestInstant()} and may not indicate a total success.
   *
   * @param inetAddress the server's IP address
   * @param port the server's port
   */
  void success(InetAddress inetAddress, int port);

  /**
   * Called when communication with the specified server failed.
   *
   * <p>Depending on the {@link SntpClient} implementation, this could happen once or several times
   * per call to {@link SntpClient#requestInstant()} and may not indicate a total failure.
   *
   * @param address the server's IP address
   * @param port the server's port
   * @param e the exception associated with the failure
   */
  void failure(InetAddress address, int port, Exception e);
}
