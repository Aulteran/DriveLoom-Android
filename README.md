# DriveLoom for Android

The native Android client for [DriveLoom](https://github.com/Aulteran/DriveLoom), an open-source, self-hosted fuel tracker. It talks only to the server you configure and has no analytics or third-party requests.

## Build

JDK 17+ and the Android SDK (platform 35):

```sh
./gradlew assembleDebug lintDebug testDebugUnitTest :core:test
```

The API contract is the server's `apps/api/openapi/openapi.yaml` on its `dev` branch.

## License

[AGPL-3.0-only](LICENSE). The bundled fonts, Space Grotesk and JetBrains Mono, are under the SIL Open Font License 1.1 ([licenses/](licenses/)).
