package dev.frosthaven.havencreateuiux.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** LivingEntity's protected attackStrengthTicker, for the bar's attack cooldown (HudExtras.attackCharge). */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("attackStrengthTicker")
    int haven_create_ui_ux$attackStrengthTicker();
}
