#!/usr/bin/env bash
# Installs the desktop game for this user: the app in ~/.local/lib/kush-acres (where its updater swaps in new
# versions), the icon, and a start-menu entry. Builds first unless given a tarball, or --latest: the Linux build
# attached to the latest GitHub release (what Void Store installs).
# Usage: desktop/install.sh [KushAcres-linux.tar.gz | --latest] | --uninstall   (PREFIX, default ~/.local, picks where it goes)
# Saves live in ~/.local/share/kush-acres and are kept by reinstalls and by --uninstall.
set -euo pipefail
cd "$(dirname "$0")/.."
PREFIX=${PREFIX:-$HOME/.local}
data=$PREFIX/share
dest=$PREFIX/lib/kush-acres
if [[ ${1:-} == --uninstall ]]; then
  rm -rf "$dest"
  rm -f "$data/applications/kush-acres.desktop" "$data/icons/hicolor/256x256/apps/kush-acres.png"
  update-desktop-database -q "$data/applications" 2>/dev/null || true
  kbuildsycoca6 >/dev/null 2>&1 || true
  echo "Kush Acres removed; saves are kept."
  exit 0
fi
tarball=${1:-}
if [[ $tarball == --latest ]]; then
  dl=$(mktemp -d)
  trap 'rm -rf "$dl"' EXIT
  tarball=$dl/KushAcres-linux.tar.gz
  echo "Downloading the latest release from GitHub…"
  curl -fL --retry 2 --no-progress-meter -o "$tarball" https://github.com/M4S4T0-V01D/kush-acres/releases/latest/download/KushAcres-linux.tar.gz
elif [[ -z $tarball ]]; then ./build.sh :desktop:createDistributable -q; tarball=KushAcres-linux.tar.gz; fi
tarball=$(realpath "$tarball")

mkdir -p "$PREFIX/lib"
stage=$(mktemp -d "$PREFIX/lib/.kush-acres-XXXXXX")
tar -xzf "$tarball" -C "$stage"
rm -rf "$dest"
mv "$stage/KushAcres" "$dest"
rmdir "$stage"

mkdir -p "$data/icons/hicolor/256x256/apps" "$data/applications"
cp desktop/src/main/resources/kush-acres.png "$data/icons/hicolor/256x256/apps/kush-acres.png"
cat > "$data/applications/kush-acres.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=Kush Acres
GenericName=Farming game
Comment=Grow, cook and sell on your own pixel farm
Exec=$dest/bin/KushAcres
Icon=kush-acres
Terminal=false
Categories=Game;Simulation;
StartupWMClass=dev-m4s4t0-kushacres-desktop-MainKt
EOF
update-desktop-database -q "$data/applications" 2>/dev/null || true
kbuildsycoca6 >/dev/null 2>&1 || true
echo "Installed to $dest; Kush Acres is in the start menu under Games."
