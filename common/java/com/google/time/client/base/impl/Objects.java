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

import java.util.Arrays;

/**
 * A stand-in for java.util.Objects, which is not available on all platform versions supported by
 * java-time-client.
 */
public final class Objects {
  private Objects() {}

  /**
   * Returns the argument if it is non-null. Throws {@link java.lang.NullPointerException} if the
   * argument is {@code null}.
   */
  public static <T> T requireNonNull(T object) {
    return requireNonNull(object, null);
  }

  /**
   * Returns the argument if it is non-null. Throws {@link java.lang.NullPointerException} if the
   * argument is {@code null}.
   */
  public static <T> T requireNonNull(T object, String message) {
    if (object == null) {
      throw new NullPointerException(message);
    }
    return object;
  }

  /** Returns a hashCode value for the supplied arguments. */
  public static int hash(Object... toHash) {
    return Arrays.hashCode(toHash);
  }

  /**
   * Returns {@code true} if both arguments are {@code null} or if {@link Object#equals} returns
   * {@code code}.
   */
  public static boolean equals(Object one, Object two) {
    return one == two || (one != null && one.equals(two));
  }

  /**
   * Returns the result of calling {@link Object#toString()} or "null" if {@code objectOrNull} is
   * {@code null}.
   */
  public static String toString(Object objectOrNull) {
    return objectOrNull == null ? "null" : objectOrNull.toString();
  }
}
