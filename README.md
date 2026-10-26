# Kush Acres

A pixel-art isometric farming game for Android (Kotlin + Jetpack Compose). Grow strains, cook goods,
fill orders and grow the farm.

## Install / update
Download `KushAcres.apk` from the [latest release](https://github.com/M4S4T0-V01D/kush-acres/releases/latest).
After that the game updates itself: it checks the latest GitHub release on launch (and from
Farm Book → Settings → Check) and installs the new APK through Android's installer.

## Build
`./build.sh` (release APK → `KushAcres.apk`), `./build.sh testDebugUnitTest` for tests.
Releases: bump `version.properties`, then `./release.sh "notes"`.

Every release must be signed with the same key or Android refuses the update.

See [PLAN.md](PLAN.md) for the roadmap and progress.
