"""Lists every Block subclass (by class-file heuristics) in the pack's jars that declares its own useWithoutItem / useItemOn
(Mojang names at runtime in NeoForge), with the jar. Reads class files' constant pools only: no code runs."""
import os, sys, zipfile, struct, io
MODS = sys.argv[1]; OUT = sys.argv[2]
def methods(data):
    # minimal class parser: constant pool, then method names
    b = io.BytesIO(data); b.read(8)
    n = struct.unpack('>H', b.read(2))[0]; cp = [None]*n; i = 1
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
    b.read(2); this = struct.unpack('>H', b.read(2))[0]; sup = struct.unpack('>H', b.read(2))[0]
    ic = struct.unpack('>H', b.read(2))[0]; b.read(2*ic)
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
rows = []
def scan(z, jar):
    for e in z.namelist():
        if e.endswith('.jar'):
            try: scan(zipfile.ZipFile(io.BytesIO(z.read(e))), jar + '!' + e.split('/')[-1])
            except Exception: pass
        if not e.endswith('.class') or '$' in e: continue
        try: names, cp = methods(z.read(e))
        except Exception: continue
        if not names: continue
        if ('useWithoutItem' in names or 'useItemOn' in names) and any(isinstance(c,str) and 'BlockState' in c for c in cp):
            rows.append((jar, e[:-6].replace('/', '.'), 'W' if 'useWithoutItem' in names else '', 'I' if 'useItemOn' in names else ''))
for f in sorted(os.listdir(MODS)):
    if f.endswith('.jar'):
        try: scan(zipfile.ZipFile(os.path.join(MODS, f)), f)
        except Exception: pass
with open(OUT, 'w') as o:
    for r in rows: o.write('\t'.join(r) + '\n')
print(len(rows), 'block classes with their own use code')
