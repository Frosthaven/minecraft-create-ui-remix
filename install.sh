#!/usr/bin/env bash
# Builds the mod and copies it and the resource pack into the Prism instance.
# The pack is copied, not linked: Minecraft refuses symlinked packs without allowed_symlinks.txt.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
instance="${1:-$HOME/.local/share/PrismLauncher/instances/Create Ultimate Selection 2/minecraft}"

(cd "$here/mod" && JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}" ./gradlew build -q --no-configuration-cache)
rm -f "$instance"/mods/createdarkinventory-*.jar
cp "$here"/mod/build/libs/createdarkinventory-*.jar "$instance/mods/"
rm -rf "$instance/resourcepacks/DarkModeEverywhere-Veil-Fix"
cp -r "$here/resourcepack" "$instance/resourcepacks/DarkModeEverywhere-Veil-Fix"
echo "Installed. Restart the game for the mod; Options > Resource Packs > Done reloads the pack."
