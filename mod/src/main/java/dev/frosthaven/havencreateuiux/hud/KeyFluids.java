package dev.frosthaven.havencreateuiux.hud;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Contextual key hints for items and fluids moved by hand, each from its mod's own rule: Create's Item Hatch (tooltip:
 * use deposits the held item, sneak-use everything but the hotbar), Create Dragons Plus' Fluid Hatch (use empties the
 * held container into the tank, or fills it when it cannot; sneak-use the other order first), Dreams n' Desires' Fluid
 * Hatch (built as Create's Item Hatch: the held container, sneak-use all but the hotbar), Create Enchantment
 * Industry's Experience Hatch (ponder: use stores experience, sneak-use takes it), Mekanism fluid tanks with a fluid
 * container, Create's only in creative or a Creative Fluid Tank (Empty bucket / Fill bucket, Deposit / Withdraw), and the world: an empty bucket on any fluid source a bucket picks up (BucketPickup,
 * modded fluids too), a full one on a block (Empty bucket); Mekanism's portable tanks in bucket mode the same.
 */
final class KeyFluids {
    private KeyFluids() {}

    /** Into the held container (a bucket: Fill bucket), out of it (Empty bucket); other fluid containers (a Mekanism
     *  tank item, a canister): Fill tank / Empty tank. Deposit and Withdraw are item-inventory words. */
    private static String in(ItemStack hand) { return hand.getItem() instanceof BucketItem ? "Fill bucket" : "Fill tank"; }
    private static String out(ItemStack hand) { return hand.getItem() instanceof BucketItem ? "Empty bucket" : "Empty tank"; }

    /** Rows for the crosshair's target with this hand; true when one of these rules applied. */
    static boolean rows(List<KeyRules.Row> rows, Minecraft mc, LocalPlayer p, ItemStack hand) {
        var fh = hand.isEmpty() ? null : hand.getCapability(Capabilities.FluidHandler.ITEM);
        boolean full = fh != null && fh.getTanks() > 0 && !fh.getFluidInTank(0).isEmpty();
        // room for more: a bucket holds one bucket (full means no room); a Mekanism tank item fills up to its capacity
        boolean room = fh != null && fh.getTanks() > 0 && fh.getFluidInTank(0).getAmount() < fh.getTankCapacity(0);
        if (mc.hitResult instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK && mc.level != null) {
            var b = mc.level.getBlockState(bh.getBlockPos()).getBlock();
            if (Reflect.is(b, "com.simibubi.create.content.logistics.itemHatch.ItemHatchBlock")) {
                if (!hand.isEmpty()) KeyRules.add(rows, "Deposit", "key.use");
                KeyRules.add(rows, "Deposit all", "key.sneak", "key.use");
                return true;
            }
            if (Reflect.is(b, "plus.dragons.createdragonsplus.common.fluids.hatch.FluidHatchBlock")) {
                if (fh == null) return true;
                KeyRules.add(rows, full ? out(hand) : in(hand), "key.use");
                if (full && room) KeyRules.add(rows, in(hand), "key.sneak", "key.use");   // sneak-use tries filling first
                return true;
            }
            if (Reflect.is(b, "dev.lopyluna.dndesires.content.blocks.logistics.fluid_hatch.FluidHatchBlock")) {
                if (full) KeyRules.add(rows, out(hand), "key.use");
                KeyRules.add(rows, "Deposit all", "key.sneak", "key.use");   // every container but the hotbar's
                return true;
            }
            if (Reflect.is(b, "plus.dragons.createenchantmentindustry.common.fluids.experience.ExperienceHatchBlock")) {
                KeyRules.add(rows, "Deposit", "key.use");
                KeyRules.add(rows, "Withdraw", "key.sneak", "key.use");
                return true;
            }
            // Mekanism's tank takes and gives fluid in survival (FluidUtils.handleTankInteraction); Create's only for a player in
            // creative or on a Creative Fluid Tank (FluidTankBlock.useItemOn: isCreative() || creative, else it passes)
            boolean createTank = Reflect.is(b, "com.simibubi.create.content.fluids.tank.FluidTankBlock");
            if (fh != null && (Reflect.is(b, "mekanism.common.block.basic.BlockFluidTank")
                    || createTank && (p.isCreative() || Reflect.bool(Reflect.get(b, "creative"))))) {
                // the tank's own contents (its fluid handler on the face looked at): a held container fills only from a tank
                // that holds fluid, and empties only into one with room for that fluid; unreadable: the plain words
                var tank = mc.level.getCapability(Capabilities.FluidHandler.BLOCK, bh.getBlockPos(), bh.getDirection());
                if (tank != null && tank.getTanks() > 0) {
                    var in = tank.getFluidInTank(0);
                    if (full) {
                        var held = fh.getFluidInTank(0);
                        boolean fits = in.getAmount() < tank.getTankCapacity(0) && (in.isEmpty() || net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(in, held));
                        if (fits) KeyRules.add(rows, out(hand), "key.use");
                    } else if (!in.isEmpty()) KeyRules.add(rows, in(hand), "key.use");
                } else KeyRules.add(rows, full ? out(hand) : in(hand), "key.use");
                return true;
            }
        }
        boolean bucket = hand.getItem() instanceof BucketItem;
        boolean mekBucket = !bucket && fh != null && Reflect.is(hand.getItem(), "mekanism.common.item.block.machine.ItemBlockFluidTank")
                && Reflect.call(hand.getItem(), "getMode", hand) instanceof Boolean on && on;
        if (!bucket && !mekBucket) return false;
        boolean holds = bucket ? ((BucketItem) hand.getItem()).content != Fluids.EMPTY : full;
        boolean mekRoom = mekBucket && room;
        if (!holds || mekRoom) {
            // a fluid source in reach, traced as BucketItem traces it (Item.getPlayerPOVHitResult: sources only, flowing fluid
            // passed through; the normal crosshair target ignores fluids)
            var eye = p.getEyePosition();
            var end = eye.add(p.getViewVector(1f).scale(p.blockInteractionRange()));
            HitResult h = mc.level.clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.OUTLINE,
                    net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY, p));
            if (h instanceof BlockHitResult fb && h.getType() == HitResult.Type.BLOCK) {
                var state = mc.level.getBlockState(fb.getBlockPos());
                if (state.getFluidState().isSource() && state.getBlock() instanceof BucketPickup) {
                    KeyRules.add(rows, bucket ? "Fill bucket" : "Fill tank", "key.use");
                    return true;
                }
            }
        }
        if (holds && mc.hitResult instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) {
            KeyRules.add(rows, bucket ? "Empty bucket" : "Empty tank", "key.use");
            return true;
        }
        return false;
    }
}
