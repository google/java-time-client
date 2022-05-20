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

function delete_if_same_as() {
  F1=$1
  F2=$2

  if [[ ! -a ${F1} ]]; then return; fi

  diff ${F1} ${F2} > /dev/null
  if (( $? == 0 )); then
    echo Deleting ${F1}
    rm ${F1}
  else
    echo ${F1} differs from ${F2} Skipping
  fi
}

PROJECT_ROOT=`dirname $0`

COMMON_FILES_INFO=$(find ${PROJECT_ROOT}/common -type f -printf "%d|%f|%h|%H|%p|%P\n")
for FILE_INFO in ${COMMON_FILES_INFO}; do
  IFS="|" read -a FILE_INFO_ARR <<< ${FILE_INFO}
  RELATIVE_FILE=${FILE_INFO_ARR[5]}
  delete_if_same_as ${PROJECT_ROOT}/android/${RELATIVE_FILE} ${PROJECT_ROOT}/common/${RELATIVE_FILE}
  delete_if_same_as ${PROJECT_ROOT}/javase/${RELATIVE_FILE} ${PROJECT_ROOT}/common/${RELATIVE_FILE}
done
