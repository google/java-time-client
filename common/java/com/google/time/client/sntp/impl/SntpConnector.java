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

import com.google.time.client.sntp.InvalidNtpResponseException;
import com.google.time.client.sntp.NtpServerNotReachableException;

/** The interface for classes that handle routing requests to servers. */
public interface SntpConnector {

  /** Creates a "session" for an attempt to obtain an instant using SNTP. */
  Session createSession();

  /**
   * An abstraction over a sequence of IP addresses to communicate with.
   *
   * <p>Users are expected to call {@link #canTrySend()} and, if it returns {@code true}, call
   * {@link #trySend(NtpMessage)}. Each call to {@link #trySend(NtpMessage)} will try the next
   * address in a list. {@link #trySend(NtpMessage)} can fail due to networking issues (IP addresses
   * not reachable, no server listening, timeouts).
   *
   * <p>Every failure to communicate is reported as an {@link NtpServerNotReachableException}.
   * {@link #trySend(NtpMessage)} performs basic syntactic validation of the response, but callers
   * are expected to validate the response and call {@link #reportInvalidResponse(SntpSessionResult,
   * InvalidNtpResponseException)} if it is found to be invalid.
   */
  interface Session {

    /**
     * Returns {@code true} if {@link #trySend(NtpMessage)} can be called, e.g. if there are more IP
     * addresses to try in this session.
     */
    boolean canTrySend();

    /**
     * Attempts an SNTP request/response with a single IP address, returning a result on success, or
     * throwing an exception.
     *
     * <p>The result returned may still be invalid according to the SNTP spec. See {@link Session}
     * for details.
     */
    SntpSessionResult trySend(NtpMessage request) throws NtpServerNotReachableException;

    /** Reports that the response from the previous {@link #trySend} call was invalid. */
    void reportInvalidResponse(SntpSessionResult sessionResult, InvalidNtpResponseException reason);
  }
}
