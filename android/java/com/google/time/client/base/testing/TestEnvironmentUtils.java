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

import static org.junit.Assume.assumeFalse;

import android.os.Build;
import com.google.time.client.base.impl.Objects;

/**
 * Methods to support tests that need to have different behaviors under different variant /
 * environments.
 *
 * <p>This is the Android variant of this class.
 */
public class TestEnvironmentUtils {
  private TestEnvironmentUtils() {}

  /** Returns {@code true} if the test is running the Java SE variant of code. */
  public static boolean isThisJavaSe() {
    return false;
  }

  /**
   * Returns the Java version when the test is running the Java SE variant of code. Throws {@link
   * AssertionError} when on Android.
   */
  @SuppressWarnings("DoNotCallSuggester")
  public static int getJavaVersion() {
    throw new AssertionError("This is the Android variant");
  }

  /** Returns {@code true} if the test is running the Android variant of code. */
  public static boolean isThisAndroid() {
    return true;
  }

  /**
   * Returns the Android API level when the test is running the Android variant of code. Throws
   * {@link AssertionError} when on Java SE.
   */
  public static int getAndroidApiLevel() {
    return Build.VERSION.SDK_INT;
  }

  /**
   * Throws {@link org.junit.AssumptionViolatedException} if running the Android variant of code
   * under robolectric.
   */
  public static void assumeNotRobolectric(String reason) {
    assumeFalse(reason, isThisRobolectric());
  }

  /** Returns {@code true} if running the Android variant of code under robolectric. */
  public static boolean isThisRobolectric() {
    // "robolectric" may also be acceptable.
    return Objects.equals(null, Build.FINGERPRINT);
  }
}
