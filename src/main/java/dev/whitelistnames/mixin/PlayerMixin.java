package dev.whitelistnames.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.whitelistnames.NickStore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Display name is what chat, join/leave and death messages use. Only the name inside it is swapped,
 * so vanilla's hover (shows the real username) and click-to-message still work.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@ModifyExpressionValue(method = "getDisplayName",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getName()Lnet/minecraft/network/chat/Component;"))
	private Component whitelistnames$useNickname(Component name) {
		if ((Object) this instanceof ServerPlayer player) {
			String nick = NickStore.getNick(player.getUUID());
			if (nick != null) return Component.literal(nick);
		}
		return name;
	}
}
