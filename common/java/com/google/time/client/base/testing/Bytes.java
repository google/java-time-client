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

package com.google.time.client.base.testing;

/** Utility methods associated with primitive bytes and {@link java.lang.Byte}. */
public final class Bytes {

  private Bytes() {}

  /**
   * Coerces ints into bytes. Will accept anything in the range -128 to 255. i.e. it accepts values
   * that can be interpreted as signed or unsigned.
   */
  public static byte[] bytes(int... ints) {
    byte[] bytes = new byte[ints.length];
    for (int i = 0; i < ints.length; i++) {
      int byteAsInt = ints[i];
      if (byteAsInt < -128 || byteAsInt > 255) {
        throw new ArithmeticException("Value outside signed and unsigned byte range:" + byteAsInt);
      }
      bytes[i] = (byte) byteAsInt;
    }
    return bytes;
  }
}
