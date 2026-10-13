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

- **haven-create-hud** (resource pack): the HUD (hotbar, hearts, food, armor, air, XP, mod meters), the inventory
  and the backpack screens, and Dark Mode Everywhere's shaders fixed so they load next to Veil.
- **haven-create-ui-ux** (mod): dark mode for Create's panels, and it leaves the screens haven-create-hud
  restyles alone.
- **Dark Mode Everywhere** by Buuz135 (MIT; license included), the mod the fixes are for.
