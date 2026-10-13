# Handoff: create-hud-overhaul session (2026-10-13)

What happened in the session that started this project, so work can pick up here. `CLAUDE.md` has
the reference (layout, extractor, sprite list); this file has the history, the user's feedback and
where things stand.

The full session transcript (it also covers unrelated work on the Haven web map) is
`~/.claude/projects/-home-frosthaven/7121a85b-1a74-4908-8db5-797cc518e4c9.jsonl`, under "GUI Panel
Preview" and "HUD Redesign".

## How it started

1. The user asked for UI mods that cover every mod in the pack without missing a panel. Answer:
   - No resource pack covers about 280 mods.
   - A runtime recolor mod does: **Dark Mode Everywhere**, for NeoForge 1.21.1, darkens any GUI
     with shaders and is client-only.
2. The user then asked for a Chrome tab previewing the current UI panels, reconstructed from their
   textures:
   - one tab per mod;
   - seams with "almost invisible dotted lines".
   - That became the **almanac**.
3. While it was being built, the user asked for:
   - the HUD to be included (action bar, health, food and so on): this became the assembled **HUD** tab;
   - no forced page width or height: the page fills the browser. A test viewport emulation had
     fixed it at 1400x900 and was removed.
4. Then, in another tab, keeping the almanac open:
   - "an interesting create-themed hud redesign";
   - "remember location of things and socket locations".
5. Feedback on the redesign, in order:
   - "id like the page to show before and after": both views side by side, plus a before/after
     pair for each sprite. The earlier "compare" toggle was removed.
   - "can we make it less pixelated styling? this is supposed to be a designer hud overhaul": the
     pixel-art version was replaced with vector art at 8x resolution. Before stays pixel-exact.
6. "create a project for this ... private github ... with a claude.md": this repo,
   `Frosthaven/minecraft-create-hud-overhaul` (private).
   - All the user's Minecraft repos were then renamed to start with `minecraft-`, folders and
     GitHub repos alike: `minecraft-haven-map`, `minecraft-haven-axiom-kit` (public),
     `minecraft-bluemap-haven`.
   - Remotes, the haven-map `CLAUDE.md`, memory notes and the Haven guide's link were updated.

## State now

- **Almanac** (`site/index.html`): works.
  - 3134 textures, 136 mods, a HUD tab.
  - Resource packs are applied in load order: Vanilla Experience+ overrides the HUD, Colourful
    Containers the vanilla containers.
  - Checked visually on the HUD, Mekanism, Amendments and Minecraft `container/` panels.
- **HUD Redesign** (`site/redesign/index.html`): first full pass of the core HUD in vector style.
  - Checked visually; the user had not reacted to the vector version yet when the project was
    created.
  - Remaining sprites and the resource pack packaging are in `CLAUDE.md` "Next steps".
- Fixes made along the way:
  - Mod names: `displayName` can be single quoted (Mekanism), and it now reads either.
  - Folders: Mekanism's screen art is under `assets/mekanism/gui/`; the extractor takes that and
    other screen folders.
  - Small multi-part sprites (Mekanism `arrow_selection`) were cropped to one stroke; they are now
    shown whole.

## Decisions

- The extracted textures (`site/tex`, `site/manifest.json`) are not committed: they are mod and pack
  art. Regenerate them with `tools/extract.py` (command in `CLAUDE.md`).
- The redesign keeps vanilla sprite names, logical sizes and socket positions exactly, so it can ship
  as a high-resolution resource pack.
- The redesign's "before" reads the almanac's manifest (`../manifest.json`), so run the extractor
  first.

## Open questions for the user

- Does the vector direction land? Which icons to push further? The tea mugs and gauges are the most
  stylized; the hearts and cogs are the closest to vanilla.
- Should the pack replace Vanilla Experience+'s HUD entirely, or only some sprites?
- Next after the HUD: inventory and container panels in the same theme?

## Resume

```sh
cd /mnt/vault/github/repositories-personal/minecraft-create-hud-overhaul
python3 -I tools/extract.py \
  "$HOME/.local/share/PrismLauncher/instances/Create Ultimate Selection 2/minecraft" \
  "$HOME/.local/share/PrismLauncher/libraries/com/mojang/minecraft/1.21.1/minecraft-1.21.1-client.jar" \
  site
cd site && python3 -I -m http.server 18777 --bind 127.0.0.1
# almanac: http://127.0.0.1:18777/   redesign: http://127.0.0.1:18777/redesign/
```

The user views pages in Chrome tabs opened through the chrome-devtools tools. Keep the almanac tab
open and work in other tabs.

## Working with this user (from the session)

- Show work in the browser and check it visually (screenshots) before saying it is done.
- Changes are made step by step, with feedback arriving while work is in progress: fold new
  requests in as they come.
- No em dashes. Keep files under about 500 lines.
- Commit and push when asked; personal repos get no Claude co-author trailer. Git identity:
  `Frosthaven <shane@thedragon.dev>`, from `~/.gitconfigs/personal.gitconfig`.
