# Create UI Remix

A dark steampunk UI for the **Create Ultimate Selection 2** modpack (Minecraft 1.21.1, NeoForge): a new HUD,
inventory and Sophisticated Backpacks screens, plus fixes that make Dark Mode Everywhere work with Create's
screens. Client only; nothing to install on the server.

## Download

**[create-ui-remix.zip](https://github.com/Frosthaven/minecraft-create-ui-remix/releases/latest/download/create-ui-remix.zip)**
(always the latest build).

## Install

1. Copy all three jars in the zip's `mods/` (haven-create-ui-ux, haven-mob-health-bars and Dark Mode Everywhere) into
   your instance's `mods` folder (Prism: right-click the instance > Folder, then `minecraft/mods`).
2. Copy `resourcepacks/haven-create-hud.zip` into the instance's `resourcepacks` folder.
3. Start the game. Options > Resource Packs: enable **haven-create-hud** at the top of the list.
4. The "Dark Mode" button on inventory screens cycles 4 shades, from vanilla light mode to perfect dark.

To update, download the zip again and replace the files.

## What is in it

Mods:
- **haven-create-ui-ux** (built here): the compact action bar (orbs or bars) in place of the vanilla and mod HUD rows,
  buffs and debuffs above it, the dark mode split for the inventory and backpack screens, Dark Mode Everywhere fixes
  for Create's screens, and a config screen. Needs Create. Worn gear gets a cyan status icon by the buffs: MekaSuit
  energy, Mekanism jetpack (and MekaSuit jetpack unit) hydrogen, Ad Astra Jet Suit energy, and a Create Jetpack in
  flight (the backtank air it drains). Breathing gear takes the right orb's ring (bars: its top row) in copper with
  its time left while in use: the Create backtank (diving), Creating Space O2 backtanks, the Mekanism scuba tank, the
  Ad Astra space suit.
- **haven-mob-health-bars** (built here): floating health bars over mobs and players within 10 blocks, and on the one
  you look at up to 64 blocks away when nothing blocks the view, with its own config.
  Works on its own.
- **Dark Mode Everywhere** 1.4.0 by Buuz135 (MIT; license included), supplied unmodified (not part of the modpack): a
  button on inventory screens cycles 4 shades, from vanilla light mode through toasted light and less perfect dark to perfect dark.

Resource packs:
- **haven-create-hud** (built here): the action bar's art, the inventory, Curios, Cosmetic Armor, TrashSlot and
  Sophisticated Backpacks screens with their dark mode layers, and Dark Mode Everywhere's shaders fixed for Veil.
