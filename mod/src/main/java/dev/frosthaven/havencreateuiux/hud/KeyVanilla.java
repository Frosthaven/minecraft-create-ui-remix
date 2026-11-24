package dev.frosthaven.havencreateuiux.hud;

import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.UseAnim;

/**
 * Hints from Minecraft's own checks, so every mod's items and mobs built on them count with no list: an entity's use
 * with what the hand holds (contextual), and a held item's use type (stateful). Each rule names the vanilla method that
 * decides it; nothing here touches a mod class.
 */
final class KeyVanilla {
    private KeyVanilla() {}

    /** Contextual rows for an entity under the crosshair; true when one applied. */
    static boolean entity(List<KeyRules.Row> rows, LocalPlayer p, Entity e, ItemStack hand) {
        if (e instanceof net.minecraft.world.entity.decoration.ArmorStand stand && !stand.isMarker()) {   // ArmorStand.interactAt
            if (!hand.isEmpty() && stand.getEquipmentSlotForItem(hand) != null) KeyRules.add(rows, "Place", "key.use");
            else if (hand.isEmpty()) KeyRules.add(rows, "Take", "key.use");
            return !rows.isEmpty();
        }
        if (e instanceof net.minecraft.world.entity.vehicle.ContainerEntity && e instanceof net.minecraft.world.MenuProvider) {   // chest minecart, chest boat
            KeyRules.add(rows, "Open", "key.use");
            return true;
        }
        if (e instanceof net.minecraft.world.entity.animal.Bucketable && hand.is(Items.WATER_BUCKET) && ((LivingEntity) e).isAlive()) {
            KeyRules.add(rows, "Catch", "key.use");   // Bucketable.bucketMobPickup: fish, axolotl, tadpole
            return true;
        }
        if (e instanceof net.minecraft.world.entity.animal.horse.AbstractChestedHorse ch && ch.isTamed() && !ch.hasChest() && hand.is(Items.CHEST)) {
            KeyRules.add(rows, "Add chest", "key.use");   // AbstractChestedHorse.mobInteract: llama, donkey, mule
            return true;
        }
        if (e instanceof net.minecraft.world.entity.TamableAnimal t) {   // wolves: bone, cats: fish, parrots: seeds; own pets sit or stand
            boolean tameItem = t instanceof net.minecraft.world.entity.animal.Wolf ? hand.is(Items.BONE)
                    : t instanceof net.minecraft.world.entity.animal.Parrot ? hand.is(net.minecraft.tags.ItemTags.PARROT_FOOD) : t.isFood(hand);
            if (!t.isTame() && tameItem && !(t instanceof net.minecraft.world.entity.animal.Wolf w && w.isAngry())) { KeyRules.add(rows, "Tame", "key.use"); return true; }
            if (t.isTame() && t.isOwnedBy(p) && !t.isFood(hand)) { KeyRules.add(rows, t.isOrderedToSit() ? "Stand" : "Sit", "key.use"); return true; }
        }
        if (e instanceof ItemFrame f) {   // ItemFrame.interact: put the held item in, else turn the one inside
            if (!f.getItem().isEmpty()) KeyRules.add(rows, "Rotate", "key.use");
            else if (!hand.isEmpty()) KeyRules.add(rows, "Place item", "key.use");
            return !rows.isEmpty();
        }
        if (hand.is(Items.NAME_TAG) && hand.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) && !(e instanceof net.minecraft.world.entity.player.Player)) {
            KeyRules.add(rows, "Name", "key.use");   // NameTagItem.interactLivingEntity: a named tag on a living entity
            return true;
        }
        if (hand.is(Items.LEAD) && e instanceof Mob m && m.canBeLeashed()) { KeyRules.add(rows, "Leash", "key.use"); return true; }   // Leashable
        if (e instanceof Shearable s && hand.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "tools/shear")))
                && s.readyForShearing()) { KeyRules.add(rows, "Shear", "key.use"); return true; }
        if (e instanceof Cow c && !c.isBaby() && hand.is(Items.BUCKET)) { KeyRules.add(rows, "Milk", "key.use"); return true; }   // Cow.mobInteract
        if (e instanceof Saddleable s && hand.is(Items.SADDLE) && s.isSaddleable() && !s.isSaddled()) { KeyRules.add(rows, "Saddle", "key.use"); return true; }
        if (e instanceof AbstractHorse h) {   // AbstractHorse.mobInteract: food feeds; an empty hand rides (an untamed one bucks: taming)
            if (h.isFood(hand)) KeyRules.add(rows, "Feed", "key.use");
            else if (!h.isVehicle() && !h.isBaby()) KeyRules.add(rows, h.isTamed() ? "Ride" : "Tame (ride)", "key.use");
            if (h.isTamed() && !h.isBaby()) KeyRules.add(rows, h.getType().getDescription().getString() + " inventory", "key.sneak", "key.use");
            return !rows.isEmpty();
        }
        if (e instanceof net.minecraft.world.entity.animal.Pig pig && pig.isSaddled() && !pig.isVehicle() && hand.isEmpty()) { KeyRules.add(rows, "Ride", "key.use"); return true; }
        if (e instanceof Animal a && a.isFood(hand)) {   // Animal.mobInteract: food makes an adult breed (when it can) or a baby grow
            KeyRules.add(rows, a.isBaby() ? "Feed" : a.canFallInLove() ? "Breed" : "Feed", "key.use");
            return true;
        }
        return false;
    }

    private static boolean hasLeashed(LocalPlayer p) {
        return !p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(7), m -> m.getLeashHolder() == p).isEmpty();
    }

    /** Stateful rows for a held item by its use type (Item.getUseAnimation) or a thrown projectile; empty for others. */
    static void held(List<KeyRules.Row> rows, ItemStack s, LocalPlayer p) {
        if (s.isEmpty()) return;
        if (s.getItem() instanceof net.minecraft.world.item.FishingRodItem) { KeyRules.add(rows, p.fishing != null ? "Reel" : "Cast", "key.use"); return; }
        if (s.getItem() instanceof net.minecraft.world.item.FireworkRocketItem && p.isFallFlying()) { KeyRules.add(rows, "Boost", "key.use"); return; }
        UseAnim anim = s.getUseAnimation();
        String word = switch (anim) {
            case EAT -> "Eat (hold)";
            case DRINK -> "Drink (hold)";
            case BLOCK -> "Block (hold)";
            case BOW -> "Draw (hold)";
            case CROSSBOW -> "Load (hold)";
            case SPEAR -> "Throw (hold)";
            case SPYGLASS -> "Zoom (hold)";
            case TOOT_HORN -> "Play";
            case BRUSH -> "Brush (hold)";
            default -> null;
        };
        if (word == null && s.getItem() instanceof ProjectileItem) word = "Throw";   // snowballs, eggs, pearls, splash potions
        if (word != null) KeyRules.add(rows, word, "key.use");
    }

    /** Contextual rows for vanilla's hand-used blocks with more than one use (by class, so mod blocks built on them count);
     *  true when one applied. Each line names the vanilla method that decides it. */
    static boolean block(List<KeyRules.Row> rows, net.minecraft.client.Minecraft mc, LocalPlayer p, ItemStack hand,
                         net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.phys.BlockHitResult bh) {
        var b = state.getBlock();
        var pos = bh.getBlockPos();
        String w = null;
        if (b instanceof net.minecraft.world.level.block.CakeBlock) {   // CakeBlock.useItemOn / useWithoutItem
            if (hand.is(net.minecraft.tags.ItemTags.CANDLES) && state.getValue(net.minecraft.world.level.block.CakeBlock.BITES) == 0) w = "Add candle";
            else if (p.canEat(false)) w = "Eat";
        } else if (b instanceof net.minecraft.world.level.block.ComposterBlock) {   // ComposterBlock: COMPOSTABLES, level 8 full
            int level = state.getValue(net.minecraft.world.level.block.ComposterBlock.LEVEL);
            if (level == 8) w = "Take bone meal";
            else if (level < 7 && net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.containsKey(hand.getItem())) w = "Compost";
        } else if (b instanceof net.minecraft.world.level.block.JukeboxBlock) {   // JukeboxBlock: HAS_RECORD; JUKEBOX_PLAYABLE items
            if (state.getValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD)) w = "Eject";
            else if (hand.has(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE)) w = "Insert disc";
        } else if (b instanceof net.minecraft.world.level.block.LecternBlock) {   // LecternBlock: HAS_BOOK; a writable or written book
            if (state.getValue(net.minecraft.world.level.block.LecternBlock.HAS_BOOK)) w = "Read";
            else if (hand.is(net.minecraft.tags.ItemTags.LECTERN_BOOKS)) w = "Place book";
        } else if (b instanceof net.minecraft.world.level.block.FlowerPotBlock pot) {   // FlowerPotBlock.useItemOn: plant into an empty pot, else take
            if (pot.getPotted() != net.minecraft.world.level.block.Blocks.AIR) w = "Take";
            else if (hand.getItem() instanceof net.minecraft.world.item.BlockItem bi
                    && ((net.minecraft.world.level.block.FlowerPotBlock) net.minecraft.world.level.block.Blocks.FLOWER_POT).getFullPotsView().containsKey(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(bi.getBlock()))) w = "Plant";
        } else if (b instanceof net.minecraft.world.level.block.EndPortalFrameBlock) {
            if (hand.is(Items.ENDER_EYE) && !state.getValue(net.minecraft.world.level.block.EndPortalFrameBlock.HAS_EYE)) w = "Insert eye";
        } else if (b instanceof net.minecraft.world.level.block.RespawnAnchorBlock) {   // RespawnAnchorBlock: glowstone charges; charged sets spawn
            int charge = state.getValue(net.minecraft.world.level.block.RespawnAnchorBlock.CHARGE);
            if (hand.is(Items.GLOWSTONE) && charge < 4) w = "Charge";
            else if (charge > 0) w = "Set spawn";
        } else if (b instanceof net.minecraft.world.level.block.BeehiveBlock) {   // BeehiveBlock.useItemOn at honey level 5
            if (state.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL) >= 5) {
                if (hand.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHEARS_HARVEST)) w = "Harvest";
                else if (hand.is(Items.GLASS_BOTTLE)) w = "Fill bottle";
            }
        } else if (b instanceof net.minecraft.world.level.block.SweetBerryBushBlock) {
            if (state.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE) > 1 && !hand.is(Items.BONE_MEAL)) w = "Pick";
        } else if (b instanceof net.minecraft.world.level.block.CaveVines) {
            if (net.minecraft.world.level.block.CaveVines.hasGlowBerries(state)) w = "Pick";
        } else if (b instanceof net.minecraft.world.level.block.PumpkinBlock) {
            if (hand.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHEARS_CARVE)) w = "Carve";
        } else if (b instanceof net.minecraft.world.level.block.DecoratedPotBlock) {
            if (!hand.isEmpty()) w = "Insert";
        } else if (b instanceof net.minecraft.world.level.block.LayeredCauldronBlock || b instanceof net.minecraft.world.level.block.LavaCauldronBlock) {
            // CauldronInteraction: a full one fills a bucket, water fills a bottle
            boolean full = b instanceof net.minecraft.world.level.block.LavaCauldronBlock || ((net.minecraft.world.level.block.AbstractCauldronBlock) b).isFull(state);
            if (hand.is(Items.BUCKET) && full) w = "Fill bucket";
            else if (hand.is(Items.GLASS_BOTTLE) && state.is(net.minecraft.world.level.block.Blocks.WATER_CAULDRON)) w = "Fill bottle";
        } else if (b instanceof net.minecraft.world.level.block.SpawnerBlock) {
            if (hand.getItem() instanceof net.minecraft.world.item.SpawnEggItem && p.getAbilities().instabuild) w = "Set mob";
        } else if (b instanceof net.minecraft.world.level.block.SignBlock) {   // SignBlock.useItemOn / useWithoutItem
            var be = mc.level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sb ? sb : null;
            if (be == null || be.isWaxed()) return true;
            if (hand.getItem() instanceof net.minecraft.world.item.DyeItem) w = "Dye";
            else if (hand.is(Items.GLOW_INK_SAC)) w = "Glow";
            else if (hand.is(Items.HONEYCOMB)) w = "Wax";
            else if (hand.isEmpty() || !(hand.getItem() instanceof net.minecraft.world.item.SignApplicator)) w = "Edit";
        } else if (b instanceof net.minecraft.world.level.block.CampfireBlock) {   // a food it can cook goes on a free spot
            if (mc.level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity cb && cb.getCookableRecipe(hand).isPresent()) w = "Cook";
            else return false;
        } else if (b instanceof net.minecraft.world.level.block.BrushableBlock) {
            if (hand.getItem() instanceof net.minecraft.world.item.BrushItem) w = "Brush (hold)";
        } else if (b instanceof net.minecraft.world.level.block.FenceBlock) {   // FenceBlock: a held lead's mob ties to it
            if (!hand.is(Items.LEAD) || !hasLeashed(p)) return false;
            w = "Tie";
        } else if (b instanceof net.minecraft.world.level.block.DaylightDetectorBlock) {
            if (p.mayBuild()) w = "Invert";
        } else return false;
        if (w != null) KeyRules.add(rows, w, "key.use");
        return true;   // the vanilla block decided (a row, or nothing for this hand)
    }
}
