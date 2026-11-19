# Changelog — NOOR

The format follows *Keep a Changelog*. Versions match `noor.versionName` in `gradle.properties`.
Only verified results are recorded.

## [Unreleased]

### Added
- Persistent project memory: `CLAUDE.md`, `docs/PROJECT_MEMORY.md`, `docs/ARCHITECTURE.md`,
  `docs/FEATURE_STATUS.md`, `docs/DECISIONS.md` and this changelog.

## [0.2.2] — 2026-11-19 (versionCode 4, not yet published)

### Changed
- Update source: GitHub Releases of `Koptan919/Test` (user's choice).
- The update checker picks the newest stable `vX.Y.Z` release with an APK, ignoring `content-*` releases.
- Release workflow: publishes to the same repository with the built-in token; has a `publish` input
  (draft by default).

### Verified
- Local: core 15/15, app 12/12, lint 0 errors.
- A release build signed with the new key passed `apksigner verify` (package `com.noor.app`, versionCode 4).

## [0.2.1] — 2026-11-17 (versionCode 3, not published)

### Added
- Automatic, verified self-updates. A daily Wi-Fi check downloads the APK and installs it through
  PackageInstaller only if the SHA-256, package name, newer version and signing certificate all match.
- An update-ready notification, a settings toggle and a shortcut to the install permission.

### Fixed
- Verification now fails closed on any `Throwable`.
- Flaky CI journey: assertions now wait for asynchronous Room/DataStore content.

### Verified
- Local: core 15/15, app 11/11, lint 0 errors, debug APK built.
- GitHub Actions run `44155104371`: success.
- Not device-tested.

## [0.2.0] — 2026-11-17 (versionCode 2, not published)

### Added
- **Visual identity:** new palette (Titanium Black, Deep Burgundy, Champagne Gold, Warm Ivory,
  Muted Gray), flatter glass, ambient hero, occasion themes, reduced motion.
- **Home:** redesigned dashboard.
- **New screens:** Library, Sahifa and Nahj shelves, Marja' links, Qibla compass, audio library.
- **Reading:** line spacing, focus mode, keep screen awake, source details.
- **Updates and content:** GitHub update checker, verified content sync.
- **Settings:** split into basic and advanced.
- **Data:** Room v2 with migration.
- **QA and CI:** Robolectric UI journeys with screenshots, CI and release workflows.

### Fixed
- Text was drawn black on dark backgrounds (no content colour was provided).
- Tasbih lost taps made before the saved state loaded.
- Low-contrast primary buttons.

## [0.1.0] — 2026-11-17 (versionCode 1, not published)

### Added
- **First native app:** Kotlin, Compose, M3, MVVM, Room, DataStore and WorkManager.
- **Content and tools:** verified Quran reader, du'a catalogue, Ja'fari prayer times with city
  selection, Tasbih, Hijri calendar, favourites.
- **Settings:** Arabic/English settings with full RTL.
- **Branding:** launcher icon.
