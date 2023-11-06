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
package com.google.time.client.base.impl;

import com.google.time.client.base.Duration;
import com.google.time.client.base.Network;
import com.google.time.client.base.Ticker;
import com.google.time.client.base.Ticks;
import com.google.time.client.base.impl.ClusteredServiceOperation.ServiceOperation.ServiceResult;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A support class for performing an idempotent operation against a clustered network service, i.e.
 * a service that is hosted on multiple IP addresses. An operation is tried against each IP address
 * in turn until one succeeds, there is a "halting" failure (i.e. where one server in a cluster
 * responds with something that indicates trying others is not reasonable), the time allowed has
 * been exceeded, or all IP addresses have been tried unsuccessfully.
 *
 * <p>This class is not thread-safe.
 *
 * @param <R> the type of the operation-specific {@link #execute} parameter (use Void if there is no
 *     parameter)
 * @param <S> the type of the operation-specific success result
 * @param <T> the type of the operation-specific failure result
 */
public final class ClusteredServiceOperation<R, S, T> {

  /**
   * The result of attempting a network operation against one or more IP addresses.
   *
   * <p>A success result can contain (non-halting) failures. A failure result only contains
   * failures.
   *
   * @param <S> the type of the success result
   * @param <T> the type of the failure result
   */
  public static final class ClusteredServiceResult<S, T> {
    /** The overall result was a success. */
    public static final int TYPE_SUCCESS = 11;

    /**
     * This means the overall transaction exceeded its allowed time. Note: Protocol-level timeouts
     * on network operations will typically be considered a non-halting failure and NOT related to
     * exceeding the time allowed.
     */
    public static final int TYPE_TIME_ALLOWED_EXCEEDED = 12;

    /** The overall result was a failure. See also {@link #isHalted()}. */
    public static final int TYPE_FAILURE = 13;

    private final List<InetAddress> inetAddresses;
    private final int type;
    private final ServiceResult<S, T> successOpResult;
    private final List<ServiceResult<S, T>> failureOpResults = new ArrayList<>();
    private final boolean halted;

    private ClusteredServiceResult(
        List<InetAddress> inetAddresses,
        int type,
        ServiceResult<S, T> successOpResult,
        List<ServiceResult<S, T>> failureOpResults,
        boolean halted) {
      this.inetAddresses = new ArrayList<>(inetAddresses);
      if (type < TYPE_SUCCESS || type > TYPE_FAILURE) {
        throw new IllegalArgumentException("Unknown type=" + type);
      }
      this.type = type;
      this.successOpResult = successOpResult;
      this.failureOpResults.addAll(failureOpResults);
      this.halted = halted;
    }

    /**
     * Creates a success result.
     *
     * @param allIpAddresses all the IP addresses that could have been tried
     * @param success the success result
     * @param failures the non-halting failures, if any
     */
    public static <S, T> ClusteredServiceResult<S, T> success(
        List<InetAddress> allIpAddresses,
        ServiceResult<S, T> success,
        List<ServiceResult<S, T>> failures) {
      return new ClusteredServiceResult<S, T>(
          allIpAddresses, TYPE_SUCCESS, success, failures, /*halted=*/ true);
    }

    /**
     * Creates a result that indicates the time allowed has been exceeded.
     *
     * @param allIpAddresses all the IP addresses that could have been tried
     * @param failures the failures, if any
     */
    public static <S, T> ClusteredServiceResult<S, T> timeAllowedExceeded(
        List<InetAddress> allIpAddresses, List<ServiceResult<S, T>> failures) {
      return new ClusteredServiceResult<>(
          allIpAddresses, TYPE_TIME_ALLOWED_EXCEEDED, null, failures, /*halted=*/ false);
    }

    /**
     * Creates a failure result.
     *
     * @param allIpAddresses all the IP addresses that could have been tried
     * @param failures the failures, if any
     * @param halted if the operation stopped because of a halting failure
     */
    public static <S, T> ClusteredServiceResult<S, T> failure(
        List<InetAddress> allIpAddresses, List<ServiceResult<S, T>> failures, boolean halted) {
      return new ClusteredServiceResult<>(allIpAddresses, TYPE_FAILURE, null, failures, halted);
    }

