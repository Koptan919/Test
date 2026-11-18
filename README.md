# Noor (نور) – Twelver Shia companion app for Android

Native Android app built with Kotlin, Jetpack Compose (Material 3), MVVM, Room,
DataStore and WorkManager. Works fully offline, with no account and no tracking.

> **Renaming the app:** edit `noor.appName`, `noor.appNameArabic` (as `\uXXXX` escapes) and
> `noor.applicationId` in `gradle.properties`. Nothing else needs to change.

## Project memory

Contributors and AI sessions should start with [`CLAUDE.md`](CLAUDE.md), then read
[`docs/PROJECT_MEMORY.md`](docs/PROJECT_MEMORY.md) and [`docs/FEATURE_STATUS.md`](docs/FEATURE_STATUS.md).

## Modules

| Module | Contents |
|---|---|
| `:core` | Pure Kotlin, unit-tested: prayer-time engine (Ja'fari / Tehran), Hijri calendar (Umm al-Qura + tabular fallback + user adjustment), Shia occasions, cities, Quran parser with SHA-256 integrity check, du'a catalogue |
| `:app` | Android app: Compose UI, navigation, Room, DataStore, WorkManager notifications, location |

## Features

* **Home**: greeting, Hijri and Gregorian dates, and a prayer hero card with a live countdown, ambient light and today's five times. Below it: quick access tiles, the verified du'a of the day, the next occasion, continue reading, recently opened items, tools, and an offline indicator.
* **Quran**: the verified Uthmani text (SHA-256 checked) with surah and juz navigation, search, ayah bookmarks, reading progress and resume. Reading options: font size, line spacing, focus mode (bars hidden), keep screen awake, and source details.
* **Library**: the Quran, the *Mafatih al-Jinan* collections (supplications, ziyarat, munajat, tasbihat), al-Sahifa al-Sajjadiyya, Nahj al-Balagha, the audio library, and links to the official websites of the maraji'. Texts appear only from verified, attributed sources. Anything that isn't bundled shows a clear *unavailable* state with a way to download or import it.
* **Prayer times**: Ja'fari or Tehran method, city list or optional location, day navigation, and per-prayer notifications.
* **Qibla compass**: great-circle bearing, corrected for magnetic declination, using the rotation-vector sensor, with alignment haptics. Without a sensor it falls back to the bearing in degrees.
* **Tasbih**: Tasbih al-Zahra (34/33/33) and a free dhikr mode, persisted, with haptics.
* **Hijri calendar**: month grid, Shia occasions, and a ±2-day moon-sighting adjustment.
* **Occasion themes**: subtle Ramadan, Muharram and Arba'een atmospheres with relevant links. They can be turned off.
* **Audio**: plays the user's own files offline. No unlicensed recordings are bundled.
* **Updates and content sync**: checks GitHub for new stable releases and shows an Arabic or English dialog with the changelog. Approved content packs download with a checksum check.
* **Accessibility**: large touch targets, screen-reader headings and labels, adjustable text, reduced motion (in-app setting and system setting), and full RTL support.

## Quality assurance

`./gradlew :app:testDebugUnitTest` runs Robolectric journeys that launch the real
app, click through every screen in English and Arabic, on a phone, a small phone
and a tablet, in dark and light themes, and write screenshots to
`app/build/outputs/screenshots/`. Data tests cover the database migration and
content-pack validation. The `:core` tests cover the prayer engine, Hijri
calendar, Qibla bearings, version comparison and Quran integrity.

## Building

Requirements: JDK 17+, the Android SDK (platform 35 and build-tools), and internet access to
`dl.google.com` (Google Maven) and Maven Central.

```bash
echo "sdk.dir=/path/to/Android/sdk" > local.properties   # or set ANDROID_HOME
./gradlew :core:test          # JVM unit tests for the domain logic
./gradlew :app:testDebugUnitTest  # UI journeys + screenshots
./gradlew :app:assembleDebug  # → app/build/outputs/apk/debug/app-debug.apk
```

## Releases

See `docs/RELEASING.md`. The CI workflow builds and tests every push. The release workflow
produces a signed, verified APK as a **draft** release, which you publish manually.

## Content sources

* Quran text: Tanzil Project, Uthmani (tanzil.net), CC BY 3.0, used verbatim. The bundled file `app/src/main/assets/quran/quran-uthmani.txt` was cross-checked against an independent Uthmani edition (King Fahd Complex text via QuranEnc). The consonantal skeleton matches for 6234 of 6236 verses; the other two (12:39 and 12:41) differ only in a known orthographic variant.
* Prayer calculation: PrayTimes.org algorithm (Hamid Zarrabi-Zadeh).
* Fonts: Amiri, Amiri Quran and Cairo, under the SIL Open Font License (`docs/licenses`).
