# DriveLoom Android repository instructions

Pre-release Kotlin and Jetpack Compose client for the DriveLoom server (`Aulteran/DriveLoom`). The full app was built on 2026-11-12 and awaits owner review, a first run on a device, and a security review ([docs/SECURITY-NOTES.md](docs/SECURITY-NOTES.md)). Choices made without the owner are in [docs/ASSUMPTIONS.md](docs/ASSUMPTIONS.md).

- The server repo is the API source of truth: `apps/api/openapi/openapi.yaml` and `docs/CONTRACTS.md` on its `dev` branch. Do not invent endpoints or shapes. Decimals are strings; keep them out of floating point.
- Layout: `core/` is pure Kotlin (API client, session, formatting, validation) with JVM unit tests. `app/…/ui/` is Compose in the Loom HUD design ([docs/DESIGN.md](docs/DESIGN.md)) and must stay free of Android framework imports; Android specifics go in `app/…/platform/` behind the `Platform` interface.
- The app talks only to the configured server. Add no analytics, remote fonts, images or other third-party requests.
- Work on scoped, descriptively named branches and open PRs into `dev`, the default branch. `main` is the release branch; never push feature work to it.
- Before opening a PR, run `./gradlew assembleDebug lintDebug testDebugUnitTest :core:test` (JDK 17+, Android SDK 35). Versions live in `gradle/libs.versions.toml`.
- DriveLoom is a working name: the application id is `driveloom.applicationId` in `gradle.properties`, the display name is `app_name` in `strings.xml`.
- Never commit keystores, `local.properties` or `google-services.json`.
