#!/usr/bin/env bash
# Builds the mod and copies it and the resource packs into the Prism instance.
# Packs are copied, not linked: Minecraft refuses symlinked packs without allowed_symlinks.txt.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
instance="${1:-$HOME/.local/share/PrismLauncher/instances/Create Ultimate Selection 2/minecraft}"

(cd "$here/mod" && JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}" ./gradlew build -q --no-configuration-cache)
rm -f "$instance"/mods/createdarkinventory-*.jar
cp "$here"/mod/build/libs/createdarkinventory-*.jar "$instance/mods/"
for pack in "$here"/resourcepacks/*/; do
  name="$(basename "$pack")"
  # Never overwrite work done in the instance copy: copy it into the repo first.
  if [ -d "$instance/resourcepacks/$name" ] && ! diff -rq "$instance/resourcepacks/$name" "$pack" >/dev/null; then
    echo "resourcepacks/$name in the instance differs from the repo; not overwriting it." >&2
    echo "Copy your changes into $pack (or delete the instance copy), then run this again." >&2
    exit 1
  fi
  rm -rf "$instance/resourcepacks/$name"
  cp -r "$pack" "$instance/resourcepacks/$name"
done
echo "Installed. Restart the game for the mod; Options > Resource Packs > Done reloads the packs."
