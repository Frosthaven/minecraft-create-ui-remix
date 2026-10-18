# mod/: haven-create-ui-ux (`haven_create_ui_ux`)

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

## DME settings carried by the mod (`HavenCreateUiUx`)

No player config edits needed; the instance config stays at DME's defaults.
- IMC `dme-shaderblacklist` -> `StockKeeperRequestScreen:drawItemCount`: the Stock Keeper's item
  counts blit directly, so DME inverted them on Create's light panel.
- DME's inventory button (60x20 "Dark Mode", default X 32) overlapped the bottom-left icon
  buttons; the mod raises it 32 px (above the 28 px icons) on container screens, only while it is at the default X and Y.
- Title screen: no dark mode (the pack's FancyMenu layout). `DarkModeClientProxyMixin`
  (`haven_create_ui_ux.dme.mixins.json`, optional) answers DME's `isElementNameBlacklisted`
  with true while `TitleScreen` is open; the mod removes DME's title button there.
- DME's button kept focus after a click, and a focused widget stops Create's "hold W to ponder";
  the mod clears that focus on mouse release.
- Main HUD with a Create HUD pack: while a selected pack id contains "create-hud" (`file/haven-create-hud`),
  `HudLayers` flags NeoForge's hotbar, health, armor, food, air, experience bar/level, action bar
  layers and Create's `remaining_air`; `DarkModeClientProxyMixin` skips DME while one draws.
  ImmediatelyFast `hud_batching` (on in this pack) draws the whole HUD later in
  `BatchingBuffers.endHudBatching`, where DME sees ImmediatelyFast as the caller (so DME's own
  `net.minecraft.client.gui.Gui` blacklist never matched either). `BatchingBuffersMixin`
  (`haven_create_ui_ux.immediatelyfast.mixins.json`, optional) flags that flush and
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
  texture (`slots_background.png`). `TabSlots` (`haven_create_ui_ux.sophisticated.mixins.json`,
  optional) draws tab slots from `sophisticatedcore:textures/gui/upgrade_slots_background.png`
  when a pack has it; the storage grid is told apart by caller (`StorageScreenBase`,
  `SettingsScreen`).
- Pack textures: keep alpha binary (0 or 255). Many GUI blits run without blending, so a
  half-transparent pixel shows at full color (the bright clumps on the backpack window corners).

## Built-in pack and pack defaults

- `BuiltInPack`: registers `resourcepacks/haven_create_hud` in the jar (copied from `../resourcepacks/haven-create-hud` by
  processResources) on `AddPackFindersEvent` as a required client pack fixed at the top, id
  `mod/haven_create_ui_ux:resourcepacks/haven_create_hud`. Log prefix `[built-in pack]`.
- `PackDefaults`: one-time defaults from `config/haven-pack-defaults.json` on the first title screen of a session (format
  in the root CLAUDE.md): enables listed resource packs once (ordered by `above`/`below`), sets Iris's shader per OS on
  the first run (reflection: `Iris.getIrisConfig().setShaderPackName/setShadersEnabled/save`, `Iris.reload()`), and
  writes per-OS shader pack options (`shaderOptions`) into Iris's `shaderpacks/<pack>.txt` (a properties file): a key
  is written when the file lacks it or still holds the value written before, then `Iris.reload()`; a value the player
  changed stays. First use: Complementary Unbound on macOS at Block Reflection Quality Low, because Apple's OpenGL has
  16 texture units and Deep Seas 3.4's hull sampler overflowed composite1 at High. The player's later choices always
  win. Log prefix `[pack defaults]`.

## Compact action bar (`hud/`)

The HUD designed on the redesign page `hud-compact.html`, drawn by the mod while the pack's textures
(`createhud:textures/gui/compact/{parts,orbs,vanilla,plate_*}.png`, exported by `hud-compact-export.js` and
`hud-vanilla-export.js`) are present and the player is
not a spectator.
- Paint order: ImmediatelyFast batches HUD blits and fills per render layer (one per texture, one for fills) and draws
  the batches out of call order, which put the dash dial's chevrons (orbs atlas) under its housing (parts). Every
  draw names its kind (`HudDraw.use`); a change of kind draws the batch first (`drawBatched`: `g.flush()` and IF's
  `BatchingBuffers.tryForceDrawHudBuffers` while it batches). Immediate meshes (`sector`, `HudJoint`) flush first.
  Every flush and immediate mesh is followed by `HudDraw.restoreBlend` (default function, then enable LAST): drawing a
  batched render type ends with its clearRenderState (blending off), and ImmediatelyFast records the blend state with
  each blit, so without it everything after a flush drew opaque (solid white glass highlight, black empty glass, digit
  cells as blocks, the selection frame's margin as a second border, the glow lighting the rim). `checkBlend` logs once
  if blending is off where a draw group starts.
- `HudJoint`: the plate joints (see the root CLAUDE.md), one immediate mesh per pass; the profile is cached per (mode,
  side scale) and logged once. `CompactAtlas.checkPieces` logs any atlas piece that is blank or outside its texture.
- `AirGear`: breathing gear on the ring (see the GearStatus note below). Side bars size: the config times
  `HudFrame.WING_BASE` (0.85).
- `CompactHud`: the layer `haven_create_ui_ux:compact_hud` (above vanilla's HOTBAR). Hides what it replaces in
  `RenderGuiLayerEvent.Pre`: vanilla hotbar, health, armor, food, vehicle health, air, XP bar and level, jump meter;
  `irons_spellbooks:mana_overlay`, `ars_nouveau:mana_hud`, `create:remaining_air`, AppleSkin's four layers. Draws at 1/8
  GUI px (pose scale) so the 8x textures land texel for texel; positions are the page's layout (x from the screen
  centre, the bar's bottom at 50) times 8. Items at vanilla's exact spots (bottom - 19, centre - 88 + 20 i), so mods that
  mark hotbar slots line up. Sets `gui.leftHeight/rightHeight` to 33 for later layers. Key "Show HUD numbers (hold)",
  Left Alt by default (Controls); numbers also show while a screen is open. No dark mode on it (`HudLayers.MAIN`).
- In game the whole bar (items included) is drawn at the config's size (`UiConfig.HUD_SCALE`, 1.0) around the screen's
  bottom centre.
- Classes: `CompactHud` (registration, the plate, hotbar, XP pipe and level, dash dial, floating plates,
  items), `HudFrame` (one frame's state, the wing ends), `OrbHud` (orb mode), `BarHud` (bar mode), `VanillaHud` (Almost
  Vanilla: no plate; see below), `HudDraw` (blits,
  circle and pill clipped fills, textured sectors that reveal a ring by angle, the digit sets), `Smooth` (breath and the
  backtank eased between the game's steps).
- Glows are drawn on black in the atlas and added with a screen blend (`ONE, ONE_MINUS_SRC_COLOR`); ImmediatelyFast's
  HUD batching keeps the blend state (`RenderSystemState`).
- Exhaustion: `FoodData.getExhaustionLevel()` (NeoForge): bars a slim solid strip under food (fills, `BarHud`
  EXH_*); orbs the crescent glow on the food half.
- `CompactAtlas`: the textures' layout; KEEP IN STEP with `hud-compact-export.js`. `checkLayout` logs any overlap between
  parts-atlas pieces once (a cell reaching into a neighbour draws a stray bit of it: the wing casing once sat inside the
  selection cell and showed under the selected item).
- `MipTexture`: the bar's textures load with 3 mip levels (alpha-weighted, transparent texels bled with their
  neighbours' colour) and linear filtering. With vanilla's nearest sampling, 8x textures drawn at 1/8 skip texels at GUI
  scale 2 to 4, so thin bright lines (rims, rivets, plate edges) showed as white specks. Logged once per texture.
- `OrbHud`: the orbs at `S` (2) x around their centres (pose scale), their numbers and the tank time too. `BarHud`: the
  meter blocks at `S` (2) x around (±ANCHOR, BY). `HudFrame.Ends`: the page's ccEnds (dial sizes, item centring).
- Side bars size (`UiConfig.SIDE_SCALE`): `HudDraw.side` scales each wing's parts around (±WING_ANCHOR, the bottom);
  the plate is drawn in three crops (centre, each wing); the offhand item follows; the effects rail moves with it.
- Exhaustion: `HudFrame.exhaustShown` eases (Smooth, 400 ms for the reset jump); orbs reveal the screen-blended
  `exhaust` cell by angle, bars draw the strip's width.
- Attack cooldown: on the selected socket whenever the attack indicator is not OFF, vanilla's attack strength (the
  ticker, via `LivingEntityAccessor`, plus the partial tick) over a delay from the held item's own attack speed
  modifiers: on an item change the client's ATTACK_SPEED keeps the old item's until the server's attribute packet,
  so `getAttackStrengthScale` refilled twice. The indicator mode is logged on change.
- `HudStats`: Iron's mana (`ClientMagicData.getPlayerMana`, attribute `irons_spellbooks:max_mana`), Ars mana
  (`CapabilityRegistry.getMana`), the dodge roll (`RollAttachment.fromPlayer`: rollCount, getCooldown), Create's backtank
  (under water / in lava: persistent `VisualBacktankAir`; out of the water while the tanks' `BacktankUtil.getAir` total
  falls, a Create Jetpack in flight, and 3 s after: that total; capacity `maxAir` over `getAllWithAir`), by reflection: a
  missing mod hides its row.
- `VanillaHud` (meter display Almost Vanilla, `UiConfig.MeterDisplay.ALMOST_VANILLA`; Bars is the default; the page's
  hud-vanilla*.js, layout in the root CLAUDE.md): reuses `CompactHud.hotbar`, `offhandSocket`, `hotbarItems`, `take`,
  `dialAt` (the dial at 9 / 7.75 x, filling the offhand's 18 x 18 box) and `instruments` (with a top); draws the gauge,
  rows and icons from `compact/vanilla` (`CompactAtlas` VAN_*). Faded icons (glows, previews, the rows dimmed to 40 %
  under numbers) draw one at a time with `RenderSystem.setShaderColor` alpha, the batch flushed before and after. Sets
  `gui.leftHeight/rightHeight` to the stack's height.
- `RollHudMixin` (`haven_create_ui_ux.bosses.mixins.json`, optional): cancels Block Factory's Bosses' own roll icon
  (`ClientEvents.renderGUI`, a RenderGuiEvent.Pre we cannot hide as a layer) while the bar is active.
- `Followers`: Haven Link's web map followers from its chat lines; reset on logout.
- `HudLog`: logging in latest.log, all lines prefixed `[compact HUD]`, never per frame: once per event or when a value
  changes (activation and why, each hidden layer, mods found and their values, extensions, mode, vehicle, followers,
  the roll mixin), errors with stack traces once per kind; a failing frame is skipped, not crashed.
- Every preview feature is in game: `HudExtras` reads each mod the way its own HUD does (Iron's cast, MekaSuit energy,
  Immersive Aircraft health and engine, Create train speed / toolbox slots / schematic slot, Artifacts Helium Flamingo
  and cooldowns, Immersive Armors steampunk chestplate, AppleSkin food and health previews, vanilla attack indicator),
  through `Reflect` (cached, absent mod = hidden, logged once). `HudEffects`: buffs and debuffs (replaces EFFECTS) where
  `UiConfig.EFFECTS_ANCHOR` puts them: `corner` (TOP_LEFT, the default: buffs row then debuffs row from (2, 2) GUI px at
  2x, the bar's size undone so it follows the screen, wrapping at half the screen; called by `CompactHud.draw` after
  either mode) or `draw` (ACTION_BAR: the rail above the left wing, inside the modes' K pose); hover tooltip while a
  screen is open; worn gear as cyan sockets in the buffs line (`GearStatus.Gear`: fraction, name,
  the mod's amount text, the worn item rendered as the icon; percent, no sign, in the `CYAN` digit row; effect times in
  `BUFF` / `DEBUFF`). `GearStatus` reads (bytecode of the pack's jars): MekaSuit energy (`HudExtras.mekaSuit`); Mekanism
  jetpack / MekaSuit chest with the jetpack unit enabled: chemical tank 0 through `Capabilities.CHEMICAL.item()`
  (hydrogen); Ad Astra Jet Suit energy `getEnergy(stack, ModifyOnlyContext)` (full set; no separate fuel); a Create
  Jetpack in flight (`HudStats.jetpackFlight`: the tanks' air falling out of water, and 3 s after; the first tank as
  the icon); the Helium Flamingo's charge (its item as the icon) and an aircraft's hull (the vehicle's pick item), from
  `HudExtras.flamingo` / `aircraftHealth`. The ring and bar row 1 are breathing only (`HudStats.backtank`: under water
  or in lava, plus AirGear's sources).
- Attack cooldown: `CompactHud.attackVeil`, after the items (z 250), a cool grey veil over the selected item hanging
  from the slot's top, shrinking as the swing recharges, a light and an amber line at its bottom.
  `AirGear` puts breathing gear on the ring (the frame's `tank`, seconds {left, capacity}), shown while in use (under
  water, without breathable air, or its amount fell in the last 3 s), the one falling now first, else Create backtank >
  Creating Space > Mekanism scuba > Ad Astra (logged as "breathing ring: ..."): Create (`HudStats.backtank`); Creating
  Space `OxygenBacktankItem` chest, `OxygenBacktankUtil.getOxygen` / `maxOxygen(ItemStack)`, 1 O2 a second
  (CSEventHandler every 20 ticks); Mekanism `ItemScubaTank` chest with `CommonPlayerTickHandler.isScubaMaskOn`, tank 0,
  1 mB a tick under water; Ad Astra space suit full set, `getOxygenAmount` over `FluidAmounts.toPlatformAmount(tankSize)`,
  1 mB per 12 ticks without air (`OxygenApi.API.hasOxygen`) and per 12 with the eyes in water (`inventoryTick`). Hidden: Creating Space's
  `creatingspace:remaining_oxygen` layer (its O2 dial alone). Kept: Mekanism's top-left `MekanismHUD` (one layer for all
  its gear's lines, not separable) and Ad Astra's overlay (oxygen, battery and the rocket launch bar in one draw call). The bar hides those mods' own layers (`REPLACED`).

