package dev.whitelistnames.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.whitelistnames.NameTags;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla won't let entities ride anything that isn't saved to disk (like players).
 * Our nametag displays are temporary anyway, so let them through.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@ModifyExpressionValue(method = "startRiding",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"),
			require = 0)
	private boolean whitelistnames$allowNametagOnPlayer(boolean canSerialize) {
		return canSerialize || NameTags.isNametag((Entity) (Object) this);
	}
}
