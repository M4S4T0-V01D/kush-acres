#!/usr/bin/env bash
# Installs the desktop game for this user: the app in ~/.local/lib/kush-acres (where its updater swaps in new
# versions), the icon, and a start-menu entry. Builds first unless given a tarball.
# Usage: desktop/install.sh [KushAcres-linux.tar.gz]
set -euo pipefail
cd "$(dirname "$0")/.."
tarball=${1:-}
if [[ -z $tarball ]]; then ./build.sh :desktop:createDistributable -q; tarball=KushAcres-linux.tar.gz; fi
tarball=$(realpath "$tarball")

data=${XDG_DATA_HOME:-$HOME/.local/share}
dest=$HOME/.local/lib/kush-acres
stage=$(mktemp -d "$HOME/.local/lib/.kush-acres-XXXXXX")
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
