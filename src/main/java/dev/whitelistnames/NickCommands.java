package dev.whitelistnames;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class NickCommands {
	public static final int MAX_LENGTH = 32;
	/** Longest name vanilla player arguments (/tp, /msg, ...) accept. */
	public static final int SELECTOR_MAX_LENGTH = 16;

	private NickCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		CommandNode<CommandSourceStack> vanillaWhitelist = dispatcher.getRoot().getChild("whitelist");

		// /whitelist add <player> <name>
		// Merges into vanilla's /whitelist, so it keeps vanilla's permission check.
		// (Only exists on dedicated servers, same as vanilla /whitelist.)
		if (vanillaWhitelist != null) {
			dispatcher.register(Commands.literal("whitelist")
					.then(Commands.literal("add")
							.then(Commands.argument("targets", GameProfileArgument.gameProfile())
									.then(Commands.argument("name", StringArgumentType.greedyString())
											.executes(NickCommands::whitelistAddWithName)))));
		}

		// /changename <username or current name> <new name>
		// Same permission as /whitelist (or /gamemode in singleplayer/LAN).
		CommandNode<CommandSourceStack> permSource = vanillaWhitelist != null
				? vanillaWhitelist : dispatcher.getRoot().getChild("gamemode");
		Predicate<CommandSourceStack> permission = permSource != null ? permSource.getRequirement() : s -> false;

		dispatcher.register(Commands.literal("changename")
				.requires(permission)
				.then(Commands.argument("player", StringArgumentType.string())
						.suggests(NickCommands::suggestNames)
						.then(Commands.argument("newname", StringArgumentType.greedyString())
								.executes(NickCommands::changeName))));
	}

	private static int whitelistAddWithName(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		MinecraftServer server = source.getServer();
		String nick = clean(StringArgumentType.getString(ctx, "name"));
		if (nick == null) {
			source.sendFailure(Component.literal("That name is empty or longer than " + MAX_LENGTH + " characters."));
			return 0;
		}

		int count = 0;
		for (var profile : GameProfileArgument.getGameProfiles(ctx, "targets")) {
			String username = profile.name();
			UUID id = profile.id();
			if (isTaken(server, nick, id)) {
				source.sendFailure(Component.literal("\"" + nick + "\" is already someone else's name or username."));
				continue;
			}

			// Let vanilla do the actual whitelisting (and print its usual message)
			server.getCommands().performPrefixedCommand(source, "whitelist add " + username);

			NickStore.set(id, username, nick);
			ServerPlayer online = server.getPlayerList().getPlayer(id);
			if (online != null) NameTags.refresh(server, online);

			source.sendSuccess(() -> Component.literal(username + " will now go by \"" + nick + "\""), true);
			count++;
		}
		return count;
	}

	private static int changeName(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		MinecraftServer server = source.getServer();
		String query = StringArgumentType.getString(ctx, "player");
		String nick = clean(StringArgumentType.getString(ctx, "newname"));
		if (nick == null) {
			source.sendFailure(Component.literal("That name is empty or longer than " + MAX_LENGTH + " characters."));
			return 0;
		}

		UUID id;
		String username;
		var found = NickStore.find(query);
		if (found != null) {
			id = found.getKey();
			username = found.getValue().username();
		} else {
			ServerPlayer player = server.getPlayerList().getPlayerByName(query);
			if (player == null) {
				source.sendFailure(Component.literal("No player with username or name \"" + query + "\"."));
				return 0;
			}
			id = player.getUUID();
			username = player.getName().getString();
		}

		if (isTaken(server, nick, id)) {
			source.sendFailure(Component.literal("\"" + nick + "\" is already someone else's name or username."));
			return 0;
		}

		String old = NickStore.getNick(id);
		NickStore.set(id, username, nick);
		ServerPlayer online = server.getPlayerList().getPlayer(id);
		if (online != null) NameTags.refresh(server, online);

		String from = old != null ? old : username;
		source.sendSuccess(() -> Component.literal("Renamed " + from + " (" + username + ") to \"" + nick + "\""), true);
		return 1;
	}

	private static CompletableFuture<Suggestions> suggestNames(CommandContext<CommandSourceStack> ctx,
															   SuggestionsBuilder builder) {
		List<String> options = new ArrayList<>();
		for (NickStore.Entry e : NickStore.all()) {
			options.add(quoteIfNeeded(e.username()));
			options.add(quoteIfNeeded(e.nickname()));
		}
		for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
			String name = p.getName().getString();
			if (!options.contains(name)) options.add(name);
		}
		return SharedSuggestionProvider.suggest(options, builder);
	}

	/**
	 * True if another player already uses this as a nickname or username, which would make
	 * "username or nickname" lookups ambiguous.
	 */
	private static boolean isTaken(MinecraftServer server, String nick, UUID self) {
		for (var e : NickStore.entries()) {
			if (e.getKey().equals(self)) continue;
			if (e.getValue().nickname().equalsIgnoreCase(nick) || e.getValue().username().equalsIgnoreCase(nick)) {
				return true;
			}
		}
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (!p.getUUID().equals(self) && p.getName().getString().equalsIgnoreCase(nick)) return true;
		}
		return false;
	}

	public static String quoteIfNeeded(String s) {
		for (char c : s.toCharArray()) {
			if (!StringReader.isAllowedInUnquotedString(c)) {
				return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
			}
		}
		return s;
	}

	/** Trims, strips formatting/control characters and surrounding quotes, enforces max length. Returns null if invalid. */
	private static String clean(String raw) {
		StringBuilder sb = new StringBuilder();
		for (char c : raw.toCharArray()) {
			if (c != '§' && !Character.isISOControl(c)) sb.append(c);
		}
		String s = sb.toString().strip();
		// The new name takes the rest of the line, so "Mr Notch" would otherwise keep its quotes.
		if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
			s = s.substring(1, s.length() - 1).strip();
		}
		return (s.isEmpty() || s.length() > MAX_LENGTH) ? null : s;
	}
}
