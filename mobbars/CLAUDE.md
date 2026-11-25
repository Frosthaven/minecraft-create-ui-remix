# mobbars/: haven-mob-health-bars (`haven_mob_health_bars`)

Its own client-only mod (split out of haven-create-ui-ux): needs only NeoForge and Minecraft 1.21.1. Part of the
minecraft-create-ui-remix monorepo (root CLAUDE.md: layout, install, release). Build like `mod/` (JDK 17 for Gradle).
Classes: `HavenMobHealthBars` (registers config, config screen, the RenderLivingEvent listener), `MobBarsConfig`,
`MobBars`, `Log`, `mixin/RecentAttackerMixin` (hostile players: any player who hit a player in the last 30 s).

## Mob health bars (`MobBars`)

Designed on the redesign page `mob-bars.html`. A flat, dim bar over every living entity within `MobBarsConfig` `range`
blocks (default 10): hostile (`Enemy`, bosses included) #a83a3a, players #4a76b8, every other mob #5c9a46, each kind
toggled in the client config (`haven_mob_health_bars-client.toml`); `notHostileWhen` WHEN_DAMAGED (default) or ALWAYS
(the green bars only below full health, after the damage trail drains). Not the player, the mount the player rides
(`hasIndirectPassenger`), entities not in the client level (a mob held with Carry On is a drawn copy), armor stands, invisible or dying entities.
- Beyond the range, only the entity under the crosshair gets a bar (`LookTarget`, config `lookedAt` default true,
  `lookRange` default 64, 8-128): once per client tick a vanilla-style pick (`ProjectileUtil.getEntityHitResult`,
  boxes inflated by pick radius) from the camera, then a block raycast (`ClipContext` OUTLINE, fluid NONE) eye to hit
  point; a block in between means no target. Same kind toggles and filters as the normal bars.
- `RenderLivingEvent.Post`, drawn into the entity pass's buffers with `RenderType.textBackground()` (as a name tag
  background: same quad order, which matters for culling), billboarded with `cameraOrientation()`, in model px (1/16
  block). Placed like vanilla's name tag (`NameTags`): at the NAME_TAG attachment plus half a block (riders, babies, scaled and modded mobs line up); when the tag is drawn this frame (`RenderNameTagEvent` result at LOWEST priority, else the renderer's own `shouldShowName` via `EntityRendererInvoker`, within the nameplate distance), just above the tag's background (anchor + 0.025) with a 0.04 gap, so it never covers the text. Depth-tested, so walls hide it (no
  line-of-sight raycast); full brightness (the colours are already dim); outside the GUI, so DME never sees it.
- The pieces are coplanar and never overlap (edge strips, fill with a lighter top strip, trail, empty backing): no
  z-fighting, no doubled alpha.
- Damage trail per entity (`WeakHashMap`): holds the pre-hit health 400 ms, then drains over 600 ms; healing clears it.
- Armour durability strip (`ArmourWear`, config `armourDurability` default true): with damageable armour worn, the bar
  splits at the same size, health on the top 75 %, the durability left (sum of max damage - damage over sum of max
  damage) on the bottom 25 % (0.35 of 1.4 model px) in steel grey #9aa1a8, no trail. Slots HEAD, CHEST, LEGS, FEET and BODY (wolf armour; horse
  armour only with a `max_damage` component, vanilla's is not damageable), plus a Drekki's barding
  (`DrekkiEntity.bardingItem()` by MethodHandle). Below 100 % it counts as damaged for WHEN_DAMAGED, which covers every
  non-hostile bar: other mobs and friendly (blue) players (`notHostileWhen`; hostile mobs and players always show). Computed at most once
  per client tick per entity (`WeakHashMap`, keyed by game time), only for entities that passed the other filters.
  The client sees other entities' wear: vanilla resends a worn stack when its components change (`equipmentHasChanged`).
  Players are read in `RenderPlayerEvent.Pre` at HIGHEST (`ArmourWear.beforePlayerRender`): Cosmetic Armor Reworked swaps
  the worn armour for the cosmetic stacks (EMPTY for a "show skin" slot) from its Pre (HIGH) to its Post (LOW), the window
  the bar draws in, so players who hide their armour got no strip (2026-11-24, both players on the server hide all four).
  Drekki Rancher (to 0.5.1) keeps the barding stack on the server only (synced: tier and glint), so the client reads
  EMPTY and a Drekki gets no strip until that mod syncs the stack.
- Iris: skipped in the shadow pass (`IrisApi.isRenderingShadowPass`, by reflection). Hidden with F1.
- Log prefix `[mob bars]` (`Log`): config state on change, Iris found or not, errors once.
