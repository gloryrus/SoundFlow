# Native visual review — 0.9, 2026-10-24
432×936 Android API26. Each PNG inspected individually after native transition settled. Synthetic QA data only.

- home.png: PASS — hero, scroll list, mini-player and navigation separated, readable labels.
- player.png: PASS — artwork placeholder, title, seek, controls and equalizer fit without overlap.
- settings.png: PASS — network and animation switches, explanatory copy and license button fit.
- downloads.png: PASS — one autosave action and retry/details controls, no duplicate queue control.
- reasons.png: PASS — distinct NO_OFFLINE_SOURCE / IO_ERROR copy, no clipping.
- write403.png: PASS — safe refusal code and missing-rights explanation, no looping login action or false success.

Animations independently passed eight instrumentation assertions plus two Android-global disable assertions. Playback paused for static layout capture. Earlier mislabeled captures were replaced, not counted as evidence.
