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

# Creates copies of files from common rather than symlinks.
# Be careful! This is useful for working with tooling that doesn't like
# symlinks such as coverage tools.
# Use rm-copies-of-common.sh to remove (exact) copies again and
# regenerate-common-symlinks.sh to regenerate the symlinks.

PROJECT_ROOT=`dirname $0`

find ${PROJECT_ROOT}/android -type l | xargs rm
find ${PROJECT_ROOT}/javase -type l | xargs rm

COMMON_FILES_INFO=$(find ${PROJECT_ROOT}/common -type f -printf "%d|%f|%h|%H|%p|%P\n")
for FILE_INFO in ${COMMON_FILES_INFO}; do
  IFS="|" read -a FILE_INFO_ARR <<< ${FILE_INFO}
  RELATIVE_FILE=${FILE_INFO_ARR[5]}
  echo Copying ${RELATIVE_FILE}
  cp ${PROJECT_ROOT}/common/${RELATIVE_FILE} ${PROJECT_ROOT}/android/${RELATIVE_FILE}
  cp ${PROJECT_ROOT}/common/${RELATIVE_FILE} ${PROJECT_ROOT}/javase/${RELATIVE_FILE}
done
