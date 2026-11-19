# Feature status — NOOR

Last verified: **2026-11-19**.

**Status values:** Not started · In progress · Implemented · Tested · Blocked

- **Tested** means the feature's main user flow is covered by automated tests that **ran and passed**
  (`:core:test` and/or the Robolectric journeys in `app/src/test`).
- **No feature has been tested on a real device or emulator yet.** That column says "no" for everything.
- **Implemented** means the code exists and builds, but its main flow lacks automated coverage.
- **Blocked** means the code is ready but something outside the code is missing (verified content, repos or secrets).

| Feature | Status | Evidence / scope | Device-tested |
|---|---|---|---|
| Smart home dashboard | Tested | Journeys: greeting, dates, prayer hero with countdown, quick access, dua of the day, occasion, continue reading, recents, tools, offline chip (EN/AR, phone/small/tablet) | no |
| Quran reader | Tested | `QuranTest` (6236 verses, SHA-256, tamper rejected). Journeys: surah list, reader, bookmark, focus mode, reading sheet | no |
| Mafatih al-Jinan | Blocked | Catalogue, categories, sources and *unavailable* states tested. **Texts are not bundled**; they need a verified, licensed content pack | no |
| Duas and Ziyarat | In progress | 13 Quranic du'as and Tasbih al-Zahra show verified text (tested). Mafatih texts are blocked (see above) | no |
| Nahj al-Balagha | Blocked | Shelf and empty state tested. Entries come only from a verified pack (`category: nahj`) | no |
| Sahifa Sajjadiya | Blocked | Same as Nahj al-Balagha (`category: sahifa`); the catalogue lists Du'a Makarim al-Akhlaq without text | no |
| Ja'fari prayer times | Tested | `PrayerTimesTest`: London solstice, 54 cities weekly through 2026 in order, Ja'fari Maghrib offset, next/previous prayer. Screen rendered. **Not compared with official timetables** | no |
| Qibla compass | Implemented | Bearing math tested (London 119°, New York 58.5°); the screen renders Najaf at 202°. The rotation-vector sensor is untested | no |
| Digital Tasbih | Tested | Journey: 5 taps → counter and total are 5 (this found and fixed a lost-tap bug). Persisted in Room | no |
| Hijri calendar | Tested | `HijriTest` (Umm al-Qura known dates, tabular round trip, adjustment). Calendar screen rendered EN/AR | no |
| Islamic occasions | Tested | `HijriTest.occasionsFoundOnTheirDays`; occasions listed on home and in the calendar | no |
| Audio library | Implemented | The empty state is tested. Adding files and playback are untested; there is no background media service | no |
| Universal search | Tested | Explore search ("kahf" → Al-Kahf). **Matches titles only** (surahs, du'as), not verse text | no |
| Favorites | Tested | Journey: an ayah bookmark appears in Saved. Migration test keeps favourites | no |
| Offline reading | Tested | The Quran is bundled and verified; journeys run with no network (offline chip shown) | no |
| Notifications | Implemented | `PrayerScheduler` / `PrayerReminderWorker` with per-prayer toggles. No automated test | no |
| Shia forum integration | Not started | — (must distinguish forum discussions from verified rulings; no API assumed) | no |
| Religious source registry | In progress | Per-entry `source` fields, source sheets in readers, About sources, marja' official links. There is no central registry module yet | no |
| GitHub update checker | Implemented | Release parsing, the Arabic dialog, refusal of unverified APKs, and choosing the newest stable `vX.Y.Z` release over `content-*` releases (`UpdateSelectionTest`) are tested. A signing key exists (local release build verified with `apksigner`). **Waiting on:** the user adding the GitHub secrets and the first published release. Install flow untested on a device | no |
| Content synchronization | Implemented | Pack validation is tested. **Blocked in practice:** no `content-*` release has been published | no |

## Also present

| Feature | Status | Evidence |
|---|---|---|
| Settings (basic + advanced) | Tested | Journeys open both screens. Language switch verified in the Arabic journey (RTL asserted) |
| Library screen and Marja' links | Tested | Journeys: library shelves, marja list (`www.sistani.org` shown) |
| Occasion themes (Ramadan / Muharram / Arba'een) | Implemented | `Seasons` unit-tested. Visuals not captured (they depend on the current date) |
| Reduced motion | Implemented | Used by all journeys. The system animator-scale detection is untested |
| Room migration v1 → v2 | Tested | `DataTest.migrationFromV1KeepsUserData` |
| CI (GitHub Actions) | Tested | Run `44155104371`: all steps green |
| Signed release workflow | Implemented | Signing path verified locally with a throwaway key. The workflow itself has never run (no secrets) |
