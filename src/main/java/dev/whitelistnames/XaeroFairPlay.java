package dev.whitelistnames;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Xaero's Minimap / World Map fair-play code: when a client sees it in chat, it disables cave mode
 * (including on the world map) and the entity radar. It's only formatting codes, so players just
 * see an empty chat line. Toggled with /minimapfairplay [enable|disable] (ops only).
 */
public final class XaeroFairPlay {
	private static final String CODE = "§f§a§i§r§x§a§e§r§o"; // §f§a§i§r§x§a§e§r§o

	private XaeroFairPlay() {}

	public static void onJoin(ServerPlayer player) {
		if (Settings.isXaeroFairPlay()) send(player);
	}

	private static void send(ServerPlayer player) {
		player.sendSystemMessage(Component.literal(CODE));
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("minimapfairplay")
				.requires(NickCommands.opOnly(dispatcher))
				.executes(XaeroFairPlay::status)
				.then(Commands.literal("enable").executes(ctx -> set(ctx, true)))
				.then(Commands.literal("disable").executes(ctx -> set(ctx, false))));
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		boolean on = Settings.isXaeroFairPlay();
		ctx.getSource().sendSuccess(() -> Component.literal("Minimap fair-play message is "
				+ (on ? "on (sent to players when they join)." : "off.")), false);
		return on ? 1 : 0;
	}

	private static int set(CommandContext<CommandSourceStack> ctx, boolean on) {
		Settings.setXaeroFairPlay(on);
		if (on) {
			// Apply it to everyone already online too
			for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) send(player);
			ctx.getSource().sendSuccess(() -> Component.literal(
					"Minimap fair-play message is now on, and was sent to everyone online."), true);
		} else {
			ctx.getSource().sendSuccess(() -> Component.literal("Minimap fair-play message is now off. "
					+ "Players who already got it keep fair-play until they rejoin."), true);
		}
		return 1;
	}
}
