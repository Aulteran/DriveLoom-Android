# DriveLoom for Android

The native Android client for [DriveLoom](https://github.com/Aulteran/DriveLoom), an open-source, self-hosted fuel tracker.

**Status: pre-release, not yet security reviewed.** The app covers the server's user-facing features and builds, lints and passes its unit tests in CI. It has not yet been run on a device against a live server, and the areas listed in [docs/SECURITY-NOTES.md](docs/SECURITY-NOTES.md) are waiting for a security review. DriveLoom is the working name.

## What it does

| Area | In the app |
| --- | --- |
| Connect | Enter your server's address. HTTPS by default; plain HTTP only after an explicit warning and opt-in. The server's API version is checked first. |
| Sign-in | Email and password, authenticator-app or emailed second step, recovery codes, registration (setup token, invitation code or link), email verification, password recovery. |
| Garage | Vehicles you own or share, with photos, roles and archived vehicles. Add a vehicle or import one from a JSON export. |
| Fuel log | Add, correct and delete fill-ups with explicit units, partial fills, missed purchases, costs in another currency, and notes. Economy per full-to-full interval. |
| Statistics | Average economy, distance, fuel, spend, cost per distance and an economy chart over 30 days, 90 days, a year or all time. |
| Sharing | Members and roles, ownership transfer, leaving a vehicle. Friends, requests, and aggregate comparison with per-vehicle opt-in. |
| Photos | One photo per vehicle, picked with the system photo picker, resized and stripped of metadata before upload. |
| Import and export | CSV and JSON export to a location you choose; CSV and JSON import with a preview before anything is saved. |
| Settings | Display units and currency, profile, password, authenticator app, signed-in devices, account deletion, theme (dark, light, system). |
| Administration | For administrators: accounts, invitations and the audit log. |

Not in the app yet, because the server contract does not support them for a native client: passkey sign-in and identity-provider (OIDC) sign-in. See [docs/ASSUMPTIONS.md](docs/ASSUMPTIONS.md).

## Privacy

The app sends requests to exactly one host: the server you configure. There are no analytics, crash reporters, ad or tracking libraries, and no downloaded fonts or images from anywhere else. The only permission is `INTERNET`. The server address and session are encrypted on the device and excluded from backups.

## Server and API contract

The source of truth for the HTTP API is the server repository, not this one:

- [OpenAPI document](https://github.com/Aulteran/DriveLoom/blob/dev/apps/api/openapi/openapi.yaml) (`/v1`)
- [Contracts and compatibility policy](https://github.com/Aulteran/DriveLoom/blob/dev/docs/CONTRACTS.md): decimals are strings, errors are RFC 9457 problems, and `GET /v1/version` reports what a server supports

The app needs a server with API version 0.6 or newer.

## Layout

| Path | Contents |
| --- | --- |
| `core/` | Pure Kotlin, no Android: API models and client, session state machine, server-address handling, formatting, fill-up validation. Unit-tested on the JVM with a mock web server. |
| `app/…/ui/` | The Compose interface in the Loom HUD design ([docs/DESIGN.md](docs/DESIGN.md)). Free of Android framework types; everything platform-specific comes through the `Platform` interface. |
| `app/…/platform/` | Android specifics: Keystore-encrypted storage, system pickers, image handling. |
| `docs/` | [Design](docs/DESIGN.md), [assumptions awaiting owner approval](docs/ASSUMPTIONS.md), [security notes](docs/SECURITY-NOTES.md). |

## Building

Requirements: JDK 17 or newer and the Android SDK (platform 35). Android Studio sets both up.

```sh
./gradlew assembleDebug                    # build a debug APK
./gradlew testDebugUnitTest :core:test     # unit tests
./gradlew lintDebug                        # Android lint
```

`minSdk` 26, `compileSdk` and `targetSdk` 35. Dependency versions are in `gradle/libs.versions.toml`. Third-party code is limited to AndroidX/Compose, OkHttp, kotlinx.serialization and kotlinx.coroutines.

**Renaming.** DriveLoom is a working name. The application id is one line in `gradle.properties` (`driveloom.applicationId`) and the displayed name one string in `app/src/main/res/values/strings.xml`. Source packages do not need to change.

## Branches

Work happens on `dev`, the default branch: branch from `dev` and open pull requests into `dev`. `main` is the release branch. Never commit feature work directly to `main`. CI builds, lints and runs unit tests on every pull request and on pushes to `dev`.

## License

[AGPL-3.0-only](LICENSE), the same as the server. The bundled fonts, Space Grotesk and JetBrains Mono, are under the SIL Open Font License 1.1 ([licenses/](licenses/)).
