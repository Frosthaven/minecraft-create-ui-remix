package dev.frosthaven.havencreateuiux.hud;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Addon uses that run in events or in the mod's own entity code, each from that mod's checks: Cataclysm's stun, Waxed
 * Up's bottle filling, Pipez pipe sides and its destination tool, Easy Villagers blocks, Climbable Ropes' rope grab,
 * the Living Things elephant, the Steam 'n' Rails conductor and Curios' equip on use. True when a rule decided.
 */
final class KeyAddons {
    private KeyAddons() {}

    private static final String PIPEZ = "de.maxhenkel.pipez.", EV = "de.maxhenkel.easyvillagers.";

    /** Cataclysm's stun (ServerEventHandler.onPlayerInteract*): every click is cancelled while it lasts. */
    static boolean stunned(LocalPlayer p) {
        var stun = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("cataclysm", "stun"));
        return stun.isPresent() && p.hasEffect(stun.get());
    }

    /** Block uses decided by events before the block's own use. */
    static boolean block(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, ItemStack hand, BlockState state, BlockHitResult bh) {
        Object b = state.getBlock(), item = hand.getItem();
        var be = mc.level.getBlockEntity(bh.getBlockPos());
        // Waxed Up (WaxedUpEvents.fillBottleFromTank): a glass bottle on a fluid handler that holds a bottled fluid
        if (hand.is(Items.GLASS_BOTTLE) && Reflect.cls("dev.mrmulle.waxedup.WaxedUpEvents") != null) {
            Object fluids = mc.level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, bh.getBlockPos(), bh.getDirection());
            if (fluids != null && Reflect.callStatic("dev.mrmulle.waxedup.WaxedUpEvents", "findBottleMatch", fluids) != null) {
                KeyRules.add(rows, "Fill bottle", "key.use");
                return true;
            }
        }
        // Pipez (BlockEvents.onDestinationToolClick): the filter destination tool marks any block entity but a pipe
        if (Reflect.is(item, PIPEZ + "items.FilterDestinationToolItem") && be != null && !Reflect.is(b, PIPEZ + "blocks.PipeBlock")) {
            KeyRules.add(rows, "Set destination", "key.use");
            return true;
        }
        // Pipez pipe sides (PipeBlock.onPipeSideForceActivated, then the pipe's useItemOn): on the side looked at, an
        // extracting side opens its screen; sneaking with an upgrade swaps that side's upgrade. Wrenches: KeyWrench.
        if (Reflect.is(b, PIPEZ + "blocks.PipeBlock") && !KeyWrench.isWrench(hand)) {
            Object sel = Reflect.call(b, "getSelection", state, mc.level, bh.getBlockPos(), bh.getLocation());
            if (Reflect.call(sel, "getKey") instanceof Direction side && Reflect.bool(Reflect.call(b, "isExtracting", mc.level, bh.getBlockPos(), side))) {
                if (Reflect.is(item, PIPEZ + "items.UpgradeItem")) KeyRules.add(rows, "Swap upgrade", "key.sneak", "key.use");
                if (!p.isSecondaryUseActive() || hand.isEmpty()) KeyRules.add(rows, "Open", "key.use");
            }
            return true;
        }
        // Easy Villagers (VillagerBlockBase.overrideClick): sneaking with an empty hand still uses the block, which takes
        // out what it holds; a trader takes a valid workstation, a farmer a valid seed
        if (Reflect.is(b, EV + "blocks.VillagerBlockBase")) {
            if (Reflect.is(be, EV + "blocks.tileentity.TraderTileentityBase") && item instanceof net.minecraft.world.item.BlockItem bi
                    && !Reflect.bool(Reflect.call(be, "hasWorkstation")) && Reflect.bool(Reflect.call(be, "isValidBlock", bi.getBlock()))) {
                KeyRules.add(rows, "Add workstation", "key.use");
                return true;
            }
            if (Reflect.is(be, EV + "blocks.tileentity.FarmerTileentity") && Reflect.call(be, "getCrop") == null
                    && Reflect.bool(Reflect.call(be, "isValidSeed", hand.getItem()))) {
                KeyRules.add(rows, "Plant seed", "key.use");
                return true;
            }
            if (hand.isEmpty() && (Reflect.bool(Reflect.call(be, "hasVillager")) || Reflect.bool(Reflect.call(be, "hasVillager1"))
                    || Reflect.bool(Reflect.call(be, "hasVillager2")))) KeyRules.add(rows, "Take out", "key.sneak", "key.use");
            return false;   // the block's data rows (Insert, Open) still apply
        }
        return false;
    }

    /** Climbable Ropes (RopeRideDispatcher.tryHoverEmbark): use with an empty main hand grabs the strand looked at. */
    static boolean rope(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p) {
        String strand = "dev.matejhozlar.climbableropes.client.ride.StrandClimbController";
        if (Reflect.cls(strand) == null || !p.getMainHandItem().isEmpty() || climbing()) return false;
        if (Reflect.callStatic(strand, "findVerticalHover", mc, p) == null) return false;
        KeyRules.add(rows, "Grab rope", "key.use");
        return true;
    }

    /** On a Climbable Ropes strand, plunger rope or zipline (each controller's isRiding). */
    static boolean climbing() {
        String r = "dev.matejhozlar.climbableropes.client.ride.";
        return Reflect.bool(Reflect.callStatic(r + "StrandClimbController", "isRiding"))
                || Reflect.bool(Reflect.callStatic(r + "PlungerClimbController", "isRiding"))
                || Reflect.bool(Reflect.callStatic(r + "PlungerZiplineController", "isRiding"));
    }

    /** Mobs whose own mobInteract decides. */
    static boolean entity(List<KeyRules.Row> rows, LocalPlayer p, Entity e, ItemStack hand) {
        if (Reflect.is(e, "com.tristankechlo.livingthings.entity.ElephantEntity") && e instanceof net.minecraft.world.entity.TamableAnimal el) {
            // ElephantEntity.mobInteract: tamed adults carry riders (saddled) and a chest (sneak opens it)
            boolean tame = el.isTame(), baby = el.isBaby();
            boolean saddled = Reflect.bool(Reflect.call(e, "isSaddled")), chest = Reflect.bool(Reflect.call(e, "hasChest"));
            if (hand.isEmpty() && tame && !baby) {
                if (chest) KeyRules.add(rows, "Inventory", "key.sneak", "key.use");
                if (saddled && e.getPassengers().isEmpty()) KeyRules.add(rows, "Ride", "key.use");
            } else if (Reflect.bool(Reflect.call(e, "isFood", hand))) {
                KeyRules.add(rows, baby ? "Feed" : tame && el.getHealth() < el.getMaxHealth() ? "Heal" : "Breed", "key.use");
            } else if (!baby && !tame && Reflect.bool(Reflect.call(e, "isTamingItem", hand))) KeyRules.add(rows, "Tame", "key.use");
            else if (tame && !baby && hand.is(Items.SADDLE) && !saddled) KeyRules.add(rows, "Saddle", "key.use");
            else if (tame && !baby && saddled && hand.is(Items.CHEST) && !chest) KeyRules.add(rows, "Add chest", "key.use");
            return true;
        }
        if (Reflect.is(e, "com.railwayteam.railways.content.conductor.ConductorEntity") && e instanceof LivingEntity c) {
            // ConductorEntity.mobInteract
            boolean box = Reflect.bool(Reflect.call(e, "isCarryingToolbox"));
            if (hand.getItem() instanceof net.minecraft.world.item.DyeItem) KeyRules.add(rows, "Dye", "key.use");
            else if (KeyRules.id(hand).equals("create:andesite_casing") && c.getHealth() < c.getMaxHealth()) KeyRules.add(rows, "Repair", "key.use");
            else if (!box && Reflect.bool(Reflect.call(e, "isToolbox", hand))) KeyRules.add(rows, "Give toolbox", "key.use");
            else if (box) {
                if (hand.isEmpty()) KeyRules.add(rows, "Take toolbox", "key.sneak", "key.use");
                KeyRules.add(rows, "Open toolbox", "key.use");
            } else if (hand.isEmpty() && Reflect.call(e, "getHeldSchedules") instanceof List<?> s && !s.isEmpty()) KeyRules.add(rows, "Take schedules", "key.use");
            return !rows.isEmpty();
        }
        return false;
    }

    /** Curios (CuriosEventHandler.curioRightClick): a held curio that equips on use, while a slot for it is free. */
    static boolean curioEquip(LocalPlayer p, ItemStack s) {
        if (s.isEmpty() || Reflect.cls("top.theillusivec4.curios.api.CuriosApi") == null) return false;
        if (!(Reflect.callStatic("top.theillusivec4.curios.api.CuriosApi", "getCurio", s) instanceof java.util.Optional<?> c) || c.isEmpty()) return false;
        if (!(Reflect.callStatic("top.theillusivec4.curios.api.CuriosApi", "getCuriosInventory", p) instanceof java.util.Optional<?> inv) || inv.isEmpty()) return false;
        if (!(Reflect.call(inv.get(), "getCurios") instanceof java.util.Map<?, ?> slots)) return false;
        Class<?> ctx = Reflect.cls("top.theillusivec4.curios.api.SlotContext");
        if (ctx == null) return false;
        for (var en : slots.entrySet()) {
            Object stacks = Reflect.call(en.getValue(), "getStacks");
            int n = Reflect.call(stacks, "getSlots") instanceof Integer i ? i : 0;
            for (int i = 0; i < n; i++) {
                if (!(Reflect.call(stacks, "getStackInSlot", i) instanceof ItemStack in) || !in.isEmpty()) continue;
                try {
                    Object sc = ctx.getConstructor(String.class, LivingEntity.class, int.class, boolean.class, boolean.class)
                            .newInstance(en.getKey(), p, i, false, true);
                    if (Reflect.bool(Reflect.callStatic("top.theillusivec4.curios.api.CuriosApi", "isStackValid", sc, s))
                            && Reflect.bool(Reflect.call(c.get(), "canEquipFromUse", sc)) && Reflect.bool(Reflect.call(c.get(), "canEquip", sc))) return true;
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    return false;
                }
            }
        }
        return false;
    }
}
