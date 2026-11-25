package dev.frosthaven.havencreateuiux.hud;

import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Contextual key hint rules for blocks (KeyRules.prompts calls them in order): seats, farming, fire and tool uses, vanilla
 * hand-used blocks by type, screens (vanilla and the mods that open theirs in code), Mekanism screens, Drekki Rancher's
 * hatchery, Diagonal Blocks' angle toggle, and the fallback Use for a mod block's own use code. Mod classes by name only
 * (Reflect): an absent mod is no rule.
 */
final class KeyBlocks {
    private KeyBlocks() {}

    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> RCH_BLACKLIST =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("rightclickharvest", "blacklist"));
    /** Create's tag of blocks its wrench picks up with a sneak-use though they are not IWrenchable. */
    static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> WRENCH_PICKUP =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "wrench_pickup"));

    /** Scrape and wax off before strip: a mod in the pack answers the strip query for copper as well (the block is
     *  scraped either way), so strip comes last to keep copper's word right. */
    private static final net.neoforged.neoforge.common.ItemAbility[] TOOL_USES = {
            net.neoforged.neoforge.common.ItemAbilities.AXE_SCRAPE, net.neoforged.neoforge.common.ItemAbilities.AXE_WAX_OFF,
            net.neoforged.neoforge.common.ItemAbilities.AXE_STRIP, net.neoforged.neoforge.common.ItemAbilities.HOE_TILL,
            net.neoforged.neoforge.common.ItemAbilities.SHOVEL_DOUSE, net.neoforged.neoforge.common.ItemAbilities.SHOVEL_FLATTEN};
    private static final String[] TOOL_WORDS = {"Scrape", "Remove wax", "Strip", "Till", "Put out", "Make path"};
    /** Till and path need an open top; scrape, wax off, strip and douse (a lit campfire, a brazier) do not. */
    private static final boolean[] TOOL_OPEN_TOP = {false, false, false, true, false, true};

    /** Seats by their mods' block classes: Create's seat (sits on use unless sneaking), Handcrafted's chairs, couches,
     *  benches and cushions, Bits 'n' Bobs' chair. */
    static boolean seat(net.minecraft.world.level.block.Block b) {
        return Reflect.is(b, "com.simibubi.create.content.contraptions.actors.seat.SeatBlock")
                || Reflect.is(b, "earth.terrarium.handcrafted.common.blocks.ChairBlock") || Reflect.is(b, "earth.terrarium.handcrafted.common.blocks.CouchBlock")
                || Reflect.is(b, "earth.terrarium.handcrafted.common.blocks.BenchBlock") || Reflect.is(b, "earth.terrarium.handcrafted.common.blocks.CushionBlock")
                || Reflect.is(b, "com.kipti.bnb.content.chair.ChairBlock");
    }

    /** What use on a seat does, or null for the block's other rules (KeyRules): Create's SeatBlock.useItemOn in its order:
     *  a dye of another colour dyes it; a seat a player sits on does nothing; a mob on the player's lead is seated
     *  (getLeashed); else the player sits (a mob sitting there is put off). Handcrafted seats with a cushion item take the
     *  cushion (the audited data rows). "" = no row. */
    static String seatWord(Minecraft mc, LocalPlayer p, ItemStack hand, net.minecraft.world.level.block.state.BlockState state, BlockHitResult bh) {
        var b = state.getBlock();
        if (!seat(b) || p.isSecondaryUseActive()) return null;
        if (hand.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("handcrafted", "cushions")))) return null;
        String seat = "com.simibubi.create.content.contraptions.actors.seat.SeatBlock";
        if (!Reflect.is(b, seat)) return "Sit";
        var dye = net.minecraft.world.item.DyeColor.getColor(hand);
        if (dye != null && dye != Reflect.call(b, "getColor")) return "Dye";
        var seats = mc.level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(bh.getBlockPos()),
                e -> Reflect.is(e, "com.simibubi.create.content.contraptions.actors.seat.SeatEntity"));
        if (!seats.isEmpty() && !seats.get(0).getPassengers().isEmpty()) {
            return seats.get(0).getPassengers().get(0) instanceof net.minecraft.world.entity.player.Player ? "" : "Sit";
        }
        Object leashed = Reflect.callStatic(seat, "getLeashed", mc.level, p);
        return leashed instanceof com.google.common.base.Optional<?> o && o.isPresent() ? "Seat mob" : "Sit";
    }

    /** Create: Oxidation's own handler (CreateOxidizedNeoForge.onRightClickBlock, an event before the block's use): on a
     *  block it oxidizes, honeycomb waxes; an AxeItem (only: a Mekanism paxel is a DiggerItem) removes wax, else scrapes. */
    static String oxidationWord(ItemStack hand, net.minecraft.world.level.block.state.BlockState state) {
        String ox = "dev.mrmulle.createoxidized.OxidationProperties";
        if (Reflect.cls(ox) != null && Reflect.bool(Reflect.callStatic(ox, "isOxidizable", state))) {
            boolean waxed = Reflect.getStatic(ox, "WAXED") instanceof net.minecraft.world.level.block.state.properties.BooleanProperty w && state.hasProperty(w) && state.getValue(w);
            Object stage = Reflect.getStatic(ox, "STAGE") instanceof net.minecraft.world.level.block.state.properties.Property<?> st && state.hasProperty(st) ? state.getValue(st) : null;
            if (hand.is(net.minecraft.world.item.Items.HONEYCOMB) && !waxed) return "Wax";
            if (hand.getItem() instanceof net.minecraft.world.item.AxeItem) {
                if (waxed) return "Remove wax";
                if (stage != null && !String.valueOf(stage).equalsIgnoreCase("unaffected")) return "Scrape";
            }
        }
        return null;
    }

    /** Farm and tool words as rows. Create: Oxidation's event runs before the block's use (plain use). The other words
     *  come from the held item's useOn, which runs only when the block's own use passes: on a block that opens a screen,
     *  plain use opens it, and the item acts only while sneaking (a vanilla copper chest: Open, sneak Scrape). */
    static boolean farm(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, ItemStack hand, net.minecraft.world.level.block.state.BlockState state, BlockHitResult bh) {
        String ox = oxidationWord(hand, state);
        if (ox != null) { KeyRules.add(rows, ox, "key.use"); return true; }
        String w = farmWord(mc, p, hand, state, bh);
        if (w == null) return false;
        if (opensScreen(mc, state, bh.getBlockPos())) {
            KeyRules.add(rows, "Open", "key.use");
            KeyRules.add(rows, w, "key.sneak", "key.use");
        } else KeyRules.add(rows, w, "key.use");
        return true;
    }

    /** Farming and fire on use, as the game decides it: flint and steel or a fire charge (light, ignite, start a fire); bone meal on what it grows (BonemealableBlock.isValidBonemealTarget),
     *  a plant item on a spot it can stand (top face, Block.canSurvive), a tool that changes the block (NeoForge's
     *  getToolModifiedState with HOE_TILL, AXE_STRIP, SHOVEL_FLATTEN, simulated: modded logs and soils count), a mature
     *  crop with Right Click Harvest (any hand: the pack's requireHoe is false). Null otherwise. */
    static String farmWord(Minecraft mc, LocalPlayer p, ItemStack hand, net.minecraft.world.level.block.state.BlockState state, BlockHitResult bh) {
        var b = state.getBlock();
        var pos = bh.getBlockPos();
        if (hand.getItem() instanceof net.minecraft.world.item.BoneMealItem && b instanceof net.minecraft.world.level.block.BonemealableBlock g
                && g.isValidBonemealTarget(mc.level, pos, state)) return "Grow";
        if (bh.getDirection() == net.minecraft.core.Direction.UP && hand.getItem() instanceof net.minecraft.world.item.BlockItem bi
                && bi.getBlock() instanceof net.minecraft.world.level.block.BushBlock && mc.level.isEmptyBlock(pos.above())
                && bi.getBlock().defaultBlockState().canSurvive(mc.level, pos.above())) return "Plant";
        for (int i = 0; i < TOOL_USES.length; i++) {
            if (!hand.canPerformAction(TOOL_USES[i])) continue;
            if (TOOL_OPEN_TOP[i] && (bh.getDirection() == net.minecraft.core.Direction.DOWN || !mc.level.isEmptyBlock(pos.above()))) continue;
            var ctx = new net.minecraft.world.item.context.UseOnContext(p, net.minecraft.world.InteractionHand.MAIN_HAND, bh);
            if (state.getToolModifiedState(ctx, TOOL_USES[i], true) != null) return TOOL_WORDS[i];
        }
        if (hand.getItem() instanceof net.minecraft.world.item.HoneycombItem && net.minecraft.world.item.HoneycombItem.getWaxed(state).isPresent()) return "Wax";
        if (hand.getItem() instanceof net.minecraft.world.item.FlintAndSteelItem || hand.getItem() instanceof net.minecraft.world.item.FireChargeItem) {
            // FlintAndSteelItem / FireChargeItem.useOn: light a campfire or candle, else fire on the face (TNT ignites)
            if (net.minecraft.world.level.block.CampfireBlock.canLight(state) || net.minecraft.world.level.block.CandleBlock.canLight(state)
                    || net.minecraft.world.level.block.CandleCakeBlock.canLight(state)) return "Light";
            if (b instanceof net.minecraft.world.level.block.TntBlock) return "Ignite";
            if (net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(mc.level, pos.relative(bh.getDirection()), bh.getDirection())) return "Start fire";
        }
        // Right Click Harvest (onBlockUse): main hand, any item (the pack's requireHoe is false), not crouching, not a
        // block in its rightclickharvest:blacklist tag; ripe crops, nether wart, cocoa, and sugar cane or cactus above their base
        if (net.neoforged.fml.ModList.get().isLoaded("rightclickharvest") && !p.isCrouching() && !state.is(RCH_BLACKLIST)
                && ((b instanceof net.minecraft.world.level.block.SugarCaneBlock || b instanceof net.minecraft.world.level.block.CactusBlock)
                    && mc.level.getBlockState(pos.below()).is(b)
                || b instanceof net.minecraft.world.level.block.CropBlock c && c.isMaxAge(state)
                    || b instanceof net.minecraft.world.level.block.NetherWartBlock && state.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE) >= 3
                    || b instanceof net.minecraft.world.level.block.CocoaBlock && state.getValue(net.minecraft.world.level.block.CocoaBlock.AGE) >= 2)) return "Harvest";
        return null;
    }

    /** Vanilla's hand-used blocks by type (so mod blocks built on them count too), the word for their use now: levers,
     *  buttons, doors / trapdoors / fence gates (Open or Close by their open state; iron ones need redstone: none), bells,
     *  note blocks, repeaters, comparators, beds. Null for any other block. */
    static String useWord(net.minecraft.world.level.block.state.BlockState state) {
        var b = state.getBlock();
        boolean open = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN);
        if (b instanceof net.minecraft.world.level.block.LeverBlock) return "Pull";
        if (b instanceof net.minecraft.world.level.block.ButtonBlock) return "Press";
        if (b instanceof net.minecraft.world.level.block.DoorBlock d) return d.type().canOpenByHand() ? (open ? "Close" : "Open") : null;
        if (b instanceof net.minecraft.world.level.block.TrapDoorBlock t) return Reflect.call(t, "getType") instanceof net.minecraft.world.level.block.state.properties.BlockSetType bt && bt.canOpenByHand() ? (open ? "Close" : "Open") : null;
        if (b instanceof net.minecraft.world.level.block.FenceGateBlock) return open ? "Close" : "Open";
        if (b instanceof net.minecraft.world.level.block.BellBlock) return "Ring";
        if (b instanceof net.minecraft.world.level.block.NoteBlock) return "Tune";
        if (b instanceof net.minecraft.world.level.block.RepeaterBlock) return "Delay";
        if (b instanceof net.minecraft.world.level.block.ComparatorBlock) return "Mode";
        if (b instanceof net.minecraft.world.level.block.BedBlock) return "Sleep";
        return null;
    }

    /** A block that opens a screen on use: its block entity is a screen provider (vanilla chests, furnaces, most mod
     *  machines), the block names one (Block.getMenuProvider), or it is a container block entity (BaseContainerBlockEntity);
     *  and mods that open screens in their own use code: Sophisticated Storage (StorageBlockBase) and a placed Sophisticated
     *  backpack, Create's toolbox, Mekanism blocks with AttributeGui.
     *  Vanilla's use rule: sneaking with something in either hand places instead of opening. */
    static boolean opensScreen(Minecraft mc, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
        var be = mc.level.getBlockEntity(pos);
        var b = state.getBlock();
        // Mods that open their screens in their own use code: each mod's own condition.
        if (Reflect.is(b, "net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase")
                || Reflect.is(b, "net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock")
                || Reflect.is(b, "com.simibubi.create.content.equipment.toolbox.ToolboxBlock")) return true;
        if (b.getClass().getName().startsWith("mekanism.") && mekanismGui(b)) return true;
        return be instanceof MenuProvider || be instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity
                || state.getMenuProvider(mc.level, pos) != null;
    }

    private static java.lang.reflect.Method mekHas;
    private static Class<?> mekGui;
    private static boolean mekMissing;

    /** Mekanism: Attribute.has(Block, AttributeGui.class), the overload taking a Block (there are three named has). */
    static boolean mekanismGui(net.minecraft.world.level.block.Block b) {
        if (mekMissing) return false;
        try {
            if (mekHas == null) {
                mekGui = Class.forName("mekanism.common.block.attribute.AttributeGui");
                mekHas = Class.forName("mekanism.common.block.attribute.Attribute").getMethod("has", net.minecraft.world.level.block.Block.class, Class.class);
            }
            return (boolean) mekHas.invoke(null, b, mekGui);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            mekMissing = true;
            HudLog.error("mek-gui", "key hints: Mekanism's Attribute.has(Block, Class) is not usable (no Open on its machines)", e);
            return false;
        }
    }

    /** Per block class: 1 = a mod class declares useWithoutItem (any hand), 2 = only useItemOn (with an item), 0 = none. */
    private static final Map<Class<?>, Integer> USE_CODE = new java.util.concurrent.ConcurrentHashMap<>();

    /** The fallback: the block's own mod class (not vanilla's) declares a use method that applies to this hand. */
    static boolean fallbackUse(net.minecraft.world.level.block.Block b, ItemStack hand) {
        int kind = USE_CODE.computeIfAbsent(b.getClass(), c -> {
            int k = 0;
            for (Class<?> t = c; t != null && !t.getName().startsWith("net.minecraft."); t = t.getSuperclass()) {
                for (java.lang.reflect.Method m : t.getDeclaredMethods()) {
                    if (m.getName().equals("useWithoutItem")) return 1;
                    if (m.getName().equals("useItemOn")) k = 2;
                }
            }
            return k;
        });
        return kind == 1 || kind == 2 && !hand.isEmpty();
    }

    /** Drekki Rancher's HatcheryBlock: an Intriguing Egg goes in the nest, a dye of another colour recolours it; any other
     *  hand: on the nest (top face) with a ready egg it hatches, else its screen opens. */
    static void hatchery(List<KeyRules.Row> rows, Minecraft mc, ItemStack hand, net.minecraft.world.level.block.state.BlockState state, BlockHitResult bh) {
        if (KeyRules.id(hand).equals("rancher_companion:intriguing_egg")) { KeyRules.add(rows, "Place egg", "key.use"); return; }
        if (hand.getItem() instanceof net.minecraft.world.item.DyeItem dye) {
            var color = state.getProperties().stream().filter(pr -> pr.getName().equals("color")).findFirst();
            if (color.isEmpty() || !Integer.valueOf(dye.getDyeColor().getId() + 1).equals(state.getValue(color.get()))) { KeyRules.add(rows, "Dye", "key.use"); return; }
        }
        var be = mc.level.getBlockEntity(bh.getBlockPos());
        boolean ready = bh.getDirection() == net.minecraft.core.Direction.UP && be != null && Reflect.bool(Reflect.call(be, "hasEgg"))
                && Reflect.bool(Reflect.callStatic("dev.frosthaven.ranchercompanion.look.Incubation", "ready", Reflect.call(be, "incubation")));
        KeyRules.add(rows, ready ? "Hatch" : "Open", "key.use");
    }

    /** Diagonal Fences / Walls / Windows (their Diagonal Blocks library, DiagonalBlockHandler.onUseBlock): sneak-use with an
     *  empty hand swaps a block with its diagonal twin, both ways (each DiagonalBlockType's getBlockConversions). The word,
     *  or null for any other block. */
    static String diagonal(net.minecraft.world.level.block.Block b) {
        if (!(Reflect.getStatic("fuzs.diagonalblocks.api.v2.DiagonalBlockType", "TYPES") instanceof java.util.Set<?> types)) return null;
        for (Object t : types) {
            if (!(Reflect.call(t, "getBlockConversions") instanceof java.util.Map<?, ?> m)) continue;
            if (m.containsKey(b)) return "Angles on";
            if (m.containsValue(b)) return "Angles off";
        }
        return null;
    }
}
