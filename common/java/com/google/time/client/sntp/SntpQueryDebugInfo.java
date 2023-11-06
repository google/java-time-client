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

import com.google.time.client.base.NetworkOperationResult;
import com.google.time.client.base.impl.Objects;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Information about a SNTP query that could be useful for debugging, metrics and alerting about
 * server or networking problems.
 */
public final class SntpQueryDebugInfo {

  private final List<InetAddress> sntpQueryAddresses = new ArrayList<>();
  private final List<NetworkOperationResult> sntpQueryOperationResults = new ArrayList<>();

  /**
   * @param sntpQueryAddresses the list of IP addresses that were resolved for the SNTP query
   */
  public SntpQueryDebugInfo(List<InetAddress> sntpQueryAddresses) {
    this.sntpQueryAddresses.addAll(sntpQueryAddresses);
  }

  /**
   * Adds a {@link NetworkOperationResult} for a single SNTP query operation. The results must be
   * added in the same order as {@link #getSntpQueryAddresses()}.
   */
  public void addSntpQueryOperationResults(NetworkOperationResult sntpQueryOperationResult) {
    int nextResultIndex = sntpQueryOperationResults.size();
    if (nextResultIndex >= sntpQueryAddresses.size()) {
      throw new IllegalArgumentException("No more operations expected");
    }

    InetAddress resultAddress = sntpQueryOperationResult.getSocketAddress().getAddress();
    if (!resultAddress.equals(sntpQueryAddresses.get(nextResultIndex))) {
      throw new IllegalArgumentException(
          "InetAddress at position "
              + nextResultIndex
              + " does not match "
              + sntpQueryOperationResult);
    }
    sntpQueryOperationResults.add(sntpQueryOperationResult);
  }

  /** Returns the list of IP addresses that were resolved for the SNTP query. */
  public List<InetAddress> getSntpQueryAddresses() {
    return new ArrayList<>(sntpQueryAddresses);
  }

  /**
   * Returns the results of network operations conducted for the SNTP query. This will contain one
   * or more results related to the addresses from {@link #getSntpQueryAddresses()}.
   */
  public List<NetworkOperationResult> getSntpQueryOperationResults() {
    return new ArrayList<>(sntpQueryOperationResults);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SntpQueryDebugInfo that = (SntpQueryDebugInfo) o;
    return sntpQueryAddresses.equals(that.sntpQueryAddresses)
        && sntpQueryOperationResults.equals(that.sntpQueryOperationResults);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sntpQueryAddresses, sntpQueryOperationResults);
  }

  @Override
  public String toString() {
    return "SntpQueryDebugInfo{"
        + "sntpQueryAddresses="
        + sntpQueryAddresses
        + ", sntpQueryOperationResults="
        + sntpQueryOperationResults
        + '}';
  }
}
