package dev.whitelistnames.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.whitelistnames.NameSync;
import dev.whitelistnames.NickStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shows nicknames in the player list you see when hovering the player count in the server list. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@ModifyExpressionValue(method = "buildPlayerStatus", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerPlayer;nameAndId()Lnet/minecraft/server/players/NameAndId;"))
	private NameAndId whitelistnames$statusNickname(NameAndId original) {
		String nick = NickStore.getNick(original.id());
		return nick != null && NameSync.isValidNick(nick) ? new NameAndId(original.id(), nick) : original;
	}
}
