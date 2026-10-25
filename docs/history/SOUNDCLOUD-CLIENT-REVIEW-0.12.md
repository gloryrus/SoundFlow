# SoundCloud client investigation — 0.12

Snapshot: 2026-10-24. Enumerated all 41 repositories on the three pages of the user-supplied GitHub topic, sorted by stars and cross-checked against the updated listing. This is a bounded source/README screening, NOT a security audit of every file, every GitHub repository, or proof that every client works. Some listings are archives, download links, wrappers, examples or scrapers rather than maintained write-capable clients.

## Main findings

1. **A route match alone is not an authenticated-browser transport match.** Jomvet-lovers/soundcloud-desktop direct mode documents DataDome scoring of HTTP/TLS fingerprints and sensor cookies. Its write path uses fetch from the SoundCloud website WebView. Its mirror_write changes local state only after a server success. This is the substantive difference missed in 0.11.
   - https://github.com/Jomvet-lovers/soundcloud-desktop/blob/248943cfc440a375f8de40c7d40df6ccc8f9930b/desktop/src-tauri/src/direct/webview/mod.rs
   - https://github.com/Jomvet-lovers/soundcloud-desktop/blob/248943cfc440a375f8de40c7d40df6ccc8f9930b/desktop/src-tauri/src/direct/routes/common.rs
2. **403 can be a provider-issued verification, not just a bad route or missing registered app.** Resona's interceptor preserves web cookies, detects a service-issued captcha-delivery URL, displays the verification and refreshes client configuration. Its challenge sheet uses a WebView iframe and accepts the provider's completion message. Its source does not prove what caused the user's 403, but provides a relevant implementation reference.
   - https://github.com/savo-o/resona/blob/0adbfd121e049c9cf9fbed08d3a32f51d73edf4d/app/src/main/java/com/savoo/scclient/data/remote/AuthInterceptor.kt
   - https://github.com/savo-o/resona/blob/0adbfd121e049c9cf9fbed08d3a32f51d73edf4d/app/src/main/java/com/savoo/scclient/ui/components/DataDomeChallengeSheet.kt
3. **A session-bound public client_id is not necessarily permanent.** Quantm refreshes public configuration after explicit401/403. Volna0.12's website writer observes the currently loaded site's API request and hydration configuration and saves the matched client only after a confirmed write; it does not keep blindly writing with an old public client value.
   - https://github.com/quantm-app/Quantm/blob/main/main.js
4. **Frontend routes in backend-based clients are not SoundCloud endpoint documentation.** SoundCloud-Android and the main SoundCloud-Desktop use their own session/server API abstractions. Their /likes/tracks and playlist routes cannot simply be copied onto SoundCloud's host. Their backend/decrypt/paid-access infrastructure is not imported.
5. **Old popular examples are not current protocol evidence.** favesound-redux and favesound-mobx use older me/favorites patterns and are useful historically, not proof that switching to that route fixes today's website session.
6. **Offline techniques differ.** Some downloaders/cache implementations copy ordinary full HLS; Quantm checks the author's download flag. A technically available full stream is not evidence of persistent-download permission. No non-downloadable stream copying, borrowed keys, decrypt service or entitlement alteration is added in 0.12.

## Changes actually transferred

- Existing Java visible AccountWebActivity is now the UI write transport rather than an unused legacy page. The playback service, media rendering and offline store remain native.
- The user's requested like/unlike/create/add starts one operation after the official page is loaded. No silent mutation retry after a timeout, reload or process/activity restoration. Other-account/me mismatch prevents the mutation.
- Website Authorization/client observations, ServiceWorker requests and the public hydration client are used. TLS errors cancel. There is no password-reading bridge, exported mutation activity, third-party music server or injected fake device identity.
- A service-issued403 challenge is only shown if it is HTTPS on captcha-delivery.com or a real child domain, without credentials/non443 ports. An iframe completion is accepted only from that iframe's window and provider origin, and only for a datadome cookie actually applied by the first-party page. No invented proof or automatic puzzle bypass. Completion alone never marks a like/playlist saved; the user explicitly confirms another attempt.
- Playlist bodies in the browser writer now use numeric IDs, matching the reviewed current web contract; incomplete/wrong-owner lists are not overwritten and existing order is retained.

## Offline: the four tracks in the new screenshot

