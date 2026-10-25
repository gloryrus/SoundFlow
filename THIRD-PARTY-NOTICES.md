# Volna Lab 0.12.0-browserwriter
New work: visible first-party browser write transport, live website configuration observations, restricted provider-issued challenge display and truthful offline-source diagnostics. Native audio/offline licensing paths and provider-permission checks are unchanged. Jomvet-lovers/soundcloud-desktop (MIT) writer transport and Resona (GPL-3.0) challenge flow were reviewed as interoperability references; the Java implementation builds on Volna's existing visible AccountWebActivity. No foreign backend, app secret, DRM content key/decryptor or challenge forgery is included. Details and source links: docs/SOUNDCLOUD-CLIENT-REVIEW-0.12.md. The combined distribution remains GPL-3.0; previous notices are historical.

# Volna Lab 0.11.0-webapi
New work in 0.11: consistent website-session mutation contracts and stream selection/fallback/preview classification. Reviewed Quantm and Resona GPL-3.0 sources for endpoint and payload interoperability; no project was bundled wholesale. See docs/SOUNDCLOUD-CLIENT-REVIEW.md for pinned revisions. New Java implementation and tests remain under this distribution's GPL-3.0 terms. No borrowed app secret, mobile identity, remote backend or decryption service was incorporated. Earlier release descriptions below are historical.

# Volna Lab 0.10.0-localdpi
Independent experimental Android client. GNU GPL version 3.
New work in0.10: original app-local ClientHello record fragmentation without a remote server, process-bound transient UI state and guarded system-service playback resumption. No ByeDPI/REALITY core was incorporated; no TLS verification was disabled. Prior notices and GPL terms retained.
New work in0.9: documented public-API mutation routing, safe refusal-code extraction, original-download URL/progress/cancellation fixes, Retry-After scheduling and optional native motion. Prior work in0.8: audio-focus interruption/resume handling and account-scoped playback checkpoints with queue/position/modes recovery. Prior work in0.7: unified per-playlist autosave, global network settings, original five-band software PCM equalizer, session-bound client configuration and truthful offline-source diagnostics. Prior0.6 work: native mutations, persistent feed, notification stop and HLS handling. No service credentials/signing key in public source. Synthetic tests do not prove real account or license acceptance.

# Licensing and references — Volna Direct 0.3.0

This combined source distribution is provided under GNU GPL version 3 (see LICENSE), without warranty. Changes for 0.3.0 were made on 2026-10-23. Full corresponding application source and build files accompany the APK; no signing private key is included. Recipients may build, modify and redistribute under the license.

## KittyTune
Copyright and license notices of KittyTune contributors are retained in `LICENSES/KittyTune-LICENSE`. Project: https://github.com/alan7383/kittytune
Reference revision: 4a78611667378755d1accc113fdd071bfc9e384f (GPL-3.0).

Native DRM callback protocol and FrameworkMediaDrm / OfflineLicenseHelper integration were adapted/reimplemented for this Java/Media3 1.5.1 project from these references:
- https://github.com/alan7383/kittytune/blob/4a78611667378755d1accc113fdd071bfc9e384f/app/src/main/java/com/alananasss/kittytune/data/SoundCloudDrmCallback.kt
- https://github.com/alan7383/kittytune/blob/4a78611667378755d1accc113fdd071bfc9e384f/app/src/main/java/com/alananasss/kittytune/data/MusicManager.kt
- https://github.com/alan7383/kittytune/blob/4a78611667378755d1accc113fdd071bfc9e384f/app/src/main/java/com/alananasss/kittytune/data/DownloadManager.kt

Changes: direct HTTPS rather than proxy-capable client, sanitized diagnostics, no token logging, strict failed-license handling, encrypted offline descriptors, completion only after cache-only verification, no YouTube replacement. This does not imply endorsement or service permission.

## AndroidX Media3 1.5.1
Copyright The Android Open Source Project. Apache License 2.0 (`LICENSE.APACHE-2.0.txt`).
https://github.com/androidx/media/tree/1.5.1
ExoPlayer, HLS, DRM, encrypted cache and their transitive AndroidX dependencies. Notices may also be present in dependency artifacts. Android framework components remain subject to their own licenses.

## Earlier project reference
https://github.com/okeydw/SoundCloud-Android — MIT license; original notice retained in `LICENSE.reference-project.txt`.
The earlier Volna application's Apache license text is retained; combination changes are GPL-3.0.

Quantm, Resona and SoundCloud-DesktopFork were reviewed for comparison, not bundled wholesale. No remote backend, extracted DRM device keys, DRM decrypt service, YouTube fallback, or proprietary SoundCloud application code is included.

SoundCloud name and content belong to their respective owners. No SoundCloud affiliation is claimed. Open-source licenses do not grant rights to music, accounts or restricted service access.

## 0.4.0 additions (2026-10-23)
Direct mobile track-like POST routes and `likes[].target_urn` request shape were adapted from KittyTune (same pinned GPL-3.0 revision):
https://github.com/alan7383/kittytune/blob/4a78611667378755d1accc113fdd071bfc9e384f/app/src/main/java/com/alananasss/kittytune/data/network/SoundCloudApi.kt
No KittyTune official-app User-Agent, borrowed client secret, device identity, proxy, or authentication fallback was copied. Volna sends its own User-Agent to the mobile endpoint. Server authorization refusals remain refusals.
Playlist snapshots, shared offline binding, local listening feedback and multi-seed heuristic ranking are new Volna changes. They are not Spotify/Yandex algorithms and contain no code or data from those services.
