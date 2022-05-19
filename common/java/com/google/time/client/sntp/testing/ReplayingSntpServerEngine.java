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

package com.google.time.client.sntp.testing;

import com.google.time.client.sntp.impl.NtpMessage;

/** A fake NTP server logic that always returns a canned response. */
public class ReplayingSntpServerEngine implements TestSntpServerEngine {

  private NtpMessage nextResponse;

  private int numRequestsReceived;

  @Override
  public NtpMessage processRequest(NtpMessage request) {
    numRequestsReceived++;

    // This is required for the response to be accepted by the client.
    nextResponse.setOriginateTimestamp(request.getTransmitTimestamp());

    return nextResponse;
  }

  @Override
  public int numRequestsReceived() {
    return numRequestsReceived;
  }

  public void setNextResponse(NtpMessage nextResponse) {
    this.nextResponse = nextResponse;
  }
}
