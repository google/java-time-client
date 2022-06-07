load("@google_bazel_common//testing:test_defs.bzl", "gen_java_tests")

gen_java_tests(
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
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)
