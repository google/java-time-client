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

import com.google.time.client.base.impl.Objects;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetSocketAddress;

/** Indicates the result of a low-level network socket operation. */
public final class NetworkOperationResult {

  /** Types of result. */
  @Target(ElementType.TYPE_USE)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Type {}

  /** A result type that indicates the network operation succeeded. */
  public static final @Type int TYPE_SUCCESS = 10;

  /** A result type that indicates the network operation failed. */
  public static final @Type int TYPE_FAILURE = 11;

  /**
   * An indeterminate result type that indicates the blocking operation exceeded its allowed time.
   * It therefore cannot be known whether the server received the request, or what the response was,
   * if any. Different from a network timeout in protocols that have timeout built-in that would
   * usually be indicated by {@link #TYPE_FAILURE}.
   */
  public static final @Type int TYPE_TIME_ALLOWED_EXCEEDED = 12;

  private final InetSocketAddress socketAddress;

  private final @Type int type;

  private final Exception exception;

  /** Creates a {@link #TYPE_SUCCESS} instance. */
  public static NetworkOperationResult success(InetSocketAddress socketAddress) {
    return new NetworkOperationResult(socketAddress, TYPE_SUCCESS, null);
  }

  /** Creates a {@link #TYPE_TIME_ALLOWED_EXCEEDED} instance. */
  public static NetworkOperationResult timeAllowedExceeded(InetSocketAddress socketAddress) {
    return new NetworkOperationResult(socketAddress, TYPE_TIME_ALLOWED_EXCEEDED, null);
  }

  /** Creates a {@link #TYPE_FAILURE} instance. */
  public static NetworkOperationResult failure(InetSocketAddress socketAddress, Exception e) {
    return new NetworkOperationResult(socketAddress, TYPE_FAILURE, Objects.requireNonNull(e));
  }

  private NetworkOperationResult(
      InetSocketAddress socketAddress, @Type int type, Exception exception) {
    this.socketAddress = Objects.requireNonNull(socketAddress);
    this.type = type;
    this.exception = exception;
  }

  /** Returns the socket address of the operation. */
  public InetSocketAddress getSocketAddress() {
    return socketAddress;
  }

  /** Returns the return type of the operation. */
  public @Type int getType() {
    return type;
  }

  /**
   * Returns a description of the failure when {@link #getType()} is {@link #TYPE_FAILURE}.
   * Otherwise, {@code null}.
   */
  public Exception getException() {
    return exception;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NetworkOperationResult that = (NetworkOperationResult) o;
    return type == that.type
        && socketAddress.equals(that.socketAddress)
        && Objects.equals(exception, that.exception);
  }

  @Override
  public int hashCode() {
    return Objects.hash(socketAddress, type, exception);
  }

  @Override
  public String toString() {
    return "NetworkOperationResult{"
        + "socketAddress="
        + socketAddress
        + ", type="
        + type
        + ", exception="
        + exception
        + '}';
  }
}
