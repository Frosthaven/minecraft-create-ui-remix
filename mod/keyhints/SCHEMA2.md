# Key hint data for entities, items and event handlers (one JSON object per line)

Same row shape and label style as SCHEMA.md (read it: labels, "sneak" needs hand "empty" when it means sneak + use on a
block or entity, the hand types, dropping anything not exactly expressible). Additions:

## Entities (from an entity class's own mobInteract / interact / interactAt, or an event handler on entities)
{"kind":"entity", "class":"<entity class fqcn>", "entities":["ns:id", ...] (or [] = match by instanceof),
 "skip": null|"none"|"trap", "source":"<class that holds the code: the entity class or the event handler class>",
 "rows":[{"label":..., "sneak":bool, "hand":{...}, "when":[...]}]}
Allowed "when" for entities: {"owner": true|false} (OwnableEntity.getOwnerUUID equals the player), {"baby": bool},
{"vehicle": bool} (has a passenger), {"tag": "<entity type tag>"}. Nothing else.

## Items (from an item class's own use / useOn / interactLivingEntity, or a RightClickItem event handler)
{"kind":"item", "class":"<item class fqcn>", "items":["ns:id", ...] (or []), "skip":null|"none",
 "source":"...",
 "rows":[{"label":..., "sneak":bool, "target":"air"|"block"|"entity", "on":{...}, "when":[...]}]}
- target "air": using it with no block or entity in reach (Item.use). Shown as a stateful hint while held.
- target "block": useOn. "on": {"type":"any"} or {"type":"block","values":["ns:id"]} or {"type":"block_tag","values":["ns:path"]}
  or {"type":"block_class","values":["fqcn"]}. Shown as a contextual hint when looking at a matching block.
- target "entity": interactLivingEntity. "on": {"type":"any"} or {"type":"entity","values":["ns:id"]} or
  {"type":"entity_class","values":["fqcn"]}.
- "sneak": true here means sneak while using the item (allowed for items).
- Allowed "when" for items: {"has_component":"ns:id"} (the held stack has that data component), {"cooldown": false}
  (not on cooldown), {"flying": bool}, {"on_ground": bool}. Nothing else.
- Food, drink, bows, shields, spyglass, horns, brushes and thrown projectiles are already covered by the item's use
  animation: write "skip":"none" for an item whose ONLY use is one of those vanilla behaviours.

## Event handlers on blocks
{"kind":"block", ...exactly the SCHEMA.md block format..., "source":"<handler class>"}

One input line may produce several output lines (one per target thing). Drop anything you cannot express exactly.
