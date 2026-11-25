package dev.frosthaven.havencreateuiux.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

/** Stateful key hints for what the player rides: each mount's or vehicle's own keys (split from KeyRules). */
final class KeyRiding {
    private KeyRiding() {}

    /** An Immersive Aircraft control's key: its own mapping, or the vanilla key its FallbackKeyMapping uses while unbound. */
    private static String aircraftKey(String field) {
        Object k = Reflect.getStatic("immersive_aircraft.client.KeyBindings", field);
        if (!(k instanceof net.minecraft.client.KeyMapping m)) return "key.unknown";
        if (m.isDefault() && Reflect.get(k, "fallbackKey") instanceof java.util.function.Supplier<?> f && f.get() instanceof net.minecraft.client.KeyMapping fb) return fb.getName();
        return m.getName();
    }

    static void riding(List<KeyRules.Group> out, LocalPlayer p, Entity v) {
        List<KeyRules.Row> rows = new ArrayList<>();
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(v.getType()).toString();
        if (type.equals("rancher_companion:drekki")) {
            // Drekki Rancher's Riding: jump; Cloud Stompers jumps again in the air, Wings of Hope flies on held jump, Four
            // Winds dashes on sprint (Riding.holds(drekki, trait)); the inventory key opens its screen (DrekkiInteract).
            KeyRules.add(rows, "Jump", "key.jump");
            if (KeyRules.trait(v, "CLOUD_STOMPERS")) KeyRules.add(rows, "Jump again (in the air)", "key.jump");
            if (KeyRules.trait(v, "WINGS_OF_HOPE")) KeyRules.add(rows, "Fly (hold)", "key.jump");
            if (KeyRules.trait(v, "FOUR_WINDS")) KeyRules.add(rows, "Dash", "key.sprint");
            KeyRules.add(rows, "Drekki inventory", "key.inventory");
            KeyRules.add(rows, "Dismount", "key.sneak");
        } else if (v instanceof net.minecraft.world.entity.animal.camel.Camel) {
            KeyRules.add(rows, "Dash", "key.jump");   // vanilla: the camel's dash on the jump key
            KeyRules.add(rows, v.getType().getDescription().getString() + " inventory", "key.inventory");   // HasCustomInventoryScreen
            KeyRules.add(rows, "Dismount", "key.sneak");
        } else if (v instanceof net.minecraft.world.entity.animal.horse.AbstractHorse h) {
            if (!(v instanceof net.minecraft.world.entity.animal.horse.Llama)) KeyRules.add(rows, "Jump (hold to charge)", "key.jump");   // a llama is not steered
            // the inventory key opens the mount's own (LocalPlayer: HasCustomInventoryScreen; AbstractHorse needs it tamed)
            if (h.isTamed()) KeyRules.add(rows, v.getType().getDescription().getString() + " inventory", "key.inventory");
            KeyRules.add(rows, "Dismount", "key.sneak");
        } else if (type.startsWith("immersive_aircraft:")) {
            // its own keys (immersive_aircraft.client.KeyBindings); on NeoForge each is a FallbackKeyMapping that uses a
            // vanilla key while unbound: show the key that acts
            KeyRules.add(rows, "Up", aircraftKey("up"));
            KeyRules.add(rows, "Down", aircraftKey("down"));
            KeyRules.add(rows, "Throttle up", aircraftKey("push"));
            KeyRules.add(rows, "Throttle down", aircraftKey("pull"));
            if (Reflect.get(v, "weapons") instanceof java.util.Map<?, ?> w && !w.isEmpty()) KeyRules.add(rows, "Use weapon", aircraftKey("use"));
            KeyRules.add(rows, "Boost", "key.immersive_aircraft.boost");
            KeyRules.add(rows, "Dismount", "key.immersive_aircraft.dismount");
        } else if (Reflect.is(v, "rbasamoyai.createbigcannons.cannon_control.carriage.CannonCarriageEntity")) {
            // Create Big Cannons (CBCClientCommon.onClientGameTick): fire, and the carriage's pitch mode while held
            KeyRules.add(rows, "Fire", "key.createbigcannons.fire_controlled_cannon");
            KeyRules.add(rows, "Carriage mode (hold)", "key.createbigcannons.pitch_mode");
            KeyRules.add(rows, "Dismount", "key.sneak");
        } else if (Reflect.is(v, "rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity")) {
            KeyRules.add(rows, "Fire", "key.createbigcannons.fire_controlled_cannon");   // a cannon mount
            KeyRules.add(rows, "Dismount", "key.sneak");
        } else if (Reflect.is(v, "earth.terrarium.adastra.common.entities.vehicles.Rocket")) {
            KeyRules.add(rows, "Launch", "key.jump");   // Ad Astra: the jump key starts the launch
            KeyRules.add(rows, "Dismount", "key.sneak");
        } else if (Reflect.is(v, "earth.terrarium.adastra.common.entities.vehicles.Rover")) {
            KeyRules.add(rows, "Radio", "key.ad_astra.open_radio");
            KeyRules.add(rows, "Dismount", "key.sneak");
        }
        else if (Reflect.is(v, "com.maxenonyme.highseas.helm.HelmSeatEntity")) {
            // Create Deep Seas helm (HelmClientHandler.onClientTickPre): forward/back throttle, left/right steer, sneak lets go
            KeyRules.add(rows, "Throttle up", "key.forward");
            KeyRules.add(rows, "Throttle down", "key.back");
            KeyRules.add(rows, "Steer left", "key.left");
            KeyRules.add(rows, "Steer right", "key.right");
            KeyRules.add(rows, "Let go", "key.sneak");
        } else if (Reflect.is(v, "net.sprocketgames.create_aeronautics_throwable_rope_connector.entity.MountedRopeLauncherSeatEntity")) {
            // the mounted rope launcher (MountedRopeLauncherClientInput): attack fires, use releases the rope, sneak leaves
            KeyRules.add(rows, "Fire", "key.attack");
            KeyRules.add(rows, "Release rope", "key.use");
            KeyRules.add(rows, "Dismount", "key.sneak");
        }
        else {
            // a pig or strider steered by its stick: the stick's use boosts it (ItemSteerable via FoodOnAStickItem)
            if (v instanceof net.minecraft.world.entity.ItemSteerable && p.getMainHandItem().getItem() instanceof net.minecraft.world.item.FoodOnAStickItem<?>)
                KeyRules.add(rows, "Boost", "key.use");
            KeyRules.add(rows, "Dismount", "key.sneak");   // boats, minecarts, seats, cushions: vanilla's sneak dismount
        }
        KeyRules.group(out, v.getDisplayName().getString(), rows);
    }

    /** Moving without a vehicle: a Create Hypertube (ClientTravelPathMover holds the player's path; TubeEscapeHandler
     *  leaves on the held escape key) or a Climbable Ropes rope (ClimbDrive keys, RopeRide sneak and jump). */
    static void travel(List<KeyRules.Group> out, LocalPlayer p) {
        List<KeyRules.Row> rows = new ArrayList<>();
        if (Reflect.callStatic("com.pedrorok.hypertube.core.travel.client.ClientTravelPathMover", "getData", p.getId()) != null) {
            KeyRules.add(rows, "Exit tube (hold)", "key.hypertube.tube_escape");
            KeyRules.group(out, "Hypertube", rows);
        } else if (KeyAddons.climbing()) {
            KeyRules.add(rows, "Climb up", "key.forward");
            KeyRules.add(rows, "Climb down", "key.back");
            KeyRules.add(rows, "Faster", "key.sprint");
            KeyRules.add(rows, "Jump off", "key.jump");
            KeyRules.add(rows, "Let go", "key.sneak");
            KeyRules.group(out, "Rope", rows);
        }
    }
}
