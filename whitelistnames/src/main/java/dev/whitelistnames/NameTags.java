package dev.whitelistnames;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Nametags: players with a nickname join a team that hides the vanilla nametag,
 * and get a text display entity riding on their head showing the nickname.
 */
public final class NameTags {
	public static final String TEAM = "wn_nicknamed";
	public static final String COMMON_TAG = "wn_nametag";
	private static int ticks;

	private NameTags() {}

	private static String tagFor(UUID id) {
		return "wn_" + id;
	}

	private static void run(MinecraftServer server, String command) {
		server.getCommands().performPrefixedCommand(
				server.createCommandSourceStack().withSuppressedOutput(), command);
	}

	public static void setupTeam(MinecraftServer server) {
		run(server, "team add " + TEAM);
		run(server, "team modify " + TEAM + " nametagVisibility never");
	}

	public static void onJoin(MinecraftServer server, ServerPlayer player) {
		NickStore.updateUsername(player.getUUID(), player.getName().getString());
		if (NickStore.getNick(player.getUUID()) != null) {
			run(server, "team join " + TEAM + " " + player.getName().getString());
		}
	}

	/** Call after a nickname is set or changed for an online player. */
	public static void refresh(MinecraftServer server, ServerPlayer player) {
		run(server, "team join " + TEAM + " " + player.getName().getString());
		server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player));
		String nick = NickStore.getNick(player.getUUID());
		if (nick != null && shouldShow(player)) {
			spawnTag(server, player, nick);
		}
	}

	public static void tick(MinecraftServer server) {
		ticks++;
		if (ticks % 20 != 0) return; // once per second

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			String nick = NickStore.getNick(player.getUUID());
			boolean want = nick != null && shouldShow(player);
			boolean has = hasTag(player);
			if (want && !has) {
				spawnTag(server, player, nick);
			} else if (!want && has) {
				removeTag(server, player.getUUID());
			}
		}

		if (ticks % 100 == 0) cleanupOrphans(server);
	}

	private static boolean shouldShow(ServerPlayer player) {
		return player.isAlive() && !player.isSpectator() && !player.isInvisible();
	}

	private static boolean hasTag(ServerPlayer player) {
		String tag = tagFor(player.getUUID());
		for (Entity passenger : player.getPassengers()) {
			if (passenger.getTags().contains(tag)) return true;
		}
		return false;
	}

	private static void spawnTag(MinecraftServer server, ServerPlayer player, String nick) {
		String uuid = player.getUUID().toString();
		String tag = tagFor(player.getUUID());
		removeTag(server, player.getUUID());
		run(server, "execute as " + uuid + " at @s run summon minecraft:text_display ~ ~ ~ {"
				+ "text:\"" + escape(nick) + "\","
				+ "billboard:\"center\","
				+ "see_through:1b,"
				+ "Tags:[\"" + COMMON_TAG + "\",\"" + tag + "\"],"
				+ "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
				+ "translation:[0f,0.35f,0f],scale:[1f,1f,1f]}}");

		// Vanilla /ride refuses to mount anything on a player, so mount it directly.
		List<Entity> summoned = player.level().getEntities((Entity) null, player.getBoundingBox().inflate(2),
				e -> !e.isRemoved() && e.getVehicle() == null && e.getTags().contains(tag));
		for (Entity display : summoned) {
			if (!display.startRiding(player)) {
				WhitelistNames.LOGGER.warn("Could not attach nametag to {}", player.getName().getString());
				display.discard();
			}
		}
	}

	public static void removeTag(MinecraftServer server, UUID id) {
		run(server, "kill @e[type=minecraft:text_display,tag=" + tagFor(id) + "]");
	}

	/** Removes nametag displays that got left behind (not riding anyone). */
	private static void cleanupOrphans(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			List<Entity> doomed = new ArrayList<>();
			for (Entity e : level.getAllEntities()) {
				if (e.getTags().contains(COMMON_TAG) && e.getVehicle() == null) doomed.add(e);
			}
			doomed.forEach(Entity::discard);
		}
	}

	private static String escape(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
