package dev.whitelistnames.mixin;

import dev.whitelistnames.NickStore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Display name is what chat, join/leave and death messages use. */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
	private void whitelistnames$useNickname(CallbackInfoReturnable<Component> cir) {
		if ((Object) this instanceof ServerPlayer player) {
			String nick = NickStore.getNick(player.getUUID());
			if (nick != null) cir.setReturnValue(Component.literal(nick));
		}
	}
}
