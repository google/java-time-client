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

import com.google.time.client.base.impl.Objects;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The result of making an SNTP query. */
public final class SntpQueryResult {

  @Target(ElementType.TYPE_USE)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Type {}

  /** The type used when an SNTP query was successful. */
  public static final @Type int TYPE_SUCCESS = 10;
  /** The type used when an SNTP query was unsuccessful due to a transient failure. */
  public static final @Type int TYPE_RETRY_LATER = 11;
  /** The type used when an SNTP query was unsuccessful due to a protocol error. */
  public static final @Type int TYPE_PROTOCOL_ERROR = 12;
  /**
   * The type used when an SNTP query was not able to complete because the time allowed was
   * exceeded.
   */
  public static final @Type int TYPE_TIME_ALLOWED_EXCEEDED = 13;

  private final @Type int type;

  /** Information about underlying network operations. */
  private final SntpQueryDebugInfo sntpQueryDebugInfo;

  // One of the following will be set depending on the type.
  private final SntpTimeSignal sntpTimeSignal;
  private final Exception exception;

  /** Creates an {@link SntpQueryResult} to wrap the result of a successful SNTP query. */
  public static SntpQueryResult success(
      SntpQueryDebugInfo sntpQueryDebugInfo, SntpTimeSignal sntpTimeSignal) {
    return new SntpQueryResult(sntpQueryDebugInfo, TYPE_SUCCESS, sntpTimeSignal, null);
  }

  /**
   * Creates an {@link SntpQueryResult} to wrap the result of an unsuccessful SNTP query that can be
   * retried.
   */
  public static SntpQueryResult retryLater(
      SntpQueryDebugInfo sntpQueryDebugInfo, Exception exception) {
    return new SntpQueryResult(sntpQueryDebugInfo, TYPE_RETRY_LATER, null, exception);
  }

  /**
   * Creates an {@link SntpQueryResult} to wrap the result of an unsuccessful SNTP query that
   * resulted in a protocol error (and is therefore unlikely to result in a different result if
   * retried).
   */
  public static SntpQueryResult protocolError(
      SntpQueryDebugInfo sntpQueryDebugInfo, Exception exception) {
    return new SntpQueryResult(sntpQueryDebugInfo, TYPE_PROTOCOL_ERROR, null, exception);
  }

  /**
   * Creates an {@link SntpQueryResult} to wrap the result of an SNTP query that had to be stopped
   * because the time allowed was exceeded.
   */
  public static SntpQueryResult timeAllowedExceeded(SntpQueryDebugInfo sntpQueryDebugInfo) {
    return new SntpQueryResult(sntpQueryDebugInfo, TYPE_TIME_ALLOWED_EXCEEDED, null, null);
  }

  private SntpQueryResult(
      SntpQueryDebugInfo sntpQueryDebugInfo, int type, SntpTimeSignal sntpTimeSignal, Exception e) {
    this.sntpQueryDebugInfo = Objects.requireNonNull(sntpQueryDebugInfo);

    if (type < TYPE_SUCCESS || type > TYPE_TIME_ALLOWED_EXCEEDED) {
      throw new IllegalArgumentException("Unknown type=" + type);
    }
    this.type = type;
    this.sntpTimeSignal = sntpTimeSignal;
    this.exception = e;
  }

  /** Returns underling details of the query. Intended for debugging and failure metrics. */
  public SntpQueryDebugInfo getQueryDebugInfo() {
    return sntpQueryDebugInfo;
  }

  /** Returns the type of the result. */
  public @Type int getType() {
    return type;
  }

  /**
   * The time signal received when {@link #getType()} is {@link #TYPE_SUCCESS}. Otherwise, returns
   * {@code null}.
   */
  public SntpTimeSignal getTimeSignal() {
    return sntpTimeSignal;
  }

  /**
   * The exception when {@link #getType()} is {@link #TYPE_PROTOCOL_ERROR} or {@link
   * #TYPE_RETRY_LATER}. Otherwise, returns {@code null}.
   */
  public Exception getException() {
    return exception;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SntpQueryResult)) {
      return false;
    }
    SntpQueryResult that = (SntpQueryResult) o;
    return type == that.type
        && Objects.equals(sntpQueryDebugInfo, that.sntpQueryDebugInfo)
        && Objects.equals(sntpTimeSignal, that.sntpTimeSignal)
        && Objects.equals(exception, that.exception);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, sntpQueryDebugInfo, sntpTimeSignal, exception);
  }

  @Override
  public String toString() {
    return "SntpResult{"
        + "sntpQueryDebugInfo="
        + sntpQueryDebugInfo
        + ",type="
        + type
        + ", sntpTimeSignal="
        + sntpTimeSignal
        + ", exception="
        + exception
        + '}';
  }
}
