# Project memory — NOOR

Last verified: **2026-11-19** (auto-update setup session).
Update this file at the end of every meaningful session. Keep it factual.

## 1. What NOOR is

A native Android app for Twelver Shia Muslims: Quran, du'as and ziyarat, Ja'fari prayer
times, Qibla, Tasbih, Hijri calendar with Shia occasions, and a religious library.
It works offline, with no account, ads or tracking. It is bilingual (Arabic and English) with full RTL.

## 2. Repositories and branches (verified facts only)

| Item | Current reality | Intended target |
|---|---|---|
| Development repo | `Koptan919/Test` (**public**) | a private repo `noor-android`, **not created yet** |
| Working branch | `claude/noor-shia-android-app-ja4ek2` | — |
| Release repo | **`Koptan919/Test` itself** (GitHub Releases), chosen by the user on 2026-11-19. `noor.releasesRepo=Koptan919/Test`. App releases are tagged `vX.Y.Z`; content packs are tagged `content-*` | can move to `noor-releases` later (set `noor.releasesRepo` and the `NOOR_RELEASES_REPO` variable) |
| CI | `.github/workflows/ci.yml` runs on every push | — |
| Release workflow | `.github/workflows/release.yml`, manual (`version`, `publish`). With `publish` false it makes a draft; with true it publishes. Uses `github.token` for the same repo | see `RELEASING.md` |

- Creating the two repos requires user approval. Do not invent other URLs.
- **Release signing key** (created 2026-11-19, RSA 4096, PKCS12, alias `noor`, valid until 2054).
  The certificate SHA-256 is `FE:16:7D:F7:71:34:FA:5E:BE:AE:C2:3C:28:FB:AA:7D:4A:22:0E:01:98:05:E4:44:31:52:61:C2:A5:03:8B:40`.
  The key file and secret values were sent **only to the user**, then deleted from the sandbox. They are not in the repo.
  Every future release must be signed with this key, or installed apps reject the update.
- Secrets needed by `release.yml`: `NOOR_KEYSTORE_BASE64`, `NOOR_KEYSTORE_PASSWORD`, `NOOR_KEY_ALIAS`,
  `NOOR_KEY_PASSWORD` (`NOOR_RELEASES_TOKEN` only for a different release repo).
  **As of 2026-11-19 the user has not confirmed adding them.** Never store their values here.
- `downloads/noor-v0.2.1-debug.apk` is committed on the branch for direct download. It is a debug
  build, signed with the session's debug key, package `com.noor.app.debug`.

## 3. Current version

`0.2.2`, versionCode `4` (in `gradle.properties`). Debug builds use the application ID suffix `.debug`.
The first official release (`v0.2.2`, package `com.noor.app`) has **not been published yet**.

## 4. Latest verified results (2026-11-17/18)

- Local `:core:test`: **15/15 passed** (ExtrasTest 3, HijriTest 4, PrayerTimesTest 4, QuranTest 4).
- Local `:app:testDebugUnitTest`: **11/11 passed** (AppUpdaterTest 3, DataTest 3, JourneyArabicTest 1,
  JourneyEnglishDarkTest 1, SmallPhoneLightTest 1, TabletTest 1, UpdateDialogTest 1).
- Local `:app:lintDebug`: **0 errors** (warnings: dependency versions, 4 unused resources, 1 `ApplySharedPref`).
- Local `:app:assembleDebug` and `:app:assembleRelease`: succeeded. A release APK signed with a
  **throwaway** keystore through the env-var signing config passed `apksigner verify` (v2 scheme).
- GitHub Actions CI run `44155104371` (commit `a1c4c21`): **success** for all steps (core tests,
  Robolectric tests, lint, debug APK).
- **Never verified on a real device or emulator.** There was no KVM in the sandbox. The compass sensor,
  notifications, audio playback and self-update installation still need a device test.

## 5. Sandbox and environment notes

- The Android SDK was installed at `/opt/android-sdk` (cmdline-tools, platform 35, build-tools 35).
  A fresh cloud container may not have it. `dl.google.com` must be reachable.
- Robolectric with `@GraphicsMode(NATIVE)` renders the real screens. Screenshots are captured by drawing
  all window root views (see `TestSupport.shot`), because `captureToImage` times out under Robolectric.
- Robolectric cannot run the platform APK parser (`IllegalAccessError`). That is why the updater test
  asserts fail-closed behaviour instead of a positive signature match.
- Tests set *reduce motion* on and turn off update checks and sync, so they are deterministic and make
  no network calls. Each test class runs in a fresh JVM (`forkEvery = 1`).
- The session's network proxy returns 403 for repositories that aren't attached to the session. That is
  not GitHub's response; real GitHub returns 404 when there are no releases.

## 6. Open issues and next steps

1. **Content (blocked on a verified source):** the texts of Mafatih supplications and ziyarat (Kumayl,
   Tawassul, Nudba, Ahd, Ashura, Warith, …), al-Sahifa al-Sajjadiyya and Nahj al-Balagha are not bundled.
   They need a licensed, verified edition published as a content pack.
2. **First official release:** once the user adds the four secrets, run **Actions → Release** with
   `version=v0.2.2` and `publish=true` (with the user's approval). Users must install the release build
   (`com.noor.app`) once. It is a different app from the debug build (`com.noor.app.debug`), so data does
   not carry over from debug to release. After that, updates install automatically.
3. **Device QA:** test the Qibla sensor, prayer notifications (WorkManager timing in Doze), audio
   playback, and self-update (permission prompt, `USER_ACTION_NOT_REQUIRED` on Android 12+).
4. **Accuracy:** compare prayer times with official Shia timetables (for example the Najaf and Qom offices).
5. **Not started:** Shia forum integration and a formal religious source registry (see FEATURE_STATUS).
6. **Minor:**
   - Audio plays only while the app process lives (there is no foreground media service).
   - The Hijri day changes at midnight, not at Maghrib.
   - Search matches titles only (no full-text search of verses).
7. **Security note:** the development repo is currently **public**. Move it to private when the user decides.
