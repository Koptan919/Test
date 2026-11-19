# Architectural and product decisions — NOOR

Each entry records **what** was decided and **why**. Add new entries at the bottom; don't rewrite history.
If a decision is reversed, add a new entry that references the old one.

| # | Date | Decision | Reason |
|---|---|---|---|
| D1 | 2026-11-17 | Two modules: a pure-Kotlin `:core` and an Android `:app` | Domain logic (prayer, Hijri, Quran integrity) is unit-testable without Android, and was even testable before the SDK was available |
| D2 | 2026-11-17 | Manual DI via `AppContainer`, no Hilt | The app is small. Fewer annotation processors and faster builds |
| D3 | 2026-11-17 | Quran text: Tanzil Uthmani, bundled verbatim (`assets/quran/quran-uthmani.txt`) and verified by SHA-256 at runtime | Verified, attributable source (CC BY 3.0). Cross-checked against the KFGQPC text (consonantal skeleton matches in 6234 of 6236 verses) |
| D4 | 2026-11-17 | **No unverified religious text.** Long du'as and ziyarat show an *unavailable* state until a verified content pack is installed | Typing texts from memory risks errors; the user's rule forbids invented content |
| D5 | 2026-11-17 | Quranic du'as are stored as verse **references** and resolved from the verified Quran text | A single verified source of truth for the wording |
| D6 | 2026-11-17 | Prayer times: PrayTimes.org algorithm with Ja'fari (default) or Tehran parameters; Ja'fari midnight | Standard published parameters for Shia authorities |
| D7 | 2026-11-17 | Hijri dates: Umm al-Qura (java.time) with a tabular fallback and a ±2-day user adjustment | Deterministic offline calendar; the adjustment covers local moon sighting |
| D8 | 2026-11-17 | The language is stored in SharedPreferences (`LocaleManager`) and applied in `attachBaseContext` | It must be readable synchronously before the UI starts. Gives correct RTL and resources |
| D9 | 2026-11-17 | App identity (name, ID, version, release repo) lives in `gradle.properties` (`noor.*`) | Renaming in one place |
| D10 | 2026-11-17 | Fonts bundled from OFL sources: Amiri Quran, Amiri, Cairo | Correct Arabic and Quranic rendering offline; licences in `docs/licenses/` |
| D11 | 2026-11-17 | The release repository is the only update source. Draft releases need manual publishing | Production releases require explicit approval |
| D12 | 2026-11-17 | Self-update installs only if the SHA-256, package name, newer version **and identical signing certificate** all match. It fails closed on any error (including `Throwable`) | Prevents installing tampered or foreign APKs; user data is preserved by the same-key update |
| D13 | 2026-11-17 | Self-update uses PackageInstaller with `USER_ACTION_NOT_REQUIRED` (Android 12+). Android asks once for install permission | The user asked for automatic updates. This is the platform-supported way for apps outside Play |
| D14 | 2026-11-17 | Content packs are independent of APK updates (`content-*` releases + `.sha256`). A synced pack never overwrites a user-imported pack | Content can update without an app release; user choices are preserved |
| D15 | 2026-11-17 | Release signing only through environment variables / CI secrets (`NOOR_KEYSTORE_*`) | Keys never enter the repository |
| D16 | 2026-11-17 | UI QA through Robolectric native-graphics journeys and screenshots | No emulator (no KVM) in the sandbox. These exercise the real app and found real bugs |
| D17 | 2026-11-17 | Reduced motion (in-app toggle + system animator scale) disables infinite animations | Accessibility and battery; also makes tests deterministic |
| D18 | 2026-11-17 | Content width is capped at 720dp on large screens | Comfortable line length on tablets and foldables |
| D19 | 2026-11-17 | The audio library plays only the user's own files (SAF persisted URIs) | No licence to bundle or stream recitations |
| D20 | 2026-11-17 | Marja' section = links to official websites only | Noor never issues or presents rulings |
| D21 | 2026-11-17 | Debug builds use the application ID suffix `.debug` | A debug build installs next to the release build; self-update targets release builds |
| D22 | 2026-11-18 | Persistent project memory in `CLAUDE.md` + `docs/*.md` | Future sessions recover state from the repository, not from conversation history |
| D23 | 2026-11-19 | App and content releases are published in the development repo itself (`Koptan919/Test`), at the user's choice | Works immediately with `github.token`; can move to `noor-releases` later |
| D24 | 2026-11-19 | The update checker uses the newest non-draft, non-prerelease release tagged `vX.Y.Z` that has an APK, not `releases/latest` | Content-pack releases (`content-*`) in the same repository must never hide app updates |
| D25 | 2026-11-19 | The release key was generated in the sandbox, delivered only to the user, then shredded. Its certificate fingerprint is recorded in PROJECT_MEMORY | The user asked for it; keys never enter the repo or the logs |
| D26 | 2026-11-19 | Running the Release workflow manually is the approval. `publish=false` (default) makes a draft, `publish=true` publishes | Rule: no production release without explicit approval |
