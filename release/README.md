# Releasing java-time-client

This directory contains the tooling used to publish java-time-client to
[Maven Central](https://central.sonatype.com/namespace/com.google.time.client)
under the `com.google.time.client` namespace.

Artifact                                          | Platform
:------------------------------------------------ | :-------
`com.google.time.client:java-time-client`         | Java SE
`com.google.time.client:java-time-client-android` | Android

Publishing goes through Google's OSS Exit Gate (go/oss-exit-gate), which holds
the Maven Central credentials and does the actual upload. Nobody publishes to
Maven Central directly.

## How it works

```
git tag vX.Y.Z  ──►  .github/workflows/release.yml
                       ├─ build-and-stage
                       │    release/build_maven_artifacts.sh  (jar, sources, javadoc, POM)
                       │    sign with OpenPGP key, upload to the Exit Gate Artifact Registry
                       └─ trigger-publish
                            upload manifest ──► OSS Exit Gate ──► Maven Central
```

*   `build_maven_artifacts.sh` compiles the Java SE and Android variants for
    Java 8 bytecode, with reproducible jar timestamps. It excludes the
    `*.testing` packages and the command-line tools. The published POMs declare
    no dependencies: the only compile-time libraries are annotations, which
    aren't needed at runtime.
*   `pom/*.pom.in` are the POM templates. `@VERSION@` is substituted at build
    time.
*   The Exit Gate project config is at
    `google3/configs/security/opensource/exit_gate/prod/projects/java-time-client/mavencentral/`.
    It authorizes the workflow
    `google/java-time-client/.github/workflows/release.yml` when it runs for a
    tag (`refs/tags/*`).

## One-time setup

These steps have to be done once per repository. Only repository admins can do
them.

1.  **Signing key.** Maven Central requires every file to be signed. Generate a
    dedicated key for the team (not a personal key):

    ```shell
    gpg --quick-gen-key "java-time-client release <java-time-client-release@google.com>" ed25519 sign 5y
    gpg --armor --export <KEY_ID> > public.asc
    gpg --keyserver keys.openpgp.org --send-keys <KEY_ID>
    gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
    ```

    Keep a backup of the private key and passphrase in Valentine
    (go/valentine), shared with the team's admins.

2.  **GitHub secrets.** In *Settings → Secrets and variables → Actions*, add:

    *   `MAVEN_GPG_PRIVATE_KEY`: the output of
        `gpg --armor --export-secret-keys <KEY_ID>`
    *   `MAVEN_GPG_PASSPHRASE`: the key's passphrase

3.  **GitHub environment (recommended).** In *Settings → Environments*, create
    `maven-central`. Add required reviewers so that every release needs a
    second person's approval, and restrict deployments to tags matching `v*`.

4.  **Tag protection (required).** Because the Exit Gate trusts any tag in this
    repository, restrict who can create tags. In *Settings → Rules → Rulesets →
    New tag ruleset*:

    *   Enforcement: **Active**
    *   Target tags: **Include by pattern** `v*`
    *   Rules: **Restrict creations**, **Restrict updates**, **Restrict
        deletions**
    *   Bypass list: repository admins (or the release team) only

5.  **Exit Gate credentials.** The Maven Central account
    (`java-time-client-release`) password and publishing token are stored in
    Secret Manager by the Exit Gate. See
    `configs/security/opensource/exit_gate/prod/projects/java-time-client/mavencentral/README.md`.

## Cutting a release

1.  Make sure google3 is green and the change you want to release has been
    exported to GitHub.
2.  Pick the version using [Semantic Versioning](https://semver.org/). Maven
    Central versions are permanent and can never be changed or deleted.
3.  Create and push the tag from the exported commit on GitHub:

    ```shell
    git fetch origin
    git tag -s vX.Y.Z -m "java-time-client X.Y.Z" origin/main
    git push origin vX.Y.Z
    ```

4.  Approve the `maven-central` deployment in the GitHub Actions run, if
    configured.
5.  The Exit Gate emails `android-system-time-team@google.com` when publishing
    starts and when it finishes. Artifacts usually appear on Maven Central
    within 30 minutes. Search can take a few hours to update.
6.  Create the GitHub release for the tag, with release notes.
7.  Update the version in the top-level `README.md`, and update the `Maven`
    identifier in google3's `METADATA`.

## Testing the build locally

```shell
ANDROID_JAR=$ANDROID_HOME/platforms/android-35/android.jar \
  release/build_maven_artifacts.sh --version 0.0.0-test --out /tmp/jtc-maven
```

## Troubleshooting

*   **Workflow fails at "Authenticate to the OSS Exit Gate":** the workflow
    file name, repository or ref doesn't match the `builders` entry in the
    Exit Gate project config.
*   **Exit Gate reports that the version already exists:** Maven Central
    versions are immutable. Release a new patch version instead.
*   **A failed release left artifacts in Artifact Registry:** they're retried
    or overwritten by the next run for the same version. See
    go/oss-exit-gate-faq#delete-ar-artifacts to delete them.
*   **Help:** go/oss-exit-gate, or file a bug in the Exit Gate component listed
    there.
