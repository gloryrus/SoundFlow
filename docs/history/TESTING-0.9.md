# Volna Lab 0.9 — verification scope

## Current results — 2026-10-24
Dedicated Android API26 software emulator, 432×936, no KVM. All account/transport fixtures are synthetic and named QA. No personal SoundCloud session was used.

- JVM: 87 non-legacy assertions (AuthCapture23, StreamPolicy20, RecommendationRanker13, EqKernel11, FocusIntent11, ApiRefusal9). Historical browser-script40 checks are separate, not native authorization evidence.
- Android advanced:44 assertions. Documented official POST likes route, native playlist/create guards, Authorization restricted to exact API hosts, no website cookie on official writes, no false success/session deletion after401/403, safe enumerated refusal extraction, no ambiguous-create retry, permitted-original URL preservation and redirect_uri support, CDN credential isolation, progress/cancellation, partial-file cleanup, bounded Retry-After and durable delayed retry, EOF/I/O/TLS classification, existing PCM EQ and feed/autosave regression.
- Android native:16 assertions. Local WAV play/pause/seek/background, real software EQ path, notification STOP releases service, descriptor-vault/storage checks.
- Android resume:17 assertions. Audio-focus desired/suspended intent, duplicate transient losses, gain resumption, manual/permanent-loss/noisy cancellation, queue/index/position/modes and explicit continuation. Focus callbacks are synthetic; decoding and service operations are real Android operations.
- Android storage/scheduling:19 assertions. Pagination, account-scoped snapshots/plans/retries, unmetered jobs, cancellation and shared-copy reuse.
- Android motion:8 assertions. Default on, entry start/end, native pressed scale and release, waveform phase changes during playback and settles while paused, app switch disables motion. Android-global animator disable:2 additional assertions. Total current Android assertions:106.

## Visual QA
Current images in docs/qa-0.9 were captured from the actual native Android app after transitions settled. Local playback is paused for static layout capture; animation behavior is tested independently above. Individually inspected: Home, expanded player, Settings with Animations switch, Downloads detailed offline refusal report and HTTP403 write-refusal dialog. All six passed spacing/readability/overflow inspection. Local test sound, QA tracks and synthetic account are not bundled as release data.

## Scope / unresolved
- **Successful likes and playlist writes on the user's real account are NOT verified.** The login adapter captures a website session, not a registered OAuth grant for this application. Correct API routes do not grant missing permissions. HTTP403 can persist until suitable registered-app authorization is provided. No borrowed app credentials, fake local cloud success or hidden-browser mutation.
- The explicit original-download URL bug and retry/cancellation behaviors were tested with controlled transport, not every live catalog track. NO_OFFLINE_SOURCE still requires an uploader-authorized original or compatible encrypted source with a real issued offline license. No stream ripping, DRM/license extraction, preview/subscription/geographic bypass or promise that all tracks download.
- Offline-license issuance/renewal/expiry/decryption on a physical device/account, OEM battery policies and physical phone calls are not verified here. Historical0.8 process-kill/call experiments were not rerun in0.9; see docs/history/TESTING-0.8.md for their original scope.
- Software EQ targets decoded PCM16 in this player's Media3 sink, not global effects/passthrough devices. Partial-file byte-range resumption is not promised.

## Reproduce
JDK21, SDK35, Gradle8.9; signing instructions in README.md.

```bash
./tests/run-unit.sh
./gradlew --no-daemon assembleRelease assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Dedicated test emulator only. Clear between independent suites.
adb shell pm clear com.farika.soundflow
adb shell am instrument -w -e mode advanced com.farika.soundflow.test/com.farika.soundflow.LabSmoke
# Other modes: native, resume, motion; omit mode for storage/scheduling.
# Motion-disabled run sets Android animator_duration_scale=0, then restores it.
```

motion-ui is a host-acknowledged screenshot harness. It writes a stage marker, waits for a local qa_motion_ack file and uses only synthetic fixtures. Do not run fixture modes on a personal account installation. Production release contains none of the androidTest harness classes.

Logs under docs/qa-0.9 contain successful final runs only. Earlier screenshot-harness timing mistakes and a software-emulator System UI ANR were not counted as passing tests. Historical reports apply only to their original builds.
