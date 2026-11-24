package dev.frosthaven.havencreateuiux.mixin;

import dev.frosthaven.havencreateuiux.hud.CompactHud;
import dev.frosthaven.havencreateuiux.hud.HudLog;
import dev.frosthaven.havencreateuiux.hud.SlotRead;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mekanism's top-left HUD lists the held items' lines ("Mode: Wrench", the Atomic Disassembler's and Flamethrower's mode,
 * the Meka-Tool's modules). While the compact action bar is active each mode item's slot shows its mode (SlotBadges), so
 * the hand slots' mode items and the Meka-Tool are read as empty there; armor lines and Mekanism's centred mode-change
 * message stay.
 */
@Mixin(targets = "mekanism.client.render.hud.MekanismHUD", remap = false)
public abstract class MekanismHandHudMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack haven_create_ui_ux$hands(Player player, EquipmentSlot slot) {
        ItemStack s = player.getItemBySlot(slot);
        if ((slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && !s.isEmpty() && CompactHud.active() && SlotRead.hideHandHud(s)) {
            HudLog.once("mek-hand-hud", "Mekanism's HUD lines of held mode items and the Meka-Tool left out (their slots show the mode)");
            return ItemStack.EMPTY;
        }
        return s;
    }
}
