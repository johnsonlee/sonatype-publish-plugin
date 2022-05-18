## Introduction

Due to Sonatype's strict validation rules, the publishing requirement must be satisfied by every artifact which wants to be published to Sonatype.

For Java and Android library projects, the publishing configurations are very similar, but the configurations of creating publication are quite different, this gradle plugin is used to simplify the engineering complexity of publishing artifacts to [Sonatype](https://oss.sonatype.org/), developers don't need to write boilerplate publishing DSL for each project to satisfy Sonatype validation rules.

This plugin not only support publishing artifacts to [Sonatype](https://oss.sonatype.org/), but also support publishing artifacts to private Nexus repository.

## Prerequisite

* [Sonatype](https://oss.sonatype.org/) Account
* [GPG](https://gnupg.org/) key

For more information, see [References](#references)

## Getting Started

```kotlin
plugins {
    kotlin("jvm")
    id("io.johnsonlee.sonatype-publish-plugin") version "1.6.1"
}

group = "<your-group-id>"
version = "1.0.0"
```

Then, execute publish tasks:

```bash
./gradlew publishToMavenLocal
```

## Configuring Environment Variables

To publish artifacts to remote maven repository, additional configurations are quired.

### Sonatype

* `OSSRH_USERNAME`

    The account id of [Sonatype](https://oss.sonatype.org/), searching from project properties by default, otherwise searching from system env

* `OSSRH_PASSWORD`

    the account password of [Sonatype](https://oss.sonatype.org/), searching from project properties by default, otherwise searching from system env

* `OSSRH_PACKAGE_GROUP`

    The package group of [Sonatype](https://oss.sonatype.org/), e.g. `io.johnsonlee`, searching from project properties by default, otherwise searching from system env

### Nexus

* `NEXUS_URL`

    The endpoint of Nexus service, e.g. http://nexus.johnsonlee.io/, searching from project properties by default, otherwise searching from system env

* `NEXUS_USERNAME`

    The account id of Nexus, searching from project properties by default, otherwise searching from system env

* `NEXUS_PASSWORD`

    The account password of Nexus, searching from project properties by default, otherwise searching from system env

## Configuring Signing Properties

* `signing.keyId`

    The GPG key id (short format). In this example, the GPG key id is `71567BD2`

    ```
    $ gpg --list-secret-keys --keyid-format=short
    /Users/johnsonlee/.gnupg/secring.gpg
    ------------------------------------
    sec   4096R/71567BD2 2021-03-10 [expires: 2031-03-10]
    uid                  Johnson
    ssb   4096R/4BA89E7A 2021-03-10
    ```

* `signing.password`

    The password of GPG key

* `signing.secretKeyRingFile`

    The secret key ring file, e.g. */Users/johnsonlee/.gnupg/secring.gpg*

    > The best practice is putting the properties above into `~/.gradle/gradle.properties` 
    >
    > ```properties
    > OSSRH_USERNAME=johnsonlee
    > OSSRH_PASSWORD=*********
    > OSSRH_PACKAGE_GROUP=io.johnsonlee
    > signing.keyId=71567BD2
    > signing.password=*********
    > signing.secretKeyRingFile=/Users/johnsonlee/.gnupg/secring.gpg
    > ```

## Configuring Git Repository

The following git configurations are be used for generating maven POM file, please skip if already done.

* `user.name`

    ```bash
    git config user.name <username>
    ```

* `user.email`

    ```bash
    git config user.email <email-address>
    ```

* `remote.origin.url` (optional)

    The `remote.origin.url`  is available by default unless the git repository is created locally

    ```bash
    git remote add origin git@github.com:<username>/<repository>
    ```

### Configure Project Info

* `project.group`

    The `groupId` of the publication, only the root project need to configured, subproejcts will inherit from the root project

* `project.version`

    The `version` of the publication, only the root project need to configured, subproejcts will inherit from the root project

The `artifactId` of the publication is the `project.name` by default

### Configure License (optional)

Add a license file (`LICENSE`, `LICENSE.txt`, `LICENSE.md` or `LICENSE.rst`) into project, then the license type will be recognized automatically.

For more information on repository licenses, see "[Supported Licenses](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository#searching-github-by-license-type)"

### Publish Artifacts to Sonatype

```bash
./gradlew initializeSonatypeStagingRepository publishToSonatype closeAndReleaseRepository
```

### Publish Artifacts to Nexus

#### Java/Kotlin Project

```bash
./gradlew clean publish
```

#### Android Project

For Android projects,  using `-x` to disable publication tasks for *debug* variants:

```bash
./gradlew clean publish -x publishDebug
```

After release complete, the artifacts will be synced to [Maven Central](https://mvnrepository.com/repos/central) automatically

## References

- [OSSRH Requirements](https://central.sonatype.org/publish/requirements/)
- [OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Generating A New GPG Key](https://docs.github.com/en/authentication/managing-commit-signature-verification/generating-a-new-gpg-key)

