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

function report_if_exists() {
  F1=$1

  if [[ -f ${F1} && ! -L ${F1} ]]; then
    echo ${F1} exists and is also in common/.
    return 1
  fi
  return 0
}

PROJECT_ROOT=`dirname $0`

COMMON_FILES_INFO=$(find ${PROJECT_ROOT}/common -type f -printf "%d|%f|%h|%H|%p|%P\n")
DUPES_EXIST=0
for FILE_INFO in ${COMMON_FILES_INFO}; do
  IFS="|" read -a FILE_INFO_ARR <<< ${FILE_INFO}
  RELATIVE_FILE=${FILE_INFO_ARR[5]}

  report_if_exists ${PROJECT_ROOT}/android/${RELATIVE_FILE}
  if (( $? != 0 )); then
    DUPES_EXIST=1
  fi

  report_if_exists ${PROJECT_ROOT}/javase/${RELATIVE_FILE}
  if (( $? != 0 )); then
    DUPES_EXIST=1
  fi
done

exit ${DUPES_EXIST}
