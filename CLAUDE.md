# CLAUDE.md — NOOR (نور)

Native Android app for Twelver Shia Muslims. This file is the **entry point** for every
Claude Code session. Read it fully before doing anything.

## Session startup (mandatory)

1. Read this file.
2. Read [`docs/PROJECT_MEMORY.md`](docs/PROJECT_MEMORY.md) for state, environment and open issues.
3. Read [`docs/FEATURE_STATUS.md`](docs/FEATURE_STATUS.md) for what exists and how it was verified.
4. Run `git log --oneline -15` and `git status` to see what changed since the memory was written.
5. If the docs disagree with the code, trust the code. Verify, then fix the docs.
6. Continue from the latest **verified** progress. Do not redo finished work.

More references: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) ·
[`docs/DECISIONS.md`](docs/DECISIONS.md) · [`docs/CHANGELOG.md`](docs/CHANGELOG.md) ·
[`docs/RELEASING.md`](docs/RELEASING.md) · [`docs/CONTENT_PACK.md`](docs/CONTENT_PACK.md)

## Non-negotiable rules

1. Inspect the existing project before editing. Never recreate the app from scratch unless explicitly asked.
2. Preserve working functionality. **Never delete user data**: favourites, bookmarks, settings, reading progress, Tasbih state. Any Room schema change needs a migration and a migration test.
3. Write real, working code. Every button must perform its action. Never leave a fake or dead control.
4. Keep the UI simple for all ages. No unnecessary elements.
5. Prioritise stability, speed and readability. Keep the architecture modular and maintainable.
6. **Religious content:** never invent Quran verses, du'as, ziyarat, hadith, references, fatwas or sources. If a verified text is unavailable, show the *unavailable* state. Keep source attribution visible. Respect licences. Never present forum discussions as rulings.
7. Never assume a website has an API. Never scrape restricted sites.
8. Never commit or print credentials, tokens or signing keys.
9. Never claim a test, build, upload or deployment succeeded without running and checking it. Report failures as they are.
10. Never publish a production release, or create public repositories, without explicit user approval.
11. Never replace the visual identity (below) without explicit user approval.
12. Document every meaningful change (see "Session end").

## Identity (do not change without approval)

Colours: Titanium Black `#101114`, Deep Burgundy `#531C32`, Champagne Gold `#D8B878`, Warm Ivory `#F4F0E8`, Muted Gray `#A7A7AC`.
They are defined in `app/src/main/java/com/noor/app/ui/theme/Theme.kt` (`NoorColors`, `NoorPalette`).

Design: premium and minimal, subtle liquid glass (no heavy blur), smooth lightweight motion
that respects *reduce motion*, Arabic typography (Amiri Quran / Amiri / Cairo),
full RTL support, large 48dp+ touch targets, comfortable reading.

## Stack

Kotlin 2.0.21 · Jetpack Compose (BOM 2024.12.01) + Material 3 · MVVM · Room 2.6.1 (schema v2) ·
DataStore · WorkManager · Navigation Compose · AGP 8.7.3 · Gradle 8.14.3 · minSdk 26, target/compile 35 ·
GitHub Actions. Modules: `:core` (pure Kotlin, unit-tested) and `:app` (Android).

## Commands

```bash
echo "sdk.dir=/path/to/android-sdk" > local.properties   # once (gitignored)
./gradlew :core:test                 # domain tests (prayer, Hijri, Qibla, Quran integrity, versions)
./gradlew :app:testDebugUnitTest     # Robolectric UI journeys + screenshots → app/build/outputs/screenshots/
./gradlew :app:lintDebug             # must report 0 errors
./gradlew :app:assembleDebug         # → app/build/outputs/apk/debug/app-debug.apk
```

App name, application ID, version and the release repository are set in `gradle.properties` (`noor.*`).
The Arabic name is stored as `\uXXXX` escapes. UI strings live in `res/values` and `res/values-ar`;
both must contain every key the code uses.

## Change safety

Before a major change, inspect the affected components and their dependencies. After it:
run `:core:test`, `:app:testDebugUnitTest` and `:app:lintDebug`, fix what breaks, look at
the screenshots for visual regressions, and update the docs.

## Session end (mandatory after meaningful work)

Update `docs/PROJECT_MEMORY.md` (state, issues), `docs/FEATURE_STATUS.md` (status and evidence),
`docs/DECISIONS.md` (new decisions) and `docs/CHANGELOG.md`. Record **actual** test and
build results only. Never record assumptions as facts. Commit and push the docs to the
authorised branch.
