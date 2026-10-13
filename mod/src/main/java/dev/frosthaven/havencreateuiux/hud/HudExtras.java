package dev.frosthaven.havencreateuiux.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.network.chat.Component;

/**
 * Mod state for the bar's extras, each read the way the mod's own HUD reads it (verified against the 1.21.1 jars in the
 * pack; see CLAUDE.md). Every reader returns null / empty when its mod is absent or not showing.
 */
final class HudExtras {
    private HudExtras() {}

    /** Iron's Spellbooks: a non-instant spell's cast progress 0..1 (its cast bar), or null. */
    static Float ironsCast() {
        String c = "io.redspace.ironsspellbooks.player.ClientMagicData";
        if (!Reflect.bool(Reflect.callStatic(c, "isCasting"))) return null;
        Object type = Reflect.callStatic(c, "getCastType");
        if (type == null || type.toString().equals("INSTANT")) return null;
        float pct = Reflect.num(Reflect.callStatic(c, "getCastCompletionPercent"), 0);
        return type.toString().equals("CONTINUOUS") ? 1 - pct : pct;
    }

    /** Mekanism: the MekaSuit's energy over the worn pieces: the fraction, its text as Mekanism shows it ("1.2 MJ/16
     *  MJ", its configured unit), and a worn piece for the icon (the chestplate when worn). */
    record Suit(float frac, Component text, ItemStack icon) {}

    /** The MekaSuit energy (its energy bar's rule: worn pieces with an energy container), or null. */
    static Suit mekaSuit(Player p) {
        Class<?> suit = Reflect.cls("mekanism.common.item.gear.ItemMekaSuitArmor");
        if (suit == null) return null;
        long cap = 0, stored = 0;
        ItemStack icon = null;
        for (ItemStack s : p.getArmorSlots()) {
            if (!suit.isInstance(s.getItem())) continue;
            Object box = Reflect.callStatic("mekanism.common.util.StorageUtils", "getEnergyContainer", s, 0);
            if (box == null) continue;
            cap += ((Number) Reflect.call(box, "getMaxEnergy")).longValue();
            stored += ((Number) Reflect.call(box, "getEnergy")).longValue();
            if (icon == null || s.getItem() instanceof ArmorItem a && a.getType() == ArmorItem.Type.CHESTPLATE) icon = s;
        }
        if (cap <= 0) return null;
        Object display = Reflect.callStatic("mekanism.common.util.text.EnergyDisplay", "of", stored, cap);
        Object text = display == null ? null : Reflect.call(display, "getTextComponent");
        float frac = (float) ((double) stored / cap);
        return new Suit(frac, text instanceof Component c ? c : Component.literal(Math.round(frac * 100) + "%"), icon);
    }

    /** Immersive Aircraft: the vehicle's health 0..1 (its heart row), or null. */
    static Float aircraftHealth(Player p) {
        Object v = p.getRootVehicle();
        return v != p && Reflect.is(v, "immersive_aircraft.entity.VehicleEntity") ? Reflect.num(Reflect.call(v, "getHealth"), 1) : null;
    }

    /** Immersive Aircraft: engine power 0..1 (its hotbar engine gauge), or null. */
    static Float aircraftEngine(Player p) {
        Object v = p.getRootVehicle();
        if (!Reflect.is(v, "immersive_aircraft.entity.EngineVehicle")) return null;
        Object config = Reflect.callStatic("immersive_aircraft.config.Config", "getInstance");
        if (!Reflect.bool(Reflect.get(config, "showHotbarEngineGauge"))) return null;
        Object style = Reflect.call(v, "getGuiStyle");
        if (style == null || !style.toString().equals("ENGINE")) return null;
        return Reflect.num(Reflect.call(v, "getEnginePower"), 0);
    }

    /** Create: the controlled train's speed 0..1 (its train HUD), or null. */
    static Float trainSpeed() {
        String h = "com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler";
        Object c = Reflect.callStatic(h, "getContraption");
        if (!Reflect.is(c, "com.simibubi.create.content.trains.entity.CarriageContraptionEntity")) return null;
        if (Reflect.call(c, "getCarriage") == null || Reflect.callStatic(h, "getControlsPos") == null) return null;
        Object speed = Reflect.getStatic("com.simibubi.create.content.trains.TrainHUD", "displayedSpeed");
        return speed == null ? null : Reflect.num(Reflect.call(speed, "getValue"), 0);
    }

    /** Create: hotbar slots linked to a toolbox. */
    static boolean[] toolboxSlots(Player p) {
        boolean[] out = new boolean[9];
        CompoundTag t = p.getPersistentData().getCompound("CreateToolboxData");
        for (int i = 0; i < 9; i++) out[i] = t.contains(String.valueOf(i));
        return out;
    }

