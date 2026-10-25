# Volna Lab0.7 — verification scope

## Test suites
- JVM: AuthCapture23, StreamPolicy20, RecommendationRanker13, EqKernel11 checks. EQ checks include flat bit-exact behavior, low-frequency attenuation, distant-band preservation, channel isolation, stability at8/44.1/48/96kHz, reset, above-Nyquist bands leaving low-rate audio untouched, and invalid-channel rejection.
- Legacy browser account script40 stub assertions remain historical regression coverage only. They do not test the current native mutation adapter.
- Android advanced transport/control tests: native likes/private-create/add ordering;401/403 refusal stage and no false success/session deletion; unknown create not retried; stale original permission refresh and503 retry; HLS master/public PSSH resolution; durable feed/tab state; token-bound client preserved after public configuration rotation; unified autosave on/off and job cancellation; precise offline failure classification; real Media3 PCM processor passthrough, changed output samples, empty-input safety, persistent settings and release.
- Android native playback tests: encrypted descriptor vault, local authorized-original open/play/pause/seek/background, software EQ attached to the PCM playback path with gain/enable/reset, notification delete PendingIntent actually stops/releases the service, local record deletion. Test audio only.
- Android storage/scheduling: complete multi-page Favorites, cancellation/cycle refusal, sanitized and account-scoped snapshots, durable plans/retries, periodic unmetered scheduling, shared source-copy reuse and401 preserving data.

## Passed Android results
API26 native playback16 checks, advanced transport/PCM/control32 checks, storage/scheduling19 checks passed. The software EQ was available in the actual Media3 playback session, gain/enable/reset succeeded. Real cloud acceptance remains untested. The standalone PCM unit suite is independent of audio hardware.

## Important regression discovered and fixed
The first software EQ integration rejected an empty input buffer because copying the shared EMPTY_BUFFER into itself raises IllegalArgumentException. An early empty-input guard and explicit regression assertion were added before delivery. The earlier hardware-EQ prototype was discarded: this emulator lacks that system effect. The release uses its own software PCM processor instead.

## Reproduce
JDK21, Android SDK35 and private signing config (or your own key) required.

```
./tests/run-unit.sh
./gradlew --no-daemon assembleRelease assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.farika.soundflow
adb shell am instrument -w -e mode native com.farika.soundflow.test/com.farika.soundflow.LabSmoke
adb shell pm clear com.farika.soundflow
adb shell am instrument -w -e mode advanced com.farika.soundflow.test/com.farika.soundflow.LabSmoke
adb shell pm clear com.farika.soundflow
adb shell am instrument -w com.farika.soundflow.test/com.farika.soundflow.LabSmoke
```

Use a dedicated test emulator, never a personal account installation. The controls-ui mode seeds synthetic QA account/collection and local test tone for screenshot inspection; clear its data afterwards. These fixtures are not part of the release app's data.

## Visual QA
Rendered and individually inspected on actual Android API26 at432×936dp: expanded player with EQ entry, enabled five-band EQ dialog, app-wide network settings and single-button playlist autosave dashboard. All four states passed the spacing/overflow check. No duplicate Queue button or per-source Wi-Fi setting remains. Fixtures use explicitly named local QA audio/account/collections, not real SoundCloud music or credentials.

## Not verified / not promised
- The user's real SoundCloud session or successful native likes/playlist writes. The previous0.6 user session returned403. Client binding changes are a candidate mismatch fix, not proof the actual403 is resolved. The app does not bundle registered OAuth credentials, bypass write permissions, or simulate cloud success locally.
- Real SoundCloud offline-license acquisition/renewal/expiry/decryption on a physical device/account. No DRM keys are extracted. NO_OFFLINE_SOURCE is not mislabelled as an actual license denial. Forbidden originals are not saved as clear-stream copies.
- That all music can be downloaded or that free accounts gain subscription rights. Preview/geo/subscription/DRM/download restrictions are unchanged.
- Immediate JobScheduler execution under physical-device/OEM power policies. Disabling a source cancels its work; partial-file byte-level resume is not promised.
- Every phone, unusual output codec, external passthrough device or sound route. EQ targets decoded PCM16 in this player's Media3 audio sink and is not a device-wide effect.

Historical reports in docs/history and docs/qa-0.6 apply only to their original builds. Current test results and UI screenshots are under docs/qa-0.7.
