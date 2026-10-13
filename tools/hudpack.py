"""Builds the Create-HUD resource pack from the redesign pages' file lists (their PNG links): the compact action bar's
textures, the player's inventory and the Sophisticated Backpacks screens.

    python3 -I tools/hudpack.py    (with the site served on 127.0.0.1:18777)

Writes the repo's resourcepacks/Create-HUD from scratch (the pages are the source; nothing is edited by hand there).
./install.sh copies it into the instance, tools/release.sh into the release zip.

All textures are R x (8) the originals; every user scales UVs by the logical size (the GUI sprite atlas, vanilla's
256 x 256 blits, AppleSkin's and Sophisticated Core's explicit 256 x 256), so they draw at the original size.
"""
import base64, json, os, re, shutil, subprocess, sys

PAGES = {'hud-compact.html': 5, 'inventory.html': 38, 'backpack.html': 15}  # page: files it lists (with the _grey layers)
PORT = os.environ.get('HUDPACK_PORT', '18777')   # another port for a second checkout's server
# The HUD page ('') is not packed: the compact action bar (createhud:textures/gui/compact/*, drawn by the mod) replaces
# the HUD, and vanilla's own HUD sprites stay untouched, so other mods that reuse them (mob health hearts) look vanilla.


def target(name):
    """A page's file name ("path" in minecraft, or "ns: path") to its path in the pack."""
    if name == 'appleskin/icons':
        return 'assets/appleskin/textures/icons.png'
    ns, path = name.split(': ') if ': ' in name else ('minecraft', name)
    if name.startswith('hud/'):
        path = 'sprites/' + path
    if path.startswith(('item/', 'font/', 'slot/')):   # item atlas textures (empty armor and Curios slot icons), font glyphs
        return f'assets/{ns}/textures/{path}.png'
    return f'assets/{ns}/textures/gui/{path}.png'


pack = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'resourcepacks', 'Create-HUD')
files = []
for page, n in PAGES.items():
    dom = subprocess.run(['google-chrome-stable', '--headless', '--incognito', '--disable-gpu', '--virtual-time-budget=20000',
                          '--dump-dom', f'http://127.0.0.1:{PORT}/redesign/' + page], capture_output=True, text=True, timeout=120).stdout
    found = re.findall(r'<b>([^<]+)</b>[^<]*?<a download="[^"]*" href="data:image/png;base64,([^"]+)"', dom)
    m = re.search(r'id="sheet" data-count="(\d+)"', dom)
    n = n if n is not None else (int(m.group(1)) if m else -1)
    if len(found) != n:
        sys.exit(f'{page or "index"}: expected {n} files, got {len(found)} (is the site served?)')
    files += found
shutil.rmtree(os.path.join(pack, 'assets'), ignore_errors=True)
for name, b64 in files:
    out = os.path.join(pack, target(name))
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, 'wb') as f:
        f.write(base64.b64decode(b64))

# The search box's hint (Sophisticated Core writes U+1F50D): our glyph, merged into the default font by a bitmap provider.
os.makedirs(os.path.join(pack, 'assets/minecraft/font'), exist_ok=True)
with open(os.path.join(pack, 'assets/minecraft/font/default.json'), 'w', encoding='utf-8') as f:
    json.dump({'providers': [{'type': 'bitmap', 'file': 'createhud:font/search.png', 'ascent': 7, 'height': 8, 'chars': ['\U0001F50D']}]}, f, ensure_ascii=False, indent=2)
with open(os.path.join(pack, 'pack.mcmeta'), 'w') as f:
    json.dump({'pack': {'pack_format': 34, 'description': 'Create GUI: dark steampunk HUD, inventory and backpacks'}}, f, indent=2)
print(f'{len(files)} files in {pack}')
for name, _ in files:
    print(' ', target(name))
