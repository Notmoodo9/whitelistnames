package dev.whitelistnames.mixin;

import dev.whitelistnames.NickStore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shows the nickname in the tab list too. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
	private void whitelistnames$tabNickname(CallbackInfoReturnable<Component> cir) {
		String nick = NickStore.getNick(((ServerPlayer) (Object) this).getUUID());
		if (nick != null) cir.setReturnValue(Component.literal(nick));
	}
}
