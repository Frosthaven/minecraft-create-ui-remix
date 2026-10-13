# mobbars/: haven-mob-health-bars (`haven_mob_health_bars`)

Its own client-only mod (split out of haven-create-ui-ux): needs only NeoForge and Minecraft 1.21.1. Part of the
minecraft-create-ui-remix monorepo (root CLAUDE.md: layout, install, release). Build like `mod/` (JDK 17 for Gradle).
Classes: `HavenMobHealthBars` (registers config, config screen, the RenderLivingEvent listener), `MobBarsConfig`,
`MobBars`, `Log`, `mixin/RecentAttackerMixin` (hostile players: any player who hit a player in the last 30 s).

## Mob health bars (`MobBars`)

Designed on the redesign page `mob-bars.html`. A flat, dim bar over every living entity within `MobBarsConfig` `range`
blocks (default 10): hostile (`Enemy`, bosses included) #a83a3a, players #4a76b8, every other mob #5c9a46, each kind
toggled in the client config (`haven_mob_health_bars-client.toml`). Not the player, armor stands, invisible or dying entities.
- `RenderLivingEvent.Post`, drawn into the entity pass's buffers with `RenderType.textBackground()` (as a name tag
  background: same quad order, which matters for culling), billboarded with `cameraOrientation()`, in model px (1/16
  block). Placed like vanilla's name tag (`NameTags`): at the NAME_TAG attachment plus half a block (riders, babies, scaled and modded mobs line up); when the tag is drawn this frame (`RenderNameTagEvent` result at LOWEST priority, else the renderer's own `shouldShowName` via `EntityRendererInvoker`, within the nameplate distance), just above the tag's background (anchor + 0.025) with a 0.04 gap, so it never covers the text. Depth-tested, so walls hide it (no
  line-of-sight raycast); full brightness (the colours are already dim); outside the GUI, so DME never sees it.
- The pieces are coplanar and never overlap (edge strips, fill with a lighter top strip, trail, empty backing): no
  z-fighting, no doubled alpha.
- Damage trail per entity (`WeakHashMap`): holds the pre-hit health 400 ms, then drains over 600 ms; healing clears it.
- Iris: skipped in the shadow pass (`IrisApi.isRenderingShadowPass`, by reflection). Hidden with F1.
- Log prefix `[mob bars]` (`Log`): config state on change, Iris found or not, errors once.
