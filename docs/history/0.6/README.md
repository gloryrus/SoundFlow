# SoundFlow 0.6.0-wave

Independent experimental GPL-3.0 Android client; not affiliated with SoundCloud.
ApplicationId `com.farika.soundflow`, versionCode 2. Signed with the same certificate as Lab 0.5; install over 0.5 without uninstalling to retain account, queue and downloads. Version 0.4 is a separate app.

## Changes
- Modern dark native interface: gradient My Wave card, compact artwork mini-player, track cards and redesigned download dashboard.
- Wave settings: include familiar tracks or exclude them completely, plus balanced/discovery/favorites ranking. Familiar means liked tracks and listening recorded by this app, not the entire listening history from other SoundCloud clients. History stays on-device. This is a local heuristic, not Spotify/Yandex algorithms or a claim of equivalent quality.
- For You keeps its ordered feed, scroll offset and settings-scoped snapshot across tab switches and restarts. Refresh is explicit. A failed refresh falls back to the saved feed.
- Likes/unlikes, private playlist creation and adding tracks are native HTTPS operations, with current account ownership checks. No page, hidden WebView or Javascript is used to execute mutations. The official login component remains separate. Server refusal never becomes fake local success. Unknown create outcomes are not automatically retried.
- Dismissing the media notification invokes STOP: player released, queue stopped and foreground service removed. An explicit Stop action is also available because Android versions/OEMs may restrict swiping active media notifications. Closing the app task alone does not stop normal background playback.
- Offline HLS master playlists now resolve to one audio rendition with correctly resolved initialization metadata. Temporary network/408/429/5xx/CDN expiry errors receive bounded retries and refreshed track metadata/stream addresses. Original download permission is refreshed rather than trusted from an old snapshot.
- Download dashboard distinguishes pending/retry from blocked items, preserves failure codes during source refresh, and offers per-track sanitized diagnostics. Copying a report is explicit; it contains track names/IDs and stable error codes, not cookies, tokens or signed audio URLs.
- Full Favorites pagination, complete playlists, durable queue, shared audio copies and per-source autosave from 0.5 remain. Android checks nominally every 15 minutes or later; unmetered network by default.

## Limits and truthful verification
Real-account write acceptance and real Widevine offline licenses were NOT tested here. Synthetic native request tests prove routing/error handling, not that SoundCloud will accept this session. HTTP 401/403 can still occur; data/session are retained and login refresh remains explicit. No registered OAuth credentials or borrowed client secret are bundled.

Downloads still require an uploader-authorized original OR a SoundCloud-issued offline Widevine license for this device/account. Protected content remains encrypted; Android CDM owns keys. No DRM, preview, geographic or subscription bypass. Non-downloadable clear playback streams are not copied around the author's restriction. A free account does not acquire Go+ rights. These changes fix technical failures, not the service's authority to refuse a download; not all tracks are guaranteed downloadable.

Playback is native Media3, direct to SoundCloud/CDN, without a music proxy, YouTube substitution or own advertising.

## Build / signing
JDK 21, Android SDK 35 / Build Tools 35.0.0. Gradle 8.9 wrapper, AGP 8.7.3, Media3 1.5.1.

Restore the separately supplied **private signing backup** into `signing-private/` at the project root (`config.json`, `volna-lab.jks`). Then set JAVA_HOME/ANDROID_HOME and run `./build-apk.sh`.

The public source ZIP intentionally excludes signing credentials. To build your own separately signed version, generate your own keystore and JSON config with fields `password` and `alias`, or adapt Gradle's signing configuration. Your key cannot update an installation signed with another key. The old 0.4 key is unavailable; this is why Lab uses a separate applicationId.

Never publish the private signing backup. Keep it offline so future Lab updates can retain the same certificate. It contains only the new Lab signing key, not any SoundCloud login data.

## Verification
See TESTING.md for exact scope and reproducible commands. Historical 0.5/0.4 documents are not current test evidence. No synthetic test account or music is bundled into the release application.
