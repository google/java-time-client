/*
 * Copyright 2023 Google LLC
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

import static com.google.time.client.base.testing.MoreAsserts.assertEqualityMethods;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SntpQueryResultTest {

  @Test
  public void success() throws Exception {
    InetAddress address1 = InetAddress.getByAddress(new byte[] {1, 1, 1, 1});
    List<InetAddress> addresses = Collections.singletonList(address1);
    SntpQueryDebugInfo debugInfo = new SntpQueryDebugInfo(addresses);
    SntpTimeSignal timeSignal = mock(SntpTimeSignal.class);
    SntpQueryResult instance = SntpQueryResult.success(debugInfo, timeSignal);
    assertEquals(debugInfo, instance.getQueryDebugInfo());
    assertEquals(SntpQueryResult.TYPE_SUCCESS, instance.getType());
    assertEquals(timeSignal, instance.getTimeSignal());
    assertNull(instance.getException());
  }

  @Test
  public void timeAllowedExceeded() throws Exception {
    InetAddress address1 = InetAddress.getByAddress(new byte[] {1, 1, 1, 1});
    List<InetAddress> addresses = Collections.singletonList(address1);
    SntpQueryDebugInfo debugInfo = new SntpQueryDebugInfo(addresses);
    SntpQueryResult instance = SntpQueryResult.timeAllowedExceeded(debugInfo);
    assertEquals(debugInfo, instance.getQueryDebugInfo());
    assertEquals(SntpQueryResult.TYPE_TIME_ALLOWED_EXCEEDED, instance.getType());
    assertNull(instance.getTimeSignal());
    assertNull(instance.getException());
  }

  // Covers equals(Object), compareTo(Duration) & hashCode() (for equality only).
  @Test
  public void equals() throws Exception {
    InetAddress address1 = InetAddress.getByAddress(new byte[] {1, 1, 1, 1});
    List<InetAddress> addresses = Collections.singletonList(address1);
    SntpQueryDebugInfo debugInfo = new SntpQueryDebugInfo(addresses);
    SntpQueryResult one = SntpQueryResult.timeAllowedExceeded(debugInfo);

    SntpQueryResult two = SntpQueryResult.timeAllowedExceeded(debugInfo);
    assertEqualityMethods(one, two);

    SntpQueryResult three = SntpQueryResult.success(debugInfo, mock(SntpTimeSignal.class));
    assertNotEquals(one, three);
  }
}
