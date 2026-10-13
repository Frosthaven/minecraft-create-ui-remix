package dev.frosthaven.havencreateuiux.hud;

import dev.frosthaven.havencreateuiux.UiConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

/** One frame's state of everything the bar shows, read once and shared by the orb and bar layouts. */
final class HudFrame {
    private static final ResourceLocation COMFORT = ResourceLocation.fromNamespaceAndPath("farmersdelight", "comfort");
    private static final ResourceLocation NOURISHMENT = ResourceLocation.fromNamespaceAndPath("farmersdelight", "nourishment");
    private static final Smooth AIR = new Smooth(), TANK = new Smooth(), EXHAUSTION = new Smooth();

    /** A mana pool: its atlas name, value and maximum. */
    record Pool(String name, float cur, float max) {
        double frac() { return cur / Math.max(1, max); }
    }

    final Minecraft mc;
    final LocalPlayer p;
    final long now;
    final boolean orbs, hurtable, lefty, nums, underwater, showArmor, showBreath, hardcore, regen, comfort, nourished, hunger;
    final float hp, maxHp, absorb, saturation, exhaustion;
    final int armor, food;
    final long hurtSince;
    final double exhaustShown;   // 0..1 (exhaustion / 4), eased between ticks and back to 0 when a point is taken
    final String healthLiquid;
    final float[] tank, apple, irons, ars, roll;
    final double airShown, tankShown;
    final LivingEntity mount;
    final float mountHp, mountMax;
    final Float flamingo, hull;
    final List<GearStatus.Gear> gear;   // worn gear's statuses (cyan, in the effects rail): MekaSuit energy, fuel, oxygen
    final boolean primaryArs;
    final double side;   // the side bars' size (config)
    final Ends ends;

    HudFrame(Minecraft mc, LocalPlayer p, float partial, boolean numbersKey) {
        this.mc = mc;
        this.p = p;
        now = Util.getMillis();
        orbs = UiConfig.METER_DISPLAY.get() == UiConfig.MeterDisplay.ORBS;
        primaryArs = UiConfig.PRIMARY_MANA.get() == UiConfig.PrimaryMana.ARS;
        side = UiConfig.SIDE_SCALE.get();
        hurtable = mc.gameMode != null && mc.gameMode.canHurtPlayer();
        lefty = p.getMainArm() == HumanoidArm.LEFT;
        nums = numbersKey || mc.screen != null;
        underwater = p.isEyeInFluid(FluidTags.WATER);
        tank = HudStats.backtank(p, underwater);
        int air = p.getAirSupply(), maxAir = p.getMaxAirSupply();
        armor = p.getArmorValue();
        showArmor = hurtable && armor > 0;
        showBreath = hurtable && (underwater || air < maxAir || tank != null);
        // Vanilla air steps each tick, Create's backtank about once a second: both ease between updates.
        airShown = AIR.get(air / (double) maxAir, now, 50);
        tankShown = tank == null ? 0 : TANK.get(tank[0], now, 1000);
        hardcore = mc.level != null && mc.level.getLevelData().isHardcore();
        hp = p.getHealth();
        maxHp = p.getMaxHealth();
        absorb = p.getAbsorptionAmount();
        healthLiquid = p.hasEffect(MobEffects.POISON) ? "poison" : p.hasEffect(MobEffects.WITHER) ? "wither" : p.isFullyFrozen() ? "frozen" : "blood";
        hurtSince = p.hurtTime > 0 ? (long) ((p.hurtDuration - p.hurtTime + partial) * 50) : Long.MAX_VALUE / 2;
        regen = p.hasEffect(MobEffects.REGENERATION);
        comfort = p.getActiveEffects().stream().anyMatch(e -> e.getEffect().is(COMFORT));
        nourished = p.getActiveEffects().stream().anyMatch(e -> e.getEffect().is(NOURISHMENT));
        hunger = p.hasEffect(MobEffects.HUNGER);
        var fd = p.getFoodData();
        food = fd.getFoodLevel();
        saturation = fd.getSaturationLevel();
        exhaustion = fd.getExhaustionLevel();
        exhaustShown = EXHAUSTION.get(Math.min(1, exhaustion / 4.0), now, 50, 400);
        apple = hurtable ? HudExtras.appleSkin(mc, p) : null;
        mount = p.getVehicle() instanceof LivingEntity m && m.showVehicleHealth() ? m : null;
        mountHp = mount == null ? 0 : mount.getHealth();
        mountMax = mount == null ? 1 : mount.getMaxHealth();
        irons = HudStats.irons(p);
        ars = HudStats.ars(p);
        roll = HudStats.roll(p);
        gear = hurtable ? GearStatus.read(p) : List.of();
        flamingo = hurtable ? HudExtras.flamingo(p) : null;
        hull = HudExtras.aircraftHealth(p);
        ends = new Ends(orbs, lefty);
    }

    /** The mana pools present, the primary (the setting) first. */
    List<Pool> pools() {
        List<Pool> l = new ArrayList<>();
        if (irons != null) l.add(new Pool("irons", irons[0], irons[1]));
        if (ars != null) l.add(new Pool("ars", ars[0], ars[1]));
        if (primaryArs && l.size() == 2) l.add(l.remove(0));
        return l;
    }

    static String mmss(float secs) {
        int s = (int) Math.ceil(secs);
        return s / 60 + ":" + String.format("%02d", s % 60);
    }

    /** Where each wing ends (the page's ccEnds): out from the orb's housing (or the meters) a gap, the end item (the
     *  offhand slot, 18 wide, or the dash dial), then plate. Left-handed mirrors which end holds which. Units: x0, x1
     *  (the wings' outer edges), offhand (the slot's left x), dash (the dial's centre x). */
    static final class Ends {
        final double x0, x1, offhand, dash;

        Ends(boolean orbs, boolean lefty) {
            // Orbs: the dial's width is its housing's reach at the plate's edge; bars: the dial alone, in the tall wing.
            double gap = orbs ? 1.5 : 4, end = orbs ? 3 : 4, inner = orbs ? OrbHud.RX + OrbHud.HOUSING * OrbHud.S : BarHud.OUTER, slotW = 18;
            double dialW = orbs ? 2 * domeHalf(dialHousing(), DIAL_Y) : 2 * (DIAL_R + DIAL_B) * DIAL_S;
            double left = inner + gap, right = inner + gap;
            double leftW = lefty ? dialW : slotW, rightW = lefty ? slotW : dialW;
            double span = Math.max(left + leftW, right + rightW);
            // Each end item centred in its wing's end space (the wider wing's), so neither end leaves dead plate.
            double leftItem = -(left + span) / 2, rightItem = (right + span) / 2;
            x0 = -span - end;
            x1 = span + end;
            offhand = (lefty ? rightItem : leftItem) - slotW / 2;
            dash = lefty ? leftItem : rightItem;
        }
    }

    /** The dash dial (the page's CC_DASH, CC_DIAL): radius and rim at its own size, drawn DIAL_S x; orbs: in a housing
     *  (radius dialHousing()) centred at DIAL_Y; bars: centred on the meter block. */
    static final double DIAL_R = 6.2, DIAL_B = 1.1, DIAL_S = 2, DIAL_Y = 30.6;
    static double dialHousing() { return (DIAL_R + DIAL_B + 1.4) * DIAL_S; }
    /** How far a housing's outline reaches each side at the plate's top edge (its fillets' ends). */
    static double domeHalf(double r, double cy) { return Math.sqrt((r + 3) * (r + 3) - (28 - 3 - cy) * (28 - 3 - cy)); }
}
