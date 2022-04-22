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
`bazel test android/javatests/com/google/time/client/base:tests`

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


## Navigating the project

### Packages / Artifacts

+ "base" / `com.google.time.client.base`: Base classes to support higher-level
  code.  These include stand-ins for various classes that are not supported on
  earlier versions of Android.

## License

Apache 2.0
