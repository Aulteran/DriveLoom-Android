# Security notes

Status: **reviewed 2026-11-12** (code review only; the app has not yet run on a device). This lists what the app does in each security-sensitive area and where a reviewer should look. Answers to the review questions are at the end.

| Area | What the app does | Where |
| --- | --- | --- |
| Session storage | The `dl_session` cookie value and the server address are encrypted with AES-256-GCM under a non-exportable Android Keystore key, each value bound to its name, in private SharedPreferences. Excluded from backup and device transfer. A value that fails to decrypt is dropped. The CSRF token is held in memory only. | `platform/EncryptedStore.kt`, `core/session/SessionStore.kt`, `res/xml/data_extraction_rules.xml` |
| Response size | Answers larger than 32 MiB are refused while reading, declared length or not, so a misbehaving server cannot exhaust memory. | `core/api/ApiClient.kt` |
| Cookie handling | No general cookie jar. The cookie is attached by hand, only to the configured origin, and captured only from that origin's responses. Values with unsafe characters are not sent. | `core/api/ApiClient.kt` |
| CSRF | Unsafe methods send `X-CSRF-Token`. The app sends no `Origin` header, which the server accepts. | `core/api/ApiClient.kt` |
| Server address | Typed input is reduced to scheme, host and port. Credentials in the address and non-HTTP schemes are refused. HTTPS is assumed. Redirects are never followed, so the cookie cannot be carried to another host. Probing a new address sends no session. | `core/api/ServerUrl.kt`, `core/api/ApiClient.kt`, `core/session/AppSession.kt` |
| Plain HTTP | The manifest permits cleartext so home-network servers work; the app refuses `http://` until the person allows it for that server after a warning, and marks the connection as not encrypted afterwards. | `AndroidManifest.xml`, `ui/screens/AuthScreens.kt` |
| TLS trust | Platform defaults: system certificate authorities only, no pinning, no user-installed authorities. | (no code; default behaviour) |
| Passwords and codes | Held in screen memory only, never saved across process death, never logged. Password fields disable suggestions. Fields support password-manager autofill. | `ui/screens/AuthScreens.kt`, `ui/screens/SettingsScreens.kt` |
| Re-authentication | On `reauth_required` the app asks for the password or an authenticator code and retries once. | `ui/AppEnv.kt`, `ui/AppRoot.kt` |
| Authenticator setup | The TOTP secret and recovery codes are shown once; copying marks the clipboard content as sensitive. While any secret (key, recovery codes, invitation link) is on screen the window sets `FLAG_SECURE`, so it is kept out of screenshots, recordings and the recent-apps preview. The `otpauth:` link is handed only to an app that handles that scheme. | `ui/components/Components.kt` (`SecretBox`), `ui/screens/SettingsScreens.kt`, `platform/AndroidPlatform.kt` |
| Photo upload | System photo picker (no storage permission). The image is decoded, resized and re-encoded as JPEG, which strips metadata; capped at the server's 5 MiB. | `platform/Images.kt` |
| Photo display | Image bytes from the server are decoded with bounds checked first and sampled down. | `platform/Images.kt` |
| Import | Files come from the system file picker, capped at 8 MiB while reading. CSV is sent as text and never interpreted on the device. JSON is parsed only to send it as a document. Row errors from the server are shown as plain text. | `core/fuel/ImportPayload.kt`, `platform/AndroidPlatform.kt` |
| Export | Saved through the system "create document" dialog to a place the person chooses. The server's suggested file name is reduced to a plain name. Exports are plain text. | `core/api/DriveLoomApi.kt`, `platform/AndroidPlatform.kt` |
| Invitation links | A pasted link is reduced to its `invite=` value. Created links are shown once. | `core/session/Invites.kt` |
| Exposure to other apps | One exported activity (the launcher). No deep links, content providers, services or receivers. | `AndroidManifest.xml` |
| Logging | The app writes nothing to the system log. | (whole app) |
| Dependencies | OkHttp 4.12, kotlinx.serialization, kotlinx.coroutines, AndroidX and Compose. | `gradle/libs.versions.toml` |

## Questions for the review

1. Is permitting cleartext at the manifest level, with the app's own opt-in, acceptable, or should HTTP be removed?
2. Should the session be bound to device unlock or biometrics (a Keystore key that requires user authentication)?
3. Should screens that show secrets (TOTP key, recovery codes, invitation links) set `FLAG_SECURE` to block screenshots?
4. Should the app trust user-installed certificate authorities for self-hosters with a private CA?
5. Is a year-long session cookie on a phone acceptable, or should the app offer an app lock?
6. Release hardening: R8 rules have not been exercised, and no signing configuration exists.

## Review answers (2026-11-12)

1. **Cleartext.** Acceptable as built: the platform allows it, the app refuses `http://` until the person allows it for one server after a warning, and redirects are never followed, so an HTTPS server cannot be downgraded. Revisit if the app ever talks to more than one host.
2. **Device-unlock binding.** Not required. An optional app lock (biometric or device credential) is a reasonable later feature; tying the Keystore key to user authentication would sign people out whenever the lock screen changes.
3. **`FLAG_SECURE`.** Done for every screen that shows a secret (see the table).
4. **User-installed certificate authorities.** Still an owner decision. If wanted, prefer a per-server opt-in over trusting user CAs for the whole app.
5. **Session lifetime.** The server, not the app, decides: 30 days absolute and 7 days idle. The app keeps the cookie only until the server stops accepting it.
6. **Release hardening.** Still owed: R8 rules have not been exercised and no signing configuration exists.
