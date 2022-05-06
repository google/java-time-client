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

/**
 * A high-level API for obtaining an instant with metadata using SNTP. Implementations may provide
 * different behaviors around network fail-over and timeouts, security guarantees, consensus
 * generation, accuracy estimates or guarantees, and so on.
 */
public interface SntpClient {

  /**
   * Requests the current instant.
   *
   * @return the result containing the current instant with metadata, never {@code null}
   * @throws NtpServerNotReachableException if an instant cannot be obtained
   */
  SntpResult requestInstant() throws NtpServerNotReachableException;
}
