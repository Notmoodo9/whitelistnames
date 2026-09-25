package dev.whitelistnames.mixin;

import dev.whitelistnames.NameTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Never send a player their own floating nametag, like vanilla never shows your own name. */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
	private void whitelistnames$hideOwnNametag(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
		Entity self = (Entity) (Object) this;
		if (self.getVehicle() == player && NameTags.isNametag(self)) cir.setReturnValue(false);
	}
}
