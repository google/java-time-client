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

/**
 * Methods to support tests that need to have different behaviors under different variant /
 * environments.
 *
 * <p>This is the Java SE variant of this class.
 */
public final class TestEnvironmentUtils {

  private TestEnvironmentUtils() {}

  /** Returns {@code true} if the test is running the Java SE variant of code. */
  public static boolean isThisJavaSe() {
    return true;
  }

  /**
   * Returns the Java version when the test is running the Java SE variant of code. Throws {@link
   * AssertionError} when on Android.
   */
  public static int getJavaVersion() {
    String specVersion = System.getProperty("java.specification.version");
    if (specVersion.equals("1.8")) {
      return 8;
    } else {
      return Integer.parseInt(specVersion);
    }
  }

  /** Returns {@code true} if the test is running the Android variant of code. */
  public static boolean isThisAndroid() {
    return false;
  }

  /**
   * Throws {@link org.junit.AssumptionViolatedException} if running the Android variant of code
   * under robolectric.
   */
  public static void assumeNotRobolectric(String reason) {
    // This is the javase variant of this class, so by definition this assumption holds true.
  }
}
