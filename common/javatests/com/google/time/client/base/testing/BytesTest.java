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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class BytesTest {

  @Test
  public void bytes() {
    // These demonstrate how the bytes() method is more compact than Java's often verbose syntax for
    // byte literals, and inability to deal with unsigned bytes, which is why the method exists.
    assertArrayEquals(
        new byte[] {
          (byte) 0b10000000,
          (byte) 0b10000001,
          (byte) 0b11111111,
          (byte) 0b00000000,
          (byte) 0b00000001,
          (byte) 0b01111110,
          (byte) 0b01111111,
          (byte) 0b10000000,
          (byte) 0b11111110,
          (byte) 0b11111111,
        },
        Bytes.bytes(-128, -127, -1, 0, 1, 126, 127, 128, 254, 255));
    assertArrayEquals(
        new byte[] {
          -128, -127, -1, 0, 1, 126, 127, (byte) 128, (byte) 254, (byte) 255,
        },
        Bytes.bytes(-128, -127, -1, 0, 1, 126, 127, 128, 254, 255));

    assertThrows(ArithmeticException.class, () -> Bytes.bytes(256));
    assertThrows(ArithmeticException.class, () -> Bytes.bytes(Integer.MAX_VALUE));
    assertThrows(ArithmeticException.class, () -> Bytes.bytes(-129));
    assertThrows(ArithmeticException.class, () -> Bytes.bytes(Integer.MIN_VALUE));
  }
}
