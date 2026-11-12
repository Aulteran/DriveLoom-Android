# DriveLoom Android repository instructions

Pre-release Kotlin and Jetpack Compose client for the DriveLoom server (`Aulteran/DriveLoom`). Currently a skeleton: no features, and no authentication work until the server's security review is done.

- The server repo is the API source of truth: `apps/api/openapi/openapi.yaml` and `docs/CONTRACTS.md` on its `dev` branch. Do not invent endpoints or shapes. Decimals are strings; keep them out of floating point.
- Work on scoped, descriptively named branches and open PRs into `dev`, the default branch. `main` is the release branch; never push feature work to it.
- Before opening a PR, run `./gradlew assembleDebug lintDebug testDebugUnitTest` (JDK 17+, Android SDK 35). Versions live in `gradle/libs.versions.toml`.
- Never commit keystores, `local.properties` or `google-services.json`.
