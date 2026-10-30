#!/usr/bin/env bash
# Copyright 2026 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Builds the Maven Central artifacts for java-time-client.
#
# For each variant (Java SE and Android) this produces, in a Maven repository
# layout under the output directory:
#
#   com/google/time/client/<artifactId>/<version>/<artifactId>-<version>.jar
#   com/google/time/client/<artifactId>/<version>/<artifactId>-<version>-sources.jar
#   com/google/time/client/<artifactId>/<version>/<artifactId>-<version>-javadoc.jar
#   com/google/time/client/<artifactId>/<version>/<artifactId>-<version>.pom
#
# Artifacts are NOT signed or uploaded here; see .github/workflows/release.yml.
#
# Only the public library packages are published. The *.testing packages
# (which depend on JUnit) and the command-line tools are excluded.
#
# Usage:
#   release/build_maven_artifacts.sh --version 1.2.3 --out /tmp/maven-out \
#       [--variant javase|android|all]
#
# Environment:
#   ANDROID_JAR        Path to an Android platform android.jar. Required for
#                      the android variant.
#   SOURCE_DATE_EPOCH  Optional. Seconds since epoch used for reproducible jar
#                      entry timestamps. Defaults to the last git commit time,
#                      or "now" if not in a git checkout.
#   DEPS_CACHE_DIR     Optional. Where compile-only dependencies are cached.

set -euo pipefail

readonly GROUP_PATH="com/google/time/client"
readonly MODULE_NAME="com.google.time.client"
readonly PUBLIC_PACKAGES=("com.google.time.client.base" "com.google.time.client.sntp")

# Compile-only dependencies (annotations with CLASS/SOURCE retention). They are
# not needed at runtime and are not declared in the published POMs.
readonly ERRORPRONE_URL="https://repo1.maven.org/maven2/com/google/errorprone/error_prone_annotations/2.51.0/error_prone_annotations-2.51.0.jar"
readonly ERRORPRONE_SHA256="51a6d8348c7e307e47183859c07597dc8a2899c5d1b6f18a471bccaa3203d5cf"
readonly ANDROIDX_ANNOTATION_URL="https://dl.google.com/android/maven2/androidx/annotation/annotation-jvm/1.11.0/annotation-jvm-1.11.0.jar"
readonly ANDROIDX_ANNOTATION_SHA256="a83b827cc17fd6b41c7bf5e7a2a2b40a0895b00b3e30fdfdb820b49dc7b667f3"

readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

die() {
  echo "ERROR: $*" >&2
  exit 1
}

version=""
out_dir=""
variant="all"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --version) version="$2"; shift 2 ;;
    --out) out_dir="$2"; shift 2 ;;
    --variant) variant="$2"; shift 2 ;;
    -h|--help) sed -n '16,42p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) die "Unknown argument: $1" ;;
  esac
done

[[ -n "${version}" ]] || die "--version is required"
[[ -n "${out_dir}" ]] || die "--out is required"
# Maven Central versions are immutable: only allow plain SemVer releases and
# pre-releases (e.g. 1.0.0, 1.1.0-rc1). Never publish -SNAPSHOT here.
[[ "${version}" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.]+)?$ ]] \
  || die "Version '${version}' is not a valid release version (X.Y.Z[-qualifier])"
[[ "${version}" != *SNAPSHOT* ]] || die "SNAPSHOT versions cannot be released"
case "${variant}" in javase|android|all) ;; *) die "Invalid --variant: ${variant}" ;; esac

if [[ -z "${SOURCE_DATE_EPOCH:-}" ]]; then
  SOURCE_DATE_EPOCH="$(git -C "${ROOT_DIR}" log -1 --format=%ct 2>/dev/null || date +%s)"
fi
# `jar --date` expects ISO-8601 and only accepts timestamps after 1980.
jar_date="$(date -u -d "@${SOURCE_DATE_EPOCH}" +%Y-%m-%dT%H:%M:%SZ)"

for tool in javac jar javadoc curl sha256sum; do
  command -v "${tool}" >/dev/null || die "Required tool not found: ${tool}"
done

mkdir -p "${out_dir}"
out_dir="$(cd "${out_dir}" && pwd)"
work_dir="$(mktemp -d)"
trap 'rm -rf "${work_dir}"' EXIT

deps_dir="${DEPS_CACHE_DIR:-${work_dir}/deps}"
mkdir -p "${deps_dir}"

# Downloads $1 to $deps_dir and verifies its SHA-256 ($2). Prints the path.
fetch_dep() {
  local url="$1" sha="$2"
  local dest="${deps_dir}/$(basename "${url}")"
  if [[ ! -f "${dest}" ]] || ! echo "${sha}  ${dest}" | sha256sum --check --status; then
    curl --fail --silent --show-error --location --retry 3 -o "${dest}.tmp" "${url}"
    echo "${sha}  ${dest}.tmp" | sha256sum --check --status \
      || die "Checksum mismatch for ${url}"
    mv "${dest}.tmp" "${dest}"
  fi
  echo "${dest}"
}

