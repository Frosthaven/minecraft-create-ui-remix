# minecraft-dark-mode-fixes

Fixes for Dark Mode Everywhere (DME, `darkmodeeverywhere-neoforge-1.21.1-1.4.0`) in the
"Create Ultimate Selection 2" Prism instance (NeoForge 21.1.243, Create 6.0.10). Client only.
`./install.sh [instance/minecraft]` builds and copies both parts into the instance.

## resourcepacks/ (each folder copied into the instance's `resourcepacks/`)

### DarkModeEverywhere-Veil-Fix

DME's two core shaders without bodiless function declarations, so Veil's shader processor can
parse them. Must stay at the top of the pack order.

### Create-HUD

Dark steampunk HUD sprites (hotbar, hearts, food, armor, air, XP) at 8x. `food_empty.png` is the
`food_full.png` silhouette in dark tones: the empty sprite draws behind each full one, and a
different shape showed at the edges.

## mod/ (Create Dark Inventory, `createdarkinventory`)

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

Build: Gradle runs on JDK 17 (system default is 27, too new); the toolchain gets JDK 21.

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
