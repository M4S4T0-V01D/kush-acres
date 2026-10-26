#!/usr/bin/env bash
# Ships the version in version.properties: builds and verifies the APK, commits, tags vX.Y.Z, pushes, and
# publishes a GitHub release with KushAcres.apk attached. The game's updater picks up the latest release.
# Usage: ./release.sh "What's new (shown in the game's update prompt)"
set -euo pipefail
cd "$(dirname "$0")"
notes=${1:?usage: ./release.sh \"release notes\"}
ver=$(grep '^versionName=' version.properties | cut -d= -f2)
tag="v$ver"
if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then echo "$tag already exists: bump version.properties first" >&2; exit 1; fi
./build.sh testDebugUnitTest assembleRelease
git add -A
git diff --cached --quiet || git commit -q -m "Kush Acres $ver" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git tag -a "$tag" -m "Kush Acres $ver"
git push -q origin HEAD "$tag"
gh release create "$tag" KushAcres.apk --title "Kush Acres $ver" --notes "$notes"
