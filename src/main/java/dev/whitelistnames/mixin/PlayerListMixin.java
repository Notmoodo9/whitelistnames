package dev.whitelistnames.mixin;

import dev.whitelistnames.NickStore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Lets player arguments in commands (/tp, /msg, /give, ...) use a nickname as well as the username.
 * Real usernames always win; the nickname is only checked when no player has that username.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
	@Shadow
	public abstract ServerPlayer getPlayer(UUID id);

	@Inject(method = "getPlayerByName", at = @At("RETURN"), cancellable = true)
	private void whitelistnames$findByNickname(String name, CallbackInfoReturnable<ServerPlayer> cir) {
		if (cir.getReturnValue() != null) return;
		UUID id = NickStore.findByNick(name);
		if (id != null) {
			ServerPlayer player = getPlayer(id);
			if (player != null) cir.setReturnValue(player);
		}
	}
}
