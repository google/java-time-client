load("@google_bazel_common//testing:test_defs.bzl", "gen_android_local_tests")

# Android: A test_suite / generates robolectric tests.
gen_android_local_tests(
    name = "tests",
    srcs = glob([
        "*.java",
        "impl/*.java",
    ]),
    deps = [
        "//java/com/google/time/client/base",
        "//java/com/google/time/client/base/testing",
        "//java/com/google/time/client/sntp",
        "//java/com/google/time/client/sntp/testing",
        "@maven//:com_google_truth_truth",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_robolectric_robolectric",
        "@robolectric//bazel:android-all",
    ],
)

# TODO Remove duplication with other Android test libs.
# Android: A library of tests for on-device tests.
android_library(
    name = "libtests",
    srcs = glob([
        "*.java",
        "impl/*.java",
    ]),
    deps = [
        "//java/com/google/time/client/base",
        "//java/com/google/time/client/base/testing",
        "//java/com/google/time/client/sntp",
        "//java/com/google/time/client/sntp/testing",
        "@maven//:androidx_test_core",
        "@maven//:androidx_test_runner",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_android",
        "@maven//:org_mockito_mockito_core",
    ],
)

# Android: Instrumentation app for running on-device tests.
android_binary(
    name = "tests.test_app",
    testonly = 1,
    custom_package = "com.google.time.client.sntp.test_app",
    instruments = ":tests.app",
    manifest = "AndroidTestManifest.xml",
    deps = [
        ":libtests",
    ],
)

# TODO Generalize android_binary if possible for use in base too. Tests can carry the code under test too?
# Android: App containing the library for running on-device tests.
android_binary(
    name = "tests.app",
    custom_package = "com.google.time.client.sntp.app",
    manifest = "AndroidManifest.xml",
    deps = ["//java/com/google/time/client/sntp"],
)
