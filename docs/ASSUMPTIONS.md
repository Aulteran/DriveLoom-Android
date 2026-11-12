# Assumptions awaiting owner approval

The app was built in one pass without stopping for decisions. These are the choices made along the way. Each is easy to change.

## Product

1. **Passkeys are not offered.** Android passkeys need the server's domain to publish a Digital Asset Links file naming this app's signing key. The server does not serve one and the app has no release signing key yet. Passkeys added in the web client are listed and can be removed under Security.
2. **Identity-provider (OIDC) sign-in is not offered.** The server's callback ends in the web client and is bound to a browser cookie, so a native app cannot complete it under the current contract. It needs a server change (for example a native redirect with PKCE). Linked identities are listed and can be unlinked.
3. **Administration is included** (accounts, invitations, audit log) even though it was not in the listed scope, because the person who runs a server usually wants to send an invitation from their phone.
4. **No offline mode.** Every screen reads from the server and nothing but the session is stored on the device. The server roadmap also defers offline use.
5. **Server API 0.6 or newer is required**, because the emailed sign-in code arrived in that version. A newer major version shows a notice but is allowed.
6. **English only.** Text lives in the Kotlin sources, not in string resources. Translation would be a mechanical follow-up.
7. **Dark is the default theme**, with light and "follow the system" as options.
8. **The statistics screen offers 30 days, 90 days, 1 year and all time**, like the web client. The vehicle home shows the last 12 months.
9. **The fuel log loads 50 fill-ups at a time** with a "Load older" button.

## Technical

10. **Two Gradle modules.** `core` is pure Kotlin so the API client and logic are unit-tested on the JVM; `app` holds Compose and Android code.
11. **Few dependencies.** OkHttp, kotlinx.serialization and coroutines, plus AndroidX and Compose. No dependency injection framework, navigation library, image-loading library or Retrofit; the app's own code for these is small.
12. **Session storage.** The session cookie and server address are encrypted with an Android Keystore key and kept in private preferences. The deprecated `androidx.security:security-crypto` library was avoided.
13. **Plain HTTP is possible but off by default.** It must be allowed per server after a warning. This is for home-network servers without TLS.
14. **Only system-trusted certificates.** A server with a private or self-signed certificate authority is not reachable until the owner decides whether the app should trust user-installed authorities.
15. **Redirects are not followed.** If a proxy redirects, the app shows the target and asks the person to enter it.
16. **Photos are re-encoded** to JPEG at up to 2048 px before upload, which removes metadata such as location. The original file is never sent.
17. **Decimals stay strings** end to end. Money is formatted through `BigDecimal`. The chart and comparison bars convert to floating point only to place pixels.
18. **The activity handles rotation itself**, so screens keep their state without view models. After the system kills the app in the background it reopens at the garage.
19. **Application id** `io.github.aulteran.driveloom` stays provisional and is set in one line of `gradle.properties`.
20. **The user agent** is `DriveLoom-Android/<version>`, so the web client's device list shows the app by name.
21. **No release build, signing or store setup** was done. `dev` was not merged into `main`.

## Verification gap

The container that built this could not download the Android SDK, so nothing was run on a device or emulator. Evidence is: the core unit tests, a local type-check of every Compose screen, and the CI build, lint and unit tests. A first run against a real server is the next step before any release.
