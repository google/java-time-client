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

import static org.junit.Assert.assertEquals;

/** Useful assertions. */
public final class MoreAsserts {

  private MoreAsserts() {}

  public static <T extends Comparable<T>> void assertComparisonMethods(T one, T two) {
    assertSelfComparison(one);
    assertSelfComparison(two);

    assertEquals(0, one.compareTo(two));
    assertEquals(0, two.compareTo(one));
    assertEquals(one, two);
    assertEquals(two.hashCode(), one.hashCode());
  }

  @SuppressWarnings("SelfComparison")
  private static <T extends Comparable<T>> void assertSelfComparison(T obj) {
    assertEquals(obj, obj);
    assertEquals(0, obj.compareTo(obj));
    assertEquals(obj.hashCode(), obj.hashCode());
  }

  public static <T> void assertEqualityMethods(T one, T two) {
    assertSelfEquality(one);
    assertSelfEquality(two);

    assertEquals(one, two);
    assertEquals(two.hashCode(), one.hashCode());
  }

  @SuppressWarnings("SelfComparison")
  private static <T> void assertSelfEquality(T obj) {
    assertEquals(obj, obj);
    assertEquals(obj.hashCode(), obj.hashCode());
  }
}
