"""Merges keyhints/data*.jsonl (the audit converted to SCHEMA.md) into the mod's resource
src/main/resources/assets/haven_create_ui_ux/key_hints/blocks.json, with three passes: fluid-container rows get a
held_full check and the reader's short words ("Fill or empty"); "any item but the same block" actions the conversion had
to drop come back from audit*.jsonl as not_same_block rows; one entry per class (the one with the most rows)."""
import glob, json, os, re, sys
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'src/main/resources/assets/haven_create_ui_ux/key_hints/blocks.json')
FILL = {'fill bucket', 'withdraw', 'fill bottle', 'fill'}
EMPTY = {'empty bucket', 'deposit', 'pour in', 'empty'}
HANDS = {'empty', 'any', 'nonempty', 'item', 'tag', 'not_tag', 'class', 'block_item', 'same_block', 'not_same_block', 'fluid_container'}
WHEN = {'face', 'state', 'has_items', 'has_fluid', 'held_full', 'creative', 'id'}
by = {}
for f in sorted(glob.glob(os.path.join(HERE, 'data*.jsonl'))):
    for line in open(f):
        o = json.loads(line)
        for r in o['rows']:
            if r['hand']['type'] == 'fluid_container':
                l = r['label'].lower()
                if l in FILL: r['when'].append({'held_full': False}); r['label'] = 'Fill or empty'
                elif l in EMPTY: r['when'].append({'held_full': True}); r['label'] = 'Fill or empty'
        cur = by.get(o['class'])
        if cur is None or len(o['rows']) > len(cur['rows']) or (cur['skip'] is None and o['skip']): by[o['class']] = o
# "any (not the same ...)" actions back as not_same_block (only with no condition)
back = 0
for f in sorted(glob.glob(os.path.join(HERE, 'audit*.jsonl'))):
    for line in open(f):
        a = json.loads(line)
        o = by.get(a['class'])
        if not o or o['skip']: continue
        for act in a.get('actions', []):
            h = act.get('hand', '')
            if re.search(r'not (the )?same', h) and not act.get('condition') and act.get('keys') == 'use':
                if not any(r['label'] == act['label'] for r in o['rows']):
                    o['rows'].append({'label': act['label'], 'sneak': False, 'hand': {'type': 'not_same_block', 'values': []}, 'when': []}); back += 1
# Block records from the entity, item and event audits (a handler or an old-style use method on a block)
for f in sorted(glob.glob(os.path.join(HERE, 'entities*.jsonl')) + glob.glob(os.path.join(HERE, 'items*.jsonl')) + glob.glob(os.path.join(HERE, 'events*.jsonl'))):
    for line in open(f):
        o = json.loads(line)
        if o.get('kind') != 'block': continue
        cur = by.get(o['class'])
        if cur is None: by[o['class']] = {'class': o['class'], 'blocks': o.get('blocks', []), 'skip': o.get('skip'), 'rows': o.get('rows', [])}
        elif cur['skip']: continue
        elif o.get('blocks') and set(o['blocks']) < set(cur['blocks'] or o['blocks'] + ['*']):
            # a record for some blocks of the class (one valve handle colour): its own entry, its rows win by label over
            # the class's, and those blocks leave the class entry (ids match before classes in KeyData)
            labels = {r['label'] for r in o['rows']}
            rows_ = o['rows'] + [r for r in cur['rows'] if r['label'] not in labels]
            by[o['class'] + '#' + ','.join(o['blocks'])] = {'class': o['class'], 'blocks': o['blocks'], 'skip': None, 'rows': rows_}
            cur['blocks'] = [b for b in cur['blocks'] if b not in o['blocks']]   # empty: the class entry matches by class only
        else: cur['rows'] += [r for r in o.get('rows', []) if r not in cur['rows']]
entries, rows = [], 0
for o in by.values():
    for r in o['rows']:
        assert r['hand']['type'] in HANDS, r
        assert all(set(w) <= WHEN for w in r['when']), r
        assert not (r['sneak'] and r['hand']['type'] != 'empty'), r
    rows += len(o['rows'])
    entries.append({'class': o['class'], 'blocks': o['blocks'], 'skip': o['skip'], 'rows': o['rows']})
# one entry per block id: ids listed by several entries (a block's class and an event handler, a mixin) get the union of
# their rows (a trap wins; "none" only when every entry says none), ids with equal results share one entry
ids = {}
for e in entries:
    for b in e['blocks']: ids.setdefault(b, []).append(e)
multi = {b for b, es in ids.items() if len(es) > 1}
if multi:
    groups = {}
    for b in sorted(multi):
        es = ids[b]
        skip = 'trap' if any(e['skip'] == 'trap' for e in es) else ('none' if all(e['skip'] == 'none' for e in es) else None)
        rows_ = []
        for e in es:
            if e['skip'] is None: rows_ += [r for r in e['rows'] if r not in rows_]
        key = json.dumps([skip, rows_], sort_keys=True)
        g = groups.setdefault(key, {'class': es[0]['class'], 'blocks': [], 'skip': skip, 'rows': rows_})
        g['blocks'].append(b)
    for e in entries: e['blocks'] = [b for b in e['blocks'] if b not in multi]
    entries = [e for e in entries if e['blocks'] or not any(e is x for es in ids.values() for x in es)] + list(groups.values())
    rows = sum(len(e['rows']) for e in entries)
entries.sort(key=lambda e: e['class'])
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, 'w') as w: json.dump(entries, w, separators=(',', ':'))
print(f'{len(entries)} entries, {rows} rows ({back} not_same_block rows restored), skips {sum(1 for e in entries if e["skip"])} -> {os.path.relpath(OUT, HERE)}')

# Entities and items: entities*.jsonl, items*.jsonl, events*.jsonl (kind entity / item), one entry per class
DROP = {'de.maxhenkel.easyvillagers.events.VillagerEvents'}   # villager pickup: KeyRules uses Easy Villagers' own exact check
def gather(kind, ids_key):
    out = {}
    for f in sorted(glob.glob(os.path.join(HERE, 'entities*.jsonl')) + glob.glob(os.path.join(HERE, 'items*.jsonl')) + glob.glob(os.path.join(HERE, 'events*.jsonl'))):
        for line in open(f):
            o = json.loads(line)
            if o.get('kind', 'item' if 'items' in f else 'entity' if 'entities' in f else None) != kind or o.get('source') in DROP: continue
            cls = o['class']
            # one entry per class AND id set: records of one class with different ids keep their own rows (Waxed Up's
            # coating waxes, its oxidiser oxidises; an empty spool removes wire, a full one connects)
            key = (cls, tuple(sorted(o.get(ids_key, []))))
            cur = out.get(key)
            if cur is None: out[key] = {'class': cls, ids_key: sorted(o.get(ids_key, [])), 'skip': o.get('skip'), 'rows': list(o.get('rows', []))}
            else:
                if o.get('skip') and not cur['rows']: cur['skip'] = o['skip']
                cur['rows'] += [r for r in o.get('rows', []) if r not in cur['rows']]
    return sorted(out.values(), key=lambda e: (e['class'], e[ids_key]))
for kind, key, name in (('entity', 'entities', 'entities.json'), ('item', 'items', 'items.json')):
    es = gather(kind, key)
    with open(os.path.join(os.path.dirname(OUT), name), 'w') as w: json.dump(es, w, separators=(',', ':'))
    print(f'{name}: {len(es)} entries, {sum(len(e["rows"]) for e in es)} rows, skips {sum(1 for e in es if e["skip"])}')
