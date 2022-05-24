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

/**
 * An {@link InstantSource} that provides millisecond-precision access to the default system clock,
 * i.e. via {@link System#currentTimeMillis()}.
 */
public final class PlatformInstantSource extends InstantSource {

  private static final PlatformInstantSource INSTANCE = new PlatformInstantSource();

  public static InstantSource instance() {
    return INSTANCE;
  }

  private PlatformInstantSource() {}

  @Override
  public Instant instant() {
    return Instant.ofEpochMilli(System.currentTimeMillis());
  }

  @Override
  public int getPrecision() {
    return PRECISION_MILLIS;
  }

  @Override
  public String toString() {
    return "PlatformInstantSource";
  }
}
