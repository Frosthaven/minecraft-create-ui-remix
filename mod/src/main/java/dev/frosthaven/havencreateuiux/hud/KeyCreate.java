package dev.frosthaven.havencreateuiux.hud;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Create's general interactions, from its own classes (by name: without Create, no rule): every value box and filter slot
 * (ValueSettingsBehaviour on a SmartBlockEntity, as ValueSettingsInputHandler.onBlockActivated decides: testHit,
 * mayInteract, wrench-only boxes, no sneak; Create's words "Click and hold to edit", "Click with item to set",
 * "Click and hold for amount"), and elevator floors (looking at Contraption Controls on an elevator contraption, through
 * ContraptionHandlerClient.rayTraceContraption: the mouse wheel picks a floor, ElevatorControlsHandler.onScroll; use goes).
 */
final class KeyCreate {
    private static final String VSB = "com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour";
    private static final String EDGE = "com.simibubi.create.foundation.blockEntity.behaviour.edgeInteraction.EdgeInteractionBehaviour";
    private static final String LINK = "com.simibubi.create.content.redstone.link.LinkBehaviour";
    private static final String FILTER = "com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour";
    /** The mouse wheel as a cap (not a key mapping: never registered, never in options). */
    static final KeyMapping WHEEL = new KeyMapping("key.haven_create_ui_ux.wheel", InputConstants.UNKNOWN.getValue(), "key.categories.haven_create_ui_ux");

    private KeyCreate() {}

