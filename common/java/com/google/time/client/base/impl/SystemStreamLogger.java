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

import com.google.time.client.base.Logger;
import java.io.PrintStream;

/** A Logger implementation that uses System.out and System.err. */
public final class SystemStreamLogger implements Logger {

  private boolean loggingFine = false;

  @Override
  public boolean isLoggingFine() {
    return loggingFine;
  }

  public void setLoggingFine(boolean fineLogging) {
    loggingFine = fineLogging;
  }

  @Override
  public void fine(String msg) {
    fine(msg, null);
  }

  @Override
  public void fine(String msg, Throwable e) {
    if (!loggingFine) {
      return;
    }
    log(System.out, "F", msg, e);
  }

  @Override
  public void warning(String msg) {
    warning(msg, null);
  }

  @Override
  public void warning(String msg, Throwable e) {
    log(System.err, "W", msg, e);
  }

  private static void log(PrintStream stream, String level, String msg, Throwable e) {
    stream.print(level);
    stream.print(" ");
    stream.println(msg);

    if (e != null) {
      stream.print(level);
      stream.print(" ");
      e.printStackTrace(stream);
    }
  }
}
