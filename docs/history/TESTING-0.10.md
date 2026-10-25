# Volna Lab 0.10 — verification scope

2026-10-24. Dedicated API26 emulator, software rendering,432×936, no KVM. Loopback certificates/accounts/audio are synthetic and test-only. No real user account writes or provider-specific DPI bypass are asserted.

## JVM
- Existing non-legacy87 assertions (AuthCapture23, StreamPolicy20, RecommendationRanker13, EqKernel11, FocusIntent11, ApiRefusal9).
- RestartPolicy8: desired playback vs pause/suspension, app preference, timestamp freshness, future/absent timestamps and2-minute bound.
- TlsFirstFlight17: valid record framing, exact handshake-payload byte preservation, split within SNI, truncated/oversized first-flight refusal, allowlisted exact/child hosts, explicit licensing host, HTTP/non443/credential/spoof-host refusal, loopback-only listener and nonallowlisted CONNECT refusal.
- TLS live loopback5 cases: certificate-verified TLS1.2 and TLS1.3 handshakes/application bytes pass through real fragmented transport; wrong-host certificates rejected for both; untrusted certificate rejected. Generated test trust is confined to test code, never production. `tests/run-network.sh` generates a fresh local test certificate.
- Total deterministic JVM assertions112 plus5 live TLS cases. Legacy browser-script40 stub checks remain separate and do not prove native write acceptance.

## Current Android modes
- dpi7: default OFF; only a loopback proxy selected for allowlisted HTTPS; unrelated hosts unchanged; real ScApi HttpURLConnection and Media3 DefaultHttpDataSource pass through the fragmenter to a local SSL server; wrong hostname and untrusted test cert rejected. The test injects a loopback dialer and trust fixture into the test process only. No external traffic or production trust overrides.
- continuity12: native WAV keeps playing after Activity destruction/task removal; active checkpoint persists wanted/suspended/time; recent SYSTEM_RESTORE resumes full queue; explicit STOP/pause clear autoplay; paused/suspended/disabled recovery stays paused; a fresh Activity starts Home and retains the music checkpoint. SYSTEM_RESTORE is a service-path test, not itself proof of an actual OS kill/restart.
- resume17: transient-focus gain, repeated losses, manual/permanent/noisy cancellation, queue/index/duplicate/position/modes, paused restore and explicit continuation. Focus callbacks are synthetic; decoding/service operations are actual Android.
- native16: actual local/offline WAV, seek/background/PCM EQ, notification STOP releases player, descriptor vault/storage.
- advanced44: controlled native API/write refusals, no ambiguous mutation retry, original-source preservation, progress/cancel, Retry-After and failure classification, PCM pipeline, durable feed/tab cache and autosave.
- storage19: full pagination, durable account-scoped queues/retries, cancellation and scheduling/source reuse.
- motion8: entry/press/waveform and app animation disable behavior.

## Actual process-kill experiment
After seeding local audio and starting the non-instrumented native player, an external SIGKILL terminated the whole target process (PID4151). Android restarted its sticky service as PID4214, with MediaSession PLAYING again. No replacement Activity was launched by the app. See docs/qa-0.10/system-process-restart.txt. This confirms actual resumed local audio on this emulator; it does not demonstrate uninterrupted output, physical-device OEM behavior or a precise live-cloud recovery time. Position/queue tolerance is covered separately by native checkpoint tests. System restart was not instant.

Final Android suite assertions123 passed (7+12+17+16+44+19+8), plus the separate real-process experiment above. All three new settings screenshots passed individual visual review; see VISUAL-REVIEW.md. Non-legacy JVM assertions112 plus5 real loopback TLS cases passed.

## Reproduce
JDK21, SDK35, private signing backup (or your own key), build as README describes.

```bash
bash tests/run-unit.sh
bash tests/run-network.sh
./gradlew --no-daemon assembleDebug assembleDebugAndroidTest assembleRelease
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.farika.soundflow
adb shell am instrument -w -e mode dpi com.farika.soundflow.test/com.farika.soundflow.LabSmoke
# Clear data between independent suites. Other modes: continuity, resume, native, advanced, motion.
# Without mode: storage/scheduling. Test emulator only, never a personal-account installation.
```

`transport-test.p12` in androidTest/assets is a PUBLIC synthetic test key/certificate with literal test-only password. It is neither a SoundCloud identity nor the APK signing key, is not trusted by production and is not bundled in the release APK. The public ZIP excludes real signing credentials.

## Not fixed / not proved
- User's native like/playlist403. No registered OAuth app grant or secure exchange/refresh backend supplied. Website-session login is not proof of official API authorization. The same refused write is not relabelled successful. No borrowed client secrets, password collection or browser mutation fallback.
- User's NO_OFFLINE_SOURCE tracks. Real uploader permission or compatible stream plus actual issued offline licence is still required. No ripping of non-downloadable streams, extracted keys, offline/subscription/geo bypass or guarantee all tracks download.
- DPI bypass on the user's network. Local fragmentation is one experimental method, not a full ByeDPI engine, REALITY transport, remote relay, DNS repair, IP unblocker or system-wide VPN. Websites/WebView login and UDP/QUIC are not routed. HTTPS verification stays enabled. Enable affects new connections only.
- Seamless sound through a whole process kill is impossible: Android may restart its foreground sticky service and audio focus must be requested again. Force-stop/OEM power controls/denied background start are not bypassed. Normal UI opening never autoplays. Fresh active system restart is bounded to2minutes and can be disabled.

Historical0.9/0.8 test reports are in docs/history. Only current logs/images under docs/qa-0.10 apply to this build. Visual review and real-process-restart experiment, if completed, are recorded there separately rather than inferred from successful compilation.
