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

import static com.google.time.client.base.testing.Bytes.bytes;
import static org.junit.Assert.assertNotEquals;

import com.google.time.client.base.testing.MoreAsserts;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SntpQueryOperationFailureResultTest {

  @Test
  public void equals() throws Exception {
    InetSocketAddress address1 = createSocketAddress(1);
    NtpMessage message1 = createNtpMessage(1);
    Exception exception1 = createException(1);
    SntpQueryOperation.FailureResult one =
        new SntpQueryOperation.FailureResult(address1, message1, exception1);

    // equals(), hashcode(), etc.
    {
      SntpQueryOperation.FailureResult other =
          new SntpQueryOperation.FailureResult(address1, message1, exception1);
      MoreAsserts.assertEqualityMethods(one, other);
    }
    {
      InetSocketAddress address2 = createSocketAddress(2);
      SntpQueryOperation.FailureResult other =
          new SntpQueryOperation.FailureResult(address2, message1, exception1);
      assertNotEquals(one, other);
    }
    {
      NtpMessage message2 = createNtpMessage(2);
      SntpQueryOperation.FailureResult other =
          new SntpQueryOperation.FailureResult(address1, message2, exception1);
      assertNotEquals(one, other);
    }
    {
      Exception exception2 = createException(2);
      SntpQueryOperation.FailureResult other =
          new SntpQueryOperation.FailureResult(address1, message1, exception2);
      assertNotEquals(one, other);
    }
  }

  private static Exception createException(int i) {
    return new Exception("test" + i);
  }

  private static NtpMessage createNtpMessage(int i) {
    return NtpMessage.create(
        NtpHeader.Builder.createEmptyV3()
            .setOriginateTimestamp(Timestamp64.fromComponents(i, 0))
            .build());
  }

  private static InetSocketAddress createSocketAddress(int i) throws UnknownHostException {
    return new InetSocketAddress(Inet4Address.getByAddress(bytes(216, 239, 35, i)), 123);
  }
}
