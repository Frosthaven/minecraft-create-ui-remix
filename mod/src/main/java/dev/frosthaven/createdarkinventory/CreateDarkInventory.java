package dev.frosthaven.createdarkinventory;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/** Client only. All the work is in {@link dev.frosthaven.createdarkinventory.mixin.AllGuiTexturesMixin}. */
@Mod(value = CreateDarkInventory.ID, dist = Dist.CLIENT)
public final class CreateDarkInventory {
    public static final String ID = "createdarkinventory";
}
