#!/usr/bin/env bash
# Builds dist/create-ui-remix.zip: the mods and resource packs a player needs, plus install steps.
# Run locally or by .github/workflows/release.yml, which publishes it on every push to main.
set -euo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
# Dark Mode Everywhere (MIT, Buuz135) from Modrinth, pinned to the version the mod is built against.
DME_URL="https://cdn.modrinth.com/data/k3lrwGqk/versions/PEHRE9ut/darkmodeeverywhere-neoforge-1.21.1-1.4.0.jar"
DME_SHA512="$(cat "$here/tools/dme.sha512")"

work="$(mktemp -d)"; trap 'rm -rf "$work"' EXIT
out="$work/create-ui-remix"; mkdir -p "$out/mods" "$out/resourcepacks"

(cd "$here/mod" && JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}" ./gradlew build -q --no-configuration-cache)
cp "$here"/mod/build/libs/createdarkinventory-*.jar "$out/mods/"

dme="$out/mods/$(basename "$DME_URL")"
curl -fsSL "$DME_URL" -o "$dme"
echo "$DME_SHA512  $dme" | sha512sum -c --quiet
unzip -p "$dme" "LICENSE_Dark Mode Everywhere" > "$out/mods/LICENSE-DarkModeEverywhere.txt"

for pack in "$here"/resourcepacks/*/; do
  name="$(basename "$pack")"
  (cd "$pack" && zip -qrX "$out/resourcepacks/$name.zip" .)
done
cp "$here/tools/INSTALL.txt" "$out/INSTALL.txt"

mkdir -p "$here/dist"; rm -f "$here/dist/create-ui-remix.zip"
(cd "$work" && zip -qrX "$here/dist/create-ui-remix.zip" create-ui-remix)
echo "$here/dist/create-ui-remix.zip"
