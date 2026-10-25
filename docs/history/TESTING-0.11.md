# Volna Lab 0.11 — executed verification

2026-10-24. Final code: versionCode 7, versionName 0.11.0-webapi, package com.farika.soundflow. API26 software-rendered emulator without KVM. All account, licence and content data in tests are synthetic. Logs under docs/qa-0.11 apply to this version; older logs retain their historical scope.

## Deterministic JVM / TLS

- 123 non-legacy JVM assertions passed: AuthCapture23, StreamPolicy31, RecommendationRanker13, EqKernel11, FocusIntent11, ApiRefusal9, RestartPolicy8, TlsFirstFlight17.
- 40 legacy browser-script stub checks passed separately. They are not proof of native writes and are not the current mutation transport.
- 5 live local TLS handshake/rejection cases passed with real certificate validation. Local test trust is confined to tests; production TLS checks are unchanged.

## Android instrumentation

| Mode | Passed assertions |
| --- | ---: |
| webapi | 29 |
| advanced | 44 |
| native | 16 |
| dpi | 7 |
| continuity | 12 |
| resume | 17 |
| motion | 8 |
| storage | 19 |
| Total | 152 |

The new webapi suite checks website-session PUT/DELETE likes; the same API host and session-bound client_id for reads/writes; no cross-account browser cookie mixing; private playlist creation with numeric IDs; ordered updates, duplicates and incomplete reads; 204 empty-body handling; no ambiguous create retry; refusal/session preservation; no mutation redirects; fresh track metadata/authorization; full HLS before previews; clear full before DRM for playback; alternate advertised full variants after per-format failure; untrusted audio URL rejection; duration/snipped/SNIP preview classification; no silent preview downgrade after a full variant fails; terminal401/429/503 handling; and unmodified BLOCK policy.

Advanced44 covers native refusals, original-file downloads/progress/cancel/CDN auth isolation, retry/backoff, PCM pipeline and feed persistence. Its mutation contract expectations were updated to the new web API. Native16 exercises real local WAV decoding/service/seek/background/EQ/stop and offline descriptor storage. DPI7 verifies synthetic Android HTTPS and Media3 transport with correct and incorrect certificates, not actual ISP-block evasion. Other modes cover queue/focus/checkpoint recovery, animation lifecycle and storage/scheduling.

The first motion run failed its wave-animation assertion because the view had no window focus; inspection showed an unrelated «Application Not Responding: com.android.systemui» window owned focus. A key-event attempt did not dismiss that dialog, and that retry failed in the same no-focus state. The visible «Close app» action was then selected on the synthetic emulator and motion was rerun in isolation, without application-code changes. Both failed runs and the final rerun logs are retained. The final table reports the isolated rerun, not an uninterrupted all-green first run.

Offline source ranking remains DRM-first specifically for licensing, while online playback prefers available full clear formats. JVM tests assert both orderings, including manifest MIME types. Synthetic API tests do not prove an actual licence can be issued.

## Build, signature and upgrade

- Debug, AndroidTest and release builds succeeded. The final AndroidTest-only rebuild corrected a missing ID in a synthetic stale-track fixture; release code did not change in that rebuild.
- Non-debuggable release APK verified. Certificate SHA256: 38221e367706f5b3f1965b6420ce1bc5e18c7830d187f5208e110fd4261f6fa4. Same applicationId/certificate as Lab0.5–0.10; no schema reset/logout migration was added.
- Actual in-place emulator update from versionCode6/0.10 to versionCode7/0.11 preserved a synthetic private-files marker. This is a data-preservation smoke check, not proof of real-account token validity after upgrade. Test data clears were performed only on the isolated emulator between suites.
- Release ZIP entries checked: no test WAV, test transport certificate/private key or signing keystore in the release APK. Public source intentionally includes the pre-existing PUBLIC synthetic androidTest transport-test.p12 fixture, not production credentials. Production signing-private/ is excluded.
- APK SHA256: see APK-SHA256.txt.

## Explicitly unverified / not implemented

- No real user's SoundCloud like/unlike or playlist create/add was performed. Fixed request-shape and host/method mismatches are not proof that the user's ACCOUNT_LIKE_403 is resolved. A successful website login does not guarantee every endpoint accepts that session. No automatic token refresh was added.
- No guarantee of full access to all tracks, paid access, geographic rights or offline licence issuance. No DRM key extraction, preview URL rewriting, foreign mobile-app secret/identity or remote backend/proxy. A clear online stream is not copied around an uploader's download restriction.
- DPI effectiveness on the user's network and physical-device/OEM background behavior remain untested. The 0.10 real-process restart experiment was not repeated here; continuity/resume assertions here are their stated service/checkpoint tests, not a newly claimed SIGKILL experiment.

## Reproduce

JDK21, SDK35, Gradle8.9/AGP8.7.3, Media3 1.5.1; restore your own signing configuration as described in README.

```bash
bash tests/run-unit.sh
bash tests/run-network.sh
./gradlew --no-daemon assembleDebug assembleDebugAndroidTest assembleRelease
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# The following clears isolated QA data. NEVER run on a personal-account installation.
adb shell pm clear com.farika.soundflow
adb shell am instrument -w -e mode webapi com.farika.soundflow.test/com.farika.soundflow.LabSmoke
# Other modes: advanced, native, dpi, continuity, resume, motion; omit mode for storage.
# Clear only isolated QA data between independent modes.
```

## User acceptance checks

Install over Lab0.10 without uninstalling. Check one like/unlike on the actual SoundCloud website, create one private test playlist then add a second track and verify both IDs/order, and separately test normal online playback of the reported preview tracks. For old blocked downloads use the existing «Повторить ошибки» action without removing ready files. Check the site before retrying an operation whose result is unknown.
