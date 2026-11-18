# Architecture — NOOR

## Modules

| Module | Kind | Responsibility |
|---|---|---|
| `:core` | Pure Kotlin/JVM, no Android | Domain logic that can be unit-tested anywhere |
| `:app` | Android application | UI (Compose), persistence, platform services |

`:app` depends on `:core`. Nothing in `:core` may import Android.

## `:core` (`core/src/main/kotlin/com/noor/core`)

| Package | Contents |
|---|---|
| `prayer/` | `PrayerTimesCalculator` (PrayTimes.org algorithm). `CalculationMethod.JAFARI` (16°/4°/14°) and `TEHRAN` (17.7°/4.5°/14°). Ja'fari midnight. Angle-based high-latitude rule |
| `calendar/` | `HijriConverter`: `UmmAlQuraConverter` (java.time), `TabularHijriConverter` (fallback), `AdjustedHijriConverter` (±days). `Occasions` (Shia dates). `Seasons` (Ramadan/Muharram/Arba'een windows) |
| `quran/` | `QuranParser` (Tanzil `sura\|aya\|text` format, SHA-256 check), `QuranMeta`, generated `SurahData` (114 surahs, 30 juz) |
| `content/` | `DuaCatalog` (entries, categories, sources; Quranic du'as stored as verse references only), `DailyVerses` |
| `qibla/` | `Qibla.bearing` / `distanceKm` (great circle) |
| `location/` | `Cities` (built-in list with coordinates and time zones) |
| `update/` | `Version` (semantic version comparison for the update checker) |

## `:app` (`app/src/main/java/com/noor/app`)

```
NoorApplication ─ creates AppContainer; schedules prayer reminders, content sync, auto-update (off main thread)
MainActivity    ─ locale wrap (LocaleManager), theme (dark/light, season, reduce motion), NoorApp()
AppContainer    ─ manual DI: database, settings, quran, duas, prayer, location, connectivity,
                  updates (UpdateRepository), updater (AppUpdater), contentSync, audio
data/
  db/        Room NoorDatabase v2: favorites, reading_progress, tasbih_state, audio_tracks (+ MIGRATION_1_2)
  prefs/     SettingsRepository (DataStore "noor_settings"); the language lives in SharedPreferences (LocaleManager)
  quran/     QuranRepository: loads assets/quran/quran-uthmani.txt, verifies SHA-256, caches
  duas/      DuaRepository: catalogue + content pack (filesDir/content/pack.json), parse/validate, import/sync
  prayer/    PrayerRepository (location → times/snapshot), LocationProvider (coarse, optional)
  remote/    GitHubClient (HttpURLConnection, public API), Connectivity, UpdateRepository (check),
             AppUpdater (download + SHA-256 + package/version/signature checks → PackageInstaller),
             ContentSync (content-* releases + .sha256), workers
  audio/     AudioLibrary (user files via SAF persisted URIs, MediaPlayer)
notifications/ PrayerNotifier, PrayerScheduler (one WorkManager job for the next enabled prayer)
ui/
  theme/      NoorColors, NoorPalette (season-aware), LocalReduceMotion, typography, shapes
  components/ GlassCard, HeroCard, AccentButton, UnavailableState, NoorTopBar, NoorDialog, ProgressRing, …
  navigation/ NoorApp (Scaffold + bottom bar + NavHost, max content width 720dp), Routes, UpdateDialog
  home, explore (Explore, Library, Marja), favorites (Saved), quran, duas, reading, prayer,
  qibla, tasbih, calendar, audio, settings (basic + advanced)
util/        Formatters (Arabic-Indic digits, times, dates), LocaleManager
```

### Navigation routes (`ui/navigation/NoorApp.kt`)

The four tabs are `home`, `explore`, `favorites` (labelled "Saved") and `settings`.
Other routes: `settings/advanced`, `quran`, `quran/{surah}?ayah={ayah}`, `duas?category={category}`,
`dua/{id}`, `prayer`, `city`, `tasbih`, `calendar`, `qibla`, `audio`, `library` and `marja`.

## Data and content flow

- **Quran:** the bundled asset is checked against `QuranMeta.TEXT_SHA256`. On a mismatch the reader shows
  an unavailable state.
- **Du'as:** a catalogue entry has one of three kinds of text:
  - verse references, resolved from the verified Quran text;
  - a short bundled formula (Tasbih al-Zahra);
  - text from a verified content pack. Otherwise the entry shows *unavailable*.
- **Content packs:** imported by the user, or synced from `content-*` releases. Both go through
  `DuaRepository.parse` validation. A synced pack never overwrites an imported one.
- **App updates:** `UpdateRepository.check` → `AppUpdater.downloadAndVerify` → `install`
  (PackageInstaller, `UpdateInstallReceiver`). Any verification failure deletes the file (fails closed).

## Persistence and migrations

- **Room schemas** are exported to `app/schemas/`. Bump the version, add a `Migration`, and extend
  `DataTest.migrationFromV1KeepsUserData`, or add a similar test, for every schema change.
- **DataStore keys** are in `SettingsRepository.Keys`. Add defaults; never rename existing keys.

## Testing

| Suite | Location | What it covers |
|---|---|---|
| `:core:test` | `core/src/test` | Prayer, Hijri, Qibla, versions, Quran integrity and catalogue references |
| `:app:testDebugUnitTest` | `app/src/test` | Robolectric journeys through the real `MainActivity` (EN/AR, dark/light, phone/small/tablet) with screenshots; migration; content packs; update dialog; updater refusal |
