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

package com.google.time.client.base;

import com.google.time.client.base.impl.Objects;

/** A server's name and port. */
public final class ServerAddress {
  private final String name;
  private final int port;

  /** Creates an instance. */
  public ServerAddress(String name, int port) {
    this.name = Objects.requireNonNull(name);
    this.port = port;
  }

  /** Returns the server's name. */
  public String getName() {
    return name;
  }

  /** Returns the server's port. */
  public int getPort() {
    return port;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ServerAddress)) {
      return false;
    }
    ServerAddress that = (ServerAddress) o;
    return port == that.port && name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, port);
  }

  @Override
  public String toString() {
    return "ServerAddress{" + "name='" + name + '\'' + ", port=" + port + '}';
  }
}
