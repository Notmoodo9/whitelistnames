package dev.whitelistnames;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhitelistNames implements ModInitializer {
	public static final String MOD_ID = "whitelistnames";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final String XAERO_FAIR_PLAY = "\u00a7f\u00a7a\u00a7i\u00a7r\u00a7x\u00a7a\u00a7e\u00a7r\u00a7o"; // §f§a§i§r§x§a§e§r§o

	@Override
	public void onInitialize() {
		NickStore.load(FabricLoader.getInstance().getConfigDir().resolve("whitelistnames.json"));
		Settings.load(FabricLoader.getInstance().getConfigDir().resolve("whitelistnames-settings.json"));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			NickCommands.register(dispatcher);
			RecipeCommands.register(dispatcher);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			NickStore.updateUsername(handler.getPlayer().getUUID(), handler.getPlayer().getName().getString());
			// Xaero's Minimap / World Map fair-play code: disables cave mode and the entity radar.
			// It's only formatting codes, so players just see an empty chat line.
			handler.getPlayer().sendSystemMessage(Component.literal(XAERO_FAIR_PLAY));
		});

		// Clean up after older versions of the mod (floating nametag entities and their team)
		ServerLifecycleEvents.SERVER_STARTED.register(NameSync::removeLegacyTeam);
		ServerEntityEvents.ENTITY_LOAD.register(NameSync::onEntityLoad);

		// CI/dev only (set in build.gradle for runServer): make sure every mixin applies on this version.
		if (Boolean.getBoolean("whitelistnames.selftest")) {
			ServerLifecycleEvents.SERVER_STARTED.register(server -> selfTest());
		}
	}

	/**
	 * Some mixin targets only load when a player connects, so a broken mixin would only show up then.
	 * Loading them here makes it fail on startup instead.
	 */
	private static void selfTest() {
		String[] targets = {
				"net.minecraft.server.network.ServerCommonPacketListenerImpl",
				"net.minecraft.server.network.ServerGamePacketListenerImpl",
				"net.minecraft.server.level.ChunkMap$TrackedEntity",
				"net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket",
				"net.minecraft.server.level.ServerPlayer",
				"net.minecraft.world.item.crafting.RecipeCache",
		};
		try {
			for (String target : targets) Class.forName(target, false, WhitelistNames.class.getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Self-test failed", e);
		}
		LOGGER.info("Self-test OK: {} mixin targets loaded", targets.length);
	}
}
