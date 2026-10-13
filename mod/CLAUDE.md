# mod/: Create Dark Inventory (`createdarkinventory`)

Fixes for Dark Mode Everywhere (DME, `darkmodeeverywhere-neoforge-1.21.1-1.4.0`, MIT, by Buuz135) in
Create Ultimate Selection 2 (NeoForge 21.1.243, Create 6.0.10). Client only. Part of the
minecraft-create-ui-remix monorepo (see the root CLAUDE.md for layout, install and release).

How DME decides: it walks the stack for the first frame that is not GameRenderer, RenderSystem,
GuiGraphics or a `setShader` call, and skips the dark shader when that `class:method` contains an
entry of `METHOD_SHADER_BLACKLIST`. Its default list has `com.simibubi.create.foundation.gui.AllGuiTextures`,
so every Create panel stays light, and one screen cannot be told from another by caller.

The mod injects into `AllGuiTextures.render(GuiGraphics, int, int)` and, for Create's
`player_inventory.png`, `frogport_and_mailbox.png` and `clipboard.png`, draws through `DarkBlit` instead. The caller
is then `DarkBlit:blit`, which is not blacklisted, so these get the dark shader and follow DME's
toggle and shader choice. To darken more Create textures, add their paths to `DARK` in
`AllGuiTexturesMixin`. A baked dark texture would not follow the toggle: do not use one.

