# Use the Android SDK pointed to by ${ANDROID_HOME}
android_sdk_repository(name = "androidsdk")

# Required for http_archive
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# BEGIN Required for android_local_test
http_archive(
    name = "robolectric",
    sha256 = "97f169d39f19412bdd07fd6c274dcda0a7c8f623f7f00aa5a3b94994fc6f0ec4",
    strip_prefix = "robolectric-bazel-4.7.3",
    urls = ["https://github.com/robolectric/robolectric-bazel/archive/4.7.3.tar.gz"],
)

load("@robolectric//bazel:robolectric.bzl", "robolectric_repositories")

robolectric_repositories()

http_archive(
    name = "rules_jvm_external",
    sha256 = "cd1a77b7b02e8e008439ca76fd34f5b07aecb8c752961f9640dea15e9e5ba1ca",
    strip_prefix = "rules_jvm_external-4.2",
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/4.2.zip",
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        # Robolectric
        "org.robolectric:robolectric:4.7.3",
        # Truth
        "com.google.truth:truth:1.1.3",
        # Android test suppport
        "junit:junit:4.13.2",
        #"javax.inject:javax.inject:1",
        #"org.hamcrest:java-hamcrest:2.0.0.0"
        #"androidx.test.espresso:espresso-core:3.1.1",
        "androidx.test:core:aar:1.4.0",
        "androidx.test:rules:aar:1.4.0",
        "androidx.test:runner:aar:1.4.0",
    ],
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
)
# END Required for android_local_test

# BEGIN bazel-common imports
http_archive(
    name = "google_bazel_common",
    sha256 = "d8aa0ef609248c2a494d5dbdd4c89ef2a527a97c5a87687e5a218eb0b77ff640",
    strip_prefix = "bazel-common-4a8d451e57fb7e1efecbf9495587a10684a19eb2",
    urls = ["https://github.com/google/bazel-common/archive/4a8d451e57fb7e1efecbf9495587a10684a19eb2.zip"],
)
# END bazel-common imports

# BEGIN Android Test Support
ATS_COMMIT = "93d9e7a84dba5a5cf3f614495e8d94525c561f60"

http_archive(
    name = "android_test_support",
    sha256 = "647038955059a6ac9484f621296dab69d008b27a99cf8b96ea3779a8b5d47be9",
    strip_prefix = "android-test-%s" % ATS_COMMIT,
    urls = ["https://github.com/android/android-test/archive/%s.tar.gz" % ATS_COMMIT],
)

load("@android_test_support//:repo.bzl", "android_test_repositories")

android_test_repositories()
# END Android Test Support
