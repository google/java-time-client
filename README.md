This is not an officially supported Google product.

# java-time-client

A client library to support time synchronization written in the Java language.

This project is intended for both use with Java SE and Android, including both
the Android platform and Android applications.

Because of the intended use in the Android platform and Android apps that target
old API levels, it is designed to be lightweight on Android and use minimal
dependencies.

To achieve a small binary size it eschews a single library binary that can be
used on both Java SE and Android, and avoids the use of runtime abstractions /
multiple implementations of classes and reflection. Instead, some classes have
two implementations. This is handled explicitly in one git repository rather
than treating one variant as subordinate and creating a fork or branch.

Targeting Java SE is intended primarily to enable fast development interation
and testing, and size is not as much of a consideration.

## Using the project

To build Android targets, you will need the Android SDK installed and
discoverable. e.g. set the ANDROID_HOME environment variable if you haven't
already, e.g.:
```
export ANDROID_HOME=~/Android/Sdk/
```

### Building

This project uses the [bazel](https://bazel.build/) build system.
Once bazel is installed, commands like `bazel build` can be used to build the
project artifacts.

### Testing

Execute automated tests using `blaze` commands like:
`bazel test //javatests/com/google/time/client/base:tests`

Running Android tests:

bazel's android_instrumentation_test rule is not working at the time of writing,
so testing requires that you set up / make available an Android device or
emulator and make it available over adb.

To run the tests com.google.time.client.base tests on a real device use
`android/run_device_tests.sh`.

## Developing the project

### The directory structure

The java-time-client project is two projects in one. There are two bazel
WORKSPACE files, and it is structured to support work on Java SE and Android
simultaneously.

+ Most code in this project can be found under `common/`.
+ `javase/` and `android/` contain the variants of the code. Common code is
  included in the variant directory structures using symlinks, which can be
  regenerated using the `regenerate-common-symlinks.sh` script.

The use of symlinks in this project may make developing on some platforms more
difficult. Sorry.

### Code style / formatting

For Java:
+ Use https://github.com/google/google-java-format
+ e.g. `reformat-java.sh` to reformat files to comply.

For BUILD and related:
+ Use https://github.com/bazelbuild/buildtools
+ e.g. `buildifier -r --lint=warn .`

### Terminology / naming

Words like "clock" and "time" can only be used generally. If you can, be
specific:

+ Time: It's all time! Use "ticks", "instant" and "duration" as appropriate.
  For NTP "timestamp" is used to indicate an NTP timestamp.
+ Clock: We have two types of clock, so use "Ticker" or "Instant Source"
  depending on whether you're talking about a source of Ticks or Instants.

### Developing with IntelliJ IDEA

You don't have to use IntelliJ, but if you do...

+ Set ANDROID_HOME in the environment used to launch IntelliJ.
+ Install the bazel plugin as needed.
+ From "Welcome to IntelliJ", select "Import Bazel Project..."

To create the Android project:
+ Select the `java-time-client/android` directory
+ Select the android/android.bazelproject file

To create the Java SE project:
+ Repeat the steps used for the Android project but for
  `java-time-client/javase` / `javase`.

### Java versions

By default, bazel will use its own java tooling and it doesn't naturally support
Java versions < 11. To build / run with Java 8, set your JAVA_HOME to your local
install of JDK 8 and use flags to specify the JDK:

`bazel --java_runtime_version=local_jdk`

## Navigating the project

### Packages / Artifacts

+ "base" / `com.google.time.client.base` / `com.google.time.client.base.impl`
  Base classes to support higher-level code.  These include stand-ins for
  various classes that are not supported on earlier versions of Android.
  `com.google.time.client.base` contains classes that are intended to be
  considered API classes.
  `com.google.time.client.base.impl` contains code that isn't intended to be
  considered stable but can be used by other parts of java-time-client.
+ "base/testing" / `com.google.time.client.base.testing`: Base testing classes
  to support higher-level code.  This includes code that is only used for
  testing, and test versions of code in "base".
+ "sntp" / `com.google.time.client.sntp` / `com.google.time.client.sntp.impl`
  An SNTP client implementation.
+ "sntp/testing" / `com.google.time.client.sntp.testing`: SNTP testing classes
  to support higher-level code.  This includes code that is only used for
  testing, and test versions of code in "sntp" and "base".

## License

Apache 2.0
