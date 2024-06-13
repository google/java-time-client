#!/bin/bash -e
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

# A "pre-submit" script that invokes various formatting / linting tools and runs
# tests.

SCRIPT_DIR=$(dirname $0)
ROOT_DIR=$(realpath ${SCRIPT_DIR})
ORIG_DIR=$(pwd)
BAZEL_COMMAND="bazel --bazelrc=${ROOT_DIR}/java-time-client.bazelrc"

function echo_command() { echo $*; $*; }
function print_separator() { echo "============================="; }

if [[ ! -f ${ROOT_DIR}/java-time-client.config ]]; then
  echo Copy java-time-client.config_template to create java-time-client.config and edit it.
  exit 1
fi

source ${ROOT_DIR}/java-time-client.config

print_separator
echo Checking for unexpected dupes of common files...
${ROOT_DIR}/check-for-dupes-of-common.sh

print_separator
echo Formatting / linting...

if [[ -z ${JAVA_FORMAT_JAR} ]]; then
  echo JAVA_FORMAT_JAR not set
  exit 1
fi
echo_command ${ROOT_DIR}/reformat-java.sh ${JAVA_FORMAT_JAR}

echo_command ${ROOT_DIR}/regenerate-common-symlinks.sh > /dev/null
echo_command ~/go/bin/buildifier -r --lint=warn ${ROOT_DIR}

print_separator
if [[ -z ${ANDROID_SDK} ]]; then
  echo ANDROID_SDK not set
  exit 1
fi

echo "Running Android tests with robolectric, ANDROID_HOME=${ANDROID_SDK}..."
echo_command cd ${ROOT_DIR}/android
ANDROID_HOME=${ANDROID_SDK} echo_command ${BAZEL_COMMAND} test ${BAZEL_BUILD_OPTIONS} //javatests/com/google/time/client/...

print_separator
cd ${ORIG_DIR}
echo Running Java SE tests with bazel JDK...
cd ${ROOT_DIR}/javase
echo_command ${BAZEL_COMMAND} test ${BAZEL_BUILD_OPTIONS} //javatests/com/google/time/client/...

print_separator
if [[ -z ${JDK8} ]]; then
  echo JDK8 not set
  exit 1
fi
echo "Running Java SE tests with JDK 8 on path (${JDK8})..."
PATH=${JDK8}/bin:${PATH} echo_command ${BAZEL_COMMAND} test ${BAZEL_BUILD_OPTIONS} --java_runtime_version=local_jdk //javatests/com/google/time/client/...

print_separator
cd ${ORIG_DIR}
echo "Running Android tests on Android device (ANDROID_HOME=${ANDROID_SDK} for build and platform tooling)..."
ANDROID_HOME=${ANDROID_SDK} PATH=${ANDROID_SDK}/platform-tools/:${PATH} echo_command ${ROOT_DIR}/android/run_device_tests.sh

print_separator
