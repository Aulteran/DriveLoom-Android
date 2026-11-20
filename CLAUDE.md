# DriveLoom Android repository instructions

Kotlin and Jetpack Compose client for the DriveLoom server (`Aulteran/DriveLoom`).

## Source of truth

Project status, design, aims and aspirations live in the owner's Notion AI HQ, not in this repo. Check there before planning work.

- Android page: https://app.notion.com/p/41eab8277cf781a5b3b5c67438fc9c05
- DriveLoom project page: https://app.notion.com/p/41eab8277cf78132a882fa77424db1a8
- Handoff doc: https://app.notion.com/p/41eab8277cf78184baa6ffb429e3e36a

Do not add status, roadmap, design or planning documents to this repo. Keep `LICENSE` and `licenses/`.

## Rules

- The server repo is the API source of truth: `apps/api/openapi/openapi.yaml` on its `dev` branch. Do not invent endpoints or shapes. Decimals are strings; keep them out of floating point.
- Layout: `core/` is pure Kotlin (API client, session, formatting, validation) with JVM unit tests. `app/…/ui/` is Compose in the Loom HUD design and must stay free of Android framework imports; Android specifics go in `app/…/platform/` behind the `Platform` interface.
- The app talks only to the configured server. Add no analytics, remote fonts, images or other third-party requests.
- `dev` is the default branch; `main` is the release branch. Always start on a new, descriptively named branch, open PRs into `dev`, and never push feature work to `main`.
- Before opening a PR, run `./gradlew assembleDebug lintDebug testDebugUnitTest :core:test` (JDK 17+, Android SDK 35). Versions live in `gradle/libs.versions.toml`.
- DriveLoom is a working name: the application id is `driveloom.applicationId` in `gradle.properties`, the display name is `app_name` in `strings.xml`.
- Never commit keystores, `local.properties` or `google-services.json`.
