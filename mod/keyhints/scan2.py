"""Scans the pack's jars (and nested jars) by class-file constant pools, no code run:
- entities: classes that declare mobInteract / interact / interactAt and reference Entity types
- items: classes that declare use / useOn / interactLivingEntity and reference Item types
- events: classes that reference PlayerInteractEvent$RightClickBlock / $EntityInteract / $EntityInteractSpecific / $RightClickItem
Writes entities.tsv, items.tsv, events.tsv: jar, class, methods or event names."""
import os, sys, zipfile, struct, io
MODS, OUT = sys.argv[1], sys.argv[2]
EV = ['PlayerInteractEvent$RightClickBlock', 'PlayerInteractEvent$EntityInteractSpecific', 'PlayerInteractEvent$EntityInteract', 'PlayerInteractEvent$RightClickItem', 'UseBlockCallback', 'UseEntityCallback', 'UseItemCallback']
def parse(data):
    b = io.BytesIO(data); b.read(8); n = struct.unpack('>H', b.read(2))[0]; cp = [None]*n; i = 1
    while i < n:
        t = b.read(1)[0]
        if t == 1: l = struct.unpack('>H', b.read(2))[0]; cp[i] = b.read(l).decode('utf-8','replace')
        elif t in (3,4): b.read(4)
        elif t in (5,6): b.read(8); i += 1
        elif t in (7,8,16,19,20): b.read(2)
        elif t in (9,10,11,12,17,18): b.read(4)
        elif t == 15: b.read(3)
        else: return None, None
        i += 1
    b.read(6); ic = struct.unpack('>H', b.read(2))[0]; b.read(2*ic)
    fc = struct.unpack('>H', b.read(2))[0]
    for _ in range(fc):
        b.read(6); ac = struct.unpack('>H', b.read(2))[0]
        for _ in range(ac): b.read(2); l = struct.unpack('>I', b.read(4))[0]; b.read(l)
    mc = struct.unpack('>H', b.read(2))[0]; names = []
    for _ in range(mc):
        b.read(2); nm = struct.unpack('>H', b.read(2))[0]; b.read(2); names.append(cp[nm])
        ac = struct.unpack('>H', b.read(2))[0]
        for _ in range(ac): b.read(2); l = struct.unpack('>I', b.read(4))[0]; b.read(l)
    return names, cp
ent, itm, evt = [], [], []
def scan(z, jar):
    for e in z.namelist():
        if e.endswith('.jar'):
            try: scan(zipfile.ZipFile(io.BytesIO(z.read(e))), jar + '!' + e.split('/')[-1])
            except Exception: pass
            continue
        if not e.endswith('.class') or e.startswith('net/minecraft/'): continue
        try: names, cp = parse(z.read(e))
        except Exception: continue
        if not names: continue
        strs = [c for c in cp if isinstance(c, str)]
        cls = e[:-6].replace('/', '.')
        m = sorted(set(names) & {'mobInteract', 'interact', 'interactAt'})
        if m and any('world/entity' in s for s in strs) and '$' not in e: ent.append((jar, cls, ','.join(m)))
        m = sorted(set(names) & {'use', 'useOn', 'interactLivingEntity'})
        if m and any('world/item/Item' in s for s in strs) and any('InteractionResult' in s for s in strs) and '$' not in e: itm.append((jar, cls, ','.join(m)))
        ev = [x for x in EV if any(x in s for s in strs)]
        if ev: evt.append((jar, cls, ','.join(x.split('$')[-1] for x in ev)))
for f in sorted(os.listdir(MODS)):
    if f.endswith('.jar'):
        try: scan(zipfile.ZipFile(os.path.join(MODS, f)), f)
        except Exception: pass
for name, rows in (('entities', ent), ('items', itm), ('events', evt)):
    with open(os.path.join(OUT, name + '.tsv'), 'w') as o:
        for r in rows: o.write('\t'.join(r) + '\n')
    print(name, len(rows))
