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
two implementations. This is handled explicitly in one branch rather than
treating one variant as subordinate / a fork.

Targeting Java SE is intended primarily to enable fast development interation
and testing, and size is not as much of a consideration.

This project uses the [bazel](https://bazel.build/) build system.

## The directory structure

+ Most code in this project can be found under `common/`.
+ `javase/` and `android/` contain the variants of the code. Common code is
  included in the variant directory structures using symlinks, which can be
  regenerated using the `regenerate-common-symlinks.sh` script.

The use of symlinks in this project may make development on some platforms more
difficult. Sorry.

## Code style

For Java:
+ Use https://github.com/google/google-java-format
+ e.g. `reformat-java.sh` to reformat files to comply.

For BUILD and related:
+ Use https://github.com/bazelbuild/buildtools
+ e.g. `buildifier -r --lint=warn .`

## Packages

+ `com.google.time.client.base`: Base classes to support higher-level code.
  These include stand-ins for various classes that are not supported on earlier
  versions of Android.

## license

Apache 2.0
