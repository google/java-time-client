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
import static org.junit.Assert.assertThrows;

import com.google.time.client.base.NetworkOperationResult;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SntpQueryDebugInfoTest {

  @Test
  public void getSntpQueryAddresses() throws Exception {
    InetAddress address1 = InetAddress.getByAddress(new byte[] {1, 1, 1, 1});
    InetAddress address2 = InetAddress.getByAddress(new byte[] {2, 2, 2, 2});
    InetAddress address3 = InetAddress.getByAddress(new byte[] {3, 3, 3, 3});
    List<InetAddress> addresses = Arrays.asList(address1, address2, address3);
    SntpQueryDebugInfo instance = new SntpQueryDebugInfo(addresses);
    assertEquals(addresses, instance.getSntpQueryAddresses());
  }

  @Test
  public void addSntpQueryOperationResults() throws Exception {
    InetAddress address1 = InetAddress.getByAddress(new byte[] {1, 1, 1, 1});
    InetAddress address2 = InetAddress.getByAddress(new byte[] {2, 2, 2, 2});
    InetAddress address3 = InetAddress.getByAddress(new byte[] {3, 3, 3, 3});
    List<InetAddress> addresses = Arrays.asList(address1, address2, address3);

    SntpQueryDebugInfo instance = new SntpQueryDebugInfo(addresses);
    NetworkOperationResult operation1 =
        NetworkOperationResult.failure(
            new InetSocketAddress(address1, 123), new Exception("test1"));
    NetworkOperationResult operation2 =
        NetworkOperationResult.failure(
            new InetSocketAddress(address2, 123), new Exception("test2"));
    NetworkOperationResult operation3 =
        NetworkOperationResult.failure(
            new InetSocketAddress(address3, 123), new Exception("test3"));

    assertThrows(
        IllegalArgumentException.class, () -> instance.addSntpQueryOperationResults(operation2));

    instance.addSntpQueryOperationResults(operation1);
    instance.addSntpQueryOperationResults(operation2);
    instance.addSntpQueryOperationResults(operation3);

    assertEquals(
        Arrays.asList(operation1, operation2, operation3), instance.getSntpQueryOperationResults());

    assertThrows(
        IllegalArgumentException.class, () -> instance.addSntpQueryOperationResults(operation3));
  }

  // Covers equals(Object), compareTo(Duration) & hashCode() (for equality only).
  @Test
  public void equals() throws Exception {
    InetAddress address1 = InetAddress.getByAddress(new byte[] {1, 1, 1, 1});
    InetAddress address2 = InetAddress.getByAddress(new byte[] {2, 2, 2, 2});
    InetAddress address3 = InetAddress.getByAddress(new byte[] {3, 3, 3, 3});
    List<InetAddress> addresses = Arrays.asList(address1, address2, address3);

    SntpQueryDebugInfo one = new SntpQueryDebugInfo(addresses);
    NetworkOperationResult operation1 =
        NetworkOperationResult.failure(new InetSocketAddress(address1, 123), new Exception("test"));
    NetworkOperationResult operation2 =
        NetworkOperationResult.success(new InetSocketAddress(address2, 123));
    one.addSntpQueryOperationResults(operation1);
    one.addSntpQueryOperationResults(operation2);

    SntpQueryDebugInfo two = new SntpQueryDebugInfo(addresses);
    two.addSntpQueryOperationResults(operation1);
    two.addSntpQueryOperationResults(operation2);
    assertEqualityMethods(one, two);
  }
}
