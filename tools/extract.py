# Collects every GUI texture (assets/<ns>/textures/gui/**.png and other screen folders) from the pack's mod jars (and jars
# inside them), vanilla Minecraft, and the active resource packs (later packs override earlier
# ones, like the game). Writes site/tex/<id>.png and site/manifest.json for index.html.
import io, json, os, re, sys, zipfile

inst, mc_jar, out = sys.argv[1], sys.argv[2], sys.argv[3]
mods_dir = os.path.join(inst, "mods")
tex_dir = os.path.join(out, "tex")
os.makedirs(tex_dir, exist_ok=True)
# Screen art: textures/gui (most mods and vanilla), and the other folders mods use for screens
# (Mekanism keeps it in assets/<ns>/gui/). Item icons and map markers are left out.
GUI = re.compile(r"^assets/([^/]+)/((?:textures/(?:gui|guis|hud|screens?|jei|contextmenu|waystones_gui|slot|crosshair)|gui)/.+"
                 r"|textures/font/ascii|textures/icons|textures/item/empty_[a-z_]+|textures/item/trashcan|textures/mob_effect/[a-z_]+"
                 r"|textures/entity/zombie/zombie|textures/block/grass_block_top)\.png$")  # the GUI font, for the mockups;
                                                                         # AppleSkin's HUD overlay sheet; 1.21.1's
                                                                         # empty armor and shield slot icons

def mod_name(z):
    try:
        toml = z.read("META-INF/neoforge.mods.toml").decode("utf8", "replace")
    except KeyError:
        return None
    m = re.search(r"""displayName\s*=\s*(["'])(.+?)\1""", toml)
    return m.group(2) if m else None

entries = {}  # (ns, path) -> entry
owner = {}    # ns -> mod display name

def take(z, mod, source):
    for n in z.namelist():
        m = GUI.match(n)
        if not m:
            continue
        ns, path = m.group(1), re.sub(r"^textures/gui/", "", m.group(2))
        if mod is None and path.startswith("textures/entity/"):
            continue   # packs like Fresh Animations remap entity skins for their own models: keep vanilla's layout
        meta = None
        try:
            meta = json.loads(z.read(n + ".mcmeta").decode("utf8", "replace"))
        except (KeyError, ValueError):
            pass
        key = (ns, path)
        if mod and ns not in owner:
            owner[ns] = mod
        entries[key] = {"ns": ns, "path": path, "data": z.read(n), "meta": meta, "source": source,
                        "mod": entries.get(key, {}).get("mod") or mod}

class FolderPack:
    """An unzipped resource pack, read like a ZipFile (namelist, read)."""
    def __init__(self, root):
        self.root = root
    def namelist(self):
        return [os.path.relpath(os.path.join(d, f), self.root).replace(os.sep, "/")
                for d, _, files in os.walk(self.root) for f in files]
    def read(self, name):
        try:
            return open(os.path.join(self.root, name), "rb").read()
        except FileNotFoundError:
            raise KeyError(name)

def walk_jar(data, label, depth=0):
    try:
        z = zipfile.ZipFile(io.BytesIO(data))
    except zipfile.BadZipFile:
        return
    name = mod_name(z) or label
    take(z, name, "mod")
    if depth < 2:
        for n in z.namelist():
            if n.endswith(".jar"):
                walk_jar(z.read(n), os.path.basename(n)[:-4], depth + 1)

# Vanilla first, then mods, then resource packs in load order (last wins).
walk_jar(open(mc_jar, "rb").read(), "Minecraft")
for ns in ("minecraft", "realms"):
    owner[ns] = "Minecraft"
for jar in sorted(os.listdir(mods_dir)):
    if jar.endswith(".jar"):
        walk_jar(open(os.path.join(mods_dir, jar), "rb").read(), jar[:-4])

opts = open(os.path.join(inst, "options.txt"), encoding="utf8", errors="replace").read()
packs = json.loads(re.search(r"^resourcePacks:(.*)$", opts, re.M).group(1))
for p in packs:
    # haven-create-hud is this repo's own output (the pages' "after"): leave it out so "before" stays the game as it was.
    if not p.startswith("file/") or p == "file/haven-create-hud":
        continue
    path = os.path.join(inst, "resourcepacks", p[5:])
    label = re.sub(r"§.", "", p[5:]).removesuffix(".zip")
    if os.path.isfile(path):
        take(zipfile.ZipFile(path), None, "pack:" + label)
    elif os.path.isdir(path):
        take(FolderPack(path), None, "pack:" + label)

manifest = []
for i, ((ns, path), e) in enumerate(sorted(entries.items())):
    file = f"{i}.png"
    with open(os.path.join(tex_dir, file), "wb") as f:
        f.write(e["data"])
    scaling = (e["meta"] or {}).get("gui", {}).get("scaling") if isinstance(e["meta"], dict) else None
    manifest.append({"mod": owner.get(ns) or e["mod"] or ns, "ns": ns, "path": path, "file": file,
                     "scaling": scaling, "source": e["source"]})
json.dump(manifest, open(os.path.join(out, "manifest.json"), "w"))
mods = {}
for m in manifest:
    mods[m["mod"]] = mods.get(m["mod"], 0) + 1
print(len(manifest), "textures,", len(mods), "mods;", sum(1 for m in manifest if m["scaling"]), "with scaling;",
      sum(1 for m in manifest if m["source"].startswith("pack")), "from packs")
