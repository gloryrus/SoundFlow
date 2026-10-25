# Native visual review — 0.10, 2026-10-24
API26,432×936. PNGs captured after native transitions settled and inspected individually.

- dpi_off.png: PASS — default OFF switch, serverless experimental scope, limits and existing network settings readable; no overlapping controls.
- dpi_on.png: PASS — ON switch visibly distinct, stable layout, no toast covers explanatory copy.
- continuity_settings.png: PASS — background-music switch and recovery/force-stop explanation fit the scrollable settings; version and licenses remain accessible.

No generated imagery or fake brand assets needed for these settings. Existing native dark UI retained. First software-emulator motion run lost focus to a System UI ANR dialog; after choosing Wait and allowing the emulator to settle, all8 motion assertions passed. This was not counted as an application success or silently treated as a pass.
