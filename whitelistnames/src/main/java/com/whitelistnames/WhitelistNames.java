package com.whitelistnames;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhitelistNames implements ModInitializer {
    public static final String MOD_ID = "whitelistnames";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        NameStore.load();

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> ModCommands.register(dispatcher));

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> NameTagManager.update(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> NameTagManager.remove(handler.getPlayer()));

        ServerTickEvents.END_SERVER_TICK.register(NameTagManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(NameTagManager::removeAll);

        LOGGER.info("Whitelist Names loaded");
    }
}