# Prints the publishable .java files of a variant source root, one per line.
# Symlinks (used to share code from common/) are followed.
list_sources() {
  local src_root="$1"
  (cd "${src_root}" && find -L com -name '*.java' \
      -not -path '*/testing/*' \
      -not -path '*/tools/*' \
      | LC_ALL=C sort)
}

# Builds one variant.
#   $1 artifactId, $2 source root, $3 POM template, $4 compile classpath.
# Reads the global arrays JAVAC_FLAGS and JAVADOC_FLAGS for platform flags.
build_variant() {
  local artifact_id="$1" src_root="$2" pom_template="$3" classpath="$4"

  echo ">>> Building ${artifact_id}:${version}"
  local vdir="${work_dir}/${artifact_id}"
  local staged_src="${vdir}/src"
  local classes="${vdir}/classes"
  local docs="${vdir}/javadoc"
  local meta="${vdir}/meta"
  mkdir -p "${staged_src}" "${classes}" "${docs}" "${meta}/META-INF"

  # Stage a real (non-symlinked) copy of the publishable sources.
  local -a sources=()
  while IFS= read -r rel; do
    mkdir -p "${staged_src}/$(dirname "${rel}")"
    cp -L "${src_root}/${rel}" "${staged_src}/${rel}"
    sources+=("${staged_src}/${rel}")
  done < <(list_sources "${src_root}")
  [[ ${#sources[@]} -gt 0 ]] || die "No sources found in ${src_root}"

  javac -encoding UTF-8 -Xlint:-options -nowarn -g \
      -cp "${classpath}" -d "${classes}" "${JAVAC_FLAGS[@]}" "${sources[@]}"

  javadoc -quiet -encoding UTF-8 -Xdoclint:none -notimestamp \
      -cp "${classpath}" -sourcepath "${staged_src}" -d "${docs}" \
      -windowtitle "${artifact_id} ${version}" -doctitle "${artifact_id} ${version}" \
      "${JAVADOC_FLAGS[@]}" "${PUBLIC_PACKAGES[@]}" >/dev/null

  cp "${ROOT_DIR}/LICENSE" "${meta}/META-INF/LICENSE"
  cat > "${vdir}/MANIFEST.MF" <<EOF
Automatic-Module-Name: ${MODULE_NAME}
Implementation-Title: ${artifact_id}
Implementation-Version: ${version}
Implementation-Vendor: Google LLC
EOF

  local dest="${out_dir}/${GROUP_PATH}/${artifact_id}/${version}"
  local base="${dest}/${artifact_id}-${version}"
  mkdir -p "${dest}"
  rm -f "${base}"*

  jar --create --date="${jar_date}" --file "${base}.jar" --manifest "${vdir}/MANIFEST.MF" \
      -C "${classes}" . -C "${meta}" META-INF/LICENSE
  jar --create --date="${jar_date}" --file "${base}-sources.jar" --no-manifest \
      -C "${staged_src}" . -C "${meta}" META-INF/LICENSE
  jar --create --date="${jar_date}" --file "${base}-javadoc.jar" --no-manifest \
      -C "${docs}" . -C "${meta}" META-INF/LICENSE
  sed "s/@VERSION@/${version}/g" "${pom_template}" > "${base}.pom"

  echo "    ${#sources[@]} source files -> ${dest}"
}

errorprone_jar="$(fetch_dep "${ERRORPRONE_URL}" "${ERRORPRONE_SHA256}")"

if [[ "${variant}" == "javase" || "${variant}" == "all" ]]; then
  JAVAC_FLAGS=(--release 8)
  JAVADOC_FLAGS=(--release 8)
  build_variant "java-time-client" "${ROOT_DIR}/javase/java" \
      "${ROOT_DIR}/release/pom/java-time-client.pom.in" \
      "${errorprone_jar}"
fi

if [[ "${variant}" == "android" || "${variant}" == "all" ]]; then
  [[ -n "${ANDROID_JAR:-}" && -f "${ANDROID_JAR}" ]] \
    || die "ANDROID_JAR must point to an Android platform android.jar"
  androidx_jar="$(fetch_dep "${ANDROIDX_ANNOTATION_URL}" "${ANDROIDX_ANNOTATION_SHA256}")"
  # --release cannot be combined with a custom boot class path, so compile
  # against android.jar with explicit source/target levels instead.
  JAVAC_FLAGS=(-source 8 -target 8 -bootclasspath "${ANDROID_JAR}")
  JAVADOC_FLAGS=(-source 8 -bootclasspath "${ANDROID_JAR}")
  build_variant "java-time-client-android" "${ROOT_DIR}/android/java" \
      "${ROOT_DIR}/release/pom/java-time-client-android.pom.in" \
      "${errorprone_jar}:${androidx_jar}"
fi

echo ">>> Done. Artifacts are in ${out_dir}"
