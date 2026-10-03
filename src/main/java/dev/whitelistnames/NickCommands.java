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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class NickCommands {
	private static final String INVALID_NAME = "Nicknames must be 1-16 characters with no spaces or quotes "
			+ "(like a Minecraft username), and can't start with @.";

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

		// /namecheck <nickname> - who is this really? (ops only)
		dispatcher.register(Commands.literal("namecheck")
				.requires(opOnly(dispatcher))
				.then(Commands.argument("name", StringArgumentType.string())
						.suggests(NickCommands::suggestNames)
						.executes(NickCommands::nameCheck)));

		// /nicks - everyone's nickname (ops only)
		dispatcher.register(Commands.literal("nicks")
				.requires(opOnly(dispatcher))
				.executes(NickCommands::listNicks));
	}

	/** Ops only: same permission as vanilla /op (or /gamemode in singleplayer/LAN, where /op doesn't exist). */
	public static Predicate<CommandSourceStack> opOnly(CommandDispatcher<CommandSourceStack> dispatcher) {
		CommandNode<CommandSourceStack> node = dispatcher.getRoot().getChild("op");
		if (node == null) node = dispatcher.getRoot().getChild("gamemode");
		return node != null ? node.getRequirement() : s -> false;
	}

	private static int nameCheck(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		String query = StringArgumentType.getString(ctx, "name");
		UUID byNick = NickStore.findByNick(query);
		if (byNick != null) {
			String username = NickStore.getUsername(byNick);
			String nick = NickStore.getNick(byNick);
			source.sendSuccess(() -> Component.literal(nick + " is " + username), false);
			return 1;
		}
		var byUsername = NickStore.find(query);
		if (byUsername != null) {
			NickStore.Entry e = byUsername.getValue();
			source.sendSuccess(() -> Component.literal(e.username() + " goes by " + e.nickname()), false);
			return 1;
		}
		source.sendFailure(Component.literal("Nobody has the nickname or username \"" + query + "\"."));
		return 0;
	}

	private static int listNicks(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		MinecraftServer server = source.getServer();
		List<Map.Entry<UUID, NickStore.Entry>> all = new ArrayList<>();
		for (var e : NickStore.entries()) all.add(e);
		if (all.isEmpty()) {
			source.sendSuccess(() -> Component.literal("Nobody has a nickname yet."), false);
			return 0;
		}
		all.sort(Comparator.comparing(e -> e.getValue().nickname().toLowerCase(Locale.ROOT)));

		MutableComponent list = Component.literal(all.size() + " nickname(s):");
		for (var e : all) {
			boolean online = server.getPlayerList().getPlayer(e.getKey()) != null;
			String nick = e.getValue().nickname();
			list.append(Component.literal("\n" + nick + " = " + e.getValue().username()
					+ (online ? " (online)" : "")
					+ (NameSync.isValidNick(nick) ? "" : " [not shown above head, rename to fix]")));
		}
		source.sendSuccess(() -> list, false);
		return all.size();
	}

	private static int whitelistAddWithName(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		MinecraftServer server = source.getServer();
		String nick = clean(StringArgumentType.getString(ctx, "name"));
		if (nick == null) {
			source.sendFailure(Component.literal(INVALID_NAME));
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
			if (online != null) NameSync.refresh(server, online);
		server.invalidateStatus();

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
			source.sendFailure(Component.literal(INVALID_NAME));
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
		if (online != null) NameSync.refresh(server, online);
		server.invalidateStatus();

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

	/** Trims, strips formatting/control characters and surrounding quotes. Returns null if not a valid nickname. */
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
		return NameSync.isValidNick(s) ? s : null;
	}
}
