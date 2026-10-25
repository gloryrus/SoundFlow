# Volna Lab 0.5 — verification scope

## Passed
- Release and debug APK builds, Gradle 8.9 / AGP 8.7.3 / JDK21 / SDK35. Release signature verified with apksigner (APK v2).
- Standalone JVM: AuthCapture 23, StreamPolicy 20, RecommendationRanker 11 checks. No real credentials or licensed content.
- Browser account script: 40 Node VM/stub-fetch assertions. Exact generated Javascript, not a separately rewritten mock algorithm. Success/error statuses, HTTPS first-party origin, different-account guard, no duplicate ADD, preservation of existing playlist order, incomplete-playlist refusal, unknown CREATE outcome, no automatic mutation retries. These do **not** prove a real SoundCloud account can write.
- Android 8.0/API26 integration: **19 checks passed** on the application. Three pages/240 Favorites; cycle and cancellation rejection; 241 SQLite rows including duplicate playlist entries; metadata sanitization; account-scoped durable plans/queue; retry backoff preservation; persisted periodic unmetered job; Favorites cache distinct from ordinary playlists; shared-source local-copy lookup/resume; native 401 handling preserves session and downloaded record. Data and account token were synthetic.

- Android API26 native playback: **12 checks passed**. Synthetic offline-original open/play/pause/seek/background playback/delete; metadata vault roundtrip/tamper refusal; invalid URL/path refusal; no Widevine on this emulator, correctly fails closed. This is not a real license test.

- Actual Android UI inspected: Favorites bulk controls, bulk confirmation dialog, queue/autosave settings. No clipped or overlapping controls observed at the test phone viewport. Clean **release** APK installation and launcher startup succeeded on API26; empty library/profile confirmed, no synthetic preloaded content.

## Run
`./tests/run-unit.sh` requires JDK21 and Node.
`./gradlew assembleDebug assembleDebugAndroidTest` (restore private signing config or use your own key first).
Install `app-debug.apk` and `app-debug-androidTest.apk` on a dedicated test emulator with no personal account/data.

```
adb shell am instrument -w com.farika.soundflow.test/com.farika.soundflow.LabSmoke
adb shell am instrument -w -e mode native com.farika.soundflow.test/com.farika.soundflow.LabSmoke
adb shell am instrument -w -e mode fixtures com.farika.soundflow.test/com.farika.soundflow.LabSmoke
```

`native`: synthetic test WAV and synthetic authorized-original record only. No DRM-license request or real track download. `fixtures`: leaves synthetic UI data for inspection; clear application data afterwards. Never run these tests in an installation with a real account, since they replace test profile/storage metadata.

## Not verified / no guarantee
- A real user's SoundCloud 2FA, login refresh, likes, playlist creation or add-track acceptance.
- Acquisition, renewal, expiry and playback of a real Widevine offline license on a physical device/account.
- Whether the server grants a full track rather than a 30-second preview.
- Job execution timing under physical-device Doze and manufacturer power-management policies. Job constraints/persistence were verified, not immediate scheduling.
- Permission refusals are not bypassed and failed items do not count as ready. Offline licenses can expire.

The API30 Google image did not provide a stable software-emulated environment under the workspace's memory limits; no passing API30 result is asserted. API26 was used instead. Historical tests in docs/history describe only 0.4 and cannot be used as 0.5 evidence.
