load("@google_bazel_common//testing:test_defs.bzl", "gen_android_local_tests")

gen_android_local_tests(
    name = "tests",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/time/client/base",
        "//java/com/google/time/client/base/testing",
        "@maven//:com_google_truth_truth",
        "@maven//:org_robolectric_robolectric",
        "@robolectric//bazel:android-all",
    ],
)