    /** Value boxes and filter slots under the crosshair; true when one is hit (Create then takes the click). */
    @SuppressWarnings("unchecked")
    static boolean valueBox(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, ItemStack hand, BlockHitResult bh) {
        if (Reflect.cls(VSB) == null || p.isShiftKeyDown()) return false;
        var be = mc.level.getBlockEntity(bh.getBlockPos());
        if (!Reflect.is(be, "com.simibubi.create.foundation.blockEntity.SmartBlockEntity") || !(Reflect.call(be, "getAllBehaviours") instanceof Collection<?> all)) return false;
        Vec3 hit = bh.getLocation();
        boolean wrench = hand.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_WRENCH);
        for (Object b : all) {
            // frequency slots (LinkBehaviour, LinkHandler.onBlockActivated: not with a Linked Controller or a wrench; the
            // held item becomes the slot's frequency, an empty hand clears it): Create's links and every addon's
            if (Reflect.is(b, LINK) && !hand.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_WRENCH) && !KeyRules.id(hand).equals("create:linked_controller")
                    && (Reflect.bool(Reflect.call(b, "testHit", Boolean.TRUE, hit)) || Reflect.bool(Reflect.call(b, "testHit", Boolean.FALSE, hit)))) {
                KeyRules.add(rows, hand.isEmpty() ? "Clear frequency" : "Set frequency", "key.use");
                return true;
            }
            // an edge toggle (EdgeInteractionBehaviour, EdgeInteractionHandler.onBlockActivated: the behaviour's required item
            // (a crafter's: the wrench), an edge under the crosshair): mechanical crafters link their inputs this way
            if (Reflect.is(b, EDGE) && Reflect.get(b, "requiredItem") instanceof java.util.function.Predicate<?> need
                    && ((java.util.function.Predicate<Object>) need).test(hand.getItem())
                    && Reflect.callStatic("com.simibubi.create.foundation.blockEntity.behaviour.edgeInteraction.EdgeInteractionHandler", "getActivatedDirection",
                            mc.level, bh.getBlockPos(), bh.getDirection(), hit, b) != null) {
                KeyRules.add(rows, "Toggle link", "key.use");
                return true;
            }
            if (!Reflect.is(b, VSB) || !Reflect.bool(Reflect.call(b, "testHit", hit)) || !Reflect.bool(Reflect.call(b, "mayInteract", p))) continue;
            if (Reflect.bool(Reflect.call(b, "onlyVisibleWithWrench")) && !wrench) continue;
            if (Reflect.bool(Reflect.call(b, "bypassesInput", hand))) continue;
            if (Reflect.is(b, FILTER)) {
                boolean set = Reflect.call(b, "getFilter") instanceof ItemStack f && !f.isEmpty();
                if (!hand.isEmpty()) KeyRules.add(rows, set ? "Replace filter" : "Set filter", "key.use");
                if (Reflect.bool(Reflect.call(b, "isCountVisible"))) KeyRules.add(rows, "Amount (hold)", "key.use");
            } else if (Reflect.bool(Reflect.call(b, "acceptsValueSettings"))) {
                KeyRules.add(rows, "Edit (hold)", "key.use");
            }
            return true;
        }
        return false;
    }

    /** Contraption Controls on an elevator contraption under the crosshair: wheel picks a floor, use goes there. */
    static boolean elevator(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p) {
        Class<?> ace = Reflect.cls("com.simibubi.create.content.contraptions.AbstractContraptionEntity");
        if (ace == null || p.isSpectator()) return false;
        Object inputs = Reflect.callStatic("com.simibubi.create.content.contraptions.ContraptionHandlerClient", "getRayInputs", p);
        if (inputs == null) return false;
        Vec3 from = (Vec3) Reflect.call(inputs, "getFirst"), to = (Vec3) Reflect.call(inputs, "getSecond");
        for (Entity e : mc.level.getEntitiesOfClass(Entity.class, p.getBoundingBox().inflate(8), ace::isInstance)) {
            Object contraption = Reflect.call(e, "getContraption");
            if (contraption == null || !contraption.getClass().getName().endsWith("ElevatorContraption")) continue;
            if (!(Reflect.callStatic("com.simibubi.create.content.contraptions.ContraptionHandlerClient", "rayTraceContraption", from, to, e) instanceof BlockHitResult hit)) continue;
            BlockPos local = hit.getBlockPos();
            if (!(Reflect.call(contraption, "getBlocks") instanceof java.util.Map<?, ?> blocks) || !(blocks.get(local) instanceof Object info)) continue;
            Object state = Reflect.call(info, "state");
            // the controls' slot only (ElevatorControlsHandler.slot.testHit(level, local pos, state, hit inside the block))
            Object slot = Reflect.getStatic("com.simibubi.create.content.contraptions.elevator.ElevatorControlsHandler", "slot");
            if (state instanceof net.minecraft.world.level.block.state.BlockState bs
                    && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(bs.getBlock()).toString().equals("create:contraption_controls")
                    && Reflect.bool(Reflect.call(slot, "testHit", mc.level, local, bs, hit.getLocation().subtract(Vec3.atLowerCornerOf(local))))) {
                rows.add(new KeyRules.Row(List.of(WHEEL), "Choose floor", null, false));
                KeyRules.add(rows, "Go", "key.use");
                return true;
            }
        }
        return false;
    }

    /** Create's mobs (contextual): a mob seated on a train takes a schedule (ScheduleItemEntityInteraction: root vehicle a
     *  CarriageContraptionEntity, a living non-player, the schedule not on cooldown); a mob seated by a Stock Ticker is
     *  a shopkeeper (StockTickerInteractionHandler.getStockTickerPosition): its order screen, or a shopping list pays. */
    static boolean mob(List<KeyRules.Row> rows, LocalPlayer p, Entity e, ItemStack hand) {
        if (!(e instanceof net.minecraft.world.entity.LivingEntity) || e instanceof net.minecraft.world.entity.player.Player) return false;
        if (Reflect.is(e.getRootVehicle(), "com.simibubi.create.content.trains.entity.CarriageContraptionEntity")
                && Reflect.is(hand.getItem(), "com.simibubi.create.content.trains.schedule.ScheduleItem") && !p.getCooldowns().isOnCooldown(hand.getItem())) {
            KeyRules.add(rows, "Give schedule", "key.use");
            return true;
        }
        if (Reflect.callStatic("com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler", "getStockTickerPosition", e) != null) {
            KeyRules.add(rows, KeyRules.id(hand).equals("create:shopping_list") ? "Checkout" : "Order", "key.use");
            return true;
        }
        return false;
    }

    private static final java.util.Map<String, Boolean> APPLY = new java.util.concurrent.ConcurrentHashMap<>();

    /** Create's item application (deployer recipes also done by hand: casing on logs, and every mod's and KubeJS's
     *  create:item_application recipes, read from the recipe list): the looked-at block is the first ingredient and the
     *  held item the second. Cached per block and item; the cache clears when recipes reload (a new level). */
    static boolean itemApplication(List<KeyRules.Row> rows, Minecraft mc, ItemStack hand, net.minecraft.world.level.block.state.BlockState state) {
        if (hand.isEmpty() || mc.level == null) return false;
        var type = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "item_application"));
        if (type == null) return false;
        String key = System.identityHashCode(mc.level.getRecipeManager()) + "|" + state.getBlock() + "|" + hand.getItem();
        boolean match = APPLY.computeIfAbsent(key, k -> {
            ItemStack block = new ItemStack(state.getBlock().asItem());
            if (block.isEmpty()) return false;
            @SuppressWarnings({"unchecked", "rawtypes"})
            List<net.minecraft.world.item.crafting.RecipeHolder<?>> all = (List) mc.level.getRecipeManager().getAllRecipesFor((net.minecraft.world.item.crafting.RecipeType) type);
            boolean found = false;
            for (var h : all) {
                var ing = h.value().getIngredients();
                if (ing.size() >= 2 && ing.get(0).test(block) && ing.get(1).test(hand)) { found = true; break; }
            }
            return found;
        });
        if (match) KeyRules.add(rows, "Apply", "key.use");
        return match;
    }

    /** Create's mechanical crafter (MechanicalCrafterBlock.useItemOn, the front face only; onWrenched): a wrench turns its
     *  path arrow; while crafting any click ejects the grid; idle: an item goes in (a slot cover covers an empty, uncovered
     *  slot), an empty hand takes the slot's item, or takes a cover off. */
    static boolean crafter(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, ItemStack hand, net.minecraft.world.level.block.state.BlockState state, BlockHitResult bh) {
        if (!Reflect.is(state.getBlock(), "com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock")) return false;
        var facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        if (bh.getDirection() != facing) return true;   // its sides and back: nothing here (a wrench links inputs on the edges)
        boolean wrench = KeyRules.id(hand).equals("create:wrench");
        if (wrench) { KeyRules.add(rows, "Turn path", "key.use"); return true; }
        Object be = mc.level.getBlockEntity(bh.getBlockPos());
        Object phase = Reflect.get(be, "phase");
        if (phase != null && !String.valueOf(phase).equals("IDLE")) { KeyRules.add(rows, "Eject", "key.use"); return true; }
        boolean covered = Reflect.bool(Reflect.get(be, "covered"));
        Object inv = Reflect.call(be, "getInventory");
        ItemStack slot = Reflect.call(inv, "getItem", 0) instanceof ItemStack st ? st : ItemStack.EMPTY;
        if (!hand.isEmpty()) {
            if (KeyRules.id(hand).equals("create:crafter_slot_cover")) { if (!covered && slot.isEmpty()) KeyRules.add(rows, "Cover slot", "key.use"); }
            else KeyRules.add(rows, "Insert", "key.use");
            return true;
        }
        if (!slot.isEmpty()) KeyRules.add(rows, "Take", "key.use");
        else if (covered) KeyRules.add(rows, "Remove cover", "key.use");
        return true;
    }
}
