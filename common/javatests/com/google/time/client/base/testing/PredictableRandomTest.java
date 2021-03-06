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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PredictableRandomTest {

  @Test
  public void randomDefault() {
    PredictableRandom random = new PredictableRandom();
    assertEquals(1, random.nextInt());
    assertEquals(1, random.nextInt());
    assertEquals(1, random.nextInt());
    assertEquals(1, random.nextInt());
  }

  @Test
  public void randomSequence() {
    PredictableRandom random = new PredictableRandom();
    random.setIntSequence(1, 2, 3);
    assertEquals(1, random.nextInt());
    assertEquals(2, random.nextInt());
    assertEquals(3, random.nextInt());
    assertEquals(1, random.nextInt());
    assertEquals(2, random.nextInt());
    assertEquals(3, random.nextInt());
  }
}
