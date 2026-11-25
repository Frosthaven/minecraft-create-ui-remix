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
            if (Reflect.is(b, "com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour")) {
                // a factory gauge (FactoryPanelBehaviour.onShortInteract): a wrench turns its arrows; with no item set it
                // takes one; once set, a Stock Link item links it, anything else opens its settings
                boolean set = Reflect.call(b, "getFilter") instanceof ItemStack f && !f.isEmpty();
                boolean linked = Reflect.get(b, "targeting") instanceof java.util.Collection<?> t && !t.isEmpty()
                        || Reflect.get(b, "targetedByLinks") instanceof java.util.Map<?, ?> l && !l.isEmpty();
                if (wrench && linked) KeyRules.add(rows, "Cycle arrows", "key.use");
                else if (!set) KeyRules.add(rows, hand.isEmpty() ? "Choose item" : "Set item", "key.use");
                else if (Reflect.is(hand.getItem(), "com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem")) KeyRules.add(rows, "Link", "key.use");
                else KeyRules.add(rows, "Configure", "key.use");
                return true;
            }
            if (Reflect.is(b, FILTER)) {
                boolean set = Reflect.call(b, "getFilter") instanceof ItemStack f && !f.isEmpty();
                // the short click sets the filter only when the behaviour allows it for this item (FilteringBehaviour.
                // canShortInteract: not with Create's wrench or a Mechanical Arm): those get the hold-to-set screen only
                boolean shortOk = !(Reflect.call(b, "canShortInteract", hand) instanceof Boolean ok) || ok;
                if (!hand.isEmpty() && shortOk) KeyRules.add(rows, set ? "Replace filter" : "Set filter", "key.use");
                if (Reflect.bool(Reflect.call(b, "isCountVisible"))) KeyRules.add(rows, "Amount (hold)", "key.use");
            } else if (Reflect.is(b, "com.simibubi.create.content.kinetics.crank.ValveHandleBlockEntity$ValveHandleScrollValueBehaviour")) {
                // the valve's angle box: a short click still turns it (onShortInteract -> ValveHandleBlock.clicked), a hold edits
                valve(rows, p, hand, mc.level.getBlockState(bh.getBlockPos()));
                KeyRules.add(rows, "Edit angle (hold)", "key.use");
            } else if (Reflect.bool(Reflect.call(b, "acceptsValueSettings"))) {
                KeyRules.add(rows, "Edit (hold)", "key.use");
            }
            return true;
        }
        return false;
    }

    /** Contraption Controls on an elevator contraption under the crosshair: wheel picks a floor, use goes there. */
    /** A moving contraption's block under the crosshair (ContraptionHandlerClient.getRayInputs and rayTraceContraption,
     *  as Create's own click handling): the nearest one, and only when it is closer than the world block looked at. */
    record Hit(Entity entity, Object contraption, BlockPos local, net.minecraft.world.level.block.state.BlockState state, BlockHitResult hit) {}

    static Hit contraptionHit(Minecraft mc, LocalPlayer p) {
        Class<?> ace = Reflect.cls("com.simibubi.create.content.contraptions.AbstractContraptionEntity");
        if (ace == null || p.isSpectator()) return null;
        Object inputs = Reflect.callStatic("com.simibubi.create.content.contraptions.ContraptionHandlerClient", "getRayInputs", p);
        if (inputs == null) return null;
        Vec3 from = (Vec3) Reflect.call(inputs, "getFirst"), to = (Vec3) Reflect.call(inputs, "getSecond");
        double best = mc.hitResult instanceof BlockHitResult wb && wb.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                ? KeySable.world(mc.level, wb.getLocation()).distanceToSqr(from) : Double.MAX_VALUE;   // ship blocks hit in plot space
        Hit out = null;
        for (Entity e : mc.level.getEntitiesOfClass(Entity.class, p.getBoundingBox().inflate(16), ace::isInstance)) {
            Object contraption = Reflect.call(e, "getContraption");
            if (contraption == null) continue;
            if (!(Reflect.callStatic("com.simibubi.create.content.contraptions.ContraptionHandlerClient", "rayTraceContraption", from, to, e) instanceof BlockHitResult hit)
                    || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) continue;
            BlockPos local = hit.getBlockPos();
            if (!(Reflect.call(contraption, "getBlocks") instanceof java.util.Map<?, ?> blocks) || !(blocks.get(local) instanceof Object info)) continue;
            if (!(Reflect.call(info, "state") instanceof net.minecraft.world.level.block.state.BlockState bs)) continue;
            Vec3 world = Reflect.call(e, "toGlobalVector", hit.getLocation(), 1f) instanceof Vec3 v ? v : hit.getLocation();
            double d = KeySable.world(mc.level, world).distanceToSqr(from);
            if (d < best) { best = d; out = new Hit(e, contraption, local, bs, hit); }
        }
        return out;
    }

    /** Contraption Controls on an elevator contraption under the crosshair: wheel picks a floor, use goes there. */
    static boolean elevator(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, Hit h) {
        if (h == null || !h.contraption().getClass().getName().endsWith("ElevatorContraption")) return false;
        // the controls' slot only (ElevatorControlsHandler.slot.testHit(level, local pos, state, hit inside the block))
        Object slot = Reflect.getStatic("com.simibubi.create.content.contraptions.elevator.ElevatorControlsHandler", "slot");
        if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(h.state().getBlock()).toString().equals("create:contraption_controls")
                && Reflect.bool(Reflect.call(slot, "testHit", mc.level, h.local(), h.state(), h.hit().getLocation().subtract(Vec3.atLowerCornerOf(h.local()))))) {
            rows.add(new KeyRules.Row(List.of(WHEEL), "Choose floor", null, false));
            KeyRules.add(rows, "Go", "key.use");
            return true;
        }
        return false;
    }

    /** A moving contraption's block (AbstractContraptionEntity.handlePlayerInteraction): a seat seats the player unless
     *  they hold a wrench, ride already, or a player sits there (a mob is put off); a block with an interaction behaviour
     *  (Contraption.interactors: a train's controls drive it, doors, levers) uses it. True when it decided. */
    static boolean moving(List<KeyRules.Row> rows, LocalPlayer p, ItemStack hand, Hit h) {
        if (h == null) return false;
        Object seats = Reflect.call(h.contraption(), "getSeats");
        int seat = seats instanceof List<?> l ? l.indexOf(h.local()) : -1;
        if (seat >= 0 && !KeyRules.id(hand).equals("create:wrench")) {
            if (p.isPassenger()) return true;
            if (Reflect.call(h.contraption(), "getSeatMapping") instanceof java.util.Map<?, ?> map)
                for (var en : map.entrySet())
                    if (Integer.valueOf(seat).equals(en.getValue()) && h.entity().getPassengers().stream().anyMatch(x -> x.getUUID().equals(en.getKey()) && x instanceof net.minecraft.world.entity.player.Player))
                        return true;   // a player sits there
            KeyRules.add(rows, "Ride", "key.use");
            return true;
        }
        boolean wrench = KeyRules.id(hand).equals("create:wrench");
        if (Reflect.get(h.contraption(), "interactors") instanceof java.util.Map<?, ?> inter && inter.get(h.local()) instanceof Object b) {
            String word = interactorWord(b.getClass().getSimpleName(), hand, wrench);
            if (word != null) { if (!word.isEmpty()) KeyRules.add(rows, word, "key.use"); return true; }
        }
        // mounted storage (MountedStorageManager.handlePlayerStorageInteraction): chests, barrels, vaults on it open
        Object storage = Reflect.call(h.contraption(), "getStorage");
        if (Reflect.get(storage, "interactablePositions") instanceof java.util.Set<?> open && open.contains(h.local())) {
            KeyRules.add(rows, "Open", "key.use");
            return true;
        }
        // Create's wrench on a carriage block nothing else took: TrainRelocator.carriageWrenched
        if (wrench && Reflect.is(h.entity(), "com.simibubi.create.content.trains.entity.CarriageContraptionEntity")) {
            KeyRules.add(rows, "Relocate train", "key.use");
            return true;
        }
        return false;
    }

    /** The use of a moving block's interaction behaviour (Contraption.interactors), by its class; null: it passes. */
    private static String interactorWord(String cls, ItemStack hand, boolean wrench) {
        if (cls.equals("ControlsInteractionBehaviour")) return wrench ? null : "Drive";          // returns false for the wrench
        if (cls.contains("HandcarControls")) return wrench ? "Pick up handcar" : "Drive";
        if (cls.equals("ContraptionControlsMovingInteraction")) return "Toggle";                 // the contraption's actors
        if (cls.equals("DeployerMovingInteraction")) return wrench ? "Change mode" : "Swap item";
        if (cls.contains("Lever")) return "Pull";
        if (cls.contains("Depot")) return hand.isEmpty() ? "Take" : "Place";
        if (cls.contains("Door") || cls.contains("Gate") || cls.contains("GuiBlock") || cls.contains("Menu")) return "Open";
        return "Use";
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

    /** A chain conveyor's chain under the crosshair (ChainConveyorInteractionHandler.selectedLift: Create selects one only
     *  while a chain-rideable item, a Frogport or a package is held; every chain type on Create's chain conveyors): an
     *  item of create:chain_rideable in either hand rides it (sneak: removes the chain, ChainConveyorConnectionPacket);
     *  a Frogport places on it; a package hangs on it (ChainConveyorInteractionHandler.onUse). */
    static boolean chain(List<KeyRules.Row> rows, LocalPlayer p) {
        String h = "com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler";
        if (Reflect.cls(h) == null || Reflect.getStatic(h, "selectedLift") == null) return false;
        var rideable = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "chain_rideable"));
        ItemStack main = p.getMainHandItem();
        if (p.isHolding(s -> s.is(rideable))) {
            KeyRules.add(rows, "Ride", "key.use");
            KeyRules.add(rows, "Remove chain", "key.sneak", "key.use");
        } else if (KeyRules.id(main).equals("create:package_frogport")) KeyRules.add(rows, "Place Frogport", "key.use");
        else if (Reflect.bool(Reflect.callStatic("com.simibubi.create.content.logistics.box.PackageItem", "isPackage", main))) KeyRules.add(rows, "Hang package", "key.use");
        return true;
    }

    /** A Create valve handle (ValveHandleBlock.onBlockActivated, an event: any item, sneaking too, build permission):
     *  a dye of another colour dyes it, else use turns it and sneak-use turns it back; Create's wrench with sneak picks it
     *  up (the wrench's own use). True when it decided. */
    static boolean valve(List<KeyRules.Row> rows, LocalPlayer p, ItemStack hand, net.minecraft.world.level.block.state.BlockState state) {
        Object b = state.getBlock();
        if (!Reflect.is(b, "com.simibubi.create.content.kinetics.crank.ValveHandleBlock")) return false;
        if (!p.mayBuild()) return true;
        var dye = net.minecraft.world.item.DyeColor.getColor(hand);
        if (dye != null && dye != Reflect.get(b, "color")) { KeyRules.add(rows, "Dye", "key.use"); return true; }
        KeyRules.add(rows, "Turn", "key.use");
        KeyRules.add(rows, KeyRules.id(hand).equals("create:wrench") ? "Pick up" : "Turn back", "key.sneak", "key.use");
        return true;
    }

    /** Targets Create picks itself, before the block under the crosshair:
     *  a curved track (TrackBlockOutline.result; CurvedTrackInteraction.onClickInput, main hand): a station, signal or
     *  observer item places on the curve; Create's wrench with sneak removes the curve;
     *  super glue (CreateClient.GLUE_HANDLER.onMouseInput, main hand, build permission): the first click picks a corner,
     *  then use confirms and sneak-use discards; attack removes the glue looked at. True when it decided. */
    static boolean picked(List<KeyRules.Row> rows, LocalPlayer p, ItemStack hand) {
        String outline = "com.simibubi.create.content.trains.track.TrackBlockOutline";
        if (Reflect.cls(outline) != null && Reflect.getStatic(outline, "result") != null) {
            if (Reflect.is(hand.getItem(), "com.simibubi.create.content.trains.track.TrackTargetingBlockItem")) { KeyRules.add(rows, "Place on curve", "key.use"); return true; }
            if (KeyRules.id(hand).equals("create:wrench")) { KeyRules.add(rows, "Remove curve", "key.sneak", "key.use"); return true; }
        }
        if (Reflect.is(hand.getItem(), "com.simibubi.create.content.contraptions.glue.SuperGlueItem") && p.mayBuild()) {
            Object glue = Reflect.getStatic("com.simibubi.create.CreateClient", "GLUE_HANDLER");
            if (glue == null) return false;
            if (Reflect.get(glue, "selected") != null) KeyRules.add(rows, "Remove glue", "key.attack");
            if (Reflect.get(glue, "firstPos") == null) KeyRules.add(rows, "Select corner", "key.use");
            else { KeyRules.add(rows, "Confirm", "key.use"); KeyRules.add(rows, "Discard", "key.sneak", "key.use"); }
            return true;
        }
        return false;
    }
}