    /** Create: the schematic tool's hotbar slot while one is active, or -1. */
    static int schematicSlot() {
        Object h = Reflect.getStatic("com.simibubi.create.CreateClient", "SCHEMATIC_HANDLER");
        if (h == null || !Reflect.bool(Reflect.call(h, "isActive")) || Reflect.call(h, "getActiveSchematicItem") == null) return -1;
        return (int) Reflect.num(Reflect.get(h, "activeHotbarSlot"), -1);
    }

    /** Artifacts: Helium Flamingo's swim-in-air charge 0..1 while in use and not full, or null. */
    static Float flamingo(Player p) {
        Object comp = Reflect.getStatic("artifacts.registry.ModDataComponents", "SWIM_IN_AIR");
        Object type = comp == null ? null : Reflect.call(comp, "get");
        if (type == null || !Reflect.bool(Reflect.callStatic("artifacts.equipment.EquipmentHelper", "hasAbilityActive", type, p, false))) return null;
        Object helper = Reflect.callStatic("artifacts.platform.PlatformServices", "getPlatformHelper");
        Object data = Reflect.call(helper, "getSwimData", p);
        if (data == null) return null;
        float charge = 1 - Reflect.num(Reflect.call(data, "getSwimProgress"), 0);
        return charge >= 1 ? null : charge;
    }

    /** Artifacts: worn artifacts on cooldown, with the fraction left. */
    static List<Object[]> artifactCooldowns(Player p, float partial) {
        List<Object[]> out = new ArrayList<>();
        Class<?> wearable = Reflect.cls("artifacts.item.WearableArtifactItem");
        if (wearable == null) return out;
        java.util.function.Consumer<ItemStack> each = s -> {
            if (!s.isEmpty() && wearable.isInstance(s.getItem()) && p.getCooldowns().isOnCooldown(s.getItem())) {
                out.add(new Object[] {s, p.getCooldowns().getCooldownPercent(s.getItem(), partial)});
            }
        };
        Reflect.callStatic("artifacts.equipment.EquipmentHelper", "iterateEquipment", p, each);
        return out;
    }

    /** Immersive Armors: the steampunk chestplate is worn (its clock and compass). */
    static boolean steampunk(Player p) {
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        return BuiltInRegistries.ITEM.getKey(chest.getItem()).equals(ResourceLocation.fromNamespaceAndPath("immersive_armors", "steampunk_chestplate"));
    }

    /** Vanilla: the attack strength 0..1 while recharging, or null. Shown on the selected socket whenever the attack
     *  indicator is on (crosshair or hotbar: vanilla still draws its own at the crosshair), per frame (partial tick). */
    static Float attackCharge(Minecraft mc, Player p, float delta) {
        var mode = mc.options.attackIndicator().get();
        HudLog.changed("attackIndicator", mode, "attack indicator: {} ({})", mode, mode == AttackIndicatorStatus.OFF ? "no cooldown on the selected socket" : "cooldown shades the selected socket");
        if (mode == AttackIndicatorStatus.OFF) return null;
        float f = p.getAttackStrengthScale(delta);
        return f < 1 ? f : null;
    }

    /** AppleSkin: the held food's {nutrition, health it would regenerate (0 when not)}, or null (AppleSkin's rules). */
    static float[] appleSkin(Minecraft mc, Player p) {
        String helper = "squeek.appleskin.helpers.FoodHelper", cfg = "squeek.appleskin.ModConfig";
        if (Reflect.cls(helper) == null || !configOn(cfg, "SHOW_FOOD_VALUES_OVERLAY")) return null;
        if (p.getVehicle() instanceof LivingEntity v && v.showVehicleHealth()) return null;
        FoodProperties food = heldFood(helper, p, p.getMainHandItem());
        if (food == null && configOn(cfg, "SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND")) food = heldFood(helper, p, p.getOffhandItem());
        if (food == null) return null;
        float heal = 0;
        if (configOn(cfg, "SHOW_FOOD_HEALTH_HUD_OVERLAY") && p.level().getDifficulty() != Difficulty.PEACEFUL
                && p.getFoodData().getFoodLevel() < 18 && !p.hasEffect(MobEffects.POISON) && !p.hasEffect(MobEffects.WITHER)
                && !p.hasEffect(MobEffects.REGENERATION)) {
            heal = Reflect.num(Reflect.callStatic(helper, "getEstimatedHealthIncrement", p, food), 0);
        }
        return new float[] {food.nutrition(), heal};
    }

    private static FoodProperties heldFood(String helper, Player p, ItemStack s) {
        if (s.isEmpty()) return null;
        Object r = Reflect.callStatic(helper, "query", s, p);
        Object f = Reflect.get(r, "modifiedFoodProperties");
        return f instanceof FoodProperties fp && Reflect.bool(Reflect.callStatic(helper, "canConsume", p, fp)) ? fp : null;
    }

    private static boolean configOn(String cls, String field) {
        return Reflect.bool(Reflect.call(Reflect.getStatic(cls, field), "get"));
    }
}
