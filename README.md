# Noor (نور) – Twelver Shia companion app for Android

Native Android app built with Kotlin, Jetpack Compose (Material 3), MVVM, Room,
DataStore and WorkManager. Works fully offline, with no account and no tracking.

> **Renaming the app:** edit `noor.appName`, `noor.appNameArabic` and
> `noor.applicationId` in `gradle.properties`. Nothing else needs to change.

## Modules

| Module | Contents |
|---|---|
| `:core` | Pure Kotlin, unit-tested: prayer-time engine (Ja'fari / Tehran), Hijri calendar (Umm al-Qura + tabular fallback + user adjustment), Shia occasions, cities, Quran parser with SHA-256 integrity check, du'a catalogue |
| `:app` | Android app: Compose UI, navigation, Room, DataStore, WorkManager notifications, location |

## Features

* **Home dashboard**: greeting, Hijri and Gregorian dates, a live next-prayer card with a per-second countdown and progress ring, shortcuts, continue reading, today's prayer strip, verse of the day, and today's and upcoming occasions.
* **Quran**: 114 surahs and 30 juz, search, the Uthmani text in the Amiri Quran font, ayah bookmarks, surah favourites, automatic reading progress, adjustable font size, copy and share.
* **Duas & Ziyarat**: catalogue with categories and search, bookmarks and a reader. Quranic du'as are read from the verified Quran text. Other texts show an *unavailable* state until a verified content pack is imported (see `docs/CONTENT_PACK.md`).
* **Prayer times**: Ja'fari (Leva Institute, Qum) or Tehran method, manual city selection, optional coarse location, day navigation, and the next-prayer countdown. Prayer times are not shown until a location is set.
* **Tasbih**: Tasbih al-Zahra mode (34/33/33) and a free dhikr mode with targets, haptics and persistent state.
* **Calendar**: Hijri month grid with Gregorian dates, month navigation, colour-coded Shia occasions, and a ±2-day adjustment for moon sighting.
* **Favourites**: surahs, ayahs and du'as stored in Room, with filters and remove.
* **Settings**: Arabic, English or system language (full RTL), dark, light or system theme, interface and Quran text size, calculation method, Hijri adjustment, per-prayer notifications, and content-pack import.

## Building

Requirements: JDK 17+, the Android SDK (platform 35 and build-tools), and internet access to
`dl.google.com` (Google Maven) and Maven Central.

```bash
echo "sdk.dir=/path/to/Android/sdk" > local.properties   # or set ANDROID_HOME
./gradlew :core:test          # JVM unit tests for the domain logic
./gradlew :app:assembleDebug  # → app/build/outputs/apk/debug/app-debug.apk
```

## Content sources

* Quran text: Tanzil Project, Uthmani (tanzil.net), CC BY 3.0, used verbatim. The bundled file `app/src/main/assets/quran/quran-uthmani.txt` was cross-checked against an independent Uthmani edition (King Fahd Complex text via QuranEnc). The consonantal skeleton matches for 6234 of 6236 verses; the other two (12:39 and 12:41) differ only in a known orthographic variant.
* Prayer calculation: PrayTimes.org algorithm (Hamid Zarrabi-Zadeh).
* Fonts: Amiri, Amiri Quran and Cairo, under the SIL Open Font License (`docs/licenses`).
