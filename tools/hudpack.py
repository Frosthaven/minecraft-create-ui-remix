"""Builds the Create-HUD resource pack from the redesign pages' file lists (their PNG links): the HUD (with
AppleSkin's overlay sheet), the player's inventory and the Sophisticated Backpacks screens.

    python3 -I tools/hudpack.py <instance minecraft dir>    (with the site served on 127.0.0.1:18777)

All textures are R x (8) the originals; every user scales UVs by the logical size (the GUI sprite atlas, vanilla's
256 x 256 blits, AppleSkin's and Sophisticated Core's explicit 256 x 256), so they draw at the original size.
"""
import base64, json, os, re, subprocess, sys

PAGES = {'': None, 'inventory.html': 10, 'backpack.html': 7}  # page: files it lists (None: the page records it)


def target(name):
    """A page's file name ("path" in minecraft, or "ns: path") to its path in the pack."""
    if name == 'appleskin/icons':
        return 'assets/appleskin/textures/icons.png'
    ns, path = name.split(': ') if ': ' in name else ('minecraft', name)
    if name.startswith('hud/'):
        path = 'sprites/' + path
    return f'assets/{ns}/textures/gui/{path}.png'


mc = sys.argv[1]
pack = os.path.join(mc, 'resourcepacks', 'Create-HUD')
files = []
for page, n in PAGES.items():
    dom = subprocess.run(['google-chrome-stable', '--headless', '--incognito', '--disable-gpu', '--virtual-time-budget=20000',
                          '--dump-dom', 'http://127.0.0.1:18777/redesign/' + page], capture_output=True, text=True, timeout=120).stdout
    found = re.findall(r'<b>([^<]+)</b>[^<]*?<a download="[^"]*" href="data:image/png;base64,([^"]+)"', dom)
    m = re.search(r'id="sheet" data-count="(\d+)"', dom)
    n = n if n is not None else (int(m.group(1)) if m else -1)
    if len(found) != n:
        sys.exit(f'{page or "index"}: expected {n} files, got {len(found)} (is the site served?)')
    files += found
for name, b64 in files:
    out = os.path.join(pack, target(name))
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, 'wb') as f:
        f.write(base64.b64decode(b64))

with open(os.path.join(pack, 'pack.mcmeta'), 'w') as f:
    json.dump({'pack': {'pack_format': 34, 'description': 'Create GUI: dark steampunk HUD, inventory and backpacks'}}, f, indent=2)
print(f'{len(files)} files in {pack}')
for name, _ in files:
    print(' ', target(name))