Anonymous reads in this environment currently returned MONETIZE, streamable=true, downloadable=false and full unsnipped clear HLS for all four IDs:2413615341,1571435668,1228631209,2411545905. These are NOT the user's authenticated region/account results. The exact safe metadata is in offline-public-metadata.json; no client IDs, signed URLs, session values or track authorization are included.

The app's NO_OFFLINE_SOURCE branch occurs before an offline licence request when no author-enabled original and no compatible DRM source is selected. It is not evidence of a rejected licence, a failed DNS request, or an incomplete download. The new label states that full online playback may still be available. **Offline permission for these four tracks is not fixed or granted by this release.** The guard is retained; a full unprotected stream with downloadable=false is not silently converted to a permanent copy. The initial investigation hypothesis that simply adding such a cache would fix the user's request was not implemented.

## Inventory coverage

Large archives over24MiB were replaced with bounded GitHub-tree/raw-file reads. The counts below are selected files, not whole-repository audit counts; extra referenced challenge/writer dependencies were then read as needed. Source snapshots are review inputs outside the public app archive; only this report, safe inventory and metadata are redistributed. No untrusted project build/install scripts were executed.

| Repository | Initially selected files | Scope |
| --- | ---: | --- |
| 1vilio/sixonefour | 28 | Selected code and README |
| BlazeInferno64/soundcloud-core | 6 | Selected code and README |
| COMF2222/veirune | 32 | Selected code and README |
| Fleacon/SoundCloudSharp | 134 | Selected code and README |
| GraspCommercial76/soundcloud-pc-app | 3 | Selected code and README |
| Illogicalll/sctui | 33 | Selected code and README |
| Jomvet-lovers/soundcloud-desktop | 78 | Selected code and README |
| MinakowDev/freakcloud | 38 | Selected code and README |
| Orang-Studio/OrangPlayer | 1 | README/metadata only; no relevant implementation in selected snapshot |
| alexandre-abrioux/soundcloud-reverse | 10 | Selected code and README |
| andreystarkov/overabstracted | 12 | Selected code and README |
| barenddt/soundify | 3 | Selected code and README |
| bspst/soundground | 3 | Selected code and README |
| chrisdruta/nimbus | 56 | Selected code and README |
| depado/parakeet | 3 | Selected code and README |
| devilAPI/soundcloud-adfree-uwp-app | 1 | README/metadata only; no relevant implementation in selected snapshot |
| dissstructed1337/soundcloud-desktop | 3 | Selected code and README |
| dwindles/holdonquietly-soundcloud | 2 | README/metadata only; no relevant implementation in selected snapshot |
| end-sky/wisp | 3 | Selected code and README |
| enderice2/soundcloud-desktop | 3 | Selected code and README |
| giuseppebianchi/loud | 104 | Selected code and README |
| hellsontime/scdl | 9 | Selected code and README |
| imthaghost/scdl | 7 | Selected code and README |
| keshavbhatt/orange | 2 | README/metadata only; no relevant implementation in selected snapshot |
| luviel19/Music_Island | 5 | Selected code and README |
| okeydw/SoundCloud-Android | 41 | Selected code and README |
| omkarcloud/soundcloud-scraper | 13 | Selected code and README |
| polishtinsmithabyss/SoundCloud-Downloader-2026 | 1 | README/metadata only; no relevant implementation in selected snapshot |
| quantm-app/Quantm | 9 | Selected code and README |
| rvpanoz/soundload | 4 | Selected code and README |
| rwieruch/favesound-mobx | 79 | Selected code and README |
| rwieruch/favesound-redux | 93 | Selected code and README |
| savo-o/resona | 180 | Selected code and README |
| savo-o/resona-status | 1 | README/metadata only; no relevant implementation in selected snapshot |
| soroushchehresa/vue-soundcloud | 13 | Selected code and README |
| sumanthratna/soundcloud9000 | 5 | Selected code and README |
| wubly/mosaic | 17 | Selected code and README |
| yunghog/soundcloud-coverart-downloader | 3 | Selected code and README |
| yurenip/audionode | 4 | Selected code and README |
| zxcloli666/SoundCloud-Desktop | 219 | Selected code and README |
| zxczxczxczxczxczxc1111/soundcloud-desktop | 127 | Selected code and README |
