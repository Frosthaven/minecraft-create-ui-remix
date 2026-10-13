# Create UI Remix

A dark steampunk UI for the **Create Ultimate Selection 2** modpack (Minecraft 1.21.1, NeoForge): a new HUD,
inventory and Sophisticated Backpacks screens, plus fixes that make Dark Mode Everywhere work with Create's
screens. Client only; nothing to install on the server.

## Download

**[create-ui-remix.zip](https://github.com/Frosthaven/minecraft-create-ui-remix/releases/latest/download/create-ui-remix.zip)**
(always the latest build).

## Install

1. Copy everything in the zip's `mods/` into your instance's `mods` folder
   (Prism: right-click the instance > Folder, then `minecraft/mods`). Replace Dark Mode Everywhere if asked.
2. Copy `resourcepacks/haven-create-hud.zip` into the instance's `resourcepacks` folder (from an older
   version, delete the old `Create-HUD` and `DarkModeEverywhere-Veil-Fix` packs and the `createdarkinventory` jar).
3. Start the game. Options > Resource Packs: enable **haven-create-hud** at the top of the list.
4. The "Dark Mode" button on inventory screens turns dark mode on and off.

To update, download the zip again and replace the files.

## What is in it

Mods:
- **haven-create-ui-ux** (built here): the compact action bar (orbs or bars) in place of the vanilla and mod HUD rows,
  buffs and debuffs above it, the dark mode split for the inventory and backpack screens, Dark Mode Everywhere fixes
  for Create's screens, and a config screen. Needs Create.
- **haven-mob-health-bars** (built here): floating health bars over nearby mobs and players, with its own config.
  Works on its own.
- **Dark Mode Everywhere** 1.4.0 by Buuz135 (MIT; license included), supplied unmodified.

Resource packs:
- **haven-create-hud** (built here): the action bar's art, the inventory, Curios, Cosmetic Armor, TrashSlot and
  Sophisticated Backpacks screens with their dark mode layers, and Dark Mode Everywhere's shaders fixed for Veil.
