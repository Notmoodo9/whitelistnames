package dev.whitelistnames.mixin;

import dev.whitelistnames.NameSync;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Swaps real names for nicknames in the player info each player's game receives (see NameSync). */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {
	@ModifyVariable(method = {
			"send(Lnet/minecraft/network/protocol/Packet;)V",
			"send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V"
	}, at = @At("HEAD"), argsOnly = true)
	private Packet<?> whitelistnames$showNicknames(Packet<?> packet) {
		if (packet instanceof ClientboundPlayerInfoUpdatePacket info
				&& (Object) this instanceof ServerGamePacketListenerImpl game) {
			return NameSync.forViewer(info, game.player);
		}
		return packet;
	}
}
