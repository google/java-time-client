java_library(
    name = "tools",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//java/com/google/time/client/base",
        "//java/com/google/time/client/sntp",
    ],
)

java_binary(
    name = "sntp",
    args = ["time.google.com"],
    main_class = "com.google.time.client.sntp.tools.SntpTool",
    runtime_deps = [":tools"],
)

java_binary(
    name = "sntp_comp",
    args = [
        "time.google.com",
        "time.android.com",
        "pool.ntp.org",
        "uk.pool.ntp.org",
        "cn.pool.ntp.org",
        # From https://timetoolsltd.com/information/public-ntp-server/uk/
        "ntp.my-inbox.co.uk",
        "ntp1.npl.co.uk",
        "ntp2.npl.co.uk",
        "ntp1.ja.net",
        "ntp2.ja.net",
        "ntp.virginmedia.com",
        "ntp2d.mcc.ac.uk",
        "ntp2c.mcc.ac.uk",
        "ntp.exnet.com",
        "ntp0.csx.cam.ac.uk",
        "ntp1.csx.cam.ac.uk",
        "ntp2.csx.cam.ac.uk",
        "ntp.cis.strath.ac.uk",
        "ntppub.le.ac.uk",
        # From https://gist.github.com/mutin-sa/eea1c396b1e610a2da1e5550d94b0453
        "time.cloudflare.com",
        "time.facebook.com",
        "time.windows.com",
        "time.apple.com",
        "time.nist.gov",
    ],
    main_class = "com.google.time.client.sntp.tools.SntpComparisonTool",
    runtime_deps = [":tools"],
)