    /** Returns the IP addresses associated with the service. */
    public List<InetAddress> getServiceAddresses() {
      return new ArrayList<>(inetAddresses);
    }

    /** Returns {@code true} if the operation was a success. */
    public boolean isSuccess() {
      return type == TYPE_SUCCESS;
    }

    /** Returns the success value, or {@code null} if there isn't one. */
    public S getSuccessValue() {
      return successOpResult == null ? null : successOpResult.getSuccessValue();
    }

    /**
     * Returns all failure values. The returned list can be empty if there were no failures
     * recorded.
     */
    public List<T> getFailureValues() {
      List<T> values = new ArrayList<>();
      for (ServiceResult<S, T> failureOpResult : failureOpResults) {
        values.add(failureOpResult.getFailureValue());
      }
      return values;
    }

    /** The final failure value stored. Returns {@code null} if there were no failures recorded. */
    public T getLastFailureValue() {
      return failureOpResults.isEmpty()
          ? null
          : failureOpResults.get(failureOpResults.size() - 1).getFailureValue();
    }

    /**
     * Returns {@code true} if the operation failed, The operation may have failed due to a halting
     * failure or because all IP addresses were tried and failed.
     */
    public boolean isFailure() {
      return type == TYPE_FAILURE;
    }

    /**
     * Returns {@code true} if the operation "halted", either because of success or there was a
     * halting failure.
     */
    public boolean isHalted() {
      return halted;
    }

