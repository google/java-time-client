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

package com.google.time.client.base;

import static com.google.time.client.base.NetworkOperationResult.TYPE_FAILURE;
import static com.google.time.client.base.NetworkOperationResult.TYPE_SUCCESS;
import static com.google.time.client.base.NetworkOperationResult.TYPE_TIME_ALLOWED_EXCEEDED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NetworkOperationResultTest {

  @Test
  public void success() throws Exception {
    assertThrows(NullPointerException.class, () -> NetworkOperationResult.success(null));

    InetSocketAddress address =
        new InetSocketAddress(InetAddress.getByAddress(new byte[] {1, 1, 1, 1}), 1234);
    NetworkOperationResult success = NetworkOperationResult.success(address);
    assertEquals(address, success.getSocketAddress());
    assertEquals(TYPE_SUCCESS, success.getType());
    assertNull(success.getException());
  }

  @Test
  public void failure() throws Exception {
    InetSocketAddress address =
        new InetSocketAddress(InetAddress.getByAddress(new byte[] {1, 1, 1, 1}), 1234);
    Exception exception = new Exception();
    assertThrows(NullPointerException.class, () -> NetworkOperationResult.failure(null, exception));
    assertThrows(NullPointerException.class, () -> NetworkOperationResult.failure(address, null));

    NetworkOperationResult failure = NetworkOperationResult.failure(address, exception);
    assertEquals(address, failure.getSocketAddress());
    assertEquals(TYPE_FAILURE, failure.getType());
    assertEquals(exception, failure.getException());
  }

  @Test
  public void timeAllowedExceeded() throws Exception {
    assertThrows(
        NullPointerException.class, () -> NetworkOperationResult.timeAllowedExceeded(null));

    InetSocketAddress address =
        new InetSocketAddress(InetAddress.getByAddress(new byte[] {1, 1, 1, 1}), 1234);
    NetworkOperationResult timeAllowedExceeded =
        NetworkOperationResult.timeAllowedExceeded(address);
    assertEquals(address, timeAllowedExceeded.getSocketAddress());
    assertEquals(TYPE_TIME_ALLOWED_EXCEEDED, timeAllowedExceeded.getType());
    assertNull(timeAllowedExceeded.getException());
  }

  // Covers equals(Object), compareTo(Duration) & hashCode() (for equality only).
  @Test
  public void equals() throws Exception {
    InetSocketAddress address1 =
        new InetSocketAddress(InetAddress.getByAddress(new byte[] {1, 1, 1, 1}), 1234);
    InetSocketAddress address2 =
        new InetSocketAddress(InetAddress.getByAddress(new byte[] {2, 2, 2, 2}), 1234);

    assertEqualityMethods(
        NetworkOperationResult.success(address1), NetworkOperationResult.success(address1));
    assertNotEquals(
        NetworkOperationResult.success(address1), NetworkOperationResult.success(address2));

    Exception exception1 = new Exception("test1");
    Exception exception2 = new Exception("test2");
    assertEqualityMethods(
        NetworkOperationResult.failure(address1, exception1),
        NetworkOperationResult.failure(address1, exception1));
    assertNotEquals(
        NetworkOperationResult.failure(address1, exception1),
        NetworkOperationResult.failure(address1, exception2));
    assertNotEquals(
        NetworkOperationResult.failure(address2, exception1),
        NetworkOperationResult.failure(address1, exception1));

    assertEqualityMethods(
        NetworkOperationResult.timeAllowedExceeded(address1),
        NetworkOperationResult.timeAllowedExceeded(address1));
    assertNotEquals(
        NetworkOperationResult.timeAllowedExceeded(address1),
        NetworkOperationResult.timeAllowedExceeded(address2));
  }

  private static <T> void assertEqualityMethods(T one, T two) {
    assertSelfEquality(one);
    assertSelfEquality(two);

    assertEquals(one, two);
    assertEquals(two.hashCode(), one.hashCode());
  }

  @SuppressWarnings("SelfComparison")
  private static <T> void assertSelfEquality(T obj) {
    assertEquals(obj, obj);
    assertEquals(obj.hashCode(), obj.hashCode());
  }
}
