# Releasing Noor

## Repositories

| Repository | Visibility | Purpose |
|---|---|---|
| `noor-android` (this code) | private | Source, CI and release workflow |
| `noor-releases` | public | Approved APKs and approved content packs only |

The app reads releases from the repository named in `gradle.properties` under
`noor.releasesRepo`. **Currently this is `Koptan919/Test` itself**, chosen on 2026-11-19.
The workflow publishes there with the built-in token, or to the repository variable
`NOOR_RELEASES_REPO` when it is set (then it needs `NOOR_RELEASES_TOKEN`). App releases use tags
`vX.Y.Z` and content packs use `content-*`. The app ignores content tags when checking for updates.

## One-time setup

1. Create the public `noor-releases` repository.
2. In this repository, go to **Settings → Environments** and create an environment
   called `release`. Add yourself as a **required reviewer**, so no release job
   runs without approval.
3. Add these **secrets** to the `release` environment. Never commit them.
   - `NOOR_KEYSTORE_BASE64`: the release keystore, from `base64 -w0 release.jks`
   - `NOOR_KEYSTORE_PASSWORD`, `NOOR_KEY_ALIAS`, `NOOR_KEY_PASSWORD`
   - `NOOR_RELEASES_TOKEN`: a fine-grained token with *Contents: read & write* on
     `noor-releases` only
4. Keep an offline backup of the keystore. If it is lost, existing users cannot
   update without uninstalling, which deletes their data.

## Releasing an app version

1. Bump `noor.versionCode` and `noor.versionName` in `gradle.properties`, then merge.
2. Go to **Actions → Release → Run workflow**, enter `v<versionName>`, and tick **publish**
   to release straight away. Leave it unticked to create a draft first.
3. After you approve the `release` environment, the workflow:
   - runs the tests;
   - builds the signed APK and verifies it with `apksigner`;
   - writes release notes and a SHA-256 checksum;
   - creates a **draft** release in `noor-releases`.
4. Review the draft on GitHub, then click **Publish**. Only published, non-prerelease
   releases are offered to users.

### How devices update

Once a day on Wi-Fi, the app checks the latest stable release. If there is a newer
version, it downloads `noor-vX.Y.Z.apk` and installs it only after **all** of these
checks pass:

- the APK's SHA-256 matches `noor-vX.Y.Z.apk.sha256`, which the release workflow
  publishes alongside it;
- the package name is Noor's;
- the version code is higher than the installed one;
- it is signed with **the same certificate** as the installed app.

If any check fails, nothing is kept or installed.

Installation uses Android's `PackageInstaller`. The first time, Android asks the user
to allow Noor to install updates. On Android 12 and later, further updates install
without a prompt (`USER_ACTION_NOT_REQUIRED`), because Noor is then the installer of
record. On older versions, the user confirms each update with one tap. User data is
always kept. Users can turn this off in **Settings → Advanced → App updates**.

> Debug builds (`com.noor.app.debug`) and release builds (`com.noor.app`) are separate
> apps. Automatic updates apply to release builds published in `noor-releases`.

## Publishing an approved content pack

Content packs carry verified texts, such as du'as, ziyarat, al-Sahifa al-Sajjadiyya
and Nahj al-Balagha. The format is described in `CONTENT_PACK.md`.

1. Prepare `noor-content-pack.json` from a published edition whose licence allows
   redistribution, and name that edition in `source`.
2. Run `scripts/prepare-content-pack.sh noor-content-pack.json`. This validates the
   pack and writes `noor-content-pack.json.sha256`.
3. Create a release tagged `content-YYYY.MM.DD` in `noor-releases`, attach **both**
   files, and publish it.
4. Devices download the pack on Wi-Fi about once a week, or when the user taps
   *Download verified content*. They verify the checksum and structure before
   installing it. A pack the user imported manually is never overwritten.
