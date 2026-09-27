package dev.whitelistnames;

import com.mojang.authlib.GameProfile;
import dev.whitelistnames.mixin.ChunkMapAccessor;
import dev.whitelistnames.mixin.PlayerInfoPacketAccessor;
import dev.whitelistnames.mixin.TrackedEntityAccessor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows nicknames as real names: other players' games are told the nickname as the player's account
 * name, so the vanilla nametag above their head shows it. The player themselves still sees their own
 * real name, and the server keeps using real usernames.
 */
public final class NameSync {
	/** Tag used by older versions of the mod on their floating nametag entities. */
	private static final String LEGACY_TAG = "wn_nametag";
	private static final String LEGACY_TEAM = "wn_nicknamed";

	private NameSync() {}

	/**
	 * Nicknames must be valid Minecraft account names (1-16 characters, no spaces), otherwise clients
	 * could reject them. Also no leading @ (selectors) and no quotes or backslashes.
	 */
	public static boolean isValidNick(String nick) {
		return !nick.isEmpty()
				&& StringUtil.isValidPlayerName(nick)
				&& !nick.startsWith("@")
				&& nick.indexOf('"') < 0 && nick.indexOf('\'') < 0 && nick.indexOf('\\') < 0;
	}

	/** The nickname a viewer should see for this player, or null to show the real name. */
	private static String shownNick(java.util.UUID subject, ServerPlayer viewer) {
		if (viewer != null && subject.equals(viewer.getUUID())) return null; // you always see your own real name
		String nick = NickStore.getNick(subject);
		return nick != null && isValidNick(nick) ? nick : null;
	}

	/** Rewrites a player info packet for one viewer so nicknamed players' names are their nicknames. */
	public static ClientboundPlayerInfoUpdatePacket forViewer(ClientboundPlayerInfoUpdatePacket packet, ServerPlayer viewer) {
		if (!packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) return packet;

		List<ClientboundPlayerInfoUpdatePacket.Entry> entries = new ArrayList<>(packet.entries().size());
		boolean changed = false;
		for (ClientboundPlayerInfoUpdatePacket.Entry e : packet.entries()) {
			String nick = e.profile() == null ? null : shownNick(e.profileId(), viewer);
			if (nick != null && !nick.equals(e.profile().name())) {
				GameProfile real = e.profile();
				e = new ClientboundPlayerInfoUpdatePacket.Entry(e.profileId(),
						new GameProfile(real.id(), nick, real.properties()), // keep properties: the skin
						e.listed(), e.latency(), e.gameMode(), e.displayName(), e.showHat(), e.listOrder(),
						e.chatSession());
				changed = true;
			}
			entries.add(e);
		}
		if (!changed) return packet;

		ClientboundPlayerInfoUpdatePacket copy = new ClientboundPlayerInfoUpdatePacket(packet.actions(), List.<ServerPlayer>of());
		((PlayerInfoPacketAccessor) copy).whitelistnames$setEntries(entries);
		return copy;
	}

	/**
	 * Call after a player's nickname changes. Clients only read a player's name when they first learn
	 * about them, so re-introduce the player to everyone else: hide them, resend their info, show them.
	 */
	public static void refresh(MinecraftServer server, ServerPlayer player) {
		// Tab list and chat names
		server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player));

		ServerEntity serverEntity = null;
		List<ServerPlayer> watching = new ArrayList<>();
		Object tracked = ((ChunkMapAccessor) player.level().getChunkSource().chunkMap)
				.whitelistnames$entityMap().get(player.getId());
		if (tracked != null) {
			serverEntity = ((TrackedEntityAccessor) tracked).whitelistnames$serverEntity();
			for (ServerPlayerConnection connection : ((TrackedEntityAccessor) tracked).whitelistnames$seenBy()) {
				watching.add(connection.getPlayer());
			}
		}

		if (serverEntity != null) {
			for (ServerPlayer viewer : watching) serverEntity.removePairing(viewer);
		}
		ClientboundPlayerInfoRemovePacket remove = new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID()));
		for (ServerPlayer other : server.getPlayerList().getPlayers()) {
			if (other == player) continue;
			other.connection.send(remove);
			// Goes through forViewer, so it carries the new nickname
			other.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
		}
		if (serverEntity != null) {
			for (ServerPlayer viewer : watching) serverEntity.addPairing(viewer);
		}
	}

	/** Removes the scoreboard team older versions of the mod used, so vanilla nametags show again. */
	public static void removeLegacyTeam(MinecraftServer server) {
		server.getCommands().performPrefixedCommand(
				server.createCommandSourceStack().withSuppressedOutput(), "team remove " + LEGACY_TEAM);
	}

	/** Removes floating nametag entities left in the world by older versions of the mod. */
	public static void onEntityLoad(Entity entity, ServerLevel level) {
		if (entity.entityTags().contains(LEGACY_TAG)) level.getServer().execute(entity::discard);
	}
}
