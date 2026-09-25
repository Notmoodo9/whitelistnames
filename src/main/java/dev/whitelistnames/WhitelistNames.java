package dev.whitelistnames;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhitelistNames implements ModInitializer {
	public static final String MOD_ID = "whitelistnames";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		NickStore.load(FabricLoader.getInstance().getConfigDir().resolve("whitelistnames.json"));
		Settings.load(FabricLoader.getInstance().getConfigDir().resolve("whitelistnames-settings.json"));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			NickCommands.register(dispatcher);
			RecipeCommands.register(dispatcher);
		});

		ServerLifecycleEvents.SERVER_STARTED.register(NameTags::setupTeam);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				NameTags.onJoin(server, handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				NameTags.onDisconnect(server, handler.getPlayer()));
		ServerLifecycleEvents.SERVER_STOPPING.register(NameTags::removeAll);
		ServerTickEvents.END_SERVER_TICK.register(NameTags::tick);
	}
}
