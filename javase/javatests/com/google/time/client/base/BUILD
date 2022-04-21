load("@google_bazel_common//testing:test_defs.bzl", "gen_java_tests")

gen_java_tests(
    name = "tests",
    srcs = glob(["*.java"]),
    deps = [
        "//javase/java/com/google/time/client/base",
        "//javase/java/com/google/time/client/base/testing",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)
