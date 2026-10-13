# minecraft-create-ui-remix

A designer HUD (and later GUI) overhaul for the Create Ultimate Selection 2 modpack (the Haven
server's pack), themed on Create. Two local web pages:

- **Almanac** (`site/index.html`): every GUI texture in the pack, one tab per mod, reconstructed into
  panels, with the active resource packs applied. The reference for what exists and where.
- **HUD Redesign** (`site/redesign/index.html`): the Create-themed HUD, shown before (current HUD) and
  after (redesign), work in progress.

## Run

```sh
# 1. Extract the pack's GUI textures into site/tex + site/manifest.json (gitignored: mod and pack art).
python3 -I tools/extract.py \
  "$HOME/.local/share/PrismLauncher/instances/Create Ultimate Selection 2/minecraft" \
  "$HOME/.local/share/PrismLauncher/libraries/com/mojang/minecraft/1.21.1/minecraft-1.21.1-client.jar" \
  site
# 2. Serve (fetch() needs http, not file://) and open http://127.0.0.1:18777/ and /redesign/.
cd site && python3 -I -m http.server 18777 --bind 127.0.0.1
```

Re-run step 1 after changing mods or resource packs. Both pages hot reload on save (`site/live.js` polls
the page's `Last-Modified`, on localhost only).

## Extractor (`tools/extract.py`)

- Sources, in load order (later wins, like the game):
  1. vanilla client jar;
  2. every mod jar in the instance's `mods/` (and jars nested inside them, two levels);
  3. the active resource packs from `options.txt` (`resourcePacks`, `file/...` entries, zips or folders), except
     `Create-HUD` (our own output: "before" must stay the game without it).
- Paths taken (plus `assets/minecraft/textures/font/ascii.png`, the GUI font, for the mockups):
  - `assets/<ns>/textures/{gui,guis,hud,screen,screens,jei,contextmenu,waystones_gui,slot,crosshair}/**`;
  - `assets/<ns>/gui/**` (Mekanism keeps its screen art there).
  - A leading `textures/gui/` is dropped from the stored path, so vanilla HUD sprites are `sprites/hud/...`.
- Tab name: the owning mod's `displayName` from `META-INF/neoforge.mods.toml` (single or double quoted), by namespace.
- `.png.mcmeta` `gui.scaling` (nine_slice, tile, stretch) goes into the manifest.
- Output: `site/tex/<n>.png` and `site/manifest.json` with entries `{mod, ns, path, file, scaling, source}`.
  `source` is `mod` or `pack:<name>`.
- Numbers at the last run: 3134 textures, 136 mods, 97 with scaling, 403 from packs.

## Almanac (`site/index.html`)

- A tab per mod plus a **HUD** tab, with a search over mods and texture names. The last tab is kept in
  localStorage (`gui-preview-tab`).
- Reconstruction (in the browser, canvas):
  - **nine_slice:** corners kept, edges and center tiled (as 1.21 draws them), shown larger.
  - **tile:** repeated 3x2.
  - **stretch:** scaled 2x.
  - **Sheets** (a panel plus loose parts on one image, transparent between): 8-connected opaque
    regions. The panel is the region with the first opaque pixel, or the biggest one. It is cropped,
    and the other parts are shown below it.
  - Images under 64 px, or whose main region is under 12 % of the image, are shown whole
    (strokes of one drawing, not a sheet).
- Seams (where pieces join) get faint dotted lines (`--seam`).
- **HUD** tab: the vanilla HUD sprites assembled at the game's positions (see "Layout" below), with
  sample values.
- Known gaps:
  - Mods that draw screens in code (Mekanism machines) show only their pieces.
  - Some mods use shared libraries: Sophisticated Backpacks' panels are under **Sophisticated Core**.
- Resource packs active at extraction: Vanilla Experience+ overrides the whole HUD, and Colourful
  Containers recolors the vanilla containers.

## HUD Redesign (`site/redesign/index.html`), work in progress

- Before (current HUD from the almanac's manifest, crisp pixels) and after (redesign), side by side,
  plus a sprite list with each sprite before and after and a PNG download of the redesign.
- Controls:
  - **Show sockets:** dotted outlines on item sockets and icon cells.
  - **Damage** and **Eat:** cycle the sample values.
  - Keys **1-9:** select a slot.
- The redesign is vector art (canvas paths and gradients) drawn at `R = 8` x the vanilla size.
  `hires(w, h)` gives a canvas whose context works in logical units. Minecraft draws GUI sprites at
  their logical size whatever the texture resolution, so a high-resolution pack keeps the layout.
  It looks best at GUI scale 2 or more.
- Sprite code is in `site/redesign/sprites.js`, material textures (noise grain, tarnish, brushing) in
  `material.js`; the page assembles and compares. Stylized with some material, not photoreal.
- Theme: moody dark steampunk with strong contrast. Value plan: near-black iron bodies, mid bronze,
  crisp bright brass edges where light hits, and the brightest values only in things that give light.
  - Trim is industrial steel with cogwheels: blackened iron hotbar plate in a brushed steel frame with
    gear-rack teeth, brass socket bezels around black wells, steel hex bolts.
  - Selected slot: steel frame, ember halo, an amber indicator lamp.
  - Offhand: iron bracket in a steel frame, a steel shaft into the hotbar with a bolted brass coupling.
  - Art cannot go outside a sprite's box (the hotbar is always 182x22), so flourishes stay inside it.
  - XP: steel pipe, glass window, glowing liquid experience (Create Enchantment Industry), bolted steel
    clamp bands, brass end caps.
  - Hearts: glowing blood vials in iron frames. Food: roast drumsticks; `food_half` fills the right
    side (the food row empties from the left).
  - Armor: heater shields, a thick brass rim around a steel field with a brass cog. Air: brass diving
    portholes; bursting: cracked glass.
- Sprites done (vanilla names under `assets/minecraft/textures/gui/sprites/`), each at its vanilla
  logical size:
  - `hud/hotbar` 182x22, `hud/hotbar_selection` 24x23, `hud/hotbar_offhand_left` 29x24;
  - `hud/experience_bar_background` 182x5, `hud/experience_bar_progress` 182x5;
  - `hud/heart/container`, `hud/heart/full`, `hud/heart/half` (9x9);
  - `hud/food_empty`, `hud/food_full`, `hud/food_half` (9x9);
  - `hud/armor_full`, `hud/armor_half`, `hud/armor_empty` (9x9);
  - `hud/air`, `hud/air_bursting` (9x9).
- Not done yet (all exist in vanilla, see the almanac's HUD and Minecraft tabs):
  - `hotbar_offhand_right`, crosshair and attack indicators, `jump_bar_*`;
  - the other heart states (hardcore, poisoned, withered, frozen, absorbing, vehicle, blinking);
  - `food_*_hunger`, `effect_background*`.

### Layout (Gui.java, 1.21): keep these exact

All positions are logical pixels; `cx` is the screen center and `bottom` the screen height.

| Element | Position |
| --- | --- |
| Hotbar | `(cx - 91, bottom - 22)`; item `i` at `(cx - 91 + 3 + 20 i, bottom - 19)`, 16x16 |
| Selection | `(cx - 91 - 1 + 20 slot, bottom - 23)` |
| Offhand (left) | `(cx - 91 - 29, bottom - 23)`; item at `(cx - 91 - 26, bottom - 19)` |
| XP bar | `(cx - 91, bottom - 29)`; the progress sprite is drawn cropped to the fill width |
| Hearts | `(cx - 91 + 8 i, bottom - 39)`, 9x9 cells 8 px apart; container first, full or half over it |
| Armor | one row above the hearts (`bottom - 49` with one health row) |
| Food | `(cx + 91 - 8 i - 9, bottom - 39)`, right to left |
| Air | one row above the food |

## Compact HUD concept (`site/redesign/hud-compact.html`, `hud-compact.js`): preview only, not packed

One iron action bar instead of the floating rows: hotbar in the raised middle with the XP pipe on its top edge and a
level badge; left wing offhand, health (absorption in gold) with armor plating on top, Iron's mana; right wing food
with AppleSkin saturation on top, Ars mana, dodge roll charges; air rises out of the right wing under water. Shipping
it means Create Dark Inventory cancels the vanilla and mod HUD layers and draws this layout (sprites from the pack).
- Meters: health, food (left wing, vitals); Iron's mana, Ars mana (right wing, magic). Armor and breath show only when
  the game shows them (armor worn; under water), extending the raised hotbar band
  sideways over the left and right wing (same height, darker iron), as slim pipes level with the XP pipe. Breath is the vanilla bubbles;
  a Create backtank adds its air on top in copper (like absorption on health), drained first, with the tank's time left
  (m:ss, as Create's `remaining_air` layer) always on the meter while it has air: players rely on it.
- Numbers (value bright, "/max" dim, the liquid dimmed behind them): on the page by hovering a meter. In game (decided):
  while a key is held, Left Alt by default, a KeyMapping so it is rebindable in Controls, and while an inventory
  screen is open.

## Inventory Redesign (`site/redesign/inventory.html`), work in progress

- Before (Colourful Containers' `container/inventory`, its slot icons and the mod buttons) and after, side
  by side, with slot outlines and a recipe book hover toggle, plus PNG downloads.
- Code: `inventory.js` (layout and panel), `panel-art.js` (reusable illustrations: schematic scrolls,
  planks, straps, brackets, tools), on the helpers in `sprites.js` and `material.js`. Styles in `page.css`.
- Rule for every panel: keep the spirit of the original (its groups, motifs and hints) in our style;
  brass only as small accents; thin sunken cells for slot grids (no thick bezels).
- Spirit kept here: scrolls become Create schematics (blueprints on wooden rollers) for the armor column and
  for the offhand and recipe book; the crafting table becomes a workbench top (oak planks in an L, a flush
  title plank, an inlaid chamfered tray under the grid, a chamfered iron result frame, burnt-in arrow and
  tools, small corner brackets). A flat engraved cog fills the strip between the player and the table.
  Empty-slot silhouettes (`container/slot/helmet` and the others) are blueprint line drawings.
- The viewport is drawn narrower than the player box (`28.5` to `72.5`, symmetric about the player at
  x 50.5), which frees room to centre both scrolls on their slots.
- Layout (InventoryMenu, InventoryScreen 1.21.1), item top left, panel 176x166: armor `(8, 8 + 18 i)`,
  offhand `(77, 62)`, crafting `(98 + 18 j, 18 + 18 i)`, result `(154, 28)`, main `(8 + 18 j, 84 + 18 i)`,
  hotbar `(8 + 18 j, 142)`, player box `(26, 8)` to `(75, 78)` (about 1 px free around it), title
  "Crafting" at `(97, 8)`, recipe book button `(104, 61)` 20x18.
- **Curios screen** (toggle, `?curios`; `inv-curios.js`): Curios' side panel, cosmetics tab, page buttons and render
  toggles (`curios:textures/gui/curios/inventory.png`, cut as described at the top of the file), the cosmetics button
  sprites, and an icon per slot type. The player's 19 slots come from `data/*/curios/slots` + `curios/entities` in the
  jars (`CURIO_TYPES`, `CURIOS.slots`, in order); each icon is drawn for what the slot holds (spell focus, hat,
  necklace, cape, ring, bangle, glove, waist pouch, belt, charm, spellbook, bundle sack), written to the texture the
  slot names (`curios:slot/*`, `cataclysm:item/*`). Re-read the slot data when mods change.
- TrashSlot: every attached piece has the join to the panel's border built into both ends (in game the bottom-right
  piece sits a few px inside the panel's edge, not flush as the mod's code suggests).
- Mod buttons (from the jars and the instance config): Curios `(26, 8)` 10x10 (`curios:curios/button`);
  Cosmetic Armor Reworked `(65, 67)` 10x10 and a toggle `(59, 72)` 5x5.
- The game draws container titles in fixed `#404040`: each title needs a light ground.
- Controls: slot outlines, recipe book hover, GUI scale 2 to 4, and **Game sampling** (also on the HUD page;
  URL `?scale=2&nearest`).
- The title is drawn with the game's font (`textures/font/ascii`, extracted for this; `mcfont.js`).
- Not done: the effect backgrounds.

## Backpack Redesign (`site/redesign/backpack.html`), work in progress

- Sophisticated Backpacks (Sophisticated Core 1.4.90). Before and after are assembled by the same code
  (`backpack.js`) with the mod's cutting rules, for every tier (Leather 9x3 to Netherite 12x10), the
  small-screen scrolling layout (`_wider` textures) and an open crafting tab. Art in `backpack-art.js`.
- Files: `sophisticatedcore:textures/gui/storage_background_{9,12}[_wider]`, `slots_background`,
  `gui_controls` (pack sheet with our regions painted over) and `icons` (the icons this screen uses).
- Cutting rules the art must obey:
  - Background: top 17 + h rows glued to bottom 97 + h rows (h = visible storage height / 2). Rows 17 to
    150 vary only across x; rows 150 to 159 are seen only from the bottom slice; 159 to 256 are the
    player pocket (label at 162, cells at `(off + 7 + 18 j, 173 + 18 i)`, hotbar at 231).
  - Width W = cols x 18 + 14 (+6 wider); pocket offset 0 / 3 / 27 / 30.
  - Tabs: top half and bottom half of `gui_controls` x 128..256, cut from the right; a 3 px strip from
    x 128 joins the tab to the panel edge (painted as the panel's leather).
  - Upgrade column: `gui_controls` x 0..26, rows 4..198 cropped to 6 + 16 N, period 16.
- Structure follows the original: the full-width body runs through the "Inventory" row; on 12 column and
  wider packs it steps in to the 176 wide player pocket at row 172 (as the pack's texture does).
- Spirit: a leather pack (stitched body, canvas title label, the player slots in a pocket below), a
  leather side pouch for upgrades whose leather bulges out to hold each switch, and tabs in the pouch's
  design in tan leather (light enough for the game's #404040 tab titles; a separate title band is
  impossible, since closed tabs are cut from the top 12 and bottom 12 rows of the same art). Seams 3 px in from the edges; every
  stitch path is closed or ends on a join; dots 1 logical px apart stay continuous across the mod's joins.
  The joining strip of the tab art is transparent, so tabs sit flush on the panel edge.
- Upgrades shown (both views, page controls): crafting + magnet, or pump + tank. The tank takes two storage
  columns (gauge pieces from `gui_controls`, the shared gauge component). Crafting tab: the down arrow and
  result frame are the shared crafting set; select/previous/next appear only with several results. Pump
  tab: four toggles and four fluid filter slots.
- Icons in `icons`, all flat vector shapes (one solid light steel tone, rust for "not" signs; no gradient,
  shadow, glow or pixel upscaling): transfer and sort arrows, sort-by letters in the game font, the settings
  cog, the switch lever, the crafting and pump tab toggles (redrawn by meaning), the result arrows.
- Controls keep everything (shadows too) inside their box: mods place buttons 1 px from slots. Small 12 px
  buttons draw a 9.6 px face inset 1.2 px, with glyphs about 4.4 x 5.6, so they do not crowd slot rows.
- The search box is the mod's flat #777777 square with a magnifier hint; the texture frames it like the
  sort buttons beside it. Gauges have no reflection stripe.
- Upgrade tab slots: `upgrade_slots_background` (ours, blueprint cells like the armor column); Create Dark Inventory's
  `TabSlots` makes the mod draw tab slots from it. The storage grid keeps `slots_background`.
- Not restylable (drawn in code by the mod): the scrollbar, the search box (a bezel frames its collapsed
  spot), slot highlights. Upgrade item icons are item art; the mockup uses placeholders.

## Shared components (`site/redesign/components.js`)

- Crafting set (`craftTray`, `craftArrow`, `resultFrame`), `steelButton`, `switchSlot`, gauges
  (`gaugeFrame`, `gaugeGlass`, for liquid tanks and power cells). Screens with these parts use them.
- Page driver `screen-page.js` (scale buttons, game sampling, toggles, file list); panel art pieces in
  `panel-art.js`; the game font in `mcfont.js`.

## HUD meters and mod HUD (current design, installed)

- Health and food are meters (`meters.js`): each 9x9 cell is a segment of a plain shaded steel pipe (rows 3 to 8,
  window 4 to 7). Cells overlap 1 px and the game draws hearts, food and air right to left, so each cell's right
  edge (8.5 to 9) is the visible separator (0.5 wide, a darker shade of the liquid); corners round 0.5 (only the row
  ends show it). No per-cell end caps are possible. Every heart variant is generated (poison, wither, frozen,
  absorption, hardcore with a rust-tinted pipe, blinking, vehicle; vehicle half fills from the right), and the Hunger
  effect's food (`food_*_hunger`, a spoiled olive-brown). The page's Meter states section shows each by the game's rules.
- AppleSkin (`meterAppleskin`): saturation is a lighter broth over the full window from the right; exhaustion is a
  bare line (rows 8.5 to 9) ramping green (right) to red (left). AppleSkin draws exhaustion under the food layer, so
  nothing solid may sit over it.
- XP bar: the meters' pipe, separators lined up with the hearts' grid (left half) and the food's (right half), no caps.
- Armor: small polished steel shields; air: ultramarine water bubbles (not cyan: Iron's mana text is a fixed aqua).
- The level number, dodge roll counter (Block Factory's Bosses), Iron's mana text and Ars Nouveau's mana bar are
  placed from each mod's bytecode (`modhud.js` header has the specs). Iron's bar art is hidden (text only); Ars' is a
  brass pipe with caps, an amethyst and flourishes, 6 rows tall (its fill's full height). The preview simulates a
  screen 488 GUI px wide so the Ars bar (fixed to the bottom-left corner) sits just left of the offhand slot.
- Not restylable (drawn in code): the shake at low health and the Regeneration lift, all mod text (colours, places).

## Repo layout, install and release

One monorepo for everything a player installs (repo and folder were `minecraft-create-hud-overhaul`, and the
mod and packs were `minecraft-dark-mode-fixes`, merged in with history):

- `site/`, `tools/extract.py`: the almanac and the redesign pages (the art source).
- `resourcepacks/Create-HUD`: **generated**, never edited by hand. `python3 -I tools/hudpack.py` (site served)
  rewrites it from the file lists of the HUD page (the meter set; it records its count in `#sheet[data-count]`),
  Inventory and Backpack pages. Every user scales UVs by the logical size, so 8x textures work.
- `resourcepacks/DarkModeEverywhere-Veil-Fix`: DME's two core shaders without bodiless function declarations,
  so Veil's shader processor can parse them. Must stay at the top of the pack order.
- `mod/`: Create Dark Inventory, the DME fixes (its own `mod/CLAUDE.md`).
- `./install.sh [instance/minecraft]`: builds the mod, copies it and both packs into the Prism instance.
- `tools/release.sh`: `dist/create-ui-remix.zip` with `mods/` (our jar, DME from Modrinth checked against
  `tools/dme.sha512`, DME's license), `resourcepacks/` (each pack zipped) and `INSTALL.txt` (`tools/INSTALL.txt`).
  `.github/workflows/release.yml` runs it on every push to main touching those parts and replaces the
  `latest` release, which the README links. So: commit and push the regenerated pack to publish.
- Pack textures: keep alpha binary (0 or 255). Many GUI blits run without blending, so a half-transparent
  pixel shows at full color (the bright clumps that were on the backpack window corners).

## Next steps

1. HUD sprites not done yet: `hotbar_offhand_right`, crosshair and attack indicators, `jump_bar_*`, `effect_background*`.
2. Check in game at GUI scales 1 to 4.

## Process: how every screen is done (lessons; follow all of them)

1. **Research before drawing.** Get the exact layout from source (vanilla or the mod's code at the
   installed version): every slot, button, label, icon UV, sprite and size, every variant (tiers, open
   tabs, states such as disabled or hovered), and what is drawn in code (fills, text colours: not
   restylable). Read the instance config for mod options. Never guess positions or sizes.
2. **Study the original art.** Look at the current pack's texture (almanac) and list its structure
   (where materials change, where it steps in), groups, motifs and hints. Keep that structure and spirit.
3. **Build the mockup with the game's own rules.** One compose function draws both views (before from the
   pack's textures, after from ours) with the mod's cutting and blit rules, so a correct "before" proves
   the layout. Show every variant and state with page controls.
4. **Reuse.** Shared parts come from `components.js` / `panel-art.js` (crafting set, gauges, buttons,
   nameplates, cells). Extend a component instead of drawing a one-off.
5. **Render without a cache** (`--incognito` for headless Chrome): a reused profile serves stale scripts.
   Run renders through Python's `subprocess` (the shell is nushell: no bash loops, no `bash -c`).
6. **Critique as a designer, not only as a checker:** one concept and one focal point; a clear hierarchy
   (controls and slots strongest); a palette whose colours harmonise (no colour fighting another); balance
   and breathing room; no props or details that only add clutter. Name what fails and fix it before showing.
7. **Review before reporting, every time, as the user sees it:** the whole screen per variant beside the
   original (structure matches?), then every edge and join zoomed (corners, seams, tab joins, labels,
   buttons against slots), then GUI scale 2 with game sampling. List what was checked. Never report from a
   coarse look.
8. **Shell hygiene:** no `rm` and no `bash -c` wrappers (both trigger approval prompts); overwrite scratch
   files in the session scratchpad.

## Rendering rules (all pages)

- Minecraft samples GUI textures nearest-pixel. An 8x texture at GUI scale 2 keeps one texel in four, so a
  line under 0.5 logical px can vanish. Draw structural lines at least 0.5 wide on the half-pixel grid
  (for example a 0.5 stroke centred on x.25). Check with **Game sampling** at GUI scale 2.
- Titles: the game draws titles in fixed colours (container titles #404040). Each needs a ground it reads
  on; no art may cross a title. A plate or band behind a title must work for every cut of a texture.
- Panel icons are flat: solid shapes, one tone (rust for "not" signs), no gradient, shadow or glow, never
  upscaled pixel art. Redraw an icon by its meaning; keep it inside the original's footprint with padding.
- Only interactive things look pressable (raised, lit edge, shadow); decoration is flat, engraved, burnt in
  or inlaid. Controls keep everything (shadows too) inside their box; mods place them 1 px from slots, so
  small buttons draw an inset face.
- Centre art on the fixed slots: frames, papers and plates are sized for equal margins around them; align
  the tops and bottoms of neighbouring pieces; keep seams off slot edges (3 px from panel edges).
- Every line ends on purpose: stitch paths are closed or end on a join or an edge, never under a control
  or loose beside one; dotted lines are dots 1 logical px apart (continuous across the mod's joins).
- Matching elements match (labels, buttons, tabs): same function, same size.
- Restraint: no bright fades, no glare, no flat white; edges a little lighter than their panel, not
  highlighted. Brass is a rare accent. Texture is subtle.
- Hierarchy: decoration stays quieter than slots and items; keep saturated colour off where items sit.
- Nothing overlaps a meter: icons sharing a cell with one (the food row and AppleSkin's exhaustion meter) are fitted
  above it (drumstick rows 0.3 to 6.7, meter rows 7 to 9).
- Inventory sections are flat (one tone, no texture), from the shared `ironPlate` and `playerInventory`.
- When a texture is cut and glued by the game (backpack backgrounds, tabs), every cut must look right:
  flat colour or x-only art where slices join, nothing at rows that some cut shows half of.

## Conventions

- No em dashes in text or UI copy. Keep files under about 500 lines.
- Commits: personal repo, no Claude co-author trailer.
