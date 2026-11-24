"""For every block class in the data with rows: does its own use code (useItemOn / useWithoutItem) test creative mode
(Player.isCreative, Abilities.instabuild / getAbilities)? Reads class files from the jars (no code run) via javap."""
import json, os, subprocess, sys, zipfile, io, tempfile
MODS, DATA = sys.argv[1], sys.argv[2]
want = {e['class'] for e in json.load(open(DATA)) if e['rows']}
found = {}
def scan(z):
    for e in z.namelist():
        if e.endswith('.jar'):
            try: scan(zipfile.ZipFile(io.BytesIO(z.read(e))))
            except Exception: pass
        elif e.endswith('.class'):
            c = e[:-6].replace('/', '.')
            if c in want and c not in found: found[c] = z.read(e)
for f in os.listdir(MODS):
    if f.endswith('.jar'):
        try: scan(zipfile.ZipFile(os.path.join(MODS, f)))
        except Exception: pass
hits = []
with tempfile.TemporaryDirectory() as t:
    for c, data in found.items():
        p = os.path.join(t, 'X.class'); open(p, 'wb').write(data)
        out = subprocess.run(['javap', '-c', '-p', p], capture_output=True, text=True).stdout
        body, keep = [], False
        for line in out.split('\n'):
            if ' useItemOn(' in line or ' useWithoutItem(' in line: keep = True
            elif keep and line.startswith('  ') and not line.startswith('    ') and '(' in line: keep = False
            if keep: body.append(line)
        txt = '\n'.join(body)
        if 'isCreative' in txt or 'instabuild' in txt or 'getAbilities' in txt: hits.append(c)
print(len(found), 'classes read;', len(hits), 'test creative mode:'); print('\n'.join(sorted(hits)))
