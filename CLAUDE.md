# minecraft-dark-mode-fixes

Fixes for Dark Mode Everywhere (DME, `darkmodeeverywhere-neoforge-1.21.1-1.4.0`) in the
"Create Ultimate Selection 2" Prism instance (NeoForge 21.1.243, Create 6.0.10). Client only.
`./install.sh [instance/minecraft]` builds and copies both parts into the instance.

## resourcepack/ (installed as `resourcepacks/DarkModeEverywhere-Veil-Fix`)

DME's two core shaders without bodiless function declarations, so Veil's shader processor can
parse them. Must stay at the top of the pack order.

## mod/ (Create Dark Inventory, `createdarkinventory`)

How DME decides: it walks the stack for the first frame that is not GameRenderer, RenderSystem,
GuiGraphics or a `setShader` call, and skips the dark shader when that `class:method` contains an
entry of `METHOD_SHADER_BLACKLIST`. Its default list has `com.simibubi.create.foundation.gui.AllGuiTextures`,
so every Create panel stays light, and one screen cannot be told from another by caller.

The mod injects into `AllGuiTextures.render(GuiGraphics, int, int)` and, for Create's
`player_inventory.png` and `frogport_and_mailbox.png`, draws through `DarkBlit` instead. The caller
is then `DarkBlit:blit`, which is not blacklisted, so these get the dark shader and follow DME's
toggle and shader choice. To darken more Create textures, add their paths to `DARK` in
`AllGuiTexturesMixin`. A baked dark texture would not follow the toggle: do not use one.

DME's font rule turns text light only when red, green and blue are all under 65.
`PackagePortScreenMixin` moves the frogport/mailbox title color from #3D3C48 to #3D3C40 so the
rule catches it; check other dark-panel text against the same rule.

Build: Gradle runs on JDK 17 (system default is 27, too new); the toolchain gets JDK 21.

## Instance config (not in this repo)

`config/darkmodeeverywhere-client.toml` `METHOD_SHADER_BLACKLIST` also has
`StockKeeperRequestScreen:drawItemCount`: the Stock Keeper's item counts blit directly, so DME
inverted them on Create's light panel.

`["Inventory Button"] X = 56` (default 32): at 32 DME's toggle button overlapped another mod's
button at the bottom left of the inventory.
