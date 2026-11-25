package dev.frosthaven.havencreateuiux.hud;

import java.io.DataInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Wrenches on blocks (contextual key hints). Each mod's wrench handling, in the order the game runs it:
 * <ul>
 * <li>Refurbished Furniture's wrench: its RightClickBlock handler cancels every block click (it links with attack).</li>
 * <li>Create IWrenchable blocks: Create's wrench through WrenchItem.useOn (after the block's own useItemOn: belt cut,
 *     girder sides, bogey style); any other c:tools/wrench through WrenchEventHandler.useOwnWrenchLogicForCreateBlocks
 *     (an event: the block's use never runs). Use runs onWrenched, sneak-use onSneakWrenched (pick up). The word for use
 *     follows the class that overrides onWrenched: one whose code calls a super onWrenched rotates in some case, the
 *     known ones say what they do, others say "Use".</li>
 * <li>AE2 (WrenchHook, main hand): rotate (network tool or c:tools/wrench), sneak dismantles (c:tools/wrench).</li>
 * <li>Refined Storage (AbstractBaseBlock.tryUseWrench): rotate a directional block, sneak dismantles.</li>
 * <li>Mekanism (BlockBase.useItemOn, MekanismUtils.canUseAsWrench): sneak picks up any Mekanism block, cables too; a
 *     directional machine rotates (TileEntityMekanism.tryWrench); the Configurator has its own modes.</li>
 * <li>FramedBlocks (IFramedBlock.handleUse, Utils.isWrenchRotationTool): rotate, sneak rotates back.</li>
 * <li>Pipez (PipeBlock.onWrenchClicked): sneak toggles the side looked at.</li>
 * </ul>
 * True when a wrench rule decided the rows (possibly none).
 */
final class KeyWrench {
    private KeyWrench() {}

    static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));
    private static final TagKey<Item> MEK_CONFIGURATORS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("mekanism", "configurators"));
    private static final TagKey<Item> FRAMED_COMPLEX = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("framedblocks", "complex_wrench"));
    private static final String IWRENCHABLE = "com.simibubi.create.content.equipment.wrench.IWrenchable";

    /** onWrenched overrides that do one known thing ("" = nothing on use) */
    private static final Map<String, String> WORDS = Map.ofEntries(
            Map.entry("FluidTankBlock", "Toggle window"), Map.entry("BeltTunnelBlock", "Toggle window"), Map.entry("TFMGFluidTankBlock", "Toggle window"),
            Map.entry("FluidVesselBlock", "Toggle window"),
            Map.entry("EncasedPipeBlock", "Remove casing"), Map.entry("ConcreteEncasedFluidPipeBlock", "Remove casing"),
            Map.entry("CopycatBlock", "Remove material"), Map.entry("ICopycatBlock", "Remove material"),
            Map.entry("CopycatHeadstockBlock", "Remove material"), Map.entry("CopycatCableBlock", "Remove material"),
            Map.entry("GirderEncasedShaftBlock", "Remove shaft"), Map.entry("BasinBlock", "Toggle output"), Map.entry("SignalBlock", "Signal type"),
            Map.entry("CasingBlock", ""), Map.entry("AttachedActorBlock", ""), Map.entry("TrackBlock", ""), Map.entry("WaterWheelStructuralBlock", ""));
    private static final Map<Class<?>, Boolean> CALLS_SUPER = new ConcurrentHashMap<>();

    static boolean isWrench(ItemStack s) { return !s.isEmpty() && s.is(WRENCHES); }

    static boolean rows(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, ItemStack hand, BlockState state, BlockHitResult bh) {
        if (hand.isEmpty()) return false;
        String id = KeyRules.id(hand);
        boolean create = id.equals("create:wrench"), any = hand.is(WRENCHES);
        if (id.equals("refurbished_furniture:wrench")) return true;   // NeoForgeEvents.onRightClickBlock cancels every block click
        Object block = state.getBlock();
        if ((create || any) && Reflect.is(block, IWRENCHABLE)) {
            if (!p.mayBuild()) return true;
            String word = create ? createOwnUse(block, state, bh) : null;
            if (word == null) word = onWrenchedWord(mc, block, state, bh);
            if (!word.isEmpty()) KeyRules.add(rows, word, "key.use");
            KeyRules.add(rows, "Pick up", "key.sneak", "key.use");
            return true;
        }
        if (create && state.is(KeyBlocks.WRENCH_PICKUP)) { KeyRules.add(rows, "Pick up", "key.sneak", "key.use"); return true; }
        // AE2: WrenchHook (main hand): the network tool rotates, any wrench rotates and dismantles
        if (Reflect.is(mc.level.getBlockEntity(bh.getBlockPos()), "appeng.blockentity.AEBaseBlockEntity")
                && (any || Reflect.is(hand.getItem(), "appeng.items.tools.NetworkToolItem"))) {
            Object strategy = Reflect.callStatic("appeng.api.orientation.IOrientationStrategy", "get", state);
            if (Reflect.bool(Reflect.call(strategy, "allowsPlayerRotation"))) KeyRules.add(rows, "Rotate", "key.use");
            if (any) KeyRules.add(rows, "Dismantle", "key.sneak", "key.use");
            return true;
        }
        if (KeyRules.namespace(state).equals("ae2") && any && Reflect.is(block, "appeng.block.networking.CableBusBlock")) {
            KeyRules.add(rows, "Dismantle", "key.sneak", "key.use");   // the part or cable looked at
            return true;
        }
        // Refined Storage: AbstractBaseBlock.tryUseWrench (the event: the screen never opens with a wrench)
        if (any && Reflect.is(block, "com.refinedmods.refinedstorage.common.support.AbstractBaseBlock")) {
            if (Reflect.is(block, "com.refinedmods.refinedstorage.common.support.AbstractDirectionalBlock")) KeyRules.add(rows, "Rotate", "key.use");
            KeyRules.add(rows, "Dismantle", "key.sneak", "key.use");
            return true;
        }
        // Mekanism: sneak dismantles any Mekanism block with a wrench (BlockBase.useItemOn); machines also rotate
        if (KeyRules.namespace(state).startsWith("mekanism") && hand.is(MEK_CONFIGURATORS) && !id.equals("mekanism:configurator")
                && Reflect.is(block, "mekanism.common.block.prefab.BlockBase")) {
            Object be = mc.level.getBlockEntity(bh.getBlockPos());
            if (Reflect.is(be, "mekanism.common.tile.base.TileEntityMekanism") && Reflect.bool(Reflect.call(be, "isDirectional"))) KeyRules.add(rows, "Rotate", "key.use");
            KeyRules.add(rows, "Pick up", "key.sneak", "key.use");
            return true;
        }
        // FramedBlocks: rotation before camo use
        if (Reflect.is(block, "xfacthd.framedblocks.api.block.IFramedBlock") && any && !hand.is(FRAMED_COMPLEX)) {
            KeyRules.add(rows, "Rotate", "key.use");
            KeyRules.add(rows, "Rotate back", "key.sneak", "key.use");
            return true;
        }
        // Pipez: only sneaking with a wrench, on the side looked at
        if (any && Reflect.is(block, "de.maxhenkel.pipez.blocks.PipeBlock")) {
            KeyRules.add(rows, "Toggle side", "key.sneak", "key.use");
            return true;
        }
        return false;
    }

    /** Create's wrench only: the block's own useItemOn runs before the wrench's useOn. Null: onWrenched decides. */
    private static String createOwnUse(Object block, BlockState state, BlockHitResult bh) {
        String n = block.getClass().getSimpleName();
        if (Reflect.is(block, "com.simibubi.create.content.kinetics.belt.BeltBlock")) return "Cut belt";   // BeltSlicer.useWrench
        if (Reflect.is(block, "com.simibubi.create.content.trains.bogey.AbstractBogeyBlock")) return "Change style";
        if (n.contains("Girder") && !n.contains("Encased") && !n.contains("Strut")) return "Toggle girder side";   // *GirderWrenchBehaviour
        return null;
    }

    /** The word for onWrenched on this block: by the class that overrides it. */
    private static String onWrenchedWord(Minecraft mc, Object block, BlockState state, BlockHitResult bh) {
        Class<?> owner;
        try {
            owner = block.getClass().getMethod("onWrenched", BlockState.class, net.minecraft.world.item.context.UseOnContext.class).getDeclaringClass();
        } catch (ReflectiveOperationException | LinkageError e) { return "Rotate"; }
        String n = owner.getSimpleName();
        if (owner.getName().equals(IWRENCHABLE)) return "Rotate";
        if (n.equals("IWrenchableWithBracket")) {
            Object be = mc.level.getBlockEntity(bh.getBlockPos());
            Object type = Reflect.getStatic("com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour", "TYPE");
            return Reflect.bool(Reflect.call(Reflect.call(be, "getBehaviour", type), "isBracketPresent")) ? "Remove bracket" : "Rotate";
        }
        if (n.equals("EncasedCogwheelBlock") || n.equals("TFMGEncasedCogwheelBlock")) {
            var axis = state.getOptionalValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS);
            return axis.isPresent() && bh.getDirection().getAxis() == axis.get() ? "Toggle shaft" : "Rotate";
        }
        if (n.equals("BeltBlock"))   // another mod's wrench: the event runs onWrenched (casing off); a bare belt does nothing
            return Reflect.getStatic("com.simibubi.create.content.kinetics.belt.BeltBlock", "CASING") instanceof net.minecraft.world.level.block.state.properties.BooleanProperty c
                    && state.hasProperty(c) && state.getValue(c) ? "Remove casing" : "";
        String w = WORDS.get(n);
        if (w != null) return w;
        return CALLS_SUPER.computeIfAbsent(owner, KeyWrench::callsSuperOnWrenched) ? "Rotate" : "Use";
    }

    /** The class file refers to another class's onWrenched (a super call: it rotates in some case). Read from the
     *  constant pool only; a class that cannot be read counts as rotating (the default). */
    static boolean callsSuperOnWrenched(Class<?> c) {
        String self = c.getName().replace('.', '/');
        try (var raw = c.getClassLoader() == null ? null : c.getClassLoader().getResourceAsStream(self + ".class")) {
            if (raw == null) return true;
            DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(raw));
            in.readInt(); in.readUnsignedShort(); in.readUnsignedShort();
            int n = in.readUnsignedShort();
            String[] utf = new String[n];
            int[] cls = new int[n], refClass = new int[n], refNat = new int[n], natName = new int[n];
            boolean[] isRef = new boolean[n];
            for (int i = 1; i < n; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1 -> utf[i] = in.readUTF();
                    case 3, 4 -> in.readInt();
                    case 5, 6 -> { in.readLong(); i++; }
                    case 7 -> cls[i] = in.readUnsignedShort();
                    case 8, 16, 19, 20 -> in.readUnsignedShort();
                    case 9, 10, 11 -> { isRef[i] = tag != 9; refClass[i] = in.readUnsignedShort(); refNat[i] = in.readUnsignedShort(); }
                    case 12 -> { natName[i] = in.readUnsignedShort(); in.readUnsignedShort(); }
                    case 15 -> { in.readUnsignedByte(); in.readUnsignedShort(); }
                    case 17, 18 -> in.readInt();
                    default -> { return true; }
                }
            }
            for (int i = 1; i < n; i++) {
                if (!isRef[i] || !"onWrenched".equals(utf[natName[refNat[i]]])) continue;
                String owner = utf[cls[refClass[i]]];
                if (owner != null && !owner.equals(self)) return true;
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
