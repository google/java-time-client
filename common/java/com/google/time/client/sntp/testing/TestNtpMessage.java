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
import com.google.time.client.sntp.impl.Timestamp64;

/** A read-only, copying, wrapper NtpMessage for use in tests. */
public final class TestNtpMessage {
  private final NtpMessage message;

  public static TestNtpMessage createReadonlyCopy(NtpMessage message) {
    return new TestNtpMessage(copyMessage(message));
  }

  private TestNtpMessage(NtpMessage message) {
    this.message = message;
  }

  public Timestamp64 getReferenceTimestamp() {
    return message.getReferenceTimestamp();
  }

  public Timestamp64 getOriginateTimestamp() {
    return message.getOriginateTimestamp();
  }

  public Timestamp64 getReceiveTimestamp() {
    return message.getReceiveTimestamp();
  }

  public Timestamp64 getTransmitTimestamp() {
    return message.getTransmitTimestamp();
  }

  public NtpMessage toMutableCopy() {
    return copyMessage(message);
  }

  private static NtpMessage copyMessage(NtpMessage message) {
    return NtpMessage.fromBytesForTests(
        message.toByteArray(), message.getInetAddress(), message.getPort());
  }
}
