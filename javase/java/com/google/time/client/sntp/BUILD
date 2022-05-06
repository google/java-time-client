load("@google_bazel_common//tools/javadoc:javadoc.bzl", "javadoc_library")

java_library(
    name = "sntp",
    srcs = glob([
        "*.java",
        "impl/*.java",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//java/com/google/time/client/base",
    ],
)

javadoc_library(
    name = "sntp.javadocs",
    srcs = glob(["*.java"]),
    deps = [":sntp"],
)