DME's font rule turns text light only when red, green and blue are all under 65.
`PackagePortScreenMixin` moves the frogport/mailbox title color from #3D3C48 to #3D3C40 so the
rule catches it; check other dark-panel text against the same rule.
Plain fills (no texture, no text) are not darkened: `ClipboardScreenMixin` draws the clipboard's
black mid-line cursor white while DME is on (`DarkMode.on()`, DME's `SELECTED_SHADER_VALUE`).

Build: `./install.sh` at the repo root. Gradle runs on JDK 17 locally (system default is 27, too new; CI uses 21);
the toolchain gets JDK 21.

## DME settings carried by the mod (`CreateDarkInventory`)

No player config edits needed; the instance config stays at DME's defaults.
- IMC `dme-shaderblacklist` -> `StockKeeperRequestScreen:drawItemCount`: the Stock Keeper's item
  counts blit directly, so DME inverted them on Create's light panel.
- DME's inventory button (60x20 "Dark Mode", default X 32) overlapped the bottom-left icon
  buttons; the mod raises it 32 px (above the 28 px icons) on container screens, only while it is at the default X and Y.
- Title screen: no dark mode (the pack's FancyMenu layout). `DarkModeClientProxyMixin`
  (`createdarkinventory.dme.mixins.json`, optional) answers DME's `isElementNameBlacklisted`
  with true while `TitleScreen` is open; the mod removes DME's title button there.
- DME's button kept focus after a click, and a focused widget stops Create's "hold W to ponder";
  the mod clears that focus on mouse release.
- Main HUD with a Create HUD pack: while a selected pack id contains "create-hud" (`file/Create-HUD`),
  `HudLayers` flags NeoForge's hotbar, health, armor, food, air, experience bar/level, action bar
  layers and Create's `remaining_air`; `DarkModeClientProxyMixin` skips DME while one draws.
  ImmediatelyFast `hud_batching` (on in this pack) draws the whole HUD later in
  `BatchingBuffers.endHudBatching`, where DME sees ImmediatelyFast as the caller (so DME's own
  `net.minecraft.client.gui.Gui` blacklist never matched either). `BatchingBuffersMixin`
  (`createdarkinventory.immediatelyfast.mixins.json`, optional) flags that flush and
  `tryForceDrawHudBuffers` (AppleSkin and minimap compat flush mid-HUD); DME callers starting with
  `BatchingRenderLayers` (ImmediatelyFast's HUD render types, seen with `METHOD_SHADER_DUMP` in
  debug.log as "Considering ...") are skipped too. With the pack on, DME skips the whole batched HUD.
- Also with the pack on: Sophisticated Backpacks screens and the inventory (`PackScreens`), by DME
  caller: `net.p3pp3rf1y.sophisticated*` (backpack screens only) and vanilla
  `gui.screens.inventory.*` / `gui.components.*`; JEI and other side panels stay dark.
  Mods whose asset namespace the pack contains (trash slot, Curios, Cosmetic Armor) are skipped too,
  on those screens and on their own screens: the caller class is mapped to its mod jar's ids.
  The same rule skips their HUD parts (Ars Nouveau mana bar, Iron's Spellbooks bars) while a
  GUI layer draws or no screen is open.
- Dark mode split (`SplitBlit`, `GuiGraphicsSplitMixin`, `GuiGraphicsInvoker`): the skipped screens still follow DME in
  their plain greys (panel body, slot wells, steel push buttons). The pack ships those greys as `<texture>_grey.png`
  (same size, neutral, lightened for DME to darken back; made by the pages' `greyLayer`, see the root CLAUDE.md) and
  GUI sprites as `<sprite>_grey` (the recipe book button). After every `GuiGraphics.innerBlit` of a texture, and every
  `blitSprite(sprite, x, y, z, w, h)`, that has one, `SplitBlit` draws the layer at the same place; DME's caller is then
  `SplitBlit:draw` / `drawSprite`, which is not skipped, so the layer gets DME's shader. DME off: light grey
  (vanilla-like); perfect dark: about the art's greys. No DME installed: no layer. Layers are looked up once per texture
  or sprite (cleared on resource reload) and logged once (`[dark split]`); a failing layer logs once and is dropped.
- Sophisticated upgrade tabs: Sophisticated Core draws the storage grid and tab slots from one
  texture (`slots_background.png`). `TabSlots` (`createdarkinventory.sophisticated.mixins.json`,
  optional) draws tab slots from `sophisticatedcore:textures/gui/upgrade_slots_background.png`
  when a pack has it; the storage grid is told apart by caller (`StorageScreenBase`,
  `SettingsScreen`).
- Pack textures: keep alpha binary (0 or 255). Many GUI blits run without blending, so a
  half-transparent pixel shows at full color (the bright clumps on the backpack window corners).

## Compact action bar (`hud/`)

The HUD designed on the redesign page `hud-compact.html`, drawn by the mod while the pack's textures
(`createhud:textures/gui/compact/{parts,plate_*}.png`, exported by `hud-compact-export.js`) are present and the player is
not a spectator.
- `CompactHud`: the layer `createdarkinventory:compact_hud` (above vanilla's HOTBAR). Hides what it replaces in
  `RenderGuiLayerEvent.Pre`: vanilla hotbar, health, armor, food, vehicle health, air, XP bar and level, jump meter;
  `irons_spellbooks:mana_overlay`, `ars_nouveau:mana_hud`, `create:remaining_air`, AppleSkin's four layers. Draws at 1/8
  GUI px (pose scale) so the 8x textures land texel for texel; positions are the page's layout (x from the screen
  centre, the bar's bottom at 50) times 8. Items at vanilla's exact spots (bottom - 19, centre - 88 + 20 i), so mods that
  mark hotbar slots line up. Sets `gui.leftHeight/rightHeight` to 33 for later layers. Key "Show HUD numbers (hold)",
  Left Alt by default (Controls); numbers also show while a screen is open. No dark mode on it (`HudLayers.MAIN`).
- In game the whole bar (items included) is drawn at `CompactHud.SCALE` (1.1) around the screen's bottom centre.
- Exhaustion: `FoodData.getExhaustionLevel()` (NeoForge), a hairline track under the food meter.
- `CompactAtlas`: the textures' layout; KEEP IN STEP with `hud-compact-export.js`.
- `HudStats`: Iron's mana (`ClientMagicData.getPlayerMana`, attribute `irons_spellbooks:max_mana`), Ars mana
  (`CapabilityRegistry.getMana`), the dodge roll (`RollAttachment.fromPlayer`: rollCount, getCooldown), Create's backtank
  (persistent `VisualBacktankAir`, `BacktankUtil.maxAir` over `getAllWithAir`), by reflection: a missing mod hides its row.
- `RollHudMixin` (`createdarkinventory.bosses.mixins.json`, optional): cancels Block Factory's Bosses' own roll icon
  (`ClientEvents.renderGUI`, a RenderGuiEvent.Pre we cannot hide as a layer) while the bar is active.
- `Followers`: Haven Link's web map followers from its chat lines; reset on logout.
- `HudLog`: logging in latest.log, all lines prefixed `[compact HUD]`, never per frame: once per event or when a value
  changes (activation and why, each hidden layer, mods found and their values, extensions, mode, vehicle, followers,
  the roll mixin), errors with stack traces once per kind; a failing frame is skipped, not crashed.
- Every preview feature is in game: `HudExtras` reads each mod the way its own HUD does (Iron's cast, MekaSuit energy,
  Immersive Aircraft health and engine, Create train speed / toolbox slots / schematic slot, Artifacts Helium Flamingo
  and cooldowns, Immersive Armors steampunk chestplate, AppleSkin food and health previews, vanilla attack indicator),
  through `Reflect` (cached, absent mod = hidden, logged once). `HudEffects`: buffs and debuffs (replaces EFFECTS),
  hover tooltip while a screen is open. The bar hides those mods' own layers (`REPLACED`).
