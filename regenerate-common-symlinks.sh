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

# Deletes all symlinks to under android/ and javase/ and generates symlinks from
# those directories to files under common/. This is the main script developers
# should run to keep the symlinks under android/ and javase/ directories
# correct.

PROJECT_ROOT=`dirname $0`

find ${PROJECT_ROOT}/android -type l | xargs rm
find ${PROJECT_ROOT}/javase -type l | xargs rm

COMMON_FILES_INFO=$(find ${PROJECT_ROOT}/common -type f -printf "%d|%f|%h|%H|%p|%P\n")
for FILE_INFO in ${COMMON_FILES_INFO}; do
  IFS="|" read -a FILE_INFO_ARR <<< ${FILE_INFO}
  DEPTH=${FILE_INFO_ARR[0]}
  PARENT_PATH=$(printf '../%.0s' $(eval echo {1..${DEPTH}}))
  RELATIVE_FILE=${FILE_INFO_ARR[5]}
  echo Adding symlinks for ${RELATIVE_FILE}
  ln -s ${PARENT_PATH}common/${RELATIVE_FILE} ${PROJECT_ROOT}/android/${RELATIVE_FILE}
  ln -s ${PARENT_PATH}common/${RELATIVE_FILE} ${PROJECT_ROOT}/javase/${RELATIVE_FILE}
done
