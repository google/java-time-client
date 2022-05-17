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

/** Date / time related constants that show up in code a lot and utility methods. */
public interface DateTimeConstants {
  int NANOS_PER_MILLISECOND = 1_000_000;
  int NANOS_PER_SECOND = 1_000_000_000;
  int MILLISECONDS_PER_SECOND = 1_000;
}
