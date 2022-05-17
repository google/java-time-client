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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/** Provides access to a generally suitable default {@link Random} instance. */
public final class PlatformRandom {

  private static final Random DEFAULT_INSTANCE;

  static {
    try {
      DEFAULT_INSTANCE = SecureRandom.getInstanceStrong();
    } catch (NoSuchAlgorithmException e) {
      // This should not happen.
      throw new IllegalStateException(e);
    }
  }

  private PlatformRandom() {}

  public static Random getDefaultRandom() {
    return DEFAULT_INSTANCE;
  }
}
