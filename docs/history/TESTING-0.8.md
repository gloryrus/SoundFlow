# Volna Lab0.8 — verification scope

## Test suites
- JVM: AuthCapture23, StreamPolicy20, RecommendationRanker13, EqKernel11 and FocusIntent11 checks. EQ checks include flat bit-exact behavior, low-frequency attenuation, distant-band preservation, channel isolation, stability at8/44.1/48/96kHz, reset, above-Nyquist bands leaving low-rate audio untouched, and invalid-channel rejection.
- Legacy browser account script40 stub assertions remain historical regression coverage only. They do not test the current native mutation adapter.
- Android advanced transport/control tests: native likes/private-create/add ordering;401/403 refusal stage and no false success/session deletion; unknown create not retried; stale original permission refresh and503 retry; HLS master/public PSSH resolution; durable feed/tab state; token-bound client preserved after public configuration rotation; unified autosave on/off and job cancellation; precise offline failure classification; real Media3 PCM processor passthrough, changed output samples, empty-input safety, persistent settings and release.
- Android native playback tests: encrypted descriptor vault, local authorized-original open/play/pause/seek/background, software EQ attached to the PCM playback path with gain/enable/reset, notification delete PendingIntent actually stops/releases the service, local record deletion. Test audio only.
- Android storage/scheduling: complete multi-page Favorites, cancellation/cycle refusal, sanitized and account-scoped snapshots, durable plans/retries, periodic unmetered scheduling, shared source-copy reuse and401 preserving data.

## Playback recovery added in 0.8
- `resume`: 17 Android assertions: real local WAV output, duplicate transient-focus callbacks, focus-gain resumption, transient foreground retention, manual pause/permanent loss/headphone-disconnect refusing later autoplay, queue duplicates/index/position/modes, stop/restart, prepare-during-interruption, account separation. Headphone receiver is invoked directly: Android disallows sending the protected system broadcast from the test application.
- `recovery-seed` then external `adb shell am force-stop`, followed by `recovery-check`: 8 assertions across process termination, durable queue/modes/position, last track visible, no autoplay/service start on normal activity open, paused prepare with correct seek, explicit continuation. Instrumentation also terminates its target process at the end of each run; preferences/files are not cleared between these two phases.
- JVM FocusIntent11 tests exercise desired-vs-suspended state, repeated losses/gains and cancellation. Total current non-legacy JVM assertions: 78. Legacy browser stub40 assertions remain separate.
- Current Android regression: native16, advanced32, storage19; plus recovery17+8 = 92 assertions (seed contains an additional playback readiness check). API26 emulator, software rendering,432×936, no KVM.
- Additional Android framework check: `adb emu gsm call/accept/cancel 5550108` produced PLAYING → PAUSED during ringing/active call → PLAYING after hangup at the same position. This is a simulated GSM call on API26, not a physical call. See emulated-call.txt.
- Abrupt external SIGKILL of the active app (pid3686) caused an actual sticky service restart (pid3849), restored PAUSED at10915ms without autoplay. See sticky-process-kill.txt.
- This does not prove a physical phone call, Android35+ audio-focus behavior on a physical device, OEM battery behavior or SoundCloud availability. Audio-focus callbacks in `resume` are synthetic, while WAV decoding, foreground status, seeks and service lifecycles are real Android operations.

## Passed Android results
API26 native playback16 checks, advanced transport/PCM/control32 checks, storage/scheduling19 checks passed. The software EQ was available in the actual Media3 playback session, gain/enable/reset succeeded. Real cloud acceptance remains untested. The standalone PCM unit suite is independent of audio hardware.

## Historical 0.7 regression discovered and fixed
The first software EQ integration rejected an empty input buffer because copying the shared EMPTY_BUFFER into itself raises IllegalArgumentException. An early empty-input guard and explicit regression assertion were added before delivery. The earlier hardware-EQ prototype was discarded: this emulator lacks that system effect. The release uses its own software PCM processor instead.

## Reproduce
JDK21, Android SDK35 and private signing config (or your own key) required.

```
./tests/run-unit.sh
./gradlew --no-daemon assembleRelease assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.farika.soundflow
adb shell am instrument -w -e mode resume com.farika.soundflow.test/com.farika.soundflow.LabSmoke
adb shell pm clear com.farika.soundflow
adb shell am instrument -w -e mode recovery-seed com.farika.soundflow.test/com.farika.soundflow.LabSmoke
adb shell am force-stop com.farika.soundflow
adb shell am instrument -w -e mode recovery-check com.farika.soundflow.test/com.farika.soundflow.LabSmoke
adb shell pm clear com.farika.soundflow
adb shell am instrument -w -e mode native com.farika.soundflow.test/com.farika.soundflow.LabSmoke
adb shell pm clear com.farika.soundflow
adb shell am instrument -w -e mode advanced com.farika.soundflow.test/com.farika.soundflow.LabSmoke
adb shell pm clear com.farika.soundflow
adb shell am instrument -w com.farika.soundflow.test/com.farika.soundflow.LabSmoke
```

Use a dedicated test emulator, never a personal account installation. The controls-ui mode seeds synthetic QA account/collection and local test tone for screenshot inspection; clear its data afterwards. These fixtures are not part of the release app's data.

## Visual QA
0.8: cold-open mini-player shows saved title, paused play control and Continue timestamp; expanded restored player retains the seek position and modes, without autoplay.

Prior0.7: Rendered and individually inspected on actual Android API26 at432×936dp: expanded player with EQ entry, enabled five-band EQ dialog, app-wide network settings and single-button playlist autosave dashboard. All four states passed the spacing/overflow check. No duplicate Queue button or per-source Wi-Fi setting remains. Fixtures use explicitly named local QA audio/account/collections, not real SoundCloud music or credentials.

## Not verified / not promised
- The user's real SoundCloud session or successful native likes/playlist writes. The previous0.6 user session returned403. Client binding changes are a candidate mismatch fix, not proof the actual403 is resolved. The app does not bundle registered OAuth credentials, bypass write permissions, or simulate cloud success locally.
- Real SoundCloud offline-license acquisition/renewal/expiry/decryption on a physical device/account. No DRM keys are extracted. NO_OFFLINE_SOURCE is not mislabelled as an actual license denial. Forbidden originals are not saved as clear-stream copies.
- That all music can be downloaded or that free accounts gain subscription rights. Preview/geo/subscription/DRM/download restrictions are unchanged.
- Immediate JobScheduler execution under physical-device/OEM power policies. Disabling a source cancels its work; partial-file byte-level resume is not promised.
- Every phone, unusual output codec, external passthrough device or sound route. EQ targets decoded PCM16 in this player's Media3 audio sink and is not a device-wide effect.

Historical reports in docs/history and docs/qa-0.6 apply only to their original builds. Current test results and UI screenshots are under docs/qa-0.8; docs/qa-0.7 are historical.
