package dev.frosthaven.havenmobbars.mixin;

import dev.frosthaven.havenmobbars.MobBars;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The client learns of each hit on an entity it tracks from the server's damage event, with its cause: any player
 *  hitting another player (us or anyone near) is remembered as recently aggressive (magenta bar, {@link MobBars}). */
@Mixin(LivingEntity.class)
public abstract class RecentAttackerMixin {
    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void haven_mob_health_bars$attacker(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof Player victim && victim.level().isClientSide
                && source.getEntity() instanceof Player attacker && attacker != victim) {
            MobBars.attackedBy(attacker);
        }
    }
}
