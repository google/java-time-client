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

package com.google.time.client.sntp;

/** The base class for all NTP exceptions. */
abstract class NtpException extends Exception {

  public NtpException(String s) {
    super(s);
  }

  public NtpException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public NtpException(Throwable throwable) {
    super(throwable);
  }
}
