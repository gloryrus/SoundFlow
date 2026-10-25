# SoundFlow 0.10.0-localdpi

Independent experimental GPL-3.0 Android client; not affiliated with SoundCloud. Package `com.farika.soundflow`, versionCode6. Same signing key as Lab0.5–0.9. Install over the existing Lab; do not uninstall/clear data.

## Local DPI transport (experimental, default OFF)
Settings → Локальный обход DPI → Дробление TLS ClientHello.

- No remote VPN/proxy server, subscription or configuration credentials. This is an original, deliberately limited TLS-fragmentation implementation, not the full ByeDPI engine or Xray/REALITY.
- A loopback-only ephemeral HTTP CONNECT relay is internal to this app. Native java.net/Media3 requests for exact SoundCloud/CDN/license domains are sent through it. It connects directly to the destination; it is not an external relay or a system-wide VPN. Other apps are not affected and Android VPN permission is not requested.
- First plaintext ClientHello handshake payload is split into two valid TLS records, preferably within SNI, with an8ms send gap. Handshake payload bytes, hostname/SNI, certificate validation, server identity, encryption and subsequent TLS traffic remain unchanged. No TLS interception, certificate bypass, spoofed server identity, fake signed packets or installed CA. Socket/timeout/concurrency limits are bounded; no packet/token logging. Only HTTPS443 allowlisted destinations are accepted.
- Both TLS1.2/1.3 certificate-verified loopback handshakes and rejection tests passed. Android ScApi and Media3 HTTPS transport were exercised through this relay. **These tests do not prove DPI bypass on the user's provider.** Advanced DPI can reassemble the records. IP/DNS blocking and unavailable geographic/subscription rights cannot be solved by this mode.
- Applies to new native connections, including normal media/CDN and DRM HTTP paths. Already connected streams are not interrupted. Website/WebView/browser login and QUIC/UDP are NOT covered. Turn the mode off and restart a track if a server/network rejects fragmented ClientHello. No alternate remote relay is configured.

## Music continuity
- Activity/tab state is accepted only within the process that produced it. A newly created process starts on Home instead of restoring a stale Search/playlist screen. **The saved Wave, account, library, source plans, offline files, full playback queue and checkpoint are NOT deleted.** Bitmap cache is evicted on Activity destruction. Ordinary tab switching still preserves the cached feed.
- Audio runs in the existing foreground service independently of Activity destruction/task removal. A process kill necessarily interrupts its audio output; Android may restart a sticky service, but this app cannot make a killed process keep playing.
- Checkpoints now include desired playback/suspended state and time. A system-service restart can resume an actively playing checkpoint saved within2minutes, requesting normal audio focus again. Default ON; Settings → Музыка в фоне can turn this off. Opening the app alone never autoplays.
- Manual pause, notification STOP, transient interruption and stale snapshots do not restart automatically. Explicit stop clears the auto-resume intent but retains a resumable queue. Android force-stop, user stop from system task controls, OEM shutdown and denied background start are not bypassed. This is best-effort restart, not an uninterrupted-playback guarantee or an independent process-service architecture.

## Still blocked: actual SoundCloud likes and offline rights
**The user's real ACCOUNT_LIKE_403 is not resolved by this build.** The same documented official API route remains; no real authorized OAuth app grant was supplied. The current website-session adapter cannot be represented as a registered OAuth integration. Proper official authorization needs the user's registered SoundCloud application and secure server-side token exchange/refresh; no client secret belongs inside a public APK. Documentation: https://developers.soundcloud.com/docs/api/guide . This build does not claim a token/session refresh fix or request the user's password.

**NO_OFFLINE_SOURCE remains blocked** when the service does not provide an uploader-authorized original or supported encrypted source with an issued offline licence. A transport switch cannot create offline/paid rights, turn a preview into a full track, or remove server-side region restrictions. No non-downloadable stream ripping, key extraction, borrowed official-client secrets or fake success.

Build/signing instructions and prior feature details are preserved below. Current verification scope is TESTING.md; historical screenshots/results retain their original version scope.

---

# SoundFlow 0.9.0-motion

Independent experimental GPL-3.0 Android client, not affiliated with SoundCloud. Package `com.farika.soundflow`, versionCode5. Same signing certificate as Lab0.5–0.8: install over the existing Lab without uninstalling to preserve session, downloaded records and source plans.

## Changes in 0.9 — requests, original downloads and motion
- Native track likes now use the documented `POST /likes/tracks/{id}` and unlikes `DELETE /likes/tracks/{id}` on `https://api.soundcloud.com`. Playlist mutations use that same public API. The same user session token is sent only to exact first-party API hosts; no public/borrowed `client_id` is appended to public-API writes, and no website cookie is used there. Reads and playback remain on the website adapter. No fallback retries against other endpoints after403 and no automatic retry of an ambiguous create.
- **A real-account 403 is NOT proven fixed.** The existing login captures a website session, not this application's registered OAuth grant. Changing a route cannot grant missing permissions. SoundCloud's supported user-resource access requires a registered app and OAuth authorization; this package does not contain a client secret or implement a registered OAuth backend. If the service still denies the session, the operation stays unconfirmed, the local heart does not fake success and session/downloads remain. The403 action now explains API access rather than repeatedly asking for login. Recognized protocol refusal codes are extracted from at most8KiB; arbitrary response text, tokens and headers are not displayed or persisted.
- Explicit original-download URLs returned by SoundCloud are preserved instead of being rewritten to api-v2. Both allowed first-party API hosts receive the user's Authorization header; the CDN receives neither Authorization nor website cookies. Original-file copy now reports progress and checks cancellation while copying. The `redirect_uri` response field is understood along with existing variants; partial/cancelled files are removed.
- EOF/incomplete/generic I/O errors are retryable, distinct from absent offline rights. TLS validation failures stay closed. Server Retry-After is bounded and respected by scheduling instead of immediate hammering. Existing per-source queues, ready copies, and account-scoped data remain.
- **NO_OFFLINE_SOURCE is not removed or bypassed.** An uploader-authorized original or a supported encrypted stream with an issued offline licence is still required. No ripping a non-downloadable stream, preview/region/subscription bypass, key extraction or fake download success.
- Native short motion: ~200ms tab-content and expanded-player entry, press/release feedback, ~150ms player collapse, confirmation-only expanded-heart pulse, 24fps waveform bars while playback/UI are visible. Wave callbacks stop on detach/focus loss/paused state. No per-row reanimation on scroll or network refetch on navigation. The new Appearance → Animations switch and Android's animator disable setting can turn motion off. No audio work runs in animation callbacks.

Public protocol reference: https://developers.soundcloud.com/docs/api/guide

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
