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

import com.google.time.client.base.Duration;
import com.google.time.client.base.Instant;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.testing.FakeClocks;
import com.google.time.client.base.testing.MoreAsserts;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SntpQueryOperationSuccessResultTest {

  @Test
  public void equals() throws Exception {
    FakeClocks fakeClocks = new FakeClocks();
    fakeClocks.setAutoAdvanceDuration(Duration.ofSeconds(1, 0));
    InetSocketAddress address1 = createSocketAddress(1);
    Instant instant1 = Instant.ofEpochMilli(1);
    Ticks ticks11 = fakeClocks.getFakeTicker().ticks();
    Ticks ticks12 = fakeClocks.getFakeTicker().ticks();
    NtpMessage message11 = createNtpMessage(11);
    NtpMessage message12 = createNtpMessage(12);
    SntpQueryOperation.SuccessResult one =
        new SntpQueryOperation.SuccessResult(
            address1,
            fakeClocks.getFakeInstantSource(),
            instant1,
            ticks11,
            ticks12,
            message11,
            message12);

    // equals(), hashcode(), etc. - sample a few fields only
    {
      SntpQueryOperation.SuccessResult other =
          new SntpQueryOperation.SuccessResult(
              address1,
              fakeClocks.getFakeInstantSource(),
              instant1,
              ticks11,
              ticks12,
              message11,
              message12);
      MoreAsserts.assertEqualityMethods(one, other);
    }
    {
      InetSocketAddress address2 = createSocketAddress(2);
      SntpQueryOperation.SuccessResult other =
          new SntpQueryOperation.SuccessResult(
              address2,
              fakeClocks.getFakeInstantSource(),
              instant1,
              ticks11,
              ticks12,
              message11,
              message12);
      assertNotEquals(one, other);
    }
    {
      Instant instant2 = Instant.ofEpochMilli(2);
      SntpQueryOperation.SuccessResult other =
          new SntpQueryOperation.SuccessResult(
              address1,
              fakeClocks.getFakeInstantSource(),
              instant2,
              ticks11,
              ticks12,
              message11,
              message12);
      assertNotEquals(one, other);
    }
    {
      NtpMessage message22 = createNtpMessage(22);
      SntpQueryOperation.SuccessResult other =
          new SntpQueryOperation.SuccessResult(
              address1,
              fakeClocks.getFakeInstantSource(),
              instant1,
              ticks11,
              ticks12,
              message11,
              message22);
      assertNotEquals(one, other);
    }
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
