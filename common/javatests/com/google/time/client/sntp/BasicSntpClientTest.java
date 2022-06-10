/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.time.client.base.testing.FakeClocks;
import com.google.time.client.sntp.impl.SntpClientEngine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BasicSntpClientTest {

  private FakeClocks fakeClocks;
  private SntpClientEngine mockEngine;
  private BasicSntpClient client;

  @Before
  public void setUp() throws Exception {
    fakeClocks = new FakeClocks();
    mockEngine = mock(SntpClientEngine.class);
    client = new BasicSntpClient(mockEngine, fakeClocks.getFakeInstantSource());
  }

  @Test
  public void serverUnreachable() throws Exception {
    when(mockEngine.requestInstant(fakeClocks.getFakeInstantSource()))
        .thenThrow(new NtpServerNotReachableException("Test exception"));

    assertThrows(NtpServerNotReachableException.class, () -> client.requestInstant());
  }

  @Test
  public void responseReceived() throws Exception {
    SntpResult mockResult = mock(SntpResult.class);
    when(mockEngine.requestInstant(fakeClocks.getFakeInstantSource())).thenReturn(mockResult);

    assertSame(mockResult, client.requestInstant());
  }
}
