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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ObjectsTest {

  @Test
  public void requireNonNull() {
    assertThrows(NullPointerException.class, () -> Objects.requireNonNull(null));

    Object nonNull = new Object();
    assertEquals(nonNull, Objects.requireNonNull(nonNull));
  }

  @Test
  public void hash() {
    assertEquals(Arrays.hashCode(new Object[] {null}), Objects.hash((Object) null));
    assertEquals(Arrays.hashCode(new Object[] {null, null}), Objects.hash(null, null));

    Object nonNull = new Object();
    assertEquals(Arrays.hashCode(new Object[] {nonNull}), Objects.hash(nonNull));
  }

  @Test
  public void equals() {
    assertTrue(Objects.equals(null, null));

    Object nonNull = new Object();
    assertTrue(Objects.equals(nonNull, nonNull));
    assertFalse(Objects.equals(nonNull, null));
    assertFalse(Objects.equals(null, nonNull));
  }
}
