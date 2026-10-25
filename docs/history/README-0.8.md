# SoundFlow 0.8.0-resume

Independent experimental GPL-3.0 Android client, not affiliated with SoundCloud. Package `com.farika.soundflow`, versionCode4. Same signing certificate as Lab0.5–0.7: install over the existing Lab without uninstalling to preserve session, downloaded records and source plans.

## Changes in 0.8 — playback continuity
- Temporary audio-focus loss is separate from user pause. Duplicate transient-loss callbacks preserve the pending resume intent; focus gain resumes prepared audio. Preparing a new source during interruption does not start it until focus returns. Delayed audio-focus gain is supported. Foreground service is retained during transient interruption.
- Manual pause, permanent focus loss, unplugging headphones, and notification dismissal never automatically resume on a later gain callback. Notification dismissal still stops and releases the service.
- Stable queue identifiers (including duplicates and exact index), position, shuffle and repeat are saved to a dedicated account-scoped checkpoint every approximately 5 seconds and on seek/pause/source transitions/stop/service destruction/task removal. No signed media URLs or authorization values are saved there. A hard kill can lose the last few seconds. Missing library entries are filtered on restore; source URLs/licenses are freshly resolved when needed.
- After process death, the last track and a **Продолжить · position** label appear in the mini-player. Play restores that queue/position; opening the expanded player restores paused. Ordinary opening never autoplays. A system sticky-service restart also restores paused, rather than unexpectedly starting audio. No boot receiver starts music. Logout clears the checkpoint. Local files must still exist and cloud playback still requires network/account/service access.
- Android and manufacturer power management can still terminate any app. This is recovery and correct foreground/focus handling, not a promise to make the process immortal. Physical-device calls and manufacturer-specific battery management require user testing.

## Changes in0.7
- One per-playlist/Favorites **Автозагрузка** button enables both saving the current source and periodically saving additions. The same button disables that source, retaining ready files. No bulk-download checkbox dialog or Queue navigation button. Existing plans migrate without a database schema change.
- Network policy moved to the app settings gear: Wi-Fi/unmetered only or any connected network. The policy applies to autosave jobs globally. Explicit manual retry/single-track downloads remain manual operations. Android can delay a nominal15-minute job. Enabling autosave requests an immediate constrained job, not a bypass of network constraints.
- Expanded native player has a five-band **software PCM equalizer**:60/230/910/3600/14000Hz, ±12dB, persistent enable/gains/reset, live application. It is inserted into Media3's audio processor chain, not a system/global effect. Float/offloaded passthrough is not used for this PCM path. Boosts reserve automatic headroom; hard output clamping and independent per-channel state. Source changes/seek/stop reset/release processing state. No audio data leaves the device. This does not change any DRM license rules.
- Account tokens are paired with the client configuration actually used during login verification, stored separately from the hourly public-web configuration. Native account operations capture the token/client pair together. Upgrade preserves the old public-client value once as best-effort migration, but cannot reconstruct a historically lost pairing. This fixes a possible client mismatch, NOT a proven cause of the user's403 or a guarantee of write access. Error messages distinguish READ_ACCOUNT from WRITE and include a safe action/status code.
- Offline failures are more truthful. Old ORIGINAL_DISABLED did not mean a licence request had been rejected; that code path never requested one. New NO_OFFLINE_SOURCE distinguishes absence of an authorized original/supported licensed stream from an actual LICENSE_DENIED response. A non-downloadable clear playback stream is not copied around an author's restriction.

## Still unresolved / not guaranteed
Real-account write acceptance remains unverified in this environment. The user's previous0.6 account returned403. No fake local success, foreign-client secrets, registration bypass, or hidden-browser mutation has been added. SoundCloud user actions require appropriate user authorization; a successful website/me login alone does not prove native API write rights. If the server still rejects the request, this version reports the stage instead of claiming it succeeded.
Official authentication documentation: https://developers.soundcloud.com/docs/api/guide.html

Many tracks may remain unavailable for offline use. Saving requires an uploader-authorized original or a real SoundCloud offline Widevine license for this device/account. Protected audio stays encrypted and Android CDM owns keys. No preview, geographic, DRM, download-permission or subscription bypass. No promise to download all tracks.

Prior0.6 persistent For You feed/order/scroll, familiar-track filter, direct native playback, notification STOP, full-source pagination and durable shared-copy queue remain. No own ads, proxy music server or YouTube substitutions. Recommendation ranking is a local heuristic, not Spotify/Yandex algorithms.

## Build / signing
JDK 21, Android SDK 35 / Build Tools 35.0.0. Gradle 8.9 wrapper, AGP 8.7.3, Media3 1.5.1.

Restore the separately supplied **private signing backup** into `signing-private/` at the project root (`config.json`, `volna-lab.jks`). Then set JAVA_HOME/ANDROID_HOME and run `./build-apk.sh`.

The public source ZIP intentionally excludes signing credentials. To build your own separately signed version, generate your own keystore and JSON config with fields `password` and `alias`, or adapt Gradle's signing configuration. Your key cannot update an installation signed with another key. The old 0.4 key is unavailable; this is why Lab uses a separate applicationId.

Never publish the private signing backup. Keep it offline so future Lab updates can retain the same certificate. It contains only the new Lab signing key, not any SoundCloud login data.

## Tests
See TESTING.md. UI fixtures and transport tests are synthetic; no real account/track-license acceptance is asserted. Historical reports apply only to their original versions. Signing credentials and QA data are not bundled into the release app.