    /** Returns {@code true} if the time allowed for the operation was definitely exceeded. */
    public boolean isTimeAllowedExceeded() {
      return type == TYPE_TIME_ALLOWED_EXCEEDED;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ClusteredServiceOperation.ClusteredServiceResult)) {
        return false;
      }
      ClusteredServiceResult<?, ?> that = (ClusteredServiceResult<?, ?>) o;
      return type == that.type
          && halted == that.halted
          && Objects.equals(inetAddresses, that.inetAddresses)
          && Objects.equals(successOpResult, that.successOpResult)
          && Objects.equals(failureOpResults, that.failureOpResults);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, successOpResult, failureOpResults, halted, inetAddresses);
    }

    @Override
    public String toString() {
      return "ClusteredServiceResult{"
          + "inetAddresses="
          + inetAddresses
          + ", type="
          + type
          + ", successOpResult="
          + successOpResult
          + ", failureOpResults="
          + failureOpResults
          + ", halted="
          + halted
          + '}';
    }
  }

  private final Ticker timeAllowedTicker;
  private final Network network;
  private final ServiceOperation<R, S, T> operation;

  /**
   * Creates a new service connector.
   *
   * @param timeAllowedTicker the ticker to use to track whether the time allowed has been exceeded
   * @param network the network to use to resolve server names
   * @param operation the operation to perform
   */
  public ClusteredServiceOperation(
      Ticker timeAllowedTicker, Network network, ServiceOperation<R, S, T> operation) {
    this.timeAllowedTicker = Objects.requireNonNull(timeAllowedTicker, "timeAllowedTicker");
    this.network = Objects.requireNonNull(network, "network");
    this.operation = Objects.requireNonNull(operation, "operation");
  }

  /**
   * Executes the network operation against one or more IP addresses associated with the specified
   * {@code serverName} using the supplied {@code parameter}. This is a blocking operation.
   *
   * @throws UnknownHostException if {@code serverName} could not be resolved
   */
  public ClusteredServiceResult<S, T> execute(String serverName, R parameter, Duration timeAllowed)
      throws UnknownHostException {
    if (timeAllowed != null && timeAllowed.compareTo(Duration.ZERO) <= 0) {
      throw new IllegalArgumentException("timeAllowed=" + timeAllowed + " is negative or zero");
    }

    Ticks ticksBeforeAddressLookup = timeAllowedTicker.ticks();
    List<InetAddress> allIpAddresses = Arrays.asList(network.getAllByName(serverName));
    Ticks ticksAfterAddressLookup = timeAllowedTicker.ticks();

    Duration timeAllowedRemaining =
        calculateTimeAllowedRemainingOrNull(
            timeAllowed, ticksBeforeAddressLookup, ticksAfterAddressLookup);
    return executeInternal(serverName, parameter, timeAllowedRemaining, allIpAddresses);
  }

  private ClusteredServiceResult<S, T> executeInternal(
      String serverName, R parameter, Duration timeAllowed, List<InetAddress> allIpAddresses) {
    List<ServiceResult<S, T>> failures = new ArrayList<>(allIpAddresses.size());

    Ticks startTicks = timeAllowedTicker.ticks();
    int ipAddressIndex = 0;
    InetAddress inetAddress;
    ServiceResult<S, T> result;
    while (true) {
      inetAddress = allIpAddresses.get(ipAddressIndex);

      // Check whether time allowed has been exceeded.
      Ticks nowTicks = timeAllowedTicker.ticks();
      Duration timeAllowedRemaining =
          calculateTimeAllowedRemainingOrNull(timeAllowed, startTicks, nowTicks);
      boolean timeAllowedExceeded =
          timeAllowedRemaining != null && timeAllowedRemaining.compareTo(Duration.ZERO) <= 0;
      if (timeAllowedExceeded) {
        return ClusteredServiceResult.timeAllowedExceeded(allIpAddresses, failures);
      }

      // Perform the operation.
      result = operation.execute(serverName, inetAddress, parameter, timeAllowedRemaining);
      Ticks afterOperationTicks = timeAllowedTicker.ticks();

      if (result.isSuccess()) {
        // Success - must return it immediately.
        return ClusteredServiceResult.success(allIpAddresses, result, failures);
      }

      if (result.isTimeAllowedExceeded()) {
        // Operation reported that the time allowed has been exceeded.
        timeAllowedRemaining =
            calculateTimeAllowedRemainingOrNull(timeAllowed, startTicks, afterOperationTicks);
        if (timeAllowedRemaining.compareTo(Duration.ZERO) > 0) {
          // The operation "lied" about exceeding the time remaining. Treat this as a coding error
          // and throw an exception.
          throw new IllegalStateException(
              "operation="
                  + operation
                  + " reported time allowed exceed (result="
                  + result
                  + "), but time allowed remains="
                  + timeAllowedRemaining);
        } else {
          return ClusteredServiceResult.timeAllowedExceeded(allIpAddresses, failures);
        }
      }

      // A failure of some sort
      boolean mustHalt = result.isHaltingFailure();
      failures.add(result);
      ipAddressIndex++;
      if (mustHalt || ipAddressIndex >= allIpAddresses.size()) {
        return ClusteredServiceResult.failure(allIpAddresses, failures, mustHalt);
      }
    }
  }

  private Duration calculateTimeAllowedRemainingOrNull(
      Duration timeAllowed, Ticks startTicks, Ticks nowTicks) {
    Duration timeAllowedRemaining = null;
    if (timeAllowed != null) {
      timeAllowedRemaining =
          timeAllowed.minus(timeAllowedTicker.durationBetween(startTicks, nowTicks));
    }
    return timeAllowedRemaining;
  }

  /**
   * An idempotent network service operation. This implemented by developers wanting to use {@link
   * ClusteredServiceOperation} to contain the protocol-specific aspects.
   *
   * @param <R> the type of the request
   * @param <S> the type for a successful response/result
   * @param <T> the type for a failure response/result
   */
  public interface ServiceOperation<R, S, T> {

    /**
     * The result of attempting a network operation against a single IP address.
     *
     * @param <S> the type of the success result
     * @param <T> the type of the failure result
     */
    final class ServiceResult<S, T> {

      /** Types of result. */
      @Target(ElementType.TYPE_USE)
      @Retention(RetentionPolicy.SOURCE)
      private @interface Type {}

      /** Indicates the operation succeeded. */
      private static final @Type int TYPE_SUCCESS = 111;

      /** Indicates the operation could not be completed in the time allowed. */
      private static final @Type int TYPE_TIME_ALLOWED_EXCEEDED = 112;

      /**
       * Indicates the network operation failed and the next IP address, if available, can be tried.
       */
      private static final @Type int TYPE_FAILURE_ADVANCE = 113;

      /**
       * Indicates the network operation failed and the next IP address, if available, should not be
       * tried.
       */
      private static final @Type int TYPE_FAILURE_HALT = 114;

      private final InetAddress inetAddress;
      private final @Type int type;
      private final S successValue;
      private final T failureValue;

      /**
       * Creates a success result.
       *
       * @param inetAddress the IP address that succeeded
       * @param successValue the success result
       */
      public static <S, T> ServiceResult<S, T> success(InetAddress inetAddress, S successValue) {
        return new ServiceResult<>(inetAddress, TYPE_SUCCESS, successValue, null);
      }

      /**
       * Creates a failure result.
       *
       * @param inetAddress the IP address that failed
       * @param failureValue the failure result
       * @param halt {@code true} if this failure should prevent attempts against later IP
       *     addresses, if any
       */
      public static <S, T> ServiceResult<S, T> failure(
          InetAddress inetAddress, T failureValue, boolean halt) {
        int type = halt ? TYPE_FAILURE_HALT : TYPE_FAILURE_ADVANCE;
        return new ServiceResult<S, T>(inetAddress, type, null, failureValue);
      }

      /**
       * Creates a result that indicates the time allowed has been exceeded. The operation may or
       * may not have been attempted and this should not be treated as a failure.
       *
       * @param inetAddress the IP address that timed out
       */
      public static <S, T> ServiceResult<S, T> timeAllowedExceeded(InetAddress inetAddress) {
        return new ServiceResult<S, T>(inetAddress, TYPE_TIME_ALLOWED_EXCEEDED, null, null);
      }

      private ServiceResult(
          InetAddress inetAddress, @Type int type, S successValue, T failureValue) {
        this.inetAddress = Objects.requireNonNull(inetAddress);
        if (type < TYPE_SUCCESS || type > TYPE_FAILURE_HALT) {
          throw new IllegalArgumentException("Unknown type=" + type);
        }
        this.type = type;
        this.successValue = successValue;
        this.failureValue = failureValue;
      }

      /** Returns {@code true} if the operation was a success. */
      public boolean isSuccess() {
        return type == TYPE_SUCCESS;
      }

      /** Returns {@code true} if the operation timed out. */
      public boolean isTimeAllowedExceeded() {
        return type == TYPE_TIME_ALLOWED_EXCEEDED;
      }

      /**
       * Returns {@code true} if the operation failed and the overall operation should be "halted".
       */
      public boolean isHaltingFailure() {
        return type == TYPE_FAILURE_HALT;
      }

      /** Returns the success value. {@code null} if this is not a success result. */
      public S getSuccessValue() {
        return successValue;
      }

      /** Returns the failure value. {@code null} if this is not a failure result. */
      public T getFailureValue() {
        return failureValue;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (!(o instanceof ServiceOperation.ServiceResult)) {
          return false;
        }
        ServiceResult<?, ?> that = (ServiceResult<?, ?>) o;
        return type == that.type
            && inetAddress.equals(that.inetAddress)
            && Objects.equals(successValue, that.successValue)
            && Objects.equals(failureValue, that.failureValue);
      }

      @Override
      public int hashCode() {
        return Objects.hash(inetAddress, type, successValue, failureValue);
      }

      @Override
      public String toString() {
        return "ServiceResult{"
            + "inetAddress="
            + inetAddress
            + ", type="
            + type
            + ", successValue="
            + successValue
            + ", failureValue="
            + failureValue
            + '}';
      }
    }

    /**
     * Executes the operation.
     *
     * @param serverName the name being used to access the service
     * @param inetAddress the IP address to be tried
     * @param parameter the parameter
     * @param timeAllowed the time allowed for the operation, or null if there is no overall time
     *     limit to enforce. When present, the operation must make best efforts to avoid exceeding
     *     this value
     * @return the result of attempting the operation
     */
    ServiceResult<S, T> execute(
        String serverName, InetAddress inetAddress, R parameter, Duration timeAllowed);
  }
}
