#!/bin/bash
# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


ANDROID_DIR=`dirname $0`

cd ${ANDROID_DIR}

RULES=(
  "//javatests/com/google/time/client/base:tests.app.apk"
  "//javatests/com/google/time/client/base:tests.test_app.apk"
  "//javatests/com/google/time/client/base/testing:tests.app.apk"
  "//javatests/com/google/time/client/base/testing:tests.test_app.apk"
)
APKS=(
  "javatests/com/google/time/client/base/tests.app.apk"
  "javatests/com/google/time/client/base/tests.test_app.apk"
  "javatests/com/google/time/client/base/testing/tests.app.apk"
  "javatests/com/google/time/client/base/testing/tests.test_app.apk"
)
PACKAGES=(
  "com.google.time.client.base"
  "com.google.time.client.base.test"
  "com.google.time.client.base.testing"
  "com.google.time.client.base.testing.test"
)
TEST_PACKAGES=(
  "com.google.time.client.base.test"
  "com.google.time.client.base.testing.test"
)

# TODO There is a lot of scope to remove duplication above and below.
set -e

function echo_command() { echo $*; $*; }

echo_command bazel build ${RULES[@]}

for APK in ${APKS[@]}; do
  echo_command adb install bazel-bin/${APK}
done

for TEST_PACKAGE in ${TEST_PACKAGES[@]}; do
  echo_command adb shell am instrument -w ${TEST_PACKAGE}/androidx.test.runner.AndroidJUnitRunner
done

for PACKAGE in ${PACKAGES[@]}; do
  echo_command adb uninstall ${PACKAGE}
done

