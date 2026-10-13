# minecraft-create-hud-overhaul

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

Re-run step 1 after changing mods or resource packs.

## Extractor (`tools/extract.py`)

- Sources, in load order (later wins, like the game):
  1. vanilla client jar;
  2. every mod jar in the instance's `mods/` (and jars nested inside them, two levels);
  3. the active resource packs from `options.txt` (`resourcePacks`, `file/...` entries).
- Paths taken:
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
- Theme:
  - Andesite slate rail with machined grooves.
  - Polished brass sockets with recessed wells, and bolts.
  - Orange value-box glow for the selected slot, with a cog crown.
  - Copper offhand bracket with a steel shaft.
  - Brass pipe XP bar with glowing liquid experience (Create Enchantment Industry).
  - Glass heart vials with red fluid.
  - Builder's Tea mugs for food.
  - Brass cogwheels for armor.
  - Copper pressure gauges for air, and a steam puff for a bursting bubble.
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

## Next steps

1. Finish the remaining HUD sprites (list above) in the same style.
2. Package as a resource pack:
   - `pack.mcmeta` with `pack_format` 34 (1.21.1);
   - PNGs at `assets/minecraft/textures/gui/sprites/hud/...`;
   - load it above Vanilla Experience+, which overrides the HUD now.
3. Check in game at GUI scales 1 to 4, and against the HUD mods in the pack (AppleSkin draws over the
   food row).
4. Then panels: inventory and containers in the same theme, using the almanac for sizes and slot
   positions.

## Conventions

- No em dashes in text or UI copy. Keep files under about 500 lines (the redesign page is the
  largest; split the sprite code into its own script when it grows).
- Commits: personal repo, no Claude co-author trailer.
