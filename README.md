# DriveLoom for Android

The native Android client for [DriveLoom](https://github.com/Aulteran/DriveLoom), an open-source, self-hosted fuel tracker.

**Status: pre-release.** This repository is a buildable Kotlin and Jetpack Compose skeleton with a placeholder screen. No app features exist yet, and authentication has not been started: the server's security review is still pending. DriveLoom is the working name.

## Server and API contract

The client talks to a self-hosted DriveLoom server. The source of truth for the HTTP API is in the server repository, not here:

- [OpenAPI document](https://github.com/Aulteran/DriveLoom/blob/dev/apps/api/openapi/openapi.yaml) (`/v1`)
- [Contracts and compatibility policy](https://github.com/Aulteran/DriveLoom/blob/dev/docs/CONTRACTS.md): decimals are strings, errors are RFC 9457 problems, and `GET /v1/version` reports what a server supports
- [Product scope](https://github.com/Aulteran/DriveLoom/blob/dev/docs/PRODUCT.md) and [milestones](https://github.com/Aulteran/DriveLoom/blob/dev/docs/ROADMAP.md)

## Building

Requirements: JDK 17 or newer and the Android SDK (platform 35). Android Studio sets both up.

```sh
./gradlew assembleDebug          # build a debug APK
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew lintDebug              # Android lint
```

Project settings: `minSdk` 26, `compileSdk` and `targetSdk` 35, single `app` module, dependency versions in `gradle/libs.versions.toml`. The application id `io.github.aulteran.driveloom` is provisional.

## Branches

Work happens on `dev`, the default branch: branch from `dev` and open pull requests into `dev`. `main` is the release branch. Never commit feature work directly to `main`. CI builds, lints and runs unit tests on every pull request and on pushes to `dev`.

## License

[AGPL-3.0-only](LICENSE), the same as the server.
