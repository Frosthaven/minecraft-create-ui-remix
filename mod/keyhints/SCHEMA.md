# Key hint block data: target format (one JSON object per line, file convN.jsonl)

{"class": "<fully qualified block class, exactly as in the input>",
 "blocks": ["ns:id" or "ns:*_suffix" / "ns:prefix_*" patterns; [] if unknown: the class is then matched by instanceof],
 "skip": null | "none" | "trap",          // none: no hand action; trap: never hint (e.g. SecurityCraft mines)
 "rows": [ {
    "label": "Title-case short words",    // STYLE: as short as possible, a verb first. Use this vocabulary where it fits:
                                          // Open, Take, Place, Deposit, Deposit all, Withdraw, Fill bucket, Empty bucket,
                                          // Fill bottle, Insert, Remove, Sit, Sleep, Turn, Turn back, Dye, Encase, Wax,
                                          // Scrape, Apply material, Remove material, Rotate, Toggle, Light, Configure,
                                          // Assemble, Disassemble, Lock, Unlock, Cut, Eat, Ring, Copy settings, Paste settings
    "sneak": false | true,                // true: sneak + use (then hand must be "empty": sneaking with an item skips block use)
    "hand": {"type": "empty" | "any" | "nonempty" | "item" | "tag" | "class" | "block_item" | "same_block" | "not_same_block" | "fluid_container",
             "values": [ ... ]},          // item: ["ns:id",...]; tag: ["ns:path"] (item tag, no #); class: ["fqcn"] (item class,
                                          // instanceof); others: []
    "when": [ ... ]                       // all must hold; [] = always. Allowed entries only:
                                          // {"face": "up"|"down"|"side"|"front"}  (front = the block's facing property side)
                                          // {"state": {"<property>": "<value>"}}   (block state property equals)
                                          // {"has_items": true|false}              (its block entity holds any item)
                                          // {"has_fluid": true|false}              (its block entity holds any fluid)
 } ]
}

Rules:
- One output line per input line; keep the input's class. Keep every action whose condition is fully expressible with the
  allowed "when" entries and "hand" types. DROP an action (do not write it) when its condition or hand cannot be expressed
  exactly (owner-only, server config, multiblock formed, needs power, cooldowns, depends on another block, creative only).
- "screen" kind -> one row {"label":"Open","sneak":false,"hand":{"type":"any","values":[]},"when":[]} plus any extra actions.
- "none" kind -> "skip":"none", rows []. A trap (e.g. "trap, do not hint") -> "skip":"trap", rows [].
- "unclear" -> rows [] and skip null.
- Hand "any (not X)" -> "any" ONLY if X is something the block also handles in another row; else drop the action.
- Do not invent: use only what the input line's actions/evidence say; if unsure, drop the action.
