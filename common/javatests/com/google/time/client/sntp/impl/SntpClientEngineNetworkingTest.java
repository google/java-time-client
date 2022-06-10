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

package com.google.time.client.sntp.impl;

import static com.google.time.client.sntp.testing.FakeNetwork.FAILURE_MODE_RECEIVE;
import static com.google.time.client.sntp.testing.FakeNetwork.FAILURE_MODE_RECEIVE_TIMEOUT;
import static com.google.time.client.sntp.testing.FakeNetwork.FAILURE_MODE_SEND;
import static org.junit.Assert.assertThrows;

import com.google.time.client.base.impl.NoOpLogger;
import com.google.time.client.base.testing.FakeClocks;
import com.google.time.client.sntp.NtpServerNotReachableException;
import com.google.time.client.sntp.testing.FakeNetwork;
import com.google.time.client.sntp.testing.FakeSntpServerEngine;
import com.google.time.client.sntp.testing.TestSntpServerWithNetwork;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link SntpClientEngine} networking. */
@RunWith(JUnit4.class)
public class SntpClientEngineNetworkingTest {

  @Test
  public void requestInstant_sendFailure() throws Exception {
    testNetworkTransactionWithFailureMode(FAILURE_MODE_SEND);
  }

  @Test
  public void requestInstant_receiveTimeoutFailure() throws Exception {
    testNetworkTransactionWithFailureMode(FAILURE_MODE_RECEIVE_TIMEOUT);
  }

  @Test
  public void requestInstant_receiveFailure() throws Exception {
    testNetworkTransactionWithFailureMode(FAILURE_MODE_RECEIVE);
  }

  private void testNetworkTransactionWithFailureMode(int failureModeFlag) {
    FakeClocks fakeClientClocks = new FakeClocks();
    FakeClocks fakeServerClocks = new FakeClocks();
    // Use a fake network that can simulate failures.
    TestSntpServerWithNetwork<FakeSntpServerEngine, FakeNetwork> testSntpServerWithNetwork =
        TestSntpServerWithNetwork.createCompleteFake(fakeClientClocks, fakeServerClocks);
    FakeNetwork network = testSntpServerWithNetwork.getNetwork();
    network.setFailureMode(failureModeFlag);

    SntpClientEngine engine =
        new SntpClientEngine(
            NoOpLogger.instance(),
            testSntpServerWithNetwork.createConnector(
                fakeClientClocks.getFakeInstantSource(), fakeClientClocks.getFakeTicker()));
    assertThrows(
        NtpServerNotReachableException.class,
        () -> engine.requestInstant(fakeClientClocks.getFakeInstantSource()));
  }

  @Test
  public void requestInstant_nonMatchingOriginateTime() throws Exception {
    FakeClocks fakeClientClocks = new FakeClocks();
    FakeClocks fakeServerClocks = new FakeClocks();
    // This uses a fake network with a fake server engine set to behave strangely.
    TestSntpServerWithNetwork<FakeSntpServerEngine, FakeNetwork> testSntpServerWithNetwork =
        TestSntpServerWithNetwork.createCompleteFake(fakeClientClocks, fakeServerClocks);
    testSntpServerWithNetwork
        .getSntpServerEngine()
        .setQuirkMode(FakeSntpServerEngine.QUIRK_MODE_NON_MATCHING_ORIGINATE_TIME);
    SntpClientEngine engine =
        new SntpClientEngine(
            NoOpLogger.instance(),
            testSntpServerWithNetwork.createConnector(
                fakeClientClocks.getFakeInstantSource(), fakeClientClocks.getFakeTicker()));

    assertThrows(
        NtpServerNotReachableException.class,
        () -> engine.requestInstant(fakeClientClocks.getFakeInstantSource()));

    testSntpServerWithNetwork.stop();
  }
}
