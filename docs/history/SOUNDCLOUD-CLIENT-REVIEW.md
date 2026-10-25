# SoundCloud client review — Volna Lab 0.11

Reviewed source snapshots on 2026-10-24, starting from the user-supplied topic page: https://github.com/topics/soundcloud-client?o=desc&s=updated . Repository activity does not prove correctness, authorization or availability on any user's account.

## Relevant findings and implementation decisions

| Source | Observed contract | Volna change |
| --- | --- | --- |
| Quantm, GPL-3.0 | Website api-v2 wrapper; PUT/DELETE users/owner/track_likes/track | Use that web-session contract instead of mixing website login with the official public-API mutation route |
| Resona, GPL-3.0 | The same PUT/DELETE track_likes endpoint | Independent corroboration of method/path, not proof our user's writes will be accepted |
| Quantm | playlist.tracks is an array of IDs for create and set tracks | Send numeric IDs, not public-API object records; preserve existing order, refuse incomplete reads and another owner |
| Quantm | Refetch track; use current track_authorization; prioritize non-snipped candidates; try alternatives | Keep fresh auth and full-first selection, add safe full-variant fallback and do not silently downgrade after a full variant fails |
| Quantm | Only clear progressive/HLS accepted by its Electron playback implementation; protected-only tracks reported unavailable | Do not copy that limitation: retain Android Widevine. Prefer full clear formats for playback if both exist, preserve protected preference specifically for offline licensing |
| SoundCloud-DesktopFork | Public hydration track_authorization; excludes snipped candidates; configurable backend/proxy transport | Relevant contrast only. No remote backend, third-party cookies, proxy URL or decrypt service transferred |

Pinned source references:
- Quantm API implementation: https://github.com/quantm-app/Quantm/blob/7c96c47a384db9192822a2c5980a6b1ae809d626/src/js/api.js
- Resona API interface: https://github.com/savo-o/resona/blob/0adbfd121e049c9cf9fbed08d3a32f51d73edf4d/app/src/main/java/com/savoo/scclient/data/remote/SoundCloudApi.kt
- DesktopFork source comparison: https://github.com/Teiwazik/SoundCloud-DesktopFork/blob/2c5fb686814006623521b0bc208fed8bc26d63ec/backend/src/soundcloud/sc-public-cookies.service.ts

Other downloaded source samples (SoundCloud-Android, soundcloud-core and desktop variants) were screened during discovery. No code, foreign client secrets or authentication identities from those projects were imported. The new Java adapter changes and synthetic tests were implemented here, using the web request contracts as interoperability references. Existing GPL notices and licence files remain in the source distribution.

## Limits of the findings

The 0.10 app already sent its saved user Authorization and session-bound client_id on website reads/resolution, refetched track metadata, passed track_authorization and scored full variants above previews. These are not entirely new fixes and should not be presented as proof that every preview was an app bug. Its concrete mutation mismatch was website-session login paired with official-API write routes and object-shaped playlist tracks.

0.11 does not rewrite preview URLs, suppress SNIP/BLOCK policy, alter regional routing, borrow mobile-app credentials, extract content keys or grant subscriptions. Duration-based preview detection is conservative (more than five seconds and ten percent shorter), not a claim to know all server entitlements. Unsupported DRM-only tracks can still fail; source availability and licence issuance remain provider-controlled.

An API success in synthetic instrumentation proves only request shape and app response handling. A real like/unlike and private-list create/add must still be checked on the user's own SoundCloud library. No real account mutations were performed in this environment. A registered OAuth integration is the supported official API path, but its absence does not itself prove all unofficial web-session writes must fail.

## Playback vs offline

PREVIEW_ONLY and NO_OFFLINE_SOURCE in the supplied download report are not the same condition. A full clear online stream may work while offline saving remains unavailable: this app does not rip a non-downloadable stream. An uploader-authorized original or compatible protected stream plus an actual offline licence is still required. Offline source resolution has its own protected-first preference, so the new clear-first playback preference does not accidentally discard licence-eligible sources.

## Deterministic checks

`WebApiCases` intercepts every HTTPS URL inside the instrumentation process and blocks all non-api-v2 hosts. It uses synthetic token/client/track-auth values; it cannot make real account changes. It checks routes, methods, numeric bodies, 204 success, incomplete/duplicate safeguards, no ambiguous retry, session preservation, fresh full-first resolution, full-variant fallback, preview classification, terminal auth/rate/server errors and unchanged BLOCK policy. It complements the updated AdvancedCases and JVM StreamPolicy checks; see TESTING.md for executed results.
