# Volna Lab 0.6 — verification scope

## Passed automated checks
- Release/debug/test APK compilation: Gradle8.9, AGP8.7.3, JDK21, SDK35. Same signing certificate as Lab0.5; versionCode2.
- Standalone JVM: AuthCapture23, StreamPolicy20, RecommendationRanker13 assertions. Includes hard exclusion of all familiar tracks and empty new-only candidate set. No real credentials/network.
- Legacy browser-account script:40 Node stub assertions retained as historical regression coverage only. Browser mutations are NOT the active0.6 path and these tests do NOT verify the native adapter.
- Android API26 synthetic storage/scheduling:19 checks. Complete Favorite pagination, cancellation/cycle refusal, SQLite persistence/dedup lookup, sanitized metadata, account isolation, persisted unmetered periodic job, source shared copy/resume,401 preserving session/files.
- Android API26 native playback:15 checks. Test WAV, encrypted metadata vault roundtrip/tamper refusal, invalid URL/path refusal, offline-original playback/pause/seek/background, notification delete PendingIntent actually sent and service/player released, deletion. Emulator lacks Widevine and correctly fails closed. No real licensed music tested.
- Android API26 advanced synthetic transport:19 checks. Native PUT/DELETE likes, native private create and add preserving order, no duplicate add, incomplete playlist refusal, account auth header,401/403 preserve session without mutation retry, unknown CREATE not retried; original-download503 then retry success with fresh permission; persisted per-track failure; network/preview/license classification; HLS master-to-one-rendition and public PSSH metadata resolution; owner-scoped persistent HomeFeed/scroll; actual tab switches restore same ordered15 tracks without fetching.

All transport/account data were synthetic. These checks prove client behavior under controlled inputs, NOT SoundCloud acceptance of real writes or licenses.

Release signature verified with APK v2; certificate matches Lab0.5. Machine-readable synthetic results and UI fixtures are in docs/qa-0.6.

## Visual QA
Actual Android API26 screens were rendered and inspected at a432×936dp viewport: gradient My Wave feed, familiar-track settings, redesigned download dashboard, and per-track failure report. The first settings dialog had oversized stacked actions; it was revised to a rounded dark dialog with a separate reset control and two bottom actions. Final screens show readable text and no overlapping controls at this viewport. All displayed tracks/accounts were clearly marked synthetic QA fixtures; they are not included in the release app's data.

## Reproduce
Restore the private signing configuration or use your own key; set JAVA_HOME to JDK21 and ANDROID_HOME to SDK35.

```
./tests/run-unit.sh
./gradlew --no-daemon assembleRelease assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.farika.soundflow
adb shell am instrument -w com.farika.soundflow.test/com.farika.soundflow.LabSmoke
adb shell pm clear com.farika.soundflow
adb shell am instrument -w -e mode advanced com.farika.soundflow.test/com.farika.soundflow.LabSmoke
adb shell pm clear com.farika.soundflow
adb shell am instrument -w -e mode native com.farika.soundflow.test/com.farika.soundflow.LabSmoke
```

Use a dedicated emulator, never a phone/app containing a personal account. Tests replace profile and data. The fixtures mode seeds synthetic UI data for visual inspection and must be cleared afterwards.

## Not verified / no guarantee
- Real-account login refresh, 2FA, likes or playlist creation/add acceptance. A401/403 may remain if SoundCloud refuses the session.
- Real Widevine license acquisition/expiry/renewal/playback, and DRM/device differences.
- Full rather than preview track rights or availability; downloads require uploader original permission or real offline license. No geo/paywall/DRM bypass.
- Immediate autosave under OEM/Doze constraints (Android can delay nominal15-minute checks).
- Every Android/OEM's notification swipe policy. Explicit Stop is provided in addition to deleteIntent.
- Spotify/Yandex-quality personalized recommendations; this app uses its own local reranking over SoundCloud related-track candidates.

Historical docs/history test reports apply only to their original versions.
