# SoundFlow 0.5.0 — experimental account/queue repair

Independent GPL-3.0 Android client; not affiliated with SoundCloud. Package `com.farika.soundflow`, installed alongside `com.farika.soundflow.loginfix` 0.4. It cannot read that application's private session or files. Sign in again. Do not uninstall 0.4 just to test Lab.

## What changed
- Account writes no longer use the failing native api-mobile endpoint. Likes/unlikes, creating a private playlist, and adding a track run as ordinary credentialed website fetches in a **visible, first-party SoundCloud WebView**, after `/me` matches the connected account. Explicit confirmation required. No hidden browser or native Javascript bridge. A success is shown only for a successful response. Unknown outcomes are not automatically repeated; verify the website to avoid duplicate playlist creation.
- 401/403 no longer erase the session/profile/offline library. Reconnect is an explicit action; account switching requires an explicit logout first.
- Favorites bulk download follows `next_href` until exhausted, with cycle detection. Whole-playlist reads reject an incomplete track_count. Only after the complete source list is fetched is the ordered snapshot replaced.
- SQLite queue metadata persists across process death. Use **Медиатека → Загрузки / Авто → Продолжить очередь** to resume a manual job. Committed audio is reused by source ID. Partial files may restart; byte-level resume is not promised.
- **Медиатека → Любимое → ↓ Всё любимое** or playlist ↓: optional automatic saving of new additions. Android JobScheduler checks selected sources nominally every 15 minutes; Doze/OEM limits can delay it. Default: unmetered network (usually Wi-Fi), configurable. No background foreground-service startup. Retries of unavailable items are backed off 6 hours; manual refresh can retry now.
- Cached Favorites stay under Favorites; cached playlists stay bound to their original ID/order. Ready audio appears in Offline too, without a second audio copy. Removing a source item does not automatically delete an existing downloaded copy.

## Important limitations
**Real-account write success has not been verified in this environment.** These changes remove known client errors, not SoundCloud's authority to refuse requests. The undocumented website interface can change; a 403/challenge may still require interaction on the visible official page. There is no guaranteed token refresh without a registered OAuth application; use Account → Обновить вход when necessary. No borrowed client secret is included.

Playback remains native Media3; the account component is not the audio player. No own advertisements. No YouTube substitutions or custom music proxy.

Downloads require an uploader-authorized original OR a real offline Widevine license issued by SoundCloud to the device/account. Encrypted content remains encrypted; Android MediaDrm owns keys. No DRM/geo/paywall bypass. Thirty-second previews are not relabelled or downloaded as full tracks. A free account does not gain Go+ access. Cached licenses can expire. Bulk progress is N/M, including failures, not a claim that all music is available.

The existing local recommendation heuristic remains; no claim of Spotify/Yandex-level quality or access to their algorithms.

## Build / signing
JDK 21, Android SDK 35 / Build Tools 35.0.0. Gradle 8.9 wrapper, AGP 8.7.3, Media3 1.5.1.

Restore the separately supplied **private signing backup** into `signing-private/` at the project root (`config.json`, `volna-lab.jks`). Then set JAVA_HOME/ANDROID_HOME and run `./build-apk.sh`.

The public source ZIP intentionally excludes signing credentials. To build your own separately signed version, generate your own keystore and JSON config with fields `password` and `alias`, or adapt Gradle's signing configuration. Your key cannot update an installation signed with another key. The old 0.4 key is unavailable; this is why Lab uses a separate applicationId.

Never publish the private signing backup. Keep it offline so future Lab updates can retain the same certificate. It contains only the new Lab signing key, not any SoundCloud login data.

## Tests
See TESTING.md. Historical 0.4 material in docs/history is not evidence for this version. No synthetic account or test music is preloaded into the release APK.
