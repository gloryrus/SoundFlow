# Volna Lab 0.12 — executed verification

2026-10-24. versionCode8, versionName0.12.0-browserwriter; package com.farika.soundflow. Final main debug/release builds succeeded; final Android-test build succeeded separately. All test accounts, cookies, media and API responses are synthetic. Logs under docs/qa-0.12; earlier QA directories apply only to their historical releases.

## Deterministic and TLS tests

- 128 JVM checks passed: AuthCapture28, StreamPolicy31, RecommendationRanker13, EqKernel11, FocusIntent11, ApiRefusal9, RestartPolicy8, TlsFirstFlight17.
- 40 Node browser-account-script checks passed with stub fetch, not an actual browser or service.
- 16 Node challenge origin/source/cookie checks passed with synthetic events.
- 5 real local TLS handshake/rejection cases passed with certificate validation. No production TLS relaxation.

## Android instrumentation

API26 software-rendered x86_64 emulator, without KVM. Browser-writer runs use the real official Chrome69 WebView engine in single-process test-device mode. This is an old isolated test engine, NOT a recommendation to install an old browser on a personal device; use an up-to-date supported System WebView/Chrome there.

| Mode | Passed checks |
| --- | ---: |
| writer | 40 |
| webapi | 30 |
| advanced | 44 |
| native | 16 |
| storage | 19 |
| dpi | 7 |
| continuity | 12 |
| resume | 17 |
| motion | 8 |
| Total | 193 |

The writer suite uses a real WebView, real JavaScript fetch/CORS and evaluateJavascript polling, but intercepts EVERY HTTPS response with fixtures. It checks one confirmed LIKE/UNLIKE/CREATE/ADD, current observed public client instead of stale saved value, matched session persistence, refusal without false success/replay, wrong-owner refusal, hydration-only fresh configuration, and a synthetic provider iframe completion that applies its synthetic cookie but never automatically replays or marks a mutation successful. Numeric playlist bodies and order/incomplete-list validation are checked by the separate Node40 suite; interception cannot read POST bodies here.

webapi30 covers the retained native adapter and stream selection, not the current UI write transport. It includes preserving NO_OFFLINE_SOURCE for a clear full stream with downloadable=false and avoiding a fake ready record. Advanced44 covers allowed original downloads/cancellation/progress, retry/backoff, feed persistence and PCM. Native16 uses local WAV/Media3 service/seek/EQ/background/storage. DPI7 is synthetic Android HTTPS/Media3 transport with correct/incorrect certificates, not evidence of ISP bypass. Other modes cover checkpoint/restart/focus, UI motion and download storage/scheduling.

## Failures and test-environment repairs (not hidden)

- The initial writer run had no valid installed WebView provider. Official Chrome was extracted from Google's API26 image after the repository SHA1 was verified and installed normally. Provider signature/certificate checks were not disabled or spoofed.
- Multiprocess Chrome69 rendered neither the intercepted first-party document nor its JS completion before the test deadline: requests=1, pageReady=false, NOT_STARTED. Switching only the isolated test device to its supported single-process WebView mode resolved this; the app does not change the user's WebView process setting. Both timeout logs and the first 34-check pass are retained. The final40 run includes the new hydration regression.
- The initial continuity run failed activity_destroy_does_not_stop_audio immediately after an asynchronous seek. The fixture now waits for native playback to resume after seek BEFORE closing the Activity, then retains the same post-destroy playing assertion. No player/service production code was changed. The failed run and the passing rerun are retained; this is not an uninterrupted all-green first run.

## Release checks and remaining acceptance gaps

- Release signature verified; same certificate as0.11, SHA25638221e367706f5b3f1965b6420ce1bc5e18c7830d187f5208e110fd4261f6fa4. Package/version verified, release not debuggable; no signing keys, test audio or test TLS keys in APK. Signed release installed and MainActivity launched on the isolated emulator.
- No real user account login/mutation, live DataDome acceptance, real licence issuance, user's region/network or OEM background behavior is verified. Synthetic provider-cookie tests are NOT real captcha completion.
- The four screenshot tracks remain NOT fixed for offline: anonymous metadata has full clear HLS but downloadable=false. No persistent-copy permission was inferred from playback availability. Original/offline-licence guard retained. See docs/SOUNDCLOUD-CLIENT-REVIEW-0.12.md and docs/offline-public-metadata.json.
- In-place upgrade is supported by unchanged package/signature and increased versionCode, but no personal user data was imported into this test environment. Do not uninstall/clear the user's app.
