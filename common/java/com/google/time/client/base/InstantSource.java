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

import static com.google.time.client.base.impl.DateTimeConstants.MILLISECONDS_PER_SECOND;
import static com.google.time.client.base.impl.DateTimeConstants.NANOS_PER_SECOND;

/** A source of instants. */
public abstract class InstantSource {

  /** Used to indicate Instants have millisecond precision. */
  public static final int PRECISION_MILLIS = MILLISECONDS_PER_SECOND;

  /** Used to indicate Instants have nanosecond precision. */
  public static final int PRECISION_NANOS = NANOS_PER_SECOND;

  /** Returns the current instant from the source. */
  /*@NonNull*/ public abstract Instant instant();

  /** Returns the underlying clock precision. */
  public abstract int getPrecision();
}
